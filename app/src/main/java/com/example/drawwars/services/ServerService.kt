package com.example.drawwars.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.drawwars.utils.HandShakeResult
import com.google.gson.JsonParser
import com.google.gson.JsonSerializer
import com.google.gson.internal.LinkedTreeMap
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import khttp.async
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class  ServerService: Service() {
    private val TAG = "MyService"
    private var listeners : List<ServiceListener> = ArrayList<ServiceListener>()
    val binder : IBinder = MyBinder()
    var handler: Handler? = null
    val hubConnection = HubConnectionBuilder.create("http://10.0.2.2:5000/Server").build()
    val apiUrl = "http://10.0.2.2:5000/api/drawing/"
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

        hubConnection.on("AckSession", { x-> gameContext = HandShakeResult(x.session,x.playerId );notifyListeners("AckSession",null)  }, HandShakeResult::class.java)
        hubConnection.on("AckNickname", {notifyListeners("AckNickname",null)})
        hubConnection.on("NonExistingSession",{uid->notifyListeners("NonExistingSession",uid)}, UUID::class.java)
        hubConnection.on("DrawThemes", {m -> notifyListeners("DrawThemes",m ) }, Any::class.java)//as HashMap<UUID, List<String>>

    }


    private fun notifyListeners(action:String, param:Any?) {
        var p = param
        if(action=="DrawThemes")
            p=(param as LinkedTreeMap<String, ArrayList<String>>)[gameContext!!.playerId.toString()] as Any//HashMap<UUID, List<String>>
        for(listener in listeners)
            listener.Interaction(action, p)
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
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("Inlist", room)
    }

    fun sendNickName(nickname:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("SetPlayerNickName", gameContext, nickname)
    }
    fun SetArt(draw : String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()

        var body = "{ \"SessionId\" : \"${gameContext!!.session}\", \"PlayerId\" : \"${gameContext!!.playerId}\",\"Extension\" : \"PNG\",\"Drawing\" : \"${draw}\"}"

        var res = khttp.async.post(apiUrl +"submit", headers = mapOf("Content-Type" to "application/json"), data = body, onResponse = {
            hubConnection.send()
        })
    }
    fun Ready() {
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("Ready", gameContext)
    }
    fun sendGuess(guess:String) {
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("SendGuess", gameContext, guess)
    }
}

