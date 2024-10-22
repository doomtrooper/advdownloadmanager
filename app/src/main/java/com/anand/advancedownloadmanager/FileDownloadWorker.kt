package com.anand.advancedownloadmanager

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.util.Collections
import java.util.Locale


class FileDownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString(FileParams.KEY_FILE_URL) ?: ""
        val fileName = inputData.getString(FileParams.KEY_FILE_NAME) ?: ""

        Log.d("FileDownloadWorker", "doWork: $fileUrl | $fileName")

        if (fileName.isEmpty() || fileUrl.isEmpty()) {
            Result.failure()
        }
        val uri = getSavedFileUri(
            fileName = fileName,
            fileUrl = fileUrl,
            context = context,
        )
        return if (uri != null) {
            Result.success(workDataOf(FileParams.KEY_FILE_URI to uri.toString()))
        } else {
            Result.failure()
        }

    }

    private suspend fun getSavedFileUri(
        fileName: String,
        fileUrl: String,
        context: Context
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mimeType = getMimeType(fileUrl.toUri())
            println("mimeType: $mimeType")
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/DownloaderDemo")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            return if (uri != null) {
                val outputStream = resolver.openOutputStream(uri)
                downloadAndSaveFileOkHttp(fileUrl, outputStream)
                uri
            } else {
                null
            }
        } else {
            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            val fileOutputStream = FileOutputStream(target)
            downloadAndSaveFileOkHttp(fileUrl, fileOutputStream)
            return target.toUri()
        }
    }

    private suspend fun downloadAndSaveFileOkHttp(
        fileUrl: String,
        outputStream: OutputStream?
    ) {
        val client = OkHttpClient.Builder().build()
        val headRequest: Request = Request.Builder().url(fileUrl).head().build()
        val headResponse: Response = client.newCall(headRequest).execute()

        val contentLengthFromHeadResponse =
            headResponse.headers["Content-Length"]?.toLongOrNull() ?: 0L
        val acceptRange = headResponse.headers["Accept-Ranges"]

        if (contentLengthFromHeadResponse > 0L && acceptRange != "none") {
            println("contentLengthFromHeadResponse $contentLengthFromHeadResponse")
            val maxThreads = getMaxThreads(contentLengthFromHeadResponse)
            val inputStreams = mutableListOf<InputStream>()
            var contentLength = 0L
            outputStream.use {
                withContext(Dispatchers.IO) {
                    for (i in 0 until maxThreads) {
                        val job = launch {
                            val request: Request =
                                Request.Builder().url(fileUrl).addHeader("Range",
                                    buildString {
                                        append("bytes=")
                                        append(i * minBytePart)
                                        append("-")
                                        if (i != maxThreads - 1) append(((i + 1) * minBytePart) - 1)
                                    })
                                    .build()
                            val response: Response = client.newCall(request).execute()
                            response.body?.let {
                                contentLength += it.contentLength()
                                inputStreams.add(it.byteStream())
                            }
                            println("downloadAndSaveFileOkHttp: $i api started")
                        }
                        job.join()

                    }
                    val sequenceInputStream =
                        SequenceInputStream(Collections.enumeration(inputStreams))
                    outputStream?.let { it1 ->
                        copyInputStreamToOutputStream(sequenceInputStream,
                            it1, contentLength
                        )
                    }
                }
            }
            println("done")
        } else {
            val request: Request = Request.Builder().url(fileUrl).build()
            val response: Response = client.newCall(request).execute()
            copyResponseToOutputStream(response, outputStream)
        }
    }

    private suspend fun FileDownloadWorker.copyResponseToOutputStream(
        response: Response,
        outputStream: OutputStream?
    ) {
        response.body?.let { responseBody ->
            val contentLength = responseBody.contentLength()
            println("okhttp contentLength: $contentLength")

            //here’s the download code
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var len1: Int
            var total: Long = 0
            outputStream.let { out ->
                responseBody.byteStream().use { inputStream ->
                    inputStream.let {
                        while ((inputStream.read(buffer).also { len1 = it }) > 0) {
                            if (contentLength > 0) {
                                total += len1
                                val percentCompleted = ((total * 100 / contentLength)).toInt()
//                                println("percentCompleted: $percentCompleted")
                                setProgress(
                                    Data.Builder().putInt("progress", percentCompleted).build()
                                )
                            }
//                            out?.bufferedWriter()
                            out?.write(buffer)
                        }
                    }
                }
            }
        }
    }

    private suspend fun copyInputStreamToOutputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        contentLength: Long= 0L
    ) {
        println("okhttp contentLength: $contentLength")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        outputStream.use { out ->
            while ((inputStream.read(buffer).also { bytesRead = it }) > 0) {
                if (contentLength > 0) {
                    totalBytesRead += bytesRead
                    val percentCompleted = ((totalBytesRead * 100 / contentLength)).toInt()
                    setProgress(
                        Data.Builder().putInt("progress", percentCompleted).build()
                    )
                }
                out.write(buffer, 0, bytesRead)
            }
        }
    }


    private val minBytePart = 100000L

    private fun getMaxThreads(abcd: Long): Int {
        val maxThreads = 10
        val divisions: Int = abcd.floorDiv(maxThreads).toInt()
        return if (divisions > maxThreads) maxThreads else divisions
    }

    private fun getMimeType(uri: Uri): String? {
        val mimeType: String?
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            val cr: ContentResolver = context.contentResolver
            mimeType = cr.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.lowercase(Locale.getDefault())
            )
        }
        return mimeType
    }
}