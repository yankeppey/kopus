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
actual class OpusDRED actual constructor() : AutoCloseable {
    internal val ptr: CPointer<cnames.structs.OpusDRED>

    init {
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        memScoped {
            val e = alloc<IntVar>()
            ptr = opus_dred_alloc(e.ptr) ?: error("opus_dred_alloc returned null")
            require(e.value >= 0) { "DRED allocation error ${e.value}" }
        }
    }

    actual override fun close() {
        opus_dred_free(ptr)
    }
}
