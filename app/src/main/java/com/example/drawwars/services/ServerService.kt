package com.example.drawwars.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.drawwars.utils.HandShakeResult
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList

class  ServerService: Service() {
    private val TAG = "MyService"
    private var listeners : List<ServiceListener> = ArrayList<ServiceListener>()
    val binder : IBinder = MyBinder()
    var handler: Handler? = null
    val hubConnection = HubConnectionBuilder.create("http://10.0.2.2:5000/Server").build()
    val ctx = this
    var connected = false
    var gameContext:HandShakeResult?=null

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler()
        hubConnection.start()
        if(hubConnection.connectionState == HubConnectionState.CONNECTED){
            connected = true;
        }

        hubConnection.on("AckSession", { x-> AckSession(x)  }, HandShakeResult::class.java)
        hubConnection.on("AckNickname", {success-> AckNickname(success)}, Boolean::class.java)

    }
    fun  isConnected():Boolean {
        return connected;
    }

    fun AckSession(ctx : HandShakeResult){
        gameContext = HandShakeResult(ctx.session,ctx.playerId )
        for( listener in listeners){
            listener.AckSession()
        }
    }
    fun AckNickname(success : Boolean){
        for( listener in listeners){
            listener.AckNickname()
        }
    }

    inner class MyBinder : Binder() {
        fun getService() : ServerService{
            return this@ServerService
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: called.")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: called.")
    }

    fun listen(listener:ServiceListener){
        listeners += listener
    }
    fun mute(listener: ServiceListener){
        listeners -= listener
    }

    fun Inlist(room:String){
        hubConnection.send("Inlist", room)
    }

    fun sendNickName(nickname:String){
        hubConnection.send("SetPlayerNickName", gameContext, nickname)
    }

}

