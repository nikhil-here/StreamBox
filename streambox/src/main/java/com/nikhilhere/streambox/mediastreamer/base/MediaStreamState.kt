package com.nikhilhere.streambox.mediastreamer.base

sealed class MediaStreamerState {
    data object IDLE : MediaStreamerState()
    data class Error(val exception: MediaStreamException) : MediaStreamerState()
}