package com.nikhilhere.streambox.mediasource.base

import java.util.concurrent.CopyOnWriteArrayList


interface MediaSourceListener {
    fun onOutput(output: MediaSourceOutput)
    fun onError(exception: MediaSourceException)
    fun onState(state: MediaSourceState)
}

abstract class MediaSource() {
    val listeners = CopyOnWriteArrayList<MediaSourceListener>()

    fun addListener(listener: MediaSourceListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: MediaSourceListener) {
        listeners.remove(listener)
    }

    protected fun onOutput(output: MediaSourceOutput) {
        listeners.forEach { it.onOutput(output) }
    }

    protected fun onError(exception: MediaSourceException) {
        listeners.forEach { it.onError(exception) }
    }

    protected fun onState(state: MediaSourceState) {
        listeners.forEach { it.onState(state) }
    }

    abstract fun initialize()

    abstract fun start()

    abstract fun stop()

    abstract fun getPreview(): MediaSourcePreview?
}

abstract class MediaSourceOutput(val sourceTimeStamp : Long)

class MediaSourceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

sealed class MediaSourceState {
    data object IDLE : MediaSourceState()
    data object RUNNING : MediaSourceState()
}

abstract class MediaSourcePreview