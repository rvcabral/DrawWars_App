package com.example.drawwars

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.graphics.Bitmap
import com.example.drawwars.costumview.DWCanvas

class DWCanvasViewModel : ViewModel(){

    private val canvas = MutableLiveData<DWCanvas> ()

    fun init(context:Context){
        canvas.value = DWCanvas(context)
    }

    fun getCanvas(): MutableLiveData<DWCanvas> = canvas


}