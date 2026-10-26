package com.occamzchainsaw.straightscorer

import android.app.Application

class StraightScorerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
