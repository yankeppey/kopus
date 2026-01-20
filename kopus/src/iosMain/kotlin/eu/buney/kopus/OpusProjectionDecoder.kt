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
actual class OpusProjectionDecoder actual constructor(
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val streams: Int,
    actual val coupledStreams: Int,
    demixingMatrix: ByteArray
) : AutoCloseable {

    private val ptr: CPointer<cnames.structs.OpusProjectionDecoder>

    init {
        memScoped {
            val e = alloc<IntVar>()
            ptr = demixingMatrix.usePinned { pinnedMatrix ->
                opus_projection_decoder_create(
                    sampleRate,
                    channels,
                    streams,
                    coupledStreams,
                    pinnedMatrix.addressOf(0).reinterpret(),
                    demixingMatrix.size,
                    e.ptr
                ) ?: error("opus_projection_decoder_create returned null")
            }
            require(e.value >= 0) { "Opus projection decoder create error ${e.value}" }
        }
    }

    actual override fun close() {
        opus_projection_decoder_destroy(ptr)
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
                    opus_projection_decode(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_projection_decode(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
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
                    opus_projection_decode_float(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_projection_decode_float(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
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
                    opus_projection_decode24(
                        ptr,
                        pinnedIn.addressOf(inDataOffset).reinterpret(),
                        len,
                        outPtr,
                        frameSize,
                        if (decodeFec) 1 else 0
                    )
                }
            } else {
                opus_projection_decode24(ptr, null, 0, outPtr, frameSize, if (decodeFec) 1 else 0)
            }

            decoded
        }

    actual fun ctl(request: Int, value: Int): Int {
        return opus_projection_decoder_ctl(ptr, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_projection_decoder_ctl(ptr, request, valuePtr.ptr)
            if (result >= 0) valuePtr.value else result
        }
    }
}
