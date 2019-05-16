package com.example.drawwars

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.drawwars.services.ServerService
import com.example.drawwars.services.ServiceListener

class StandByActivity : AppCompatActivity(), ServiceListener {
    var service:ServerService?=null
    override fun Interaction(action: String, param: Any?) {
        when (action){
            "TryAndGuess"->{
                startActivity(Intent(this,))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stand_by)
        service = ServerService().MyBinder().getService()
        service!!.listen(this)
    }



    override fun onPause() {
        super.onPause()
        service?.mute(this)
    }


}
