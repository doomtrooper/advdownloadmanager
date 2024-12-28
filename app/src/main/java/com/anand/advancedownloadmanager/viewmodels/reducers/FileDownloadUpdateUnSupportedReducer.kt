package com.anand.advancedownloadmanager.viewmodels.reducers

import com.anand.advancedownloadmanager.models.FileDownloadUpdate
import com.anand.advancedownloadmanager.models.FileDownloadUpdateUnSupported
import com.anand.advancedownloadmanager.redux.IReducer
import com.anand.advancedownloadmanager.viewmodels.AdmHomeUiState

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