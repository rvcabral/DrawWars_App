package com.example.drawwars

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.drawwars.services.ServerService
import com.example.drawwars.utils.HandShakeResult
import kotlinx.android.synthetic.main.activity_main.*
import com.example.drawwars.services.ServerService.MyBinder
import android.support.v4.app.BundleCompat.getBinder
import android.util.Log
import com.example.drawwars.services.ServiceListener


class MainActivity : AppCompatActivity(), ServiceListener {
    override fun AckSession() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
    }

    override fun AckNickname() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var mViewModel: ServiceViewModel? = null
    private var service: ServerService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, SetupActivity::class.java)
        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java!!)


        mViewModel?.getBinder()?.observe(this, object:Observer<ServerService.MyBinder>{
            override fun onChanged(binder: MyBinder?) {
                service = binder?.getService()
                service?.listen(this@MainActivity)
            }

        })

        SubmitButton.setOnClickListener {
                b->
            if(CodeInput.text.isNotEmpty()){
                service?.Inlist(CodeInput.text.toString())
            }

        }
    }
    override fun onResume() {
        super.onResume()
        startService()
    }

    private fun startService() {
        val serviceIntent = Intent(this, ServerService::class.java)
        startService(serviceIntent)

        bindService()
    }

    override fun onDestroy() {
        service?.mute(this)
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)
    }
}
