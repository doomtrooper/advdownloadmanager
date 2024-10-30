package com.anand.advancedownloadmanager

import androidx.work.WorkInfo

data class File(
    val name: String,
    val url: String,
    var partCount: Int = 0,
    var status: WorkInfo.State = WorkInfo.State.ENQUEUED,
    var progress: MutableSet<FileDownloadProgress>? = mutableSetOf()
)

data class FileDownloadProgress(
    val partIndex: Int,
    val progress: Int = 0
)

sealed class FileDownloadUpdate(fileUrl: String)
data class FileDownloadUpdatePartCount(val totalParts: Int, val fileUrl: String) :
    FileDownloadUpdate(fileUrl)

data class FileDownloadUpdateProgress(
    val filePartIndex: Int,
    val percentCompleted: Int,
    val fileUrl: String
) : FileDownloadUpdate(fileUrl)

data object FileDownloadUpdateUnSupported : FileDownloadUpdate("")