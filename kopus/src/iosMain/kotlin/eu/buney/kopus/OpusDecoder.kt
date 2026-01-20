/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

import kotlinx.cinterop.*
import opus.c.*

@OptIn(ExperimentalForeignApi::class)
actual class OpusDecoder actual constructor(sampleRate: Int, actual val channels: Int) : AutoCloseable {
    private val ptr: CPointer<cnames.structs.OpusDecoder>

    init {
        memScoped {
            val e = alloc<IntVar>()
            ptr = opus_decoder_create(sampleRate, channels, e.ptr) ?: error("opus_decoder_create returned null")
            require(e.value >= 0) { "Opus decoder create error ${e.value}" }
        }
    }

    actual override fun close() {
        opus_decoder_destroy(ptr)
    }

    actual fun decode(
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: ShortArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Boolean
    ): Int =
        outPcm.usePinned { pinnedOut ->
            val outPtr = pinnedOut.addressOf(outPcmOffset)

            val decoded = if (inData != null) {
                inData.usePinned { pinnedIn ->
                    opus_decode(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_decode(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
            }

            decoded
        }

    actual fun decode(
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: FloatArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Boolean
    ): Int =
        outPcm.usePinned { pinnedOut ->
            val outPtr = pinnedOut.addressOf(outPcmOffset)

            val decoded = if (inData != null) {
                inData.usePinned { pinnedIn ->
                    opus_decode_float(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_decode_float(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
            }

            decoded
        }

    actual fun decode24(
        inData: ByteArray?,
        inDataOffset: Int,
        len: Int,
        outPcm: IntArray,
        outPcmOffset: Int,
        frameSize: Int,
        decodeFec: Boolean
    ): Int =
        outPcm.usePinned { pinnedOut ->
            val outPtr = pinnedOut.addressOf(outPcmOffset)

            val decoded = if (inData != null) {
                inData.usePinned { pinnedIn ->
                    opus_decode24(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_decode24(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
            }

            decoded
        }

    actual fun ctl(request: Int, value: Int): Int {
        return opus_decoder_ctl(ptr, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_decoder_ctl(ptr, request, valuePtr.ptr)
            if (result >= 0) valuePtr.value else result
        }
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
        return outPcm.usePinned { pinnedOut ->
            opus_decoder_dred_decode(
                ptr,
                dred.ptr,
                dredOffset,
                pinnedOut.addressOf(outPcmOffset),
                frameSize
            )
        }
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
        return outPcm.usePinned { pinnedOut ->
            opus_decoder_dred_decode_float(
                ptr,
                dred.ptr,
                dredOffset,
                pinnedOut.addressOf(outPcmOffset),
                frameSize
            )
        }
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
        return outPcm.usePinned { pinnedOut ->
            opus_decoder_dred_decode24(
                ptr,
                dred.ptr,
                dredOffset,
                pinnedOut.addressOf(outPcmOffset),
                frameSize
            )
        }
    }
}
