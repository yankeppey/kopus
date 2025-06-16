/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

import kotlinx.cinterop.*
import opus.c.*

private fun chk(err: Int) {
    require(err >= 0) { "Opus error $err" }
}

@OptIn(ExperimentalForeignApi::class)
actual class OpusDecoder actual constructor(sampleRate: Int, actual val channels: Int) : AutoCloseable {
    private val ptr: CPointer<cnames.structs.OpusDecoder>

    init {
        memScoped {
            val e = alloc<IntVar>()
            ptr = opus_decoder_create(sampleRate, channels, e.ptr) ?: error("null")
            chk(e.value)
        }
    }

    actual override fun close() {
        opus_decoder_destroy(ptr)
    }

    actual fun decode(
        inData: ByteArray, inDataOffset: Int,
        len: Int, outPcm: ShortArray, outPcmOffset: Int, frameSize: Int, decodeFec: Boolean
    ): Int =
        inData.usePinned { pinnedIn ->
            outPcm.usePinned { pinnedOut ->
                val inPtr = pinnedIn.addressOf(inDataOffset)
                val outPtr = pinnedOut.addressOf(outPcmOffset)
                val decoded = opus_decode(
                    ptr,
                    inPtr.reinterpret(),
                    len,
                    outPtr,
                    frameSize,
                    if (decodeFec) 1 else 0
                )
                chk(decoded)
                decoded
            }
        }

    actual fun decode(
        inData: ByteArray, inDataOffset: Int,
        len: Int, outPcm: FloatArray, outPcmOffset: Int, frameSize: Int, decodeFec: Boolean
    ): Int =
        inData.usePinned { pinnedIn ->
            outPcm.usePinned { pinnedOut ->
                val inPtr = pinnedIn.addressOf(inDataOffset)
                val outPtr = pinnedOut.addressOf(outPcmOffset)
                val decoded = opus_decode_float(
                    ptr,
                    inPtr.reinterpret(),
                    len,
                    outPtr,
                    frameSize,
                    if (decodeFec) 1 else 0
                )
                chk(decoded)
                decoded
            }
        }


    actual fun ctl(request: Int, value: Int): Int {
        return memScoped {
            opus_decoder_ctl(ptr, request, value)
        }
    }

    actual fun ctlQuery(request: Int): Int {
        return memScoped {
            val valuePtr = alloc<IntVar>()
            val result = opus_decoder_ctl(ptr, request, valuePtr.ptr)
            if (result < 0) error("Error querying parameter: $result")
            valuePtr.value
        }
    }

}
