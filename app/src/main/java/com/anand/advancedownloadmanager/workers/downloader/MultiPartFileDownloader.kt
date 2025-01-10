package com.anand.advancedownloadmanager.workers.downloader

import androidx.work.Data
import com.anand.advancedownloadmanager.utils.FileUtils
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_DOWNLOAD_URL
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_PART_INDEX
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_PROGRESS
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_TOTAL_PARTS
import com.anand.advancedownloadmanager.utils.FileUtils.KEY_FILE_WEIGHT
import com.anand.advancedownloadmanager.utils.FileUtils.getMaxThreads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.OutputStream


/**
 * A class responsible for downloading a file in multiple parts concurrently.
 * It utilizes coroutines for asynchronous operations and OkHttp for network requests.
 */
class MultiPartFileDownloader(
    private val cacheDir: String,
    private val contentLengthFromHeadResponse: Long,
    private val client: OkHttpClient,
    private val setProgress: suspend (Data) -> Unit
) : FileDownloader {

    /**
     * Downloads the file from the given URL in multiple parts and saves it to the output stream.
     *
     * @param fileUrl The URL of the file to download.
     * @param outputStream The output stream to write the downloaded file to.
     * @return True if the download and save operation is successful, false otherwise.
     * @throws IOException
     */
    @Throws(IOException::class, CancellationException::class)
    override suspend fun downloadAndSaveFile(fileUrl: String, outputStream: OutputStream): Boolean {
        // Calculate the number of threads and bytes per thread based on content length.
        val (maxThreads, bytesPerThread) = getMaxThreads(contentLengthFromHeadResponse)
        val files = mutableListOf<Deferred<File>>()
        setProgress(
            Data.Builder()
                .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                .putInt(KEY_FILE_TOTAL_PARTS, maxThreads)
                .build()
        )
        outputStream.use {
            coroutineScope {
                for (i in 0 until maxThreads) {
                    val deferredFile = async(Dispatchers.IO) {
                        val file = File("$cacheDir$i.temp")
                        val request: Request =
                            Request.Builder()
                                .url(fileUrl)
                                .addHeader("Range",
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
                        if (response.isSuccessful.not()){
                            println(response.code)
                            println(response.message)
                            println(response.body)
                        }
                        if (response.isSuccessful.not()) cancel(CancellationException("Download failed"))
                        response.body?.let {
                            val weight =
                                ((it.contentLength() * 100f / contentLengthFromHeadResponse))
                            FileUtils.copyInputStreamToOutputStream(
                                input = it.byteStream(),
                                output = file.outputStream(),
                                contentLength = it.contentLength(),
                                progressUpdate = { completedPercentage ->
                                    println("[FDM] index: $i weight: $weight  progress: $completedPercentage ")
                                    setProgress(
                                        Data.Builder()
                                            .putString(KEY_FILE_DOWNLOAD_URL, fileUrl)
                                            .putFloat(KEY_FILE_PROGRESS, completedPercentage)
                                            .putInt(KEY_FILE_PART_INDEX, i)
                                            .putFloat(KEY_FILE_WEIGHT, weight)
                                            .build()
                                    )
                                }
                            )
                        }
                        return@async file
                    }
                    files.add(deferredFile)
                }
                mergeFiles(files, outputStream)
            }
        }
        return true
    }

    /**
     * Merges the downloaded file parts from the deferred results into the output stream.
     *
     * @param deferredFiles A list of deferred results representing the downloaded file parts.
     * @param outputStream The output stream to write the merged file to.
     */
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

}