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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
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
                downloadAndSaveFileOkHttp(
                    fileUrl,
                    outputStream,
                    fileName,
                    context.codeCacheDir.path
                )
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
            downloadAndSaveFileOkHttp(
                fileUrl,
                fileOutputStream,
                fileName,
                context.codeCacheDir.path
            )
            return target.toUri()
        }
    }

    private suspend fun downloadAndSaveFileOkHttp(
        fileUrl: String,
        outputStream: OutputStream?,
        fileName: String,
        cacheDir: String? = null
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
            val files = mutableListOf<Deferred<File>>()
            outputStream.use {
                withContext(Dispatchers.IO) {
                    for (i in 0 until maxThreads) {
                        val fileDeferred = async {
                            val file = File("$cacheDir$fileName$i.temp")
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
                            println("downloadAndSaveFileOkHttp: $i api started")
                            response.body?.let {
                                copyInputStreamToOutputStream(it.byteStream(), file.outputStream())
                            }
                            return@async file
                        }
                        files.add(fileDeferred)
                    }
                    outputStream?.let { output -> mergeFiles(files, output) }
                }
            }
            println("done")
        } else {
            val request: Request = Request.Builder().url(fileUrl).build()
            val response: Response = client.newCall(request).execute()
            val contentLength = response.body?.contentLength() ?: 0L
            response.body?.byteStream()?.let {
                outputStream?.let { out ->
                    copyInputStreamToOutputStream(
                        it,
                        out,
                        contentLength
                    )
                }
            }
        }
    }

    private suspend fun mergeFiles(
        deferredFiles: MutableList<Deferred<File>>,
        outputStream: OutputStream
    ) {
        outputStream.use { out ->
            deferredFiles.forEach { fileDeferred ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                fileDeferred.await().inputStream().use { inputStream ->
                    while ((inputStream.read(buffer).also { bytesRead = it }) > 0) {
                        out.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }

    private suspend fun copyInputStreamToOutputStream(
        input: InputStream,
        output: OutputStream,
        contentLength: Long = 0L
    ) {
        println("okhttp contentLength: $contentLength")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        input.use { inputStream ->
            output.use { outputStream ->
                while ((inputStream.read(buffer).also { bytesRead = it }) > 0) {
                    if (contentLength > 0) {
                        totalBytesRead += bytesRead
                        val percentCompleted = ((totalBytesRead * 100 / contentLength)).toInt()
                        setProgress(
                            Data.Builder().putInt("progress", percentCompleted).build()
                        )
                    }
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
    }


    private val minBytePart = 1000000L

    private fun getMaxThreads(contentLength: Long): Int {
        val maxThreads = 10
        val divisions: Int = contentLength.floorDiv(maxThreads).toInt()
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