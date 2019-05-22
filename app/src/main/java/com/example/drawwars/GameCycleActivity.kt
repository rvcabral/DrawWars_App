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
    private val StandbyTitle = "Please wait for the other players.."
    private val TryandguessTitle = "Time to Guess!"
    private val EndOfRoundTitle = "Round ended"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_cycle)


        mViewModel = ViewModelProviders.of(this).get(ServiceViewModel::class.java!!)


        mViewModel?.getBinder()?.observe(this, object: Observer<ServerService.MyBinder> {
            override fun onChanged(binder: ServerService.MyBinder?) {
                service = binder?.getService()
                service?.listen(this@GameCycleActivity)
                titleTextView.text=StandbyTitle
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
            "TryAndGuess"-> {
                guessTextBox.text.clear()
                guessTextBox.visibility = EditText.VISIBLE
                sendGuessButton.visibility = Button.VISIBLE
                titleTextView.text = TryandguessTitle
            }
            "StandBy"->{
                guessTextBox.text.clear()
                guessTextBox.visibility = EditText.INVISIBLE
                sendGuessButton.visibility = Button.INVISIBLE
                titleTextView.text = StandbyTitle
            }
            "WrongGuess"->{
                guessTextBox.text.clear()
                Toast.makeText(this, "Wrong Guess. Try again!", Toast.LENGTH_LONG)
            }
            "RightGuess"->{
                guessTextBox.text.clear()
                guessTextBox.visibility = EditText.INVISIBLE
                sendGuessButton.visibility = Button.INVISIBLE
                titleTextView.text = StandbyTitle
                Toast.makeText(this, "You got it right!", Toast.LENGTH_LONG)
            }
            "SeeResults"->{
                guessTextBox.text.clear()
                guessTextBox.visibility = EditText.INVISIBLE
                sendGuessButton.visibility = Button.INVISIBLE
                titleTextView.text = EndOfRoundTitle
            }
            "EndOfGame"->{
                this.
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    ///region service management

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
        service?.mute(this)
    }

    private fun bindService() {
        val serviceBindIntent = Intent(this, ServerService::class.java)
        bindService(serviceBindIntent, mViewModel!!.getServiceConnection(), Context.BIND_AUTO_CREATE)
    }

    ///endregion

}
