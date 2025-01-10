package com.anand.advancedownloadmanager.workers.downloader

import androidx.work.Data
import com.anand.advancedownloadmanager.utils.FileUtils
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_DOWNLOAD_URL
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_PART_INDEX
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_PROGRESS
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_TOTAL_PARTS
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_WEIGHT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.OutputStream

class SinglePartFileDownloader(
    private val client: OkHttpClient,
    private val setProgress: suspend (Data) -> Unit
) : FileDownloader {

    @Throws(IOException::class, CancellationException::class)
    override suspend fun downloadAndSaveFile(fileUrl: String, outputStream: OutputStream): Boolean {
        setProgress(
            Data.Builder()
                .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                .putInt(KEY_FILE_TOTAL_PARTS, 1)
                .build()
        )
        val request: Request = Request.Builder().url(fileUrl).build()
        withContext(Dispatchers.IO) {
            val response: Response = client.newCall(request).execute()
            val contentLength = response.body?.contentLength() ?: 0L
            if (response.isSuccessful.not()){
                println(response.code)
                println(response.message)
                println(response.body)
            }
            if (response.isSuccessful.not()) cancel(CancellationException("Download failed"))
            response.body?.let {
                FileUtils.copyInputStreamToOutputStream(
                    input = it.byteStream(),
                    output = outputStream,
                    contentLength = contentLength
                ) { completedPercentage ->
                    setProgress(
                        Data.Builder()
                            .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                            .putFloat(KEY_FILE_PROGRESS, completedPercentage)
                            .putInt(KEY_FILE_PART_INDEX, 0)
                            .putFloat(KEY_FILE_WEIGHT, 100.0f)
                            .build()
                    )
                }
            }
        }
        return true
    }
}