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
import android.widget.Button
import com.example.drawwars.services.ServiceListener
import android.util.DisplayMetrics
import android.widget.FrameLayout


class GameActivity : AppCompatActivity(), ServiceListener {


    private var service: ServerService? = null
    private var mViewModel: ServiceViewModel? = null
    private var canvas :DWCanvas?=null
    private var theme :String="";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        captionTextView.text = "Aguarde pelos outros jogadores"
        submitButton.visibility=Button.INVISIBLE
        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java!!)
        mViewModel?.getBinder()?.observe(this, object: Observer<ServerService.MyBinder> {
            override fun onChanged(binder: ServerService.MyBinder?) {
                service = binder?.getService()
                service?.listen(this@GameActivity)

            }

        })
        ReadyButton.setOnClickListener { service!!.Ready() }
        val displayMetrics = DisplayMetrics()
        canvasLayout.visibility = FrameLayout.INVISIBLE
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        canvas = DWCanvas(this, height, width);
        canvasLayout.addView(canvas)
        canvasLayout.isEnabled = false
        submitButton.setOnClickListener {

            service!!.SetArt(canvas!!.getDraw(), theme)
        }

    }


     //region  Standart Functions

    override fun onPause() {
        super.onPause()
        service?.mute(this)
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


    override fun Interaction(action: String, param: Any?) {

        when (action){
            "DrawThemes"->{
                runOnUiThread({
                    val themes = param as ArrayList<String>
                    theme = themes[0]
                    captionTextView.text = themes[0]


                    ReadyButton.visibility=Button.INVISIBLE
                    submitButton.visibility=Button.VISIBLE

                    canvasLayout.isEnabled = true
                    canvasLayout.visibility = FrameLayout.VISIBLE
                })
            }
            "StandBy"->{
                runOnUiThread({
                    startActivity(Intent(this@GameActivity, GameCycleActivity::class.java))
                })
            }
        }
    }




    //endregion

}
