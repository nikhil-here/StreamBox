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
import com.nikhilhere.streambox.mediasource.base.MediaSourceException
import com.nikhilhere.streambox.mediasource.base.MediaSourceListener
import com.nikhilhere.streambox.mediasource.base.MediaSourceOutput
import com.nikhilhere.streambox.mediasource.base.MediaSourceState
import com.nikhilhere.streambox.mediasource.camerasource.CameraSource

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

    LaunchedEffect(true) {
        cameraSource.addListener(
            object : MediaSourceListener {
                override fun onOutput(output: MediaSourceOutput) {
                    Log.i(TAG, "onOutput: $output")
                }

                override fun onError(exception: MediaSourceException) {
                    Log.e(TAG, "onError: $exception", exception)
                }

                override fun onState(state: MediaSourceState) {
                    Log.i(TAG, "onState: $state")
                }

            }
        )
        cameraSource.initialize()
    }

    cameraSource.getPreview()?.preview?.let { preview ->
        AndroidView(
            factory = {
                preview
            },
            modifier = modifier
        )
    }

}