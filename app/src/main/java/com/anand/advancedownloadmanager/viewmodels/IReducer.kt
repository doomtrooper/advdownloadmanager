package com.anand.advancedownloadmanager.viewmodels

import com.anand.advancedownloadmanager.models.FileDownloadProgress
import com.anand.advancedownloadmanager.models.FileDownloadUpdate
import com.anand.advancedownloadmanager.models.FileDownloadUpdatePartCount
import com.anand.advancedownloadmanager.models.FileDownloadUpdateProgress
import com.anand.advancedownloadmanager.models.FileDownloadUpdateUnSupported

interface IReducer<S, A> {
    fun reduce(currentState: S, action: A): S
}

object FileDownloadUpdateUnSupportedReducer : IReducer<AdmHomeUiState, FileDownloadUpdate> {
    override fun reduce(currentState: AdmHomeUiState, action: FileDownloadUpdate): AdmHomeUiState {
        if (action is FileDownloadUpdateUnSupported) {
            val filteredList = currentState.files
                .toMutableList()
                .filter { it.workerId != action.workerId }
            val matchedFile = currentState.files
                .find { it.workerId == action.workerId }
                ?.copy(status = action.state)
            val files = buildList {
                addAll(filteredList)
                matchedFile?.let { add(it) }
            }
            return currentState.copy(
                files = files.sortedBy { it.startTime }
            )
        }
        return currentState
    }

}

object FileDownloadUpdatePartCountReducer : IReducer<AdmHomeUiState, FileDownloadUpdate> {
    override fun reduce(currentState: AdmHomeUiState, action: FileDownloadUpdate): AdmHomeUiState {
        if (action is FileDownloadUpdatePartCount) {
            val filteredList = currentState.files.toMutableList()
                .filter { it.workerId != action.workerId }
            val matchedFile = currentState.files
                .find { it.workerId == action.workerId }
                ?.copy(partCount = action.totalParts)
            val files = buildList {
                addAll(filteredList)
                matchedFile?.let { add(it) }
            }
            return currentState.copy(
                files = files.sortedBy { it.startTime }
            )
        }
        return currentState
    }
}

object FileDownloadUpdateProgressReducer : IReducer<AdmHomeUiState, FileDownloadUpdate> {
    override fun reduce(currentState: AdmHomeUiState, action: FileDownloadUpdate): AdmHomeUiState {
        if (action is FileDownloadUpdateProgress) {
            val filteredList = currentState.files
                .toMutableList()
                .filter { it.workerId != action.workerId }
            val matchedFile = currentState.files
                .find { it.workerId == action.workerId }
                ?.let {
                    val fileDownloadProgress = FileDownloadProgress(
                        action.filePartIndex,
                        action.percentCompleted,
                        action.weight
                    )
                    val filterProgress = it.progress.toList()
                        .filter { p -> p.partIndex != fileDownloadProgress.partIndex }
                    val buildList = buildList {
                        addAll(filterProgress)
                        add(fileDownloadProgress)
                    }
                    it.copy(progress = buildList)
                }
            val files = buildList {
                addAll(filteredList)
                matchedFile?.let {
                    add(it)
                }
            }
            return currentState.copy(
                files = files.sortedBy { it.startTime }
            )
        }
        return currentState
    }
}
