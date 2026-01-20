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
actual class OpusMultistreamDecoder actual constructor(
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val streams: Int,
    actual val coupledStreams: Int,
    mapping: ByteArray
) : AutoCloseable {

    private val ptr: CPointer<cnames.structs.OpusMSDecoder>

    init {
        memScoped {
            val e = alloc<IntVar>()
            ptr = mapping.usePinned { pinnedMapping ->
                opus_multistream_decoder_create(
                    sampleRate,
                    channels,
                    streams,
                    coupledStreams,
                    pinnedMapping.addressOf(0).reinterpret(),
                    e.ptr
                ) ?: error("opus_multistream_decoder_create returned null")
            }
            require(e.value >= 0) { "Opus multistream decoder create error ${e.value}" }
        }
    }

    actual override fun close() {
        opus_multistream_decoder_destroy(ptr)
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
                    opus_multistream_decode(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_multistream_decode(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
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
                    opus_multistream_decode_float(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_multistream_decode_float(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
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
                    opus_multistream_decode24(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_multistream_decode24(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
            }

            decoded
        }

    actual fun ctl(request: Int, value: Int): Int {
        return opus_multistream_decoder_ctl(ptr, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_multistream_decoder_ctl(ptr, request, valuePtr.ptr)
            if (result >= 0) valuePtr.value else result
        }
    }
}
