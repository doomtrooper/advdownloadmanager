package com.anand.advancedownloadmanager

import androidx.work.WorkInfo

data class File(
    val name:String,
    val url:String,
    val status: WorkInfo.State = WorkInfo.State.ENQUEUED,
    val progress: Int = 0
)