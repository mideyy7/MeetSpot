package com.meetspot.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.meetspot.app.data.AppContainer

class MeetSpotApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        container = AppContainer(this)
    }
}
