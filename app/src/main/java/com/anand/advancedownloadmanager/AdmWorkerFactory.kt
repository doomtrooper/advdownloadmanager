package com.anand.advancedownloadmanager

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.anand.advancedownloadmanager.workers.FileDownloadWorker
import okhttp3.OkHttpClient

class AdmWorkerFactory(private val okHttpClientProvider: () -> OkHttpClient) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            FileDownloadWorker::class.java.name -> FileDownloadWorker(
                appContext,
                workerParameters,
                okHttpClientProvider.invoke()
            )

            else -> null
        }
    }
}