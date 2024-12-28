package com.anand.advancedownloadmanager.redux

interface IReducer<S, A> {
    fun reduce(currentState: S, action: A): S
}

