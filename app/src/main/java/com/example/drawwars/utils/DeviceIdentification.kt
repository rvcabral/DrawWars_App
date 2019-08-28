package com.example.drawwars.utils

import android.content.Context
import com.example.drawwars.R
import java.util.*

class DeviceIdentification {
    companion object {
        private var uniqueID: String? = null
        @Synchronized
        fun DeviceID(context: Context): String {
            if (uniqueID == null) {
                val sharedPrefs = context.getSharedPreferences(
                    context.getString(R.string.DeviceID), Context.MODE_PRIVATE
                )
                uniqueID = sharedPrefs.getString(context.getString(R.string.DeviceID), null)
                if (uniqueID == null) {
                    uniqueID = UUID.randomUUID().toString()
                    val editor = sharedPrefs.edit()
                    editor.putString(context.getString(R.string.DeviceID), uniqueID)
                    editor.commit()
                }
            }
            return uniqueID as String
        }
    }
}