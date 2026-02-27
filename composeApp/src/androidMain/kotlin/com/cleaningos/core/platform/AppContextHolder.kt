package com.cleaningos.core.platform

import android.content.Context

/** Singleton application context holder — initialized in Application.onCreate() */
object AppContextHolder {
    var appContext: Context? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
