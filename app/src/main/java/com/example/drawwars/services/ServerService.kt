package com.example.drawwars.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
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
    //val hubConnection = HubConnectionBuilder.create("ws://10.0.2.2:5000/Server").build()
    //val apiUrl = "http://10.0.2.2:5000/api/drawing/"
    //val hubConnection = HubConnectionBuilder.create("http://52.211.139.236/DrawWars/Server").build()
    val hubConnection = HubConnectionBuilder.create("ws://52.211.139.236/DrawWars/Server").build()
    val apiUrl = "http://52.211.139.236/DrawWars/api/drawing/"
    var connected = false
    var gameContext:HandShakeResult?=null


    val ENDPOINT_AckSession = "AckSession"
    val ENDPOINT_AckNickName = "AckNickname"
    val ENDPOINT_NonExistingSession = "NonExistingSession"
    val ENDPOINT_DrawThemes = "DrawThemes"
    val ENDPOINT_TryAndGuess = "TryAndGuess"
    val ENDPOINT_StandBy = "StandBy"
    val ENDPOINT_WrongGuess = "WrongGuess"
    val ENDPOINT_RightGuess = "RightGuess"
    val ENDPOINT_SeeResults = "SeeResults"
    val ENDPOINT_EndOfGame = "EndOfGame"
    val ENDPOINT_TimesUp = "TimesUp"
    val ENDPOINT_NextRound = "NextRound"
    val ENDPOINT_DrawSubmitted = "DrawSubmitted"
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

        hubConnection.on(ENDPOINT_AckSession, { x-> gameContext = HandShakeResult(x.session,x.playerId );notifyListeners(ENDPOINT_AckSession,null)  }, HandShakeResult::class.java)
        hubConnection.on(ENDPOINT_AckNickName, {notifyListeners(ENDPOINT_AckNickName,null)})
        hubConnection.on(ENDPOINT_NonExistingSession,{uid->notifyListeners(ENDPOINT_NonExistingSession,uid)}, UUID::class.java)
        hubConnection.on(ENDPOINT_DrawThemes,  {m -> notifyListeners(ENDPOINT_DrawThemes,m ) }, Any::class.java)//as HashMap<UUID, List<String>>
        hubConnection.on(ENDPOINT_TryAndGuess, {notifyListeners(ENDPOINT_TryAndGuess,null)})
        hubConnection.on(ENDPOINT_StandBy,     {notifyListeners(ENDPOINT_StandBy,null)})
        hubConnection.on(ENDPOINT_WrongGuess,  {notifyListeners(ENDPOINT_WrongGuess,null)})
        hubConnection.on(ENDPOINT_RightGuess,  {notifyListeners(ENDPOINT_RightGuess,null)})
        hubConnection.on(ENDPOINT_SeeResults,  {notifyListeners(ENDPOINT_SeeResults,null)})
        hubConnection.on(ENDPOINT_EndOfGame,   {notifyListeners(ENDPOINT_EndOfGame,null)})
        hubConnection.on(ENDPOINT_TimesUp,   {notifyListeners(ENDPOINT_TimesUp,null)})
        hubConnection.on(ENDPOINT_NextRound,   {notifyListeners(ENDPOINT_NextRound,null)})



    }

    inner class MyBinder : Binder() {
        fun getService() : ServerService{
            return this@ServerService
        }
    }

    ///region Socket Interface

    private fun notifyListeners(action:String, param:Any?) {
        var p = param
        if(action==ENDPOINT_DrawThemes)
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
        hubConnection.send("Inlist", room)
    }

    fun sendNickName(nickname:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("SetPlayerNickName", gameContext, nickname)
    }
    fun SetArt(draw : String, theme:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()

        var body = "{ \"SessionId\" : \"${gameContext!!.session}\", \"PlayerId\" : \"${gameContext!!.playerId}\",\"Extension\" : \"PNG\",\"Drawing\" : \"${draw}\", \"Theme\" : \"${theme}\"}"
        var t = Thread {
            var res = khttp.extensions.post(apiUrl +"submit", headers = mapOf("Content-Type" to "application/json"), data = body)
                .subscribe(io.reactivex.functions.Consumer {
                    if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
                        hubConnection.start()
                    hubConnection.send(ENDPOINT_DrawSubmitted, gameContext)
                    for(listener in listeners)
                        listener.Interaction(ENDPOINT_DrawSubmitted,"")
                }
            )
        }.start()
    }

    fun Ready() {
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("Ready", gameContext)
    }

    fun sendGuess(guess:String) {
        while(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send("SendGuess", gameContext, guess)
    }


    ///endregion

}

