package com.anand.advancedownloadmanager.redux

import com.anand.advancedownloadmanager.models.FileDownloadUpdate
import com.anand.advancedownloadmanager.viewmodels.AdmHomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface IStore<S, A> {
    fun getState(): S
    fun dispatch(action: A)
    fun subscribe(): StateFlow<S>
}


class AdmStore(
    private var stateFlow: MutableStateFlow<AdmHomeUiState>,
    private val reducers: List<IReducer<AdmHomeUiState, FileDownloadUpdate>>
) : IStore<AdmHomeUiState, FileDownloadUpdate> {

    override fun getState(): AdmHomeUiState {
        return stateFlow.value
    }

    override fun subscribe(): StateFlow<AdmHomeUiState> {
        return stateFlow.asStateFlow()
    }

    override fun dispatch(action: FileDownloadUpdate) {
        for (reducer in reducers) {
            stateFlow.update { reducer.reduce(it, action) }
        }
    }

}