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
import android.graphics.drawable.Drawable
import android.media.MediaExtractor
import android.util.Base64
import java.io.ByteArrayOutputStream


class DWCanvas(context: Context?) : View(context) {
    private val path = Path()
    val brush = Paint()
    var mBitmap:Bitmap? = null
    var mCanvas : Canvas? = null
    init {
        brush.isAntiAlias = false
        brush.color = Color.BLUE
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 10f

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = Canvas(mBitmap)
        mCanvas!!.drawColor(Color.TRANSPARENT)
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
        canvas?.drawBitmap(mBitmap, 0F, 0F, brush);
        canvas?.drawPath(path, brush)
        mCanvas?.drawPath(path, brush)
    }

     fun getDraw():String{


         val stream = ByteArrayOutputStream()

         mBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
         val byteArray = stream.toByteArray()
         val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
         mCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
         return encodedImage
    }
}