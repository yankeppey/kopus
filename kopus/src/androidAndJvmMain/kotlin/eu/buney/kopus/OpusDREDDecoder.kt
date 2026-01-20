/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusDREDDecoder actual constructor() : AutoCloseable {
    private var handle: Long

    init {
        OpusLoader.load()
        if (!Opus.isDredAvailable) {
            throw UnsupportedOperationException(
                "DRED is not available in this Opus build. Use the kopus-full artifact for DRED support."
            )
        }
        handle = nativeCreate()
        require(handle != 0L) { "DRED decoder creation failed" }
    }

    actual fun parse(
        dred: OpusDRED,
        data: ByteArray,
        dataOffset: Int,
        len: Int,
        maxDredSamples: Int,
        samplingRate: Int,
        deferProcessing: Boolean
    ): DredParseResult {
        val dredEnd = IntArray(1)
        val offset = nativeParse(
            handle,
            dred.handle,
            data,
            dataOffset,
            len,
            maxDredSamples,
            samplingRate,
            dredEnd,
            if (deferProcessing) 1 else 0
        )
        return DredParseResult(offset, dredEnd[0])
    }

    actual fun process(src: OpusDRED, dst: OpusDRED): Int {
        return nativeProcess(handle, src.handle, dst.handle)
    }

    actual override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    actual fun ctl(request: Int, value: Int): Int {
        return nativeCtl(handle, request, value)
    }

    actual fun ctlQuery(request: Int): Int {
        return nativeCtlQuery(handle, request)
    }

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(h: Long)
    private external fun nativeParse(
        h: Long,
        dredHandle: Long,
        data: ByteArray,
        dataOffset: Int,
        len: Int,
        maxDredSamples: Int,
        samplingRate: Int,
        dredEnd: IntArray,
        deferProcessing: Int
    ): Int
    private external fun nativeProcess(h: Long, srcDred: Long, dstDred: Long): Int
    private external fun nativeCtl(h: Long, request: Int, value: Int): Int
    private external fun nativeCtlQuery(h: Long, request: Int): Int
}
