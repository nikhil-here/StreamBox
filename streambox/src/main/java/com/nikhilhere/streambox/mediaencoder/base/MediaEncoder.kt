package com.nikhilhere.streambox.mediaencoder.base

import android.media.MediaFormat
import com.nikhilhere.streambox.mediasource.base.MediaSourceListener
import com.nikhilhere.streambox.mediasource.base.MediaSourcePreview
import java.util.concurrent.CopyOnWriteArrayList

interface MediaEncoderListener {
    fun onOutput(output: MediaEncoderOutput)
    fun onError(exception: MediaEncoderException)
    fun onState(state: MediaEncoderState)
}

class MediaEncoderOutput(val data: ByteArray) {

    override fun toString(): String {
        val previewLength = 16.coerceAtMost(data.size)
        val hexPreview = data.take(previewLength).joinToString(" ") { "%02X".format(it) }
        return "MediaEncoderOutput(size=${data.size}, hexPreview=[$hexPreview])"
    }
}

class MediaEncoderException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

sealed class MediaEncoderState {
    data object IDLE : MediaEncoderState()
    data object RUNNING : MediaEncoderState()
}

abstract class MediaSourcePreview

abstract class MediaEncoder {

    val listeners = CopyOnWriteArrayList<MediaEncoderListener>()

    fun addListener(listener: MediaEncoderListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: MediaEncoderListener) {
        listeners.remove(listener)
    }

    protected fun onOutput(output: MediaEncoderOutput) {
        listeners.forEach { it.onOutput(output) }
    }

    protected fun onError(exception: MediaEncoderException) {
        listeners.forEach { it.onError(exception) }
    }

    protected fun onState(state: MediaEncoderState) {
        listeners.forEach { it.onState(state) }
    }

    abstract fun initialize(
        format: MediaFormat,
        setSourceListener: (MediaSourceListener) -> Unit
    )

    abstract fun start()

    abstract fun stop()

}
