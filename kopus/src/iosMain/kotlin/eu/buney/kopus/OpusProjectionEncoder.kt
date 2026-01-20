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
actual class OpusProjectionEncoder private constructor(
    private val ptr: CPointer<cnames.structs.OpusProjectionEncoder>,
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val streams: Int,
    actual val coupledStreams: Int,
    actual val application: OpusApplication
) : AutoCloseable {

    actual override fun close() = opus_projection_encoder_destroy(ptr)

    actual fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return inPcm.usePinned { pinnedPcm ->
            outData.usePinned { pinnedOut ->
                val pcmPtr = pinnedPcm.addressOf(inPcmOffset)
                val outPtr = pinnedOut.addressOf(outDataOffset)
                opus_projection_encode(
                    ptr,
                    pcmPtr,
                    frameSize,
                    outPtr.reinterpret(),
                    maxDataBytes
                )
            }
        }
    }

    actual fun encode(
        inPcm: FloatArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return inPcm.usePinned { pinnedPcm ->
            outData.usePinned { pinnedOut ->
                val pcmPtr = pinnedPcm.addressOf(inPcmOffset)
                val outPtr = pinnedOut.addressOf(outDataOffset)
                opus_projection_encode_float(
                    ptr,
                    pcmPtr,
                    frameSize,
                    outPtr.reinterpret(),
                    maxDataBytes
                )
            }
        }
    }

    actual fun encode24(
        inPcm: IntArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return inPcm.usePinned { pinnedPcm ->
            outData.usePinned { pinnedOut ->
                val pcmPtr = pinnedPcm.addressOf(inPcmOffset)
                val outPtr = pinnedOut.addressOf(outDataOffset)
                opus_projection_encode24(
                    ptr,
                    pcmPtr,
                    frameSize,
                    outPtr.reinterpret(),
                    maxDataBytes
                )
            }
        }
    }

    actual fun ctl(request: Int, value: Int): Int {
        return opus_projection_encoder_ctl(ptr, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_projection_encoder_ctl(ptr, request, valuePtr.ptr)
            if (result >= 0) valuePtr.value else result
        }
    }

    /**
     * Gets the demixing matrix from the encoder.
     *
     * This matrix can be passed to [OpusProjectionDecoder] to decode the audio.
     *
     * @return The demixing matrix as a ByteArray
     */
    actual fun getDemixingMatrix(): ByteArray {
        return memScoped {
            // Get size first
            val sizePtr = alloc<IntVar>()
            val sizeResult = opus_projection_encoder_ctl(
                ptr,
                OPUS_PROJECTION_GET_DEMIXING_MATRIX_SIZE_REQUEST,
                sizePtr.ptr
            )
            require(sizeResult >= 0) { "Failed to get demixing matrix size: $sizeResult" }

            val size = sizePtr.value
            val matrix = ByteArray(size)

            matrix.usePinned { pinnedMatrix ->
                val result = opus_projection_encoder_ctl(
                    ptr,
                    OPUS_PROJECTION_GET_DEMIXING_MATRIX_REQUEST,
                    pinnedMatrix.addressOf(0),
                    size
                )
                require(result >= 0) { "Failed to get demixing matrix: $result" }
            }

            matrix
        }
    }

    actual companion object {
        actual fun createAmbisonics(
            sampleRate: Int,
            channels: Int,
            mappingFamily: Int,
            application: OpusApplication
        ): ProjectionEncoderResult {
            return memScoped {
                val streamsVar = alloc<IntVar>()
                val coupledStreamsVar = alloc<IntVar>()
                val err = alloc<IntVar>()

                val ptr = opus_projection_ambisonics_encoder_create(
                    sampleRate,
                    channels,
                    mappingFamily,
                    streamsVar.ptr,
                    coupledStreamsVar.ptr,
                    application.value,
                    err.ptr
                ) ?: error("opus_projection_ambisonics_encoder_create returned null")

                require(err.value >= 0) { "Opus projection encoder create error ${err.value}" }

                val streams = streamsVar.value
                val coupledStreams = coupledStreamsVar.value

                ProjectionEncoderResult(
                    encoder = OpusProjectionEncoder(
                        ptr, sampleRate, channels, streams, coupledStreams, application
                    ),
                    streams = streams,
                    coupledStreams = coupledStreams
                )
            }
        }
    }
}
