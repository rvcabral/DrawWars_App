package com.example.drawwars

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.android.synthetic.main.content_setup.*

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)


        SubmitButton.setOnClickListener { v-> startActivity(Intent(this, GameActivity::class.java )) }

    }

}
