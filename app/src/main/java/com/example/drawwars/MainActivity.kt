package com.example.drawwars

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.aware.WifiAwareManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.widget.Button
import com.example.drawwars.services.ServerService
import kotlinx.android.synthetic.main.activity_main.*
import com.example.drawwars.services.ServerService.MyBinder
import android.widget.Toast
import com.example.drawwars.services.ServiceListener
import com.example.drawwars.utils.SharedUtil.Companion.WifiDisabled
import kotlinx.android.synthetic.main.activity_game.*


class MainActivity : AppCompatActivity(), ServiceListener {

    override fun Interaction(action: String, param: Any?) {
        when (action){
            getString(R.string.Action_AckSession)->{
                val intent = Intent(this, SetupActivity::class.java)
                startActivity(intent)
            }
            getString(R.string.Action_NonExistingSession) -> {
                runOnUiThread{
                    Toast.makeText(this@MainActivity, getString(R.string.InvalidRoomMessage), Toast.LENGTH_LONG).show()
                }
            }
        }
    }




    private var mViewModel: ServiceViewModel? = null
    private var service: ServerService? = null

    private var receiver = object:BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
         var connectionInfo = (intent?.extras?.get(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo)
            if(connectionInfo!= null){
                if(connectionInfo.isConnected){
                    if (service == null || service!!.regainedConnection()) {
                        mViewModel = ViewModelProviders.of(this@MainActivity).get(ServiceViewModel::class.java)
                        mViewModel?.getBinder()?.observe(this@MainActivity,
                            Observer<MyBinder> { binder ->
                                service = binder?.getService()
                                service?.listen(this@MainActivity)
                            })
                        runOnUiThread { SubmitButton?.isEnabled = true }
                        startService()
                        bindService()
                    }
                }
                if(!connectionInfo.isConnected){
                    if(service==null || service!!.lostConnection())
                        runOnUiThread { SubmitButton?.isEnabled = false }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(receiver, intentFilter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnected == true

        SubmitButton?.setOnClickListener {
            if(CodeInput.text.isNotEmpty()){
                service?.Inlist(CodeInput.text.toString())
            }
        }

        if(!isConnected){
            val dialog:AlertDialog.Builder = AlertDialog.Builder(this)
            dialog.setMessage("You don't have an internet connection. Please change the settings.")
                .setPositiveButton("Ok") { _, _ -> }
                .show()
            SubmitButton.isEnabled = false
        }
        else{
            mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java)
            mViewModel?.getBinder()?.observe(this,
                Observer<MyBinder> { binder ->
                    service = binder?.getService()
                    service?.listen(this@MainActivity)
                })
            startService()
        }



    }





    override fun onResume() {
        super.onResume()
        if((this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null &&
            (this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo.isConnected){
            bindService()
            service?.listen(this@MainActivity)
        }

    }

    private fun startService() {
        val serviceIntent = Intent(this, ServerService::class.java)
        startService(serviceIntent)
    }

    override fun onStop() {
        service?.mute(this)
        if((this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null &&
            (this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo.isConnected) {
            unbindService(mViewModel!!.getServiceConnection())
        }
        unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onDestroy() {
        service?.mute(this)
        if((this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null &&
            (this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo.isConnected) {
            unbindService(mViewModel!!.getServiceConnection())
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        //Do nothing here.
    }

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)

    }


}
