/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

import kotlinx.cinterop.*
import opus.c.*

private fun check(err: Int) {
    require(err >= 0) { "Opus error $err" }
}

@OptIn(ExperimentalForeignApi::class)
actual class OpusEncoder actual constructor(
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val application: OpusApplication
): AutoCloseable {

    private val ptr: CPointer<cnames.structs.OpusEncoder>

    init {
        memScoped {
            val err = alloc<IntVar>()
            ptr = opus_encoder_create(sampleRate, channels, application.value, err.ptr)
                ?: error("opus_encoder_create returned null")
            check(err.value)
        }
    }

    actual override fun close() = opus_encoder_destroy(ptr)

    /* ------------ Encoding helpers ------------ */

    actual fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return inPcm.usePinned { pinnedPcm ->
            outData.usePinned { pinnedOut ->
                val pcmPtr = pinnedPcm.addressOf(inPcmOffset)
                val outPtr = pinnedOut.addressOf(outDataOffset)
                val len = opus_encode(
                    ptr,
                    pcmPtr,
                    frameSize,
                    outPtr.reinterpret(),
                    maxDataBytes
                )
                check(len)
                len
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
                val len = opus_encode_float(
                    ptr,
                    pcmPtr,
                    frameSize,
                    outPtr.reinterpret(),
                    maxDataBytes
                )
                check(len)
                len
            }
        }
    }

    actual fun ctl(request: Int, value: Int): Int {
        return memScoped {
            opus_encoder_ctl(ptr, request, value)
        }
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_encoder_ctl(ptr, request, valuePtr.ptr)
            if (result < 0) error("Error querying parameter: $result")
            valuePtr.value
        }
    }

}
