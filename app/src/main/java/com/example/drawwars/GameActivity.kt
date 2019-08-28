package com.example.drawwars

import android.arch.lifecycle.Observer
import android.graphics.Canvas
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.drawwars.costumview.DWCanvas
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.android.synthetic.main.activity_game.*
import java.time.Duration
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Handler
import com.example.drawwars.services.ServerService
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.support.v7.app.AlertDialog
import android.util.Base64
import android.widget.Button
import com.example.drawwars.services.ServiceListener
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.example.drawwars.utils.ThemeTimeoutWrapper
import io.reactivex.functions.Consumer
import java.io.ByteArrayOutputStream
import java.lang.Exception
import kotlin.system.exitProcess


class GameActivity : AppCompatActivity(), ServiceListener {


    private var service: ServerService? = null
    private var mViewModel: ServiceViewModel? = null
    private var canvas :DWCanvas?=null
    private var theme :String="";
    private var canvasVM : DWCanvasViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        if(intent.extras!=null && intent.extras["EndOfRound"]!=null && intent.extras["EndOfRound"] as Boolean){
            ReadyButton.visibility=Button.INVISIBLE
        }

        canvasVM = ViewModelProviders.of(this).get(DWCanvasViewModel::class.java)
        canvasVM?.getCanvas()?.observe(this,
            Observer<DWCanvas> { dwcanvas ->
                canvas = dwcanvas
                canvasLayout.visibility = FrameLayout.INVISIBLE
                canvasLayout.addView(canvas)
                canvasLayout.isEnabled = false
            })
        canvasVM?.init(this)

        captionTextView.text = getString(R.string.PleaseWaitMessage)
        submitButton.visibility=Button.INVISIBLE
        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java)
        mViewModel?.getBinder()?.observe(this,
            Observer<ServerService.MyBinder> { binder ->
                service = binder?.getService()
                service?.listen(this@GameActivity)
            })
        ReadyButton.setOnClickListener { service!!.Ready() }
        submitButton.setOnClickListener {

            service!!.SetArt(getDrawFromView()!!, theme)
            runOnUiThread {
                submitButton.isEnabled = false
            }
        }

    }


    override fun Interaction(action: String, param: Any?) {

        when (action){
            getString(R.string.Action_DrawThemes)->{
                runOnUiThread{
                    val receivedTheme = (param as ArrayList<String>)[0]
                    theme = receivedTheme
                    //themes[0]
                    captionTextView.text = receivedTheme
                    //themes[0]


                    ReadyButton.visibility=Button.INVISIBLE
                    submitButton.visibility=Button.VISIBLE

                    canvasLayout.isEnabled = true
                    canvasLayout.visibility = FrameLayout.VISIBLE
                }
            }
            getString(R.string.Action_DrawSubmitted)->{
                runOnUiThread{
                    startActivity(Intent(this@GameActivity, GameCycleActivity::class.java))
                    service!!.mute(this);
                    finish()
                }
            }
            getString(R.string.Action_TimesUp)->{
                service!!.SetArt(getDrawFromView()!!, theme)
            }
            getString(R.string.Action_ServerDied)->{
                runOnUiThread {
                    val dialog: AlertDialog.Builder = AlertDialog.Builder(this@GameActivity)
                    dialog.setMessage("Server is down")
                        .setPositiveButton("Ok") { _, _ ->
                            service?.resetGameData()
                            this@GameActivity.finishAndRemoveTask()
                            exitProcess(0);
                        }
                        .show()
                }
            }
        }
    }


    private fun getDrawFromView():String?{
        return canvas?.getDraw()
    }


     //region  Standard Functions



    override fun onResume() {
        super.onResume()
        bindService()
        service?.listen(this@GameActivity)
    }

    override fun onDestroy() {
        service?.mute(this)
        try {
            unbindService(mViewModel!!.getServiceConnection())
        } catch (e:Exception){
            Log.d("Game Activity", "Couldn't unbind");
        }

        //canvasVM?.getCanvas().removeObserver()
        super.onDestroy()
    }

    override fun onPause() {
        service?.mute(this)
        unbindService(mViewModel!!.getServiceConnection())
        super.onPause()
    }

    override fun onStop() {
        unregisterReceiver(receiver)
        super.onStop()
    }

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)
    }

    override fun onBackPressed() {
        //Do nothing here.
    }

    //endregion

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(receiver, intentFilter)
    }
    private var receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var connectionInfo = (intent?.extras?.get(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo)
            if (connectionInfo != null) {
                if (connectionInfo.isConnected) {
                    if (service != null) {
                        if (service!!.regainedConnection()) {
                            runOnUiThread {
                                ReadyButton.isEnabled = true
                                submitButton.isEnabled = true
                            }
                            service!!.InteractionsWereLost(Consumer { Yes ->
                                if (Yes) {
                                    runOnUiThread {
                                        val dialog: AlertDialog.Builder = AlertDialog.Builder(this@GameActivity)
                                        dialog.setMessage("Disconnected because of connection issues")
                                            .setPositiveButton("Ok") { _, _ ->
                                                service?.resetGameData()
                                                this@GameActivity.startActivity(
                                                    Intent(
                                                        this@GameActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                this@GameActivity.finish()
                                            }
                                            .show()
                                    }
                                } else
                                    service!!.ConnectionIdMightHaveChanged()
                            })
                        }
                    }
                }
                if (!connectionInfo.isConnected) {
                    if(service!=null && service!!.lostConnection())
                        runOnUiThread {
                            Toast.makeText(this@GameActivity, "You lost internet connection", Toast.LENGTH_LONG).show()
                            ReadyButton.isEnabled = false
                            submitButton.isEnabled = false
                        }
                }
            }
        }
    }
}
