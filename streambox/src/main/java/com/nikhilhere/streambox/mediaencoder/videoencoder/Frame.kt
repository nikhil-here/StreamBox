package com.nikhilhere.streambox.mediaencoder.videoencoder

import com.nikhilhere.streambox.mediasource.base.MediaSourceOutput
import com.nikhilhere.streambox.mediasource.camerasource.CameraSourceOutput

data class Frame(
    val format: Int,
    val width : Int,
    val height : Int,
    val timestamp: Long,
    val byteArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Frame

        if (format != other.format) return false
        if (timestamp != other.timestamp) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}


fun MediaSourceOutput.toFrame() : Frame? {
    return when(this) {
        is CameraSourceOutput -> {
            Frame(
                format = this.format,
                width = this.width,
                height = this.height,
                timestamp = this.timestamp,
                byteArray = this.data
            )
        }

        else -> null
    }
}
