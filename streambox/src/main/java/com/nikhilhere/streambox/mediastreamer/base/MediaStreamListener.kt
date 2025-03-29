package com.nikhilhere.streambox.mediastreamer.base

interface MediaStreamerListener {
    fun onState(state: MediaStreamerState)
}