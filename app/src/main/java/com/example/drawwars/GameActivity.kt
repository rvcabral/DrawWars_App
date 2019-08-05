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
import android.content.Context
import android.os.Handler
import com.example.drawwars.services.ServerService
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import android.widget.Button
import com.example.drawwars.services.ServiceListener
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.example.drawwars.utils.ThemeTimeoutWrapper
import java.io.ByteArrayOutputStream
import java.lang.Exception


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

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)
    }

    override fun onBackPressed() {
        //Do nothing here.
    }

    //endregion

}
