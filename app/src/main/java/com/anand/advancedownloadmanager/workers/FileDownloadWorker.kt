package com.anand.advancedownloadmanager.workers

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anand.advancedownloadmanager.utils.FileParams
import com.anand.advancedownloadmanager.utils.FileUtils
import com.anand.advancedownloadmanager.workers.downloader.FileDownloader
import com.anand.advancedownloadmanager.workers.downloader.MultiPartFileDownloader
import com.anand.advancedownloadmanager.workers.downloader.SinglePartFileDownloader
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class FileDownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString(FileParams.KEY_FILE_URL) ?: ""
        val fileName = inputData.getString(FileParams.KEY_FILE_NAME) ?: ""

        Log.d("FileDownloadWorker", "doWork: $fileUrl | $fileName")

        if (fileName.isEmpty() || fileUrl.isEmpty()) {
            Result.failure()
        }
        return getSavedFileUri(fileName = fileName, fileUrl = fileUrl, context = context)
    }

    private suspend fun getSavedFileUri(
        fileName: String,
        fileUrl: String,
        context: Context
    ): Result {
        val outputStream: OutputStream? = outputStream(fileUrl, context, fileName)
        return outputStream?.let {
            try {
                return@let if (downloadAndSaveFile(
                        fileUrl,
                        it,
                        context.codeCacheDir.path
                    )
                ) Result.success() else Result.failure()
            } catch (ex: IOException) {
                Result.failure()
            } catch (exp: CancellationException) {
                Result.failure()
            }
        } ?: Result.failure()

    }

    private fun outputStream(
        fileUrl: String,
        context: Context,
        fileName: String
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getOutputStream(fileUrl, context, fileName)
    } else {
        getOutputStream(fileName)
    }

    private fun getOutputStream(fileName: String): FileOutputStream {
        val target = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        return target.outputStream()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getOutputStream(
        fileUrl: String,
        context: Context,
        fileName: String
    ): OutputStream? {
        val mimeType = FileUtils.getMimeType(fileUrl.toUri(), context)
        println("mimeType: $mimeType")
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/DownloaderDemo")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        val outputStream = uri?.let { resolver.openOutputStream(it) }
        return outputStream
    }

    @Throws(IOException::class, CancellationException::class)
    private suspend fun downloadAndSaveFile(
        fileUrl: String,
        outputStream: OutputStream,
        cacheDir: String
    ): Boolean {
        val headRequest: Request = Request.Builder().url(fileUrl).head().build()
        val headResponse: Response = okHttpClient.newCall(headRequest).execute()

        val contentLengthFromHeadResponse =
            headResponse.headers["Content-Length"]?.toLongOrNull() ?: 0L
        val acceptRange = headResponse.headers["Accept-Ranges"]
        println("contentLengthFromHeadResponse: $contentLengthFromHeadResponse")
        val fileDownloader: FileDownloader
        if (contentLengthFromHeadResponse > 0L && acceptRange != "none") {
            println("contentLengthFromHeadResponse $contentLengthFromHeadResponse")
            fileDownloader = MultiPartFileDownloader(
                cacheDir,
                contentLengthFromHeadResponse,
                okHttpClient,
                setProgress = { setProgress(it) }
            )
            println("done")
        } else {
            fileDownloader = SinglePartFileDownloader(okHttpClient, setProgress = { setProgress(it) })
        }
        return fileDownloader.downloadAndSaveFile(fileUrl, outputStream)
    }

}