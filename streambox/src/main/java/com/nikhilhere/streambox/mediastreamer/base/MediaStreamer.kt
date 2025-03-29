package com.nikhilhere.streambox.mediastreamer.base

import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList


abstract class MediaStreamer() {
    val listeners = CopyOnWriteArrayList<MediaStreamerListener>()
    private val _state = MutableStateFlow<MediaStreamerState>(MediaStreamerState.IDLE)
    private val state = _state.asStateFlow()

    fun addListener(listener: MediaStreamerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: MediaStreamerListener) {
        listeners.remove(listener)
    }

    protected fun onState(state: MediaStreamerState) {
        _state.update { state }
        listeners.forEach { it.onState(state) }
    }

    abstract fun initialize(
        streamInfo: MediaStreamInfo,
        setEncoderListener: (MediaEncoderListener) -> Unit
    )
}



