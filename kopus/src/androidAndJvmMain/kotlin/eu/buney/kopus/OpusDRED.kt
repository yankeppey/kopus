/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusDRED actual constructor() : AutoCloseable {
    internal var handle: Long

    init {
        OpusLoader.load()
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        handle = nativeAlloc()
        require(handle != 0L) { "DRED allocation failed" }
    }

    actual override fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0
        }
    }

    private external fun nativeAlloc(): Long
    private external fun nativeFree(h: Long)
}
