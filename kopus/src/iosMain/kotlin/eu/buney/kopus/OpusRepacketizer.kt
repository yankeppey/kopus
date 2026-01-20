/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */

@file:OptIn(ExperimentalForeignApi::class)

package eu.buney.kopus

import kotlinx.cinterop.*
import opus.c.*
import platform.posix.memcpy

actual class OpusRepacketizer actual constructor() : AutoCloseable {

    private val ptr: CPointer<cnames.structs.OpusRepacketizer>

    // Arena for persistent packet copies - the repacketizer stores pointers to input data,
    // so we must keep copies in native memory until init() or close() is called
    private var arena = Arena()

    init {
        ptr = opus_repacketizer_create()
            ?: error("opus_repacketizer_create returned null")
    }

    actual fun init() {
        arena.clear()
        opus_repacketizer_init(ptr)
    }

    actual fun cat(data: ByteArray, len: Int): Int {
        // Allocate native memory and copy data - this memory persists until init() or close()
        val nativeCopy = arena.allocArray<ByteVar>(len)
        data.usePinned { pinned ->
            memcpy(nativeCopy, pinned.addressOf(0), len.convert())
        }
        return opus_repacketizer_cat(ptr, nativeCopy.reinterpret(), len)
    }

    actual fun getNbFrames(): Int {
        return opus_repacketizer_get_nb_frames(ptr)
    }

    actual fun outRange(begin: Int, end: Int, data: ByteArray, maxLen: Int): Int {
        return data.usePinned { pinned ->
            opus_repacketizer_out_range(
                ptr,
                begin,
                end,
                pinned.addressOf(0).reinterpret(),
                maxLen
            )
        }
    }

    actual fun out(data: ByteArray, maxLen: Int): Int {
        return data.usePinned { pinned ->
            opus_repacketizer_out(
                ptr,
                pinned.addressOf(0).reinterpret(),
                maxLen
            )
        }
    }

    actual override fun close() {
        arena.clear()
        opus_repacketizer_destroy(ptr)
    }
}
