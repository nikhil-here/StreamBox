package com.nikhilhere.streambox.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderException
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderListener
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderOutput
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderState
import com.nikhilhere.streambox.mediaencoder.videoencoder.VideoEncoder
import com.nikhilhere.streambox.mediasource.base.MediaSourceException
import com.nikhilhere.streambox.mediasource.base.MediaSourceListener
import com.nikhilhere.streambox.mediasource.base.MediaSourceOutput
import com.nikhilhere.streambox.mediasource.base.MediaSourceState
import com.nikhilhere.streambox.mediasource.camerasource.CameraSource
import com.nikhilhere.streambox.mediastreamer.base.MediaStreamInfo
import com.nikhilhere.streambox.mediastreamer.rtmpstreamer.RtmpStreamer

private const val TAG = "MainScreen"

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraSource = remember {
        CameraSource(
            context = context,
            lifecycleOwner = lifecycleOwner
        )
    }

    val videoEncoder = remember {
        VideoEncoder()
    }

    val rtmpStreamer = remember {
        RtmpStreamer()
    }

    LaunchedEffect(true) {
        cameraSource.initialize()
        videoEncoder.initialize(
            format = VideoEncoder.getDefaultFormat(),
            setSourceListener = {
                cameraSource.addListener(it)
            }
        )
        rtmpStreamer.initialize(
            streamInfo = MediaStreamInfo(
                host = "192.168.0.105",
                port = 1935
            ),
            setEncoderListener = {
                videoEncoder.addListener(it)
            }
        )
    }

    //For Debugging
    LaunchedEffect(true) {
        cameraSource.addListener(object : MediaSourceListener {
            override fun onOutput(output: MediaSourceOutput) {
                Log.i(TAG, "cameraSource onOutput: $output ")
            }

            override fun onError(exception: MediaSourceException) {
                Log.e(TAG, "cameraSource onError: $exception", exception)
            }

            override fun onState(state: MediaSourceState) {
                Log.i(TAG, "cameraSource onState: $state")
            }
        })

        videoEncoder.addListener(object : MediaEncoderListener {
            override fun onOutput(output: MediaEncoderOutput) {
                Log.i(TAG, "videoEncoder onOutput: $output")
            }

            override fun onError(exception: MediaEncoderException) {
                Log.e(TAG, "videoEncoder onError: $exception", exception)
            }

            override fun onState(state: MediaEncoderState) {
                Log.i(TAG, "videoEncoder onState: $state")
            }
        })
    }

    cameraSource.getPreview()?.preview?.let { preview ->
        AndroidView(
            factory = { preview },
            modifier = modifier
        )
    }

}