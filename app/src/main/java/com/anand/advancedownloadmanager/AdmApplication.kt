package com.anand.advancedownloadmanager

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import okhttp3.OkHttpClient

class AdmApplication : Application(), Configuration.Provider {

    private val okHttpClientProvider: () -> OkHttpClient = { OkHttpClient.Builder().build() }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(AdmWorkerFactory(okHttpClientProvider))
            .build()


    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
    }
}

