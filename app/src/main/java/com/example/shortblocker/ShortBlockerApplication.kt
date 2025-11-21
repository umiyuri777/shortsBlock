package com.example.shortblocker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShortBlockerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
