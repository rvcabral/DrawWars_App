package com.example.drawwars

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.LiveData
import com.example.drawwars.services.ServerService.MyBinder
import android.content.ServiceConnection
import android.content.ComponentName
import android.os.IBinder
import android.arch.lifecycle.MutableLiveData

import android.util.Log
import com.example.drawwars.services.ServerService
import com.example.drawwars.utils.HandShakeResult


class ServiceViewModel : ViewModel() {
    private val TAG = "ServiceViewModel"
    private val mBinder = MutableLiveData<ServerService.MyBinder>()


    // Keeping this in here because it doesn't require a context
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Log.d(TAG, "ServiceConnection: connected to service.")
            // We've bound to MyService, cast the IBinder and get MyBinder instance
            val binder = iBinder as ServerService.MyBinder
            mBinder.postValue(binder)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "ServiceConnection: disconnected from service.")
            mBinder.postValue(null)
        }
    }


    fun getServiceConnection(): ServiceConnection {
            return serviceConnection
    }

    fun getBinder(): LiveData<ServerService.MyBinder> {
        return mBinder
    }



}