package com.anand.advancedownloadmanager.workers

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.anand.advancedownloadmanager.utils.FileParams
import com.anand.advancedownloadmanager.utils.FileUtils
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_DOWNLOAD_URL
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_PART_INDEX
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_PROGRESS
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_TOTAL_PARTS
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_WEIGHT
import com.anand.advancedownloadmanager.utils.FileUtils.getMaxThreads
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
            val mimeType = FileUtils.getMimeType(fileUrl.toUri(), context)
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
            val fileOutputStream =
                withContext(Dispatchers.IO) {
                    FileOutputStream(target)
                }
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
        println("contentLengthFromHeadResponse: $contentLengthFromHeadResponse")
        if (contentLengthFromHeadResponse > 0L && acceptRange != "none") {
            println("contentLengthFromHeadResponse $contentLengthFromHeadResponse")
            val (maxThreads, bytesPerThread) = getMaxThreads(contentLengthFromHeadResponse)
            val files = mutableListOf<Deferred<File>>()
            setProgress(
                Data.Builder()
                    .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                    .putInt(KEY_FILE_TOTAL_PARTS, maxThreads)
                    .build()
            )
            outputStream.use {
                withContext(Dispatchers.IO) {
                    for (i in 0 until maxThreads) {
                        val fileDeferred = async {
                            val file = File("$cacheDir$fileName$i.temp")
                            val request: Request =
                                Request.Builder().url(fileUrl).addHeader("Range",
                                    buildString {
                                        append("bytes=")
                                        append(bytesPerThread.times(i))
                                        append("-")
                                        if (i != maxThreads - 1) {
                                            append(bytesPerThread.times(i.inc()).dec())
                                        }
                                    })
                                    .build()
                            val response: Response = client.newCall(request).execute()
                            println("downloadAndSaveFileOkHttp: $i api started")
                            response.body?.let {
                                copyInputStreamToOutputStream(
                                    input = it.byteStream(),
                                    output = file.outputStream(),
                                    fileUrl = fileUrl,
                                    contentLength = it.contentLength(),
                                    filePartIndex = i,
                                    fileTotalContentLength = contentLengthFromHeadResponse
                                )
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
            setProgress(
                Data.Builder()
                    .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                    .putInt(KEY_FILE_TOTAL_PARTS, 1)
                    .build()
            )
            val request: Request = Request.Builder().url(fileUrl).build()
            val response: Response = client.newCall(request).execute()
            val contentLength = response.body?.contentLength() ?: 0L
            response.body?.byteStream()?.let {
                outputStream?.let { out ->
                    copyInputStreamToOutputStream(
                        input = it,
                        output = out,
                        fileUrl = fileUrl,
                        contentLength = contentLength,
                        fileTotalContentLength = contentLength
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
        fileUrl: String,
        contentLength: Long = 0L,
        filePartIndex: Int = 0,
        fileTotalContentLength: Long,
    ) {
        println("okhttp contentLength: $contentLength")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        var lastPercentCompleted = 0.0f
        input.use { inputStream ->
            output.use { outputStream ->
                val weight = ((contentLength * 100f / fileTotalContentLength))
                while ((inputStream.read(buffer).also { bytesRead = it }) > 0) {
                    if (contentLength > 0) {
                        totalBytesRead += bytesRead
                        val percentCompleted: Float = (100f * totalBytesRead) / contentLength
                        if (weight > 0.0f && percentCompleted.minus(lastPercentCompleted) > 1.0) {
                            lastPercentCompleted = percentCompleted
                            println("[FDM] index: $filePartIndex weight: $weight  progress: $percentCompleted ")
                            setProgress(
                                Data.Builder()
                                    .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                                    .putFloat(KEY_FILE_PROGRESS, percentCompleted)
                                    .putInt(KEY_FILE_PART_INDEX, filePartIndex)
                                    .putFloat(KEY_FILE_WEIGHT, weight)
                                    .build()
                            )
                        }
                    }
                    outputStream.write(buffer, 0, bytesRead)
                }
                println("[FDM] index: $filePartIndex progress: completed")
                setProgress(
                    Data.Builder()
                        .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                        .putFloat(KEY_FILE_PROGRESS, 100.0f)
                        .putInt(KEY_FILE_PART_INDEX, filePartIndex)
                        .putFloat(KEY_FILE_WEIGHT, weight)
                        .build()
                )
            }
        }
    }
}