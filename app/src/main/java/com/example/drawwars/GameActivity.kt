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
import android.support.v4.os.HandlerCompat.postDelayed
import android.support.v4.app.BundleCompat.getBinder
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.example.drawwars.services.ServerService.MyBinder
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.support.v4.app.BundleCompat.getBinder






class GameActivity : AppCompatActivity() {

    private var mService: ServerService? = null
    private var mViewModel: ServiceViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        /*val hubConnection = HubConnectionBuilder.create("http://10.0.2.2:5000/Server").build()
        val context = this
        hubConnection.start()
        hubConnection.on("ReceiveNewCoordinates", {t-> Toast.makeText(context, t, Toast.LENGTH_LONG) },String::class.java )
        if(hubConnection.connectionState == HubConnectionState.DISCONNECTED){
            hubConnection.start()
        }
        hubConnection.send("MoveViewFromServer", "TEST 1" as String)
        //hubConnection.
        */

        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java!!)
        setObservers()
        val canvas = DWCanvas(this);
        //val conn = mViewModel!!.getServiceConnection()
        canvasLayout.addView(canvas)
    }

    private fun toggleUpdates() {
        if (mService != null) {
            if(mService!!.isConnected()){

            }


        }
    }

    private fun setObservers() {
        mViewModel!!.getBinder().observe(this, object : Observer<ServerService.MyBinder> {
            override fun onChanged(myBinder: ServerService.MyBinder?) {
                if (myBinder == null) {
                    Log.d("Game Activity", "onChanged: unbound from service")
                } else {
                    Log.d("Game Activity", "onChanged: bound to service.")
                    mService = myBinder!!.getService()
                }
            }
        })

        mViewModel!!.isConnected().observe(this, object : Observer<Boolean> {
            override fun onChanged( aBoolean: Boolean?) {
                val handler = Handler()


                // control what the button shows
                if (aBoolean!!) {
                    Toast.makeText( this@GameActivity,"ISConnected", Toast.LENGTH_LONG)

                } else {
                    Toast.makeText( this@GameActivity,"NotConnected", Toast.LENGTH_LONG)
                }
            }
        })
    }


    override fun onResume() {
        super.onResume()
        startService()
    }


    override fun onStop() {
        super.onStop()
        if (mViewModel!!.getBinder() != null) {
            unbindService(mViewModel!!.getServiceConnection())
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, ServerService::class.java)
        startService(serviceIntent)

        bindService()
    }

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)
    }
}
