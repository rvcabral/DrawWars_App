package com.example.drawwars

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast
import com.example.drawwars.services.ServerService
import com.example.drawwars.services.ServiceListener
import io.reactivex.functions.Consumer

import kotlinx.android.synthetic.main.content_setup.CodeInput
import kotlinx.android.synthetic.main.content_setup.SubmitButton

class SetupActivity : AppCompatActivity(), ServiceListener {
    override fun Interaction(action: String, param: Any?) {
        when (action){
            getString(R.string.Action_AckNickName)-> startActivity(Intent(this, GameActivity::class.java ))
        }
    }


    private var mViewModel: ServiceViewModel? = null
    private var service: ServerService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java)


        mViewModel?.getBinder()?.observe(this, object: Observer<ServerService.MyBinder> {
            override fun onChanged(binder: ServerService.MyBinder?) {
                service = binder?.getService()
                service?.listen(this@SetupActivity)
            }

        })

        SubmitButton.setOnClickListener {
            if(CodeInput.text.isEmpty()) Toast.makeText(this@SetupActivity, getString(R.string.SetNickMessage), Toast.LENGTH_LONG);
            else service?.sendNickName(CodeInput.text.toString())
        }

    }


    override fun onResume() {
        super.onResume()
        bindService()
        service?.listen(this@SetupActivity)
    }

    override fun onDestroy() {
        service?.mute(this)
        unbindService(mViewModel!!.getServiceConnection())
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
    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(receiver, intentFilter)
    }
    private var receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var connectionInfo = (intent?.extras?.get(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo)
            if(connectionInfo!= null){
                if(connectionInfo.isConnected){
                    if(service != null){
                        if(service!!.regainedConnection()){
                            runOnUiThread {
                                SubmitButton.isEnabled = true
                            }
                            service!!.interactionsWereLost(Consumer { Yes ->
                                if(Yes) {
                                    runOnUiThread {
                                        val dialog: AlertDialog.Builder = AlertDialog.Builder(this@SetupActivity)
                                        dialog.setMessage("Disconnected because of connection issues")
                                            .setPositiveButton("Ok") { _, _ ->
                                                service?.resetGameData()
                                                this@SetupActivity.startActivity(
                                                    Intent(
                                                        this@SetupActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                this@SetupActivity.finish()
                                            }
                                            .show()
                                    }
                                }
                                else
                                    service!!.connectionIdMightHaveChanged()
                            })
                        }

                    }
                }
                if(!connectionInfo.isConnected){
                    if(service!=null && service!!.lostConnection())
                        runOnUiThread {
                            Toast.makeText(this@SetupActivity, "You lost internet connection", Toast.LENGTH_LONG ).show()
                            SubmitButton.isEnabled = false
                        }
                }
            }
        }
    }
}
