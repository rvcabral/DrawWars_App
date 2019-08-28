package com.example.drawwars

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.drawwars.services.ServerService
import com.example.drawwars.services.ServiceListener
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_game_cycle.*
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import kotlin.system.exitProcess


class GameCycleActivity : AppCompatActivity(), ServiceListener {

    private var mViewModel: ServiceViewModel? = null
    private var service: ServerService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_cycle)


        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java!!)


        mViewModel?.getBinder()?.observe(this, object: Observer<ServerService.MyBinder> {
            override fun onChanged(binder: ServerService.MyBinder?) {
                service = binder?.getService()
                service?.listen(this@GameCycleActivity)
                titleTextView.text=getString(R.string.WaitForOtherPlayersTitle)
                guessTextBox.visibility = EditText.INVISIBLE
                sendGuessButton.visibility = Button.INVISIBLE
                sendGuessButton.setOnClickListener {
                    if(guessTextBox.text.isNotBlank()){
                        service!!.sendGuess(guessTextBox.text.toString())
                    }
                }
            }
        })
    }

    override fun Interaction(action: String, param: Any?) {
        when (action){

            getString(R.string.Action_TryAndGuess) -> {
                runOnUiThread{
                    guessTextBox.text.clear()
                    guessTextBox.visibility = EditText.VISIBLE
                    sendGuessButton.visibility = Button.VISIBLE
                    titleTextView.text = getString(R.string.TryandguessTitle)
                }
            }
            getString(R.string.Action_StandBy) ->{
                runOnUiThread {
                    guessTextBox.text.clear()
                    guessTextBox.visibility = EditText.INVISIBLE
                    sendGuessButton.visibility = Button.INVISIBLE
                    titleTextView.text = getString(R.string.StandbyTitle)
                }
            }
            getString(R.string.Action_WrongGuess) ->{
                runOnUiThread {
                    guessTextBox.text.clear()
                    Toast.makeText(this@GameCycleActivity, getString(R.string.WrongGuessFeedback), Toast.LENGTH_LONG).show()
                }
            }
            getString(R.string.Action_RightGuess) ->{
                runOnUiThread {
                    guessTextBox.text.clear()
                    guessTextBox.visibility = EditText.INVISIBLE
                    sendGuessButton.visibility = Button.INVISIBLE
                    titleTextView.text = getString(R.string.StandbyTitle)
                    Toast.makeText(this@GameCycleActivity, getString(R.string.RightGuessMessage), Toast.LENGTH_LONG).show()
                }
            }

            getString(R.string.Action_SeeResults) ->{
                runOnUiThread {
                    guessTextBox.text.clear()
                    guessTextBox.visibility = EditText.INVISIBLE
                    sendGuessButton.visibility = Button.INVISIBLE
                    titleTextView.text = getString(R.string.EndOfRoundTitle)
                }
            }
            getString(R.string.Action_NextRound)->{
                runOnUiThread {
                    var activityIntent = Intent(this@GameCycleActivity, GameActivity::class.java)
                    activityIntent.putExtra("EndOfRound", true)
                    this.startActivity(activityIntent)
                    finish()
                }
            }
            getString(R.string.Action_EndOfGame) ->{
                runOnUiThread {
                    this.startActivity(Intent(this@GameCycleActivity, MainActivity::class.java))
                    finish()
                }
            }
            getString(R.string.Action_ServerDied)->{
                runOnUiThread {
                    val dialog: AlertDialog.Builder = AlertDialog.Builder(this@GameCycleActivity)
                    dialog.setMessage("Server is down")
                        .setPositiveButton("Ok") { _, _ ->
                            service?.resetGameData()
                            this@GameCycleActivity.finishAndRemoveTask()
                            exitProcess(0);
                        }
                        .show()
                }
            }
        }
    }

    ///region service management

    override fun onResume() {
        super.onResume()
        bindService()
        service?.listen(this@GameCycleActivity)
    }

    override fun onDestroy() {
        service?.mute(this)
        try {
            unbindService(mViewModel!!.getServiceConnection())
        }
        catch (e : Exception){
            Log.d("Game Activity", "Couldn't unbind");
        }

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

    override fun onStop() {
        unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onBackPressed() {
        //Do nothing here.
    }

    ///endregion

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(receiver, intentFilter)
    }
    private var receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var connectionInfo = (intent?.extras?.get(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo)
                if (connectionInfo.isConnected) {
                    if (service != null) {
                        if (service!!.regainedConnection()) {
                            runOnUiThread {
                                sendGuessButton.isEnabled = true
                            }
                            service!!.InteractionsWereLost(Consumer { Yes ->
                                if (Yes) {
                                    runOnUiThread {
                                        val dialog: AlertDialog.Builder = AlertDialog.Builder(this@GameCycleActivity)
                                        dialog.setMessage("Disconnected because of connection issues")
                                            .setPositiveButton("Ok") { _, _ ->
                                                service?.resetGameData()
                                                this@GameCycleActivity.startActivity(
                                                    Intent(
                                                        this@GameCycleActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                this@GameCycleActivity.finish()
                                            }
                                            .show()
                                    }
                                } else
                                    service!!.ConnectionIdMightHaveChanged()
                            })
                        }
                    }
                }
                if (!connectionInfo.isConnected) {
                    if(service!=null && service!!.lostConnection())
                        runOnUiThread {
                            Toast.makeText(this@GameCycleActivity, "You lost internet connection", Toast.LENGTH_LONG).show()
                            sendGuessButton.isEnabled = false
                        }
                }

        }
    }
}
