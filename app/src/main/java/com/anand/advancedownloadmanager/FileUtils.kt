package com.anand.advancedownloadmanager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.util.Locale

object FileUtils {

    const val MIN_BYTES_PER_FILE_PART = 1000000L
    const val KEY_FILE_PROGRESS = "progress"
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

    fun getMaxThreads(contentLength: Long): Int {
        val maxThreads = 10
        val divisions: Int = contentLength.floorDiv(maxThreads).toInt()
        return if (divisions > maxThreads) maxThreads else divisions
    }

    fun adapter(keyValueMap: Map<String, Any>): FileDownloadUpdate {
        if (
            keyValueMap.containsKey(KEY_FILE_PROGRESS) &&
            keyValueMap.containsKey(KEY_FILE_DOWNLOAD_URL) &&
            keyValueMap.containsKey(KEY_FILE_PART_INDEX)
        ) {
            return FileDownloadUpdateProgress(
                percentCompleted = keyValueMap[KEY_FILE_PROGRESS] as Int,
                fileUrl = keyValueMap[KEY_FILE_DOWNLOAD_URL] as String,
                filePartIndex = keyValueMap[KEY_FILE_PART_INDEX] as Int
            )
        } else if (keyValueMap.containsKey(KEY_FILE_DOWNLOAD_URL) && keyValueMap.containsKey(
                KEY_FILE_TOTAL_PARTS
            )
        ) {
            return FileDownloadUpdatePartCount(
                totalParts = keyValueMap[KEY_FILE_TOTAL_PARTS] as Int,
                fileUrl = keyValueMap[KEY_FILE_DOWNLOAD_URL] as String,
            )
        }
        return FileDownloadUpdateUnSupported
    }
}