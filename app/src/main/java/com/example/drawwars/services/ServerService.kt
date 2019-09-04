package com.example.drawwars.services

import android.app.Service
import android.content.Intent
import android.nfc.Tag
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.drawwars.R
import com.example.drawwars.utils.DeviceIdentification
import com.example.drawwars.utils.HandShakeResult
//import com.google.gson.internal.LinkedTreeMap
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.functions.Consumer
import khttp.responses.Response
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import kotlin.collections.ArrayList



class  ServerService: Service() {

    /// region fields and constants
    private val TAG = "MyService"
    private var listeners : List<ServiceListener> = ArrayList()
    private var interactionCounter:Int = 0
    private var connectionState:Boolean = true
    val binder : IBinder = MyBinder()
    //val hubConnection = HubConnectionBuilder.create("http://10.0.2.2:5000/Server").build()
    //val apiUrl = "http://10.0.2.2:5000/api/"
    val hubConnection = HubConnectionBuilder.create("http://52.211.139.236/DrawWars/Server").build()
    val apiUrl = "http://52.211.139.236/DrawWars/api/"
    val drawingController = "drawing/"
    val gameInfoController = "GameInfo/"
    var gameContext:HandShakeResult?=null
    var IsServerDeadThread : Thread?=null
    var gameStarted : AtomicBoolean = AtomicBoolean(false)

///endregion

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        hubConnection.start()
        connectionState = true


        hubConnection.on(getString(R.string.Action_AckSession), { x-> gameContext = HandShakeResult(x.session,x.playerId ); interactionCounter++;notifyListeners(getString(R.string.Action_AckSession),null)  }, HandShakeResult::class.java)
        hubConnection.on(getString(R.string.Action_AckNickName)) {interactionCounter++; notifyListeners(getString(R.string.Action_AckNickName),null)}
        hubConnection.on(getString(R.string.Action_NonExistingSession),{uid->notifyListeners(getString(R.string.Action_NonExistingSession),uid)}, UUID::class.java)
        hubConnection.on(getString(R.string.Action_DrawThemes),  {m -> interactionCounter++; notifyListeners(getString(R.string.Action_DrawThemes),m ) }, Any::class.java)
        hubConnection.on(getString(R.string.Action_TryAndGuess)) {interactionCounter++; notifyListeners(getString(R.string.Action_TryAndGuess),null)}
        hubConnection.on(getString(R.string.Action_StandBy)) {interactionCounter++; notifyListeners(getString(R.string.Action_StandBy),null)}
        hubConnection.on(getString(R.string.Action_WrongGuess)) {interactionCounter++; notifyListeners(getString(R.string.Action_WrongGuess),null)}
        hubConnection.on(getString(R.string.Action_RightGuess)) {interactionCounter++; notifyListeners(getString(R.string.Action_RightGuess),null)}
        hubConnection.on(getString(R.string.Action_SeeResults)) {interactionCounter++; notifyListeners(getString(R.string.Action_SeeResults),null)}
        hubConnection.on(getString(R.string.Action_EndOfGame)) {interactionCounter++; notifyListeners(getString(R.string.Action_EndOfGame),null)}
        hubConnection.on(getString(R.string.Action_TimesUp)) {interactionCounter++; notifyListeners(getString(R.string.Action_TimesUp),null)}
        hubConnection.on(getString(R.string.Action_NextRound)) {interactionCounter++; notifyListeners(getString(R.string.Action_NextRound),null)}
        hubConnection.on(getString(R.string.Action_InteractionCount), { count -> notifyListeners(getString(R.string.Action_InteractionCount), count)}, Int::class.java)

    }

    inner class MyBinder : Binder() {
        fun getService() : ServerService{
            return this@ServerService
        }
    }

    ///region Socket Interface

    private fun notifyListeners(action:String, param:Any?) {
        var p = param
        IsServerDeadThread?.interrupt()
        if(gameStarted.get()){
            IsServerDeadThread = initThread()
            IsServerDeadThread!!.start()
        }

        if(action==getString(R.string.Action_DrawThemes)) {
            p = (param as Map<String, ArrayList<String>>)[gameContext!!.playerId.toString()]
            if(gameStarted.compareAndSet(false, true)) {
                IsServerDeadThread = initThread()
                IsServerDeadThread!!.start()
            }
        }

        for(listener in listeners)
         listener.Interaction(action, p)
    }
    private fun initThread():Thread{
        return Thread{
            try {
                /// IF SERVER IS INACTIVE FOR 5 MINUTES, THE APP IS CLOSED
                Thread.sleep(1000 * 60 * 5)

                for (listener in listeners)
                    listener.Interaction(getString(R.string.Action_ServerDied), null)

            }catch (exception: InterruptedException){

            }
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
        if(!listeners.contains(listener))
            listeners += listener
    }
    fun mute(listener: ServiceListener){
        listeners -= listener
    }

    fun inlist(room:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        var deviceId = DeviceIdentification.DeviceID(this);
        hubConnection.send(getString(R.string.Action_Inlist), room, deviceId)
    }
    fun sendNickName(nickname:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_SetNickname), gameContext, nickname)
        interactionCounter++;
    }
    fun setArt(draw : String, theme:String){
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()

        var body = "{ \"SessionId\" : \"${gameContext!!.session}\", \"PlayerId\" : \"${gameContext!!.playerId}\",\"Extension\" : \"PNG\",\"Drawing\" : \"$draw\", \"Theme\" : \"$theme\"}"

        khttp.extensions.post(apiUrl + drawingController +"submit", headers = mapOf("Content-Type" to "application/json"), data = body)
            .subscribe(
                {
                    interactionCounter++
                    if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
                        hubConnection.start()
                    hubConnection.send(getString(R.string.Action_DrawSubmitted), gameContext)
                    interactionCounter++
                    for(listener in listeners)
                        listener.Interaction(getString(R.string.Action_DrawSubmitted),"")
                },
                {
                        t:Throwable ->  Log.d(TAG, "Draw Post Failed ${t.message}")
                }
        )

    }
    fun ready() {
        if(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_Ready), gameContext)
        interactionCounter++
    }
    fun sendGuess(guess:String) {
        while(hubConnection.connectionState== HubConnectionState.DISCONNECTED)
            hubConnection.start()
        hubConnection.send(getString(R.string.Action_SendGuess), gameContext, guess)
        interactionCounter++
    }
    fun interactionsWereLost(callback : Consumer<Boolean>)  {
        khttp.extensions.get("$apiUrl${gameInfoController}interactionCount/${gameContext?.session}/${gameContext?.playerId}")
            .subscribe({
                val countersDiffer = String(it.content).toInt()!=interactionCounter
                callback.accept(countersDiffer)
            },
                { t ->  Log.d(TAG, "Get InteractionLost failed: ${t.message}") })
    }
    fun connectionIdMightHaveChanged() {
        while(hubConnection.connectionState== HubConnectionState.DISCONNECTED) {
            hubConnection.start()
            Thread.sleep(100) //NEEDS TIME TO PROCESS INITIAL HANDSHAKE
        }
        hubConnection.send(getString(R.string.Action_ConnectionIdChanged), gameContext)
    }

    ///endregion



    fun resetGameData(){
        listeners = ArrayList()
        interactionCounter=0
    }

    fun lostConnection():Boolean{
        if(connectionState){
            connectionState = false
            return true
        }
        return false
    }
    fun regainedConnection():Boolean{
        if(!connectionState){
            connectionState = true
            return true
        }
        return false
    }
}

