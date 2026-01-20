/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusMultistreamDecoder actual constructor(
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val streams: Int,
    actual val coupledStreams: Int,
    mapping: ByteArray
) : AutoCloseable {

    private var handle: Long

    init {
        OpusLoader.load()
        handle = nativeCreate(sampleRate, channels, streams, coupledStreams, mapping)
        require(handle != 0L) { "nativeCreate failed for multistream decoder" }
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

    actual fun decode24(
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: IntArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Boolean
    ): Int {
        return nativeDecode24Offset(
            handle, inData, inDataOffset, len, outPcm, outPcmOffset, frameSize, if (decodeFec) 1 else 0
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

    private external fun nativeCreate(
        sampleRate: Int,
        channels: Int,
        streams: Int,
        coupledStreams: Int,
        mapping: ByteArray
    ): Long

    private external fun nativeDestroy(handle: Long)

    private external fun nativeDecodeShortOffset(
        handle: Long,
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: ShortArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Int
    ): Int

    private external fun nativeDecodeFloatOffset(
        handle: Long,
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: FloatArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Int
    ): Int

    private external fun nativeDecode24Offset(
        handle: Long,
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: IntArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Int
    ): Int

    private external fun nativeCtl(handle: Long, request: Int, value: Int): Int
    private external fun nativeCtlQuery(handle: Long, request: Int): Int
}
