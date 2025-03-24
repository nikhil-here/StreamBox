package com.nikhilhere.streambox.mediasource.camerasource

import android.content.Context
import android.util.Range
import android.util.Size
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nikhilhere.streambox.mediasource.base.MediaSource
import com.nikhilhere.streambox.mediasource.base.MediaSourceException
import com.nikhilhere.streambox.mediasource.base.MediaSourceOutput
import com.nikhilhere.streambox.mediasource.base.MediaSourcePreview
import com.nikhilhere.streambox.mediasource.base.MediaSourceState
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : MediaSource() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val previewView by lazy {
        PreviewView(context)
    }

    override fun initialize() {
        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                cameraProvider = providerFuture.get(10, TimeUnit.SECONDS)
                cameraProvider?.unbindAll()
                setupCamera()
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            onError(
                exception = MediaSourceException(
                    message = "Failed to initialize CameraSource",
                    cause = e
                )
            )
        }
    }

    override fun start() {
        //do nothing
    }

    override fun stop() {
        cameraProvider?.unbindAll()
    }

    override fun getPreview(): CameraSourcePreview? {
        return CameraSourcePreview(
            preview = previewView
        )
    }

    private fun setupCamera() {
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480), // Change resolution here
                    ResolutionStrategy.FALLBACK_RULE_NONE
                )
            )
            .build()

        val preview = Preview.Builder()
            .setTargetFrameRate(Range(30, 30))
            .setResolutionSelector(resolutionSelector)
            .build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Ensures only latest frame is processed
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // YUV format
            .build()
            .apply {
                setAnalyzer(Executors.newFixedThreadPool(4)) { imageProxy ->
                    val frame = imageProxyToFrame(imageProxy)
                    onOutput(frame)
                    imageProxy.close()
                }
            }

        try {
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            onState(MediaSourceState.RUNNING)
        } catch (e: Exception) {
            onState(MediaSourceState.IDLE)
            onError(
                exception = MediaSourceException(
                    message = "Failed to bind CamearaSource",
                    cause = e
                )
            )
        }
    }

    private fun imageProxyToFrame(imageProxy: ImageProxy): MediaSourceOutput {
        val buffer = imageProxy.planes[0].buffer
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        return CameraSourceOutput(
            data = byteArray,
            timestamp = imageProxy.imageInfo.timestamp,
            width = imageProxy.width,
            height = imageProxy.height,
            format = imageProxy.format
        )
    }
}


data class CameraSourceOutput(
    val data: ByteArray,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val format: Int
) : MediaSourceOutput(
    sourceTimeStamp = timestamp
)

class CameraSourcePreview(
    val preview: View
) : MediaSourcePreview()