package com.anand.advancedownloadmanager.redux

interface IMiddleware<S, A> {
    fun apply(getState: () -> S, dispatch: (A) -> Unit, action: A): A
}