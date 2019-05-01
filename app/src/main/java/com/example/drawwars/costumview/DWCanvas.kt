package com.example.drawwars.costumview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState

class DWCanvas(context: Context?) : View(context) {
    private val path = Path()
    val brush = Paint()
    init {
        brush.isAntiAlias = false
        brush.color = Color.BLUE
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 10f
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action) {
            MotionEvent.ACTION_DOWN ->{
                path.moveTo(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE ->{
                path.lineTo(event.x, event.y)

                postInvalidate()
                return true
            }
        }
        postInvalidate()
        return false
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawPath(path, brush)
    }
}