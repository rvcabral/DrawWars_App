package com.example.drawwars

import android.graphics.Canvas
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.drawwars.costumview.DWCanvas
import kotlinx.android.synthetic.main.activity_game.*

class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val canvas = DWCanvas(this);

        canvasLayout.addView(canvas)
    }
}
