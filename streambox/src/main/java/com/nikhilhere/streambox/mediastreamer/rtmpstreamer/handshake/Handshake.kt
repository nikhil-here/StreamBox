package com.nikhilhere.streambox.mediastreamer.rtmpstreamer.handshake

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.random.Random


class Handshake(
    private val inputStream: DataInputStream,
    private val outputStream: DataOutputStream
) {

    companion object {
        private const val TAG = "Handshake"
    }

    private var rtmpVersion = -1
    private var epochTime: Int = 0


    fun performHandshake(
        version: Int = 3, epochTime: Int = (System.currentTimeMillis() / 1000).toInt()
    ) {
        Log.i(
            TAG,
            "performHandshake: started client requested version = $version epochTime = $epochTime"
        )
        writeC0(version = version)
        writeC1(epochTime = epochTime)
        outputStream.flush()

        val s0 = readS0()
        val s1 = readS1()
        rtmpVersion = s0[0].toInt()

        writeC2(s1 = s1)
        outputStream.flush()

        readS2()
        Log.i(TAG, "performHandshake: completed server selected rtmp version is $rtmpVersion")
    }

    /**
     * C0 bits
     *  0 1 2 3 4 5 6 7
     *  +-+-+-+-+-+-+-+-+
     *  | version |
     *  +-+-+-+-+-+-+-+-+
     *
     */
    private fun writeC0(
        version: Int
    ) {
        Log.i(TAG, "writeC0: ")
        val c0 = byteArrayOf(version.toByte())
        outputStream.write(c0)
    }


    /**
     * 0 1 2 3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | time (4 bytes) |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | zero (4 bytes) |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | random bytes |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | random bytes |
     *  | (cont) |
     *  | .... |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    private fun writeC1(epochTime: Int) {
        Log.i(TAG, "writeC1: ")
        val c1 = ByteArray(1536)

        //convert epochTime (08 bytes) to 04 bytes (least significant 32 bits
        this.epochTime = epochTime
        c1[0] = (this.epochTime shr 24).toByte()
        c1[1] = (this.epochTime shr 16).toByte()
        c1[2] = (this.epochTime shr 8).toByte()
        c1[3] = this.epochTime.toByte()

        //set the next 04 bytes to zero field
        for (i in 4..7) c1[i] = 0

        //fill the remaining array with random values
        for (i in 8 until 1536) {
            c1[i] = Random.nextBytes(1).first()
        }

        outputStream.write(c1)
    }


    /**
     * S0 bits
     *  0 1 2 3 4 5 6 7
     *  +-+-+-+-+-+-+-+-+
     *  | version |
     *  +-+-+-+-+-+-+-+-+
     *
     */
    private fun readS0(): ByteArray {
        Log.i(TAG, "readS0: ")
        val s0 = ByteArray(1)
        inputStream.read(s0)
        return s0
    }


    private fun readS1(): ByteArray {
        Log.i(TAG, "readS1: ")
        val s1 = ByteArray(1536)
        inputStream.readFully(s1)
        return s1
    }


    /**
     * 0 1 2 3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | time (4 bytes) |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | time2 (4 bytes) |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | random echo |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | random echo |
     *  | (cont) |
     *  | .... |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  C2 and S2 bits
     */
    private fun writeC2(s1: ByteArray) {
        Log.i(TAG, "writeC2: ")
        outputStream.write(s1)
    }

    private fun readS2() {
        Log.i(TAG, "readS2: ")
        val s2 = ByteArray(1536)
        inputStream.readFully(s2)
    }

}