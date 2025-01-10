package com.anand.advancedownloadmanager.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.work.WorkInfo
import com.anand.advancedownloadmanager.models.FileDownloadUpdate
import com.anand.advancedownloadmanager.models.FileDownloadUpdatePartCount
import com.anand.advancedownloadmanager.models.FileDownloadUpdateProgress
import com.anand.advancedownloadmanager.models.FileDownloadUpdateUnSupported
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.math.ceil

object FileUtils {

    const val MIN_BYTES_PER_FILE_PART = DEFAULT_BUFFER_SIZE * 1024 // 1Mb
    const val KEY_FILE_PROGRESS = "progress"
    const val KEY_FILE_WEIGHT = "weight"
    const val KEY_FILE_DOWNLOAD_URL = "fileDownloadUrl"
    const val KEY_FILE_PART_INDEX = "filePartIndex"
    const val KEY_FILE_TOTAL_PARTS = "fileTotalParts"


    fun getMimeType(uri: Uri, context: Context): String? {
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

    fun getMaxThreads(contentLength: Long, maxThreads: Int = 10): Pair<Int, Long> {
        val requiredThreads: Int =
            ceil(contentLength.toDouble().div(MIN_BYTES_PER_FILE_PART)).toInt()
        val threads = if (requiredThreads > maxThreads) maxThreads else requiredThreads
        val bytesPerThread = ceil((contentLength.toDouble()).div(threads)).toLong()
        return threads to bytesPerThread
    }

    fun adapter(keyValueMap: Map<String, Any?>, workInfo: WorkInfo): FileDownloadUpdate {
        if (
            keyValueMap.containsKey(KEY_FILE_PROGRESS) &&
            keyValueMap.containsKey(KEY_FILE_DOWNLOAD_URL) &&
            keyValueMap.containsKey(KEY_FILE_PART_INDEX) &&
            keyValueMap.containsKey(KEY_FILE_WEIGHT)
        ) {
            return FileDownloadUpdateProgress(
                percentCompleted = keyValueMap[KEY_FILE_PROGRESS] as Float,
                fileUrl = keyValueMap[KEY_FILE_DOWNLOAD_URL] as String,
                filePartIndex = keyValueMap[KEY_FILE_PART_INDEX] as Int,
                weight = keyValueMap[KEY_FILE_WEIGHT] as Float,
                workerId = workInfo.id
            )
        } else if (keyValueMap.containsKey(KEY_FILE_DOWNLOAD_URL) && keyValueMap.containsKey(
                KEY_FILE_TOTAL_PARTS
            )
        ) {
            return FileDownloadUpdatePartCount(
                totalParts = keyValueMap[KEY_FILE_TOTAL_PARTS] as Int,
                fileUrl = keyValueMap[KEY_FILE_DOWNLOAD_URL] as String,
                workerId = workInfo.id
            )
        }
        return FileDownloadUpdateUnSupported(workerId = workInfo.id, workInfo.state)
    }

    /**
     * Copies data from an input stream to an output stream, optionally reporting progress updates.
     *
     * This function reads data from the [input] stream in chunks and writes it to the [output] stream.
     * If [contentLength] is greater than 0(unknown), it calculates and reports progress updates using the
     * [progressUpdate] function.
     *
     * @param input The input stream to read data from.
     * @param output The output stream to write data to.
     * @param contentLength The total length of the content being copied, or 0 if unknown.
     * @param progressUpdate A function to be called with progress updates as a percentage (0.0f to 100.0f).
     * @throws IOException If an I/O error occurs during the copy operation.
     */
    @Throws(IOException::class)
    suspend fun copyInputStreamToOutputStream(
        input: InputStream,
        output: OutputStream,
        contentLength: Long = 0L,
        progressUpdate: suspend (Float) -> Unit,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        var lastPercentCompleted = 0.0f
        input.use { inputStream ->
            output.use { outputStream ->
                while ((inputStream.read(buffer).also { bytesRead = it }) > 0) {
                    if (contentLength > 0) {
                        totalBytesRead += bytesRead
                        val percentCompleted: Float = (100f * totalBytesRead) / contentLength
                        if (percentCompleted.minus(lastPercentCompleted) > 1.0) {
                            lastPercentCompleted = percentCompleted
                            progressUpdate(percentCompleted)
                        }
                    }
                    outputStream.write(buffer, 0, bytesRead)
                }
                progressUpdate(100.0f)
            }
        }
    }
}