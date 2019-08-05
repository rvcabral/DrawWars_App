package com.example.drawwars

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.drawwars.services.ServerService
import com.example.drawwars.services.ServiceListener
import kotlinx.android.synthetic.main.activity_game_cycle.*
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Duration

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
                    service!!.mute(this);
                    finish()
                }
            }
            getString(R.string.Action_EndOfGame) ->{
                runOnUiThread {
                    this.startActivity(Intent(this@GameCycleActivity, MainActivity::class.java))
                    service!!.mute(this);
                    finish()
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

    ///endregion

}
