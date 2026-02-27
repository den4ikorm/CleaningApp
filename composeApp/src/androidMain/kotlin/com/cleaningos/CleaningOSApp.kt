package com.cleaningos

import android.app.Application
import com.cleaningos.core.di.androidModule
import com.cleaningos.core.di.commonModule
import com.cleaningos.core.platform.AppContextHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CleaningOSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)
        startKoin {
            androidContext(this@CleaningOSApp)
            modules(commonModule, androidModule)
        }
    }
}
