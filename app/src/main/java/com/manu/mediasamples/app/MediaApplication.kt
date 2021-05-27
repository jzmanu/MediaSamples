package com.manu.mediasamples.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * @Desc: App
 * @Author: jzman
 */
class MediaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
    }

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}