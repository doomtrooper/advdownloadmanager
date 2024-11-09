package com.anand.advancedownloadmanager

import androidx.work.WorkInfo
import java.util.UUID

data class File(
    val name: String,
    val url: String,
    val partCount: Int = 1,
    val workerId: UUID? = null,
    val status: WorkInfo.State = WorkInfo.State.ENQUEUED,
    val progress: List<FileDownloadProgress> = listOf(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

data class FileDownloadProgress(
    val partIndex: Int,
    val progress: Float = 0.0f,
    val partWeight: Float = 100.0f
)

sealed class FileDownloadUpdate {
    abstract val workerId: UUID
}

data class FileDownloadUpdatePartCount(
    val totalParts: Int,
    val fileUrl: String,
    override val workerId: UUID
) :
    FileDownloadUpdate()

data class FileDownloadUpdateProgress(
    val filePartIndex: Int,
    val percentCompleted: Float,
    val fileUrl: String,
    val weight: Float,
    override val workerId: UUID
) : FileDownloadUpdate()

data class FileDownloadUpdateUnSupported(override val workerId: UUID, val state: WorkInfo.State) : FileDownloadUpdate()
