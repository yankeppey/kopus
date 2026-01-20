/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusDecoder actual constructor(sampleRate: Int, actual val channels: Int) : AutoCloseable {
    private var handle: Long = nativeCreate(sampleRate, channels)

    init {
        OpusLoader.load()
        require(handle != 0L) { "Opus decoder create failed" }
    }

    actual override fun close() {
        nativeDestroy(handle); handle = 0
    }

    actual fun decode(
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: ShortArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Boolean
    ): Int {
        return nativeDecodeShortOffset(
            handle, inData, inDataOffset, len, outPcm, outPcmOffset, frameSize, if (decodeFec) 1 else 0
        )
    }

    actual fun decode(
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: FloatArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Boolean
    ): Int {
        return nativeDecodeFloatOffset(
            handle, inData, inDataOffset, len, outPcm, outPcmOffset, frameSize, if (decodeFec) 1 else 0
        )
    }

    actual fun ctl(request: Int, value: Int): Int {
        return nativeCtl(handle, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return nativeCtlQuery(handle, request)
    }

    private external fun nativeCreate(fs: Int, ch: Int): Long
    private external fun nativeDestroy(h: Long)

    private external fun nativeDecodeShortOffset(
        h: Long,
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: ShortArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Int
    ): Int

    private external fun nativeDecodeFloatOffset(
        h: Long,
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: FloatArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Int
    ): Int

    private external fun nativeCtl(h: Long, request: Int, value: Int): Int
    private external fun nativeCtlQuery(h: Long, request: Int): Int
}
