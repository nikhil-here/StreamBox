package com.nikhilhere.streambox.mediaencoder.videoencoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoder
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderException
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderOutput
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderState
import com.nikhilhere.streambox.mediasource.base.MediaSourceException
import com.nikhilhere.streambox.mediasource.base.MediaSourceListener
import com.nikhilhere.streambox.mediasource.base.MediaSourceOutput
import com.nikhilhere.streambox.mediasource.base.MediaSourceState
import com.nikhilhere.streambox.mediasource.camerasource.CameraSourceOutput
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

class VideoEncoder : MediaEncoder() {

    companion object {
        private const val TAG = "VideoEncoder"
        fun getDefaultFormat() = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            1280,
            720
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
    }

    private var mediaCodec: MediaCodec? = null
    private var isRunning: Boolean = true
    internal val queue = ArrayBlockingQueue<MediaSourceOutput>(80)
    private var handlerThread: HandlerThread? = null
    private val sourceListener by lazy { SourceListener() }
    private val mediaEncoderCallback by lazy { MediaEncoderCallback() }

    override fun initialize(format: MediaFormat, setSourceListener: (MediaSourceListener) -> Unit) {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            handlerThread = HandlerThread(TAG)
            handlerThread?.start()
            val handler = Handler(handlerThread!!.getLooper())
            mediaCodec?.setCallback(mediaEncoderCallback, handler)
            mediaCodec?.start()
            isRunning = true
            setSourceListener(sourceListener)
            onState(MediaEncoderState.RUNNING)
        } catch (e: Exception) {
            isRunning = false
            onError(
                MediaEncoderException(
                    message = "Unable to initialize Encoder",
                    cause = e
                )
            )
            onState(MediaEncoderState.IDLE)
            Log.i(TAG, "initCodec: e = $e")
        }
    }

    override fun start() {

    }

    override fun stop() {
        mediaCodec?.release()
        isRunning = false
        mediaCodec = null
        onState(MediaEncoderState.IDLE)
    }

    inner class MediaEncoderCallback : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
            try {
                val inputBuffer = codec.getInputBuffer(inputBufferId)
                var frame: Frame? = null
                while (frame == null) {
                    if (!queue.isEmpty()) {
                        frame = queue.poll()?.toFrame()
                    }
                }
                if (frame != null) {
                    inputBuffer?.clear()
                    inputBuffer?.put(frame.byteArray)
                    codec.queueInputBuffer(
                        inputBufferId,
                        0,
                        frame.byteArray.size,
                        System.nanoTime() / 1000,
                        0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "onInputBufferAvailable: ${e.message}", e)
            }
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            outputBufferId: Int,
            bufferInfo: MediaCodec.BufferInfo
        ) {
            val outputBuffer = codec.getOutputBuffer(outputBufferId)
            outputBuffer?.let { processEncoderOutput(it) }
            mediaCodec?.releaseOutputBuffer(outputBufferId, false);
        }

        override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
            Log.i(TAG, "onError: exception $p1")
        }

        override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
            Log.i(TAG, "onOutputFormatChanged: format =  $p1")
        }
    }

    inner class SourceListener : MediaSourceListener {
        override fun onOutput(output: MediaSourceOutput) {
            processSourceInput(output)
        }

        override fun onError(exception: MediaSourceException) {

        }

        override fun onState(state: MediaSourceState) {

        }
    }

    private fun processSourceInput(input: MediaSourceOutput) {
        Log.i(TAG, "processSourceInput: input $input")
        when (input) {
            is CameraSourceOutput -> {
                queue.add(input)
            }
        }
    }

    private fun processEncoderOutput(byteBuffer: ByteBuffer) {
        val data = ByteArray(byteBuffer.remaining())
        byteBuffer.get(data)
        onOutput(MediaEncoderOutput(data = data))
    }
}