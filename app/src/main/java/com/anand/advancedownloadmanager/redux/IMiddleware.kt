package com.anand.advancedownloadmanager.redux

interface IMiddleware<A, S> {
    fun apply(action: A): S
}