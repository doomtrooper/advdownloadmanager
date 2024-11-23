package com.anand.advancedownloadmanager.viewmodels

import com.anand.advancedownloadmanager.models.FileDownloadUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface IStore<S, A> {
    fun getState(): S
    fun dispatch(action: A)
}


class AdmStore(
    private var stateFlow: MutableStateFlow<AdmHomeUiState>,
    private val reducers: List<IReducer<AdmHomeUiState, FileDownloadUpdate>>
) : IStore<AdmHomeUiState, FileDownloadUpdate> {

    override fun getState(): AdmHomeUiState {
        return stateFlow.value
    }

    override fun dispatch(action: FileDownloadUpdate) {
        for (reducer in reducers) {
            stateFlow.update { reducer.reduce(it, action) }
        }
    }

}