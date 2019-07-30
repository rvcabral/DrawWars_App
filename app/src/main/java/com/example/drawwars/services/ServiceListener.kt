package com.example.drawwars.services

import com.example.drawwars.ServiceViewModel
import java.util.*

interface ServiceListener {

    fun Interaction(action:String, param:Any?)
}