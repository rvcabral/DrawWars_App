package com.example.drawwars.services

import com.example.drawwars.ServiceViewModel

interface ServiceListener {

    fun AckSession()
    fun AckNickname()
    fun NonExistingSession()
    fun DrawThemes(themes:List<String>)

}