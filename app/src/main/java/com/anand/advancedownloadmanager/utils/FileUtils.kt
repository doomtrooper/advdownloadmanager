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

    fun adapter(keyValueMap: Map<String, Any>, workInfo: WorkInfo): FileDownloadUpdate {
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
}