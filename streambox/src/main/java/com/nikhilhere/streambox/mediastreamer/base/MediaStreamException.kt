package com.nikhilhere.streambox.mediastreamer.base

class MediaStreamException(
    message: String?,
    cause: Throwable? = null
) : Exception(message, cause)