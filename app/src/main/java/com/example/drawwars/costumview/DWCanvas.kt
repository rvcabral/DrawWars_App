package com.example.drawwars.costumview

import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream


class DWCanvas(context: Context?, height:Int, width:Int ) : View(context) {
    private val path = Path()
    val brush = Paint()
    val displayMetrics = DisplayMetrics()

    private var bitmap: Bitmap?=null

    init {
        brush.isAntiAlias = false
        brush.color = Color.BLUE
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 10f
        bitmap = Bitmap.createBitmap(height, width,Bitmap.Config.ALPHA_8)

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
        canvas?.drawBitmap(bitmap, matrix, null)
    }

     fun getDraw():ByteArray{
        val stream = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return byteArray
    }
}