package com.example.drawwars

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast
import com.example.drawwars.services.ServerService
import com.example.drawwars.services.ServiceListener

import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.android.synthetic.main.content_setup.*

class SetupActivity : AppCompatActivity(), ServiceListener {
    override fun Interaction(action: String, param: Any?) {
        when (action){
            "AckNickname"-> startActivity(Intent(this, GameActivity::class.java ))
        }
    }


    private var mViewModel: ServiceViewModel? = null
    private var service: ServerService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java!!)


        mViewModel?.getBinder()?.observe(this, object: Observer<ServerService.MyBinder> {
            override fun onChanged(binder: ServerService.MyBinder?) {
                service = binder?.getService()
                service?.listen(this@SetupActivity)
            }

        })

        SubmitButton.setOnClickListener {
            if(CodeInput.text.isEmpty()) Toast.makeText(this@SetupActivity, "Insira um nome", Toast.LENGTH_LONG);
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

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)
    }

    override fun onBackPressed() {
        //Do nothing here.
    }
}
