package com.example.drawwars.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.drawwars.R
import com.example.drawwars.utils.HandShakeResult
import com.google.gson.internal.LinkedTreeMap
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import java.util.*

import kotlin.collections.ArrayList



class  ServerService: Service() {

    /// region fields and constants
    private val TAG = "MyService"
    private var listeners : List<ServiceListener> = ArrayList<ServiceListener>()
    val binder : IBinder = MyBinder()
    var handler: Handler? = null
    val hubConnection = HubConnectionBuilder.create("ws://10.0.2.2:5000/Server").build()
    val apiUrl = "http://10.0.2.2:5000/api/drawing/"
    //val hubConnection = HubConnectionBuilder.create("http://52.211.139.236/DrawWars/Server").build()
    //val hubConnection = HubConnectionBuilder.create("ws://52.211.139.236/DrawWars/Server").build()
    //val apiUrl = "http://52.211.139.236/DrawWars/api/drawing/"
    var connected = false
    var gameContext:HandShakeResult?=null


///endregion

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

        hubConnection.on(getString(R.string.Action_AckSession), { x-> gameContext = HandShakeResult(x.session,x.playerId );notifyListeners(getString(R.string.Action_AckSession),null)  }, HandShakeResult::class.java)
        hubConnection.on(getString(R.string.Action_AckNickName)) {notifyListeners(getString(R.string.Action_AckNickName),null)}
        hubConnection.on(getString(R.string.Action_NonExistingSession),{uid->notifyListeners(getString(R.string.Action_NonExistingSession),uid)}, UUID::class.java)
        hubConnection.on(getString(R.string.Action_DrawThemes),  {m -> notifyListeners(getString(R.string.Action_DrawThemes),m ) }, Any::class.java)//as HashMap<UUID, List<String>>
        hubConnection.on(getString(R.string.Action_TryAndGuess)) {notifyListeners(getString(R.string.Action_TryAndGuess),null)}
        hubConnection.on(getString(R.string.Action_StandBy)) {notifyListeners(getString(R.string.Action_StandBy),null)}
        hubConnection.on(getString(R.string.Action_WrongGuess)) {notifyListeners(getString(R.string.Action_WrongGuess),null)}
        hubConnection.on(getString(R.string.Action_RightGuess)) {notifyListeners(getString(R.string.Action_RightGuess),null)}
        hubConnection.on(getString(R.string.Action_SeeResults)) {notifyListeners(getString(R.string.Action_SeeResults),null)}
        hubConnection.on(getString(R.string.Action_EndOfGame)) {notifyListeners(getString(R.string.Action_EndOfGame),null)}
        hubConnection.on(getString(R.string.Action_TimesUp)) {notifyListeners(getString(R.string.Action_TimesUp),null)}
        hubConnection.on(getString(R.string.Action_NextRound)) {notifyListeners(getString(R.string.Action_NextRound),null)}


    }

    inner class MyBinder : Binder() {
        fun getService() : ServerService{
            return this@ServerService
        }
    }

    ///region Socket Interface

    private fun notifyListeners(action:String, param:Any?) {
        var p = param
        if(action==getString(R.string.Action_DrawThemes))
         p = (param as LinkedTreeMap<String, ArrayList<String>>)[gameContext!!.playerId.toString()]

        for(listener in listeners)
         listener.Interaction(action, p)
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
        if(!listeners.contains(listener))
            listeners += listener
    }
    fun mute(listener: ServiceListener){
        listeners -= listener
    }

    fun Inlist(room:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_Inlist), room)
    }

    fun sendNickName(nickname:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_SetNickname), gameContext, nickname)
    }
    fun SetArt(draw : String, theme:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()

        var body = "{ \"SessionId\" : \"${gameContext!!.session}\", \"PlayerId\" : \"${gameContext!!.playerId}\",\"Extension\" : \"PNG\",\"Drawing\" : \"$draw\", \"Theme\" : \"$theme\"}"
        Thread {
            var res = khttp.extensions.post(apiUrl +"submit", headers = mapOf("Content-Type" to "application/json"), data = body)
                .subscribe(io.reactivex.functions.Consumer {
                    if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
                        hubConnection.start()
                    hubConnection.send(getString(R.string.Action_DrawSubmitted), gameContext)
                    for(listener in listeners)
                        listener.Interaction(getString(R.string.Action_DrawSubmitted),"")
                }
            )
        }.start()
    }

    fun Ready() {
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_Ready), gameContext)
    }

    fun sendGuess(guess:String) {
        while(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_SendGuess), gameContext, guess)
    }


    ///endregion

}

