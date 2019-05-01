package com.example.drawwars

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.graphics.Canvas
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder

class ViewModelTest(application: Application) : AndroidViewModel(application)
 {

     val hubConnection = HubConnectionBuilder.create("http://localhost:55813/GetCoordinates").build()


}