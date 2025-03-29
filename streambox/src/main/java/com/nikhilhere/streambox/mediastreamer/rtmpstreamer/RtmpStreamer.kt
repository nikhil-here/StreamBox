package com.nikhilhere.streambox.mediastreamer.rtmpstreamer

import android.util.Log
import com.nikhilhere.streambox.mediaencoder.base.MediaEncoderListener
import com.nikhilhere.streambox.mediastreamer.base.MediaStreamException
import com.nikhilhere.streambox.mediastreamer.base.MediaStreamInfo
import com.nikhilhere.streambox.mediastreamer.base.MediaStreamer
import com.nikhilhere.streambox.mediastreamer.base.MediaStreamerState
import com.nikhilhere.streambox.mediastreamer.rtmpstreamer.handshake.Handshake
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "RtmpStreamer"

class RtmpStreamer : MediaStreamer() {

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "CoroutineExceptionHandler", e)
    })

    private lateinit var input: DataInputStream
    private lateinit var output: DataOutputStream
    private lateinit var socket: Socket

    override fun initialize(
        streamInfo: MediaStreamInfo,
        setEncoderListener: (MediaEncoderListener) -> Unit
    ) {
        scope.launch {
            try {
                socket = Socket(streamInfo.host, streamInfo.port)
                input = DataInputStream(socket.getInputStream())
                output = DataOutputStream(socket.getOutputStream())

                Log.i(TAG, "connect: performHandshake")
                val handshake = Handshake(input, output)
                handshake.performHandshake()

                Log.i(TAG, "connect: sendConnectMessage")
                sendConnectMessage()

                Log.i(TAG, "connect: waitForWindowAcknowledgement")
                waitForWindowAcknowledgement()

                Log.i(TAG, "connect: sendSetChunkSize")
                sendSetChunkSize()

                Log.i(TAG, "connect: waitForSetPeerBandwidth")
                waitForSetPeerBandwidth()

                Log.i(TAG, "connect: sendWindowAcknowledgement")
                sendWindowAcknowledgement()

                Log.i(TAG, "connect: waitForUserControlMessage")
                waitForUserControlMessage()

                Log.i(TAG, "connect: waitForConnectResult")
                waitForConnectResult()

                Log.i(TAG, "connect: createStream")
                createStream()
            } catch (e: Exception) {
                Log.e(TAG, "initialize: ", e)
                onState(
                    state = MediaStreamerState.Error(
                        exception = MediaStreamException(e.message, e)
                    )
                )
            }
        }
    }

    private fun sendConnectMessage() {
        val amfCommand = encodeAmf0(
            "connect", 1, mapOf(
                "app" to "live",
                "type" to "nonprivate",
                "flashVer" to "LNX 9,0,124,2",
                "tcUrl" to "rtmp://192.168.0.105/live"
            )
        )

        sendRtmpMessage(3, 0x14, 1, amfCommand)
    }

    private fun sendSetChunkSize() {
        val chunkSize = 4096
        val message = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(chunkSize).array()
        sendRtmpMessage(2, 0x01, 0, message)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun waitForWindowAcknowledgement() {
        val response = ByteArray(4)
        input.read(response)
        Log.i(TAG, "waitForWindowAcknowledgement: ${response.toHexString()} ")
    }

    private fun waitForSetPeerBandwidth() {
        val response = ByteArray(5)
        input.read(response)
        Log.i(TAG, "waitForSetPeerBandwidth:  ")
    }

    private fun sendWindowAcknowledgement() {
        val message = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(2500000).array()
        sendRtmpMessage(2, 0x05, 0, message)
        Log.i(TAG, "sendWindowAcknowledgement:  ")
    }

    private fun waitForUserControlMessage() {
        val response = ByteArray(6)
        input.read(response)
        Log.i(TAG, "waitForUserControlMessage:  ")

    }

    private fun waitForConnectResult() {
        val response = ByteArray(1024)
        input.read(response)
        Log.i(TAG, "waitForConnectResult:  ")
    }

    private fun createStream() {
        val amfCommand = encodeAmf0("createStream", 2, mapOf())
        sendRtmpMessage(3, 0x14, 1, amfCommand)
        val streamId = readStreamIdFromResponse()
        Log.i(TAG, "Created stream with ID: $streamId")
    }

    private fun readStreamIdFromResponse(): Int {
        val response = ByteArray(1024)
        input.read(response)

        // Parse AMF0 Response to Extract Stream ID
        val buffer = ByteBuffer.wrap(response).order(ByteOrder.BIG_ENDIAN)
        buffer.position(3) // Skip first 3 bytes (chunk header)
        while (buffer.hasRemaining()) {
            val type = buffer.get().toInt()
            if (type == 0x00) { // AMF0 Number (Stream ID)
                return buffer.double.toInt()
            }
        }
        return -1 // Invalid stream ID
    }

    private fun sendRtmpMessage(csid: Int, typeId: Int, streamId: Int, payload: ByteArray) {
        val messageLength = payload.size
        val header = ByteBuffer.allocate(12).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(csid.toByte()) // Chunk Stream ID
            put(0x00).put(0x00).put(0x00) // Timestamp (3 bytes)
            put((messageLength shr 16).toByte())
            put((messageLength shr 8).toByte())
            put((messageLength and 0xFF).toByte()) // Message Length (3 bytes)
            put(typeId.toByte()) // Message Type ID
            put(0x00).put(0x00).put(0x00)
                .put(streamId.toByte()) // Message Stream ID (4 bytes, little-endian)
        }.array()
        output.write(header)
        output.write(payload)
        output.flush()
    }

    private fun encodeAmf0(
        command: String,
        transactionId: Int,
        params: Map<String, Any>
    ): ByteArray {
        val buffer = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x02) // AMF0 String
        buffer.putShort(command.length.toShort())
        buffer.put(command.toByteArray())
        buffer.put(0x00) // AMF0 Number
        buffer.putDouble(transactionId.toDouble())
        buffer.put(0x03) // AMF0 Object
        for ((key, value) in params) {
            buffer.putShort(key.length.toShort())
            buffer.put(key.toByteArray())
            when (value) {
                is String -> {
                    buffer.put(0x02)
                    buffer.putShort(value.length.toShort())
                    buffer.put(value.toByteArray())
                }

                is Double -> {
                    buffer.put(0x00)
                    buffer.putDouble(value)
                }
            }
        }
        buffer.put(0x00).put(0x00).put(0x09) // Object end marker
        return buffer.array().copyOf(buffer.position())
    }
}