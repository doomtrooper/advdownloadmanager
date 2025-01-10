package com.anand.advancedownloadmanager.workers.downloader

import java.io.OutputStream

interface FileDownloader {

    suspend fun downloadAndSaveFile(
        fileUrl: String,
        outputStream: OutputStream,
    ): Boolean
}