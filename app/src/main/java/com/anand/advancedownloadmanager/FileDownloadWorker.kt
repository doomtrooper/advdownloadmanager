package com.anand.advancedownloadmanager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class FileDownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString(FileParams.KEY_FILE_URL) ?: ""
        val fileName = inputData.getString(FileParams.KEY_FILE_NAME) ?: ""
        val fileType = inputData.getString(FileParams.KEY_FILE_TYPE) ?: ""

        Log.d("FileDownloadWorker", "doWork: $fileUrl | $fileName | $fileType")


        if (fileName.isEmpty()
            || fileType.isEmpty()
            || fileUrl.isEmpty()
        ){
            Result.failure()
        }

        val uri = getSavedFileUri(
            fileName = fileName,
            fileType = fileType,
            fileUrl = fileUrl,
            context = context
        )

        return if (uri != null){
            Result.success(workDataOf(FileParams.KEY_FILE_URI to uri.toString()))
        }else{
            Result.failure()
        }

    }

    private fun getSavedFileUri(
        fileName: String,
        fileType: String,
        fileUrl: String,
        context: Context
    ): Uri? {
        val mimeType = when (fileType) {
            "PDF" -> "application/pdf"
            "PNG" -> "image/png"
            "MP4" -> "video/mp4"
            else -> ""
        } // different types of files will have different mime type

        if (mimeType.isEmpty()) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/DownloaderDemo")
            }

            val resolver = context.contentResolver

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            return if (uri != null) {
                URL(fileUrl).openStream().use { input ->
                    resolver.openOutputStream(uri).use { output ->
                        input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                    }
                }
                uri
            } else {
                null
            }

        } else {
            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            URL(fileUrl).openStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }

            return target.toUri()
        }
    }
}