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
    private val reducers: List<IReducer<AdmHomeUiState, FileDownloadUpdate>>,
    private val middlewares: List<IMiddleware<AdmHomeUiState, FileDownloadUpdate>> = emptyList()
) : IStore<AdmHomeUiState, FileDownloadUpdate> {

    override fun getState(): AdmHomeUiState {
        return stateFlow.value
    }

    override fun subscribe(): StateFlow<AdmHomeUiState> {
        return stateFlow.asStateFlow()
    }

    override fun dispatch(action: FileDownloadUpdate) {
        loggingMiddleware(::getState, ::dispatch)
        val newAction = middlewares.fold(action) { a1, middleware ->
            middleware.apply(
                ::getState,
                ::dispatch,
                a1
            )
        }
        for (reducer in reducers) {
            stateFlow.update { reducer.reduce(it, newAction) }
        }
    }

}

fun <S, A> loggingMiddleware(getState: () -> S, dispatch: (A) -> Unit): (((A) -> S) -> (A) -> S) {
    return fun(next: (A) -> S): (A) -> S {
        return fun(action: A): S {
            // Do anything here: pass the action onwards with next(action),
            // or restart the pipeline with storeAPI.dispatch(action)
            // Can also use storeAPI.getState() here
            println("dispatching $action")
            val nextState = next(action)
            println("nextState: $nextState")
            return nextState
        }
    }
}

typealias MiddlewareType<S, A> = (A) -> S