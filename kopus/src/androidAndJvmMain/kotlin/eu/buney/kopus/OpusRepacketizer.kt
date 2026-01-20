/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusRepacketizer actual constructor() : AutoCloseable {

    private var handle: Long

    init {
        OpusLoader.load()
        handle = nativeCreate()
        require(handle != 0L) { "nativeCreate failed" }
    }

    actual fun init() {
        nativeInit(handle)
    }

    actual fun cat(data: ByteArray, len: Int): Int {
        return nativeCat(handle, data, len)
    }

    actual fun getNbFrames(): Int {
        return nativeGetNbFrames(handle)
    }

    actual fun outRange(begin: Int, end: Int, data: ByteArray, maxLen: Int): Int {
        return nativeOutRange(handle, begin, end, data, maxLen)
    }

    actual fun out(data: ByteArray, maxLen: Int): Int {
        return nativeOut(handle, data, maxLen)
    }

    actual override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(h: Long)
    private external fun nativeInit(h: Long)
    private external fun nativeCat(h: Long, data: ByteArray, len: Int): Int
    private external fun nativeGetNbFrames(h: Long): Int
    private external fun nativeOutRange(h: Long, begin: Int, end: Int, data: ByteArray, maxLen: Int): Int
    private external fun nativeOut(h: Long, data: ByteArray, maxLen: Int): Int
}
