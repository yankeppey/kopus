/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusDecoder actual constructor(sampleRate: Int, actual val channels: Int) : AutoCloseable {
    private var handle: Long

    init {
        OpusLoader.load()
        handle = nativeCreate(sampleRate, channels)
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

    actual fun ctl(request: Int, value: Int): Int {
        return nativeCtl(handle, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return nativeCtlQuery(handle, request)
    }

    actual fun decodeDred(
        dred: OpusDRED,
        dredOffset: Int,
        outPcm: ShortArray,
        outPcmOffset: Int,
        frameSize: Int
    ): Int {
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        return nativeDecodeDredShort(handle, dred.handle, dredOffset, outPcm, outPcmOffset, frameSize)
    }

    actual fun decodeDred(
        dred: OpusDRED,
        dredOffset: Int,
        outPcm: FloatArray,
        outPcmOffset: Int,
        frameSize: Int
    ): Int {
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        return nativeDecodeDredFloat(handle, dred.handle, dredOffset, outPcm, outPcmOffset, frameSize)
    }

    actual fun decodeDred24(
        dred: OpusDRED,
        dredOffset: Int,
        outPcm: IntArray,
        outPcmOffset: Int,
        frameSize: Int
    ): Int {
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        return nativeDecodeDred24(handle, dred.handle, dredOffset, outPcm, outPcmOffset, frameSize)
    }

    actual fun getNbSamples(packet: ByteArray, len: Int): Int {
        return nativeGetNbSamples(handle, packet, len)
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

    private external fun nativeDecode24Offset(
        h: Long,
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: IntArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Int
    ): Int

    private external fun nativeCtl(h: Long, request: Int, value: Int): Int
    private external fun nativeCtlQuery(h: Long, request: Int): Int

    private external fun nativeDecodeDredShort(
        h: Long,
        dredHandle: Long,
        dredOffset: Int,
        outPcm: ShortArray,
        outPcmOffset: Int,
        frameSize: Int
    ): Int

    private external fun nativeDecodeDredFloat(
        h: Long,
        dredHandle: Long,
        dredOffset: Int,
        outPcm: FloatArray,
        outPcmOffset: Int,
        frameSize: Int
    ): Int

    private external fun nativeDecodeDred24(
        h: Long,
        dredHandle: Long,
        dredOffset: Int,
        outPcm: IntArray,
        outPcmOffset: Int,
        frameSize: Int
    ): Int

    private external fun nativeGetNbSamples(h: Long, packet: ByteArray, len: Int): Int
}
