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
actual class OpusDREDDecoder actual constructor() : AutoCloseable {
    private val ptr: CPointer<cnames.structs.OpusDREDDecoder>

    init {
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        memScoped {
            val e = alloc<IntVar>()
            ptr = opus_dred_decoder_create(e.ptr) ?: error("opus_dred_decoder_create returned null")
            require(e.value >= 0) { "DRED decoder creation error ${e.value}" }
        }
    }

    actual fun parse(
        dred: OpusDRED,
        data: ByteArray,
        dataOffset: Int,
        len: Int,
        maxDredSamples: Int,
        samplingRate: Int,
        deferProcessing: Boolean
    ): DredParseResult = memScoped {
        val dredEnd = alloc<IntVar>()
        val offset = data.usePinned { pinnedData ->
            opus_dred_parse(
                ptr,
                dred.ptr,
                pinnedData.addressOf(dataOffset).reinterpret(),
                len,
                maxDredSamples,
                samplingRate,
                dredEnd.ptr,
                if (deferProcessing) 1 else 0
            )
        }
        DredParseResult(offset, dredEnd.value)
    }

    actual fun process(src: OpusDRED, dst: OpusDRED): Int {
        return opus_dred_process(ptr, src.ptr, dst.ptr)
    }

    actual override fun close() {
        opus_dred_decoder_destroy(ptr)
    }

    actual fun ctl(request: Int, value: Int): Int {
        return opus_dred_decoder_ctl(ptr, request, value)
    }

    actual fun ctlQuery(request: Int): Int = memScoped {
        val valuePtr = alloc<IntVar>()
        val result = opus_dred_decoder_ctl(ptr, request, valuePtr.ptr)
        if (result >= 0) valuePtr.value else result
    }
}
