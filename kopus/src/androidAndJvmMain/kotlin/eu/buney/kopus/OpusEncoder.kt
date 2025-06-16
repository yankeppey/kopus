/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusEncoder actual constructor(
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val application: OpusApplication
) : AutoCloseable {

    private var handle: Long

    init {
        OpusLoader.load()
        handle = nativeCreate(sampleRate, channels, application.value)
        require(handle != 0L) { "nativeCreate failed" }
    }
    actual fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return nativeEncodeShortOffset(
            handle, inPcm, inPcmOffset, frameSize, outData, outDataOffset, maxDataBytes
        )
    }
    actual fun encode(
        inPcm: FloatArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return nativeEncodeFloatOffset(
            handle, inPcm, inPcmOffset, frameSize, outData, outDataOffset, maxDataBytes
        )
    }
    actual override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    actual fun ctl(request: Int, value: Int): Int {
        return nativeCtl(handle, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return nativeCtlQuery(handle, request)
    }

    private external fun nativeCreate(fs: Int, ch: Int, app: Int): Long

    private external fun nativeEncodeShortOffset(
        h: Long,
        pcm: ShortArray,
        pcmOffset: Int,
        frame: Int,
        out: ByteArray,
        outOffset: Int,
        maxBytes: Int
    ): Int

    private external fun nativeEncodeFloatOffset(
        h: Long,
        pcm: FloatArray,
        pcmOffset: Int,
        frame: Int,
        out: ByteArray,
        outOffset: Int,
        maxBytes: Int
    ): Int

    private external fun nativeDestroy(h: Long)

    external fun nativeCtl(h: Long, request: Int, value: Int): Int
    external fun nativeCtlQuery(h: Long, request: Int): Int
}
