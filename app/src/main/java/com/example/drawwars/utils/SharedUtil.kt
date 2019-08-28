package com.example.drawwars.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import com.example.drawwars.MainActivity

class SharedUtil {
    companion object {
        fun WifiDisabled(context: Context){
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)

            builder.setMessage("You have been disconnected.").setTitle("Lost internet connection");
            builder.setPositiveButton("Ok", DialogInterface.OnClickListener{ ialog, which -> run {
                (context as Activity).finish()
                var intent = Intent(context, MainActivity::class.java)
                startActivity(context.applicationContext, intent, null)
            } })
        }
    }
}