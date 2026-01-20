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
actual class OpusMultistreamEncoder private constructor(
    private val ptr: CPointer<cnames.structs.OpusMSEncoder>,
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val streams: Int,
    actual val coupledStreams: Int,
    actual val application: OpusApplication
) : AutoCloseable {

    actual constructor(
        sampleRate: Int,
        channels: Int,
        streams: Int,
        coupledStreams: Int,
        mapping: ByteArray,
        application: OpusApplication
    ) : this(
        ptr = memScoped {
            val err = alloc<IntVar>()
            mapping.usePinned { pinnedMapping ->
                opus_multistream_encoder_create(
                    sampleRate,
                    channels,
                    streams,
                    coupledStreams,
                    pinnedMapping.addressOf(0).reinterpret(),
                    application.value,
                    err.ptr
                ) ?: error("opus_multistream_encoder_create returned null")
            }.also {
                require(err.value >= 0) { "Opus multistream encoder create error ${err.value}" }
            }
        },
        sampleRate = sampleRate,
        channels = channels,
        streams = streams,
        coupledStreams = coupledStreams,
        application = application
    )

    actual override fun close() = opus_multistream_encoder_destroy(ptr)

    actual fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return inPcm.usePinned { pinnedPcm ->
            outData.usePinned { pinnedOut ->
                val pcmPtr = pinnedPcm.addressOf(inPcmOffset)
                val outPtr = pinnedOut.addressOf(outDataOffset)
                opus_multistream_encode(
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
                opus_multistream_encode_float(
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
                opus_multistream_encode24(
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
        return opus_multistream_encoder_ctl(ptr, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_multistream_encoder_ctl(ptr, request, valuePtr.ptr)
            if (result >= 0) valuePtr.value else result
        }
    }

    actual companion object {
        actual fun createSurround(
            sampleRate: Int,
            channels: Int,
            mappingFamily: Int,
            application: OpusApplication
        ): SurroundEncoderResult {
            return memScoped {
                val streamsVar = alloc<IntVar>()
                val coupledStreamsVar = alloc<IntVar>()
                val err = alloc<IntVar>()
                val mapping = ByteArray(channels)

                val ptr = mapping.usePinned { pinnedMapping ->
                    opus_multistream_surround_encoder_create(
                        sampleRate,
                        channels,
                        mappingFamily,
                        streamsVar.ptr,
                        coupledStreamsVar.ptr,
                        pinnedMapping.addressOf(0).reinterpret(),
                        application.value,
                        err.ptr
                    ) ?: error("opus_multistream_surround_encoder_create returned null")
                }
                require(err.value >= 0) { "Opus multistream surround encoder create error ${err.value}" }

                val streams = streamsVar.value
                val coupledStreams = coupledStreamsVar.value

                SurroundEncoderResult(
                    encoder = OpusMultistreamEncoder(
                        ptr, sampleRate, channels, streams, coupledStreams, application
                    ),
                    streams = streams,
                    coupledStreams = coupledStreams,
                    mapping = mapping
                )
            }
        }
    }
}
