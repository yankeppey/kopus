/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual class OpusProjectionEncoder private constructor(
    handle: Long,
    actual val sampleRate: Int,
    actual val channels: Int,
    actual val streams: Int,
    actual val coupledStreams: Int,
    actual val application: OpusApplication
) : AutoCloseable {

    private var handle: Long = handle

    init {
        OpusLoader.load()
    }

    actual fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return nativeEncodeShortOffset(
            handle, inPcm, inPcmOffset, frameSize, outData, outDataOffset, maxDataBytes
        )
    }

    actual fun encode(
        inPcm: FloatArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return nativeEncodeFloatOffset(
            handle, inPcm, inPcmOffset, frameSize, outData, outDataOffset, maxDataBytes
        )
    }

    actual fun encode24(
        inPcm: IntArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int
    ): Int {
        return nativeEncode24Offset(
            handle, inPcm, inPcmOffset, frameSize, outData, outDataOffset, maxDataBytes
        )
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

    /**
     * Gets the demixing matrix from the encoder.
     *
     * This matrix can be passed to [OpusProjectionDecoder] to decode the audio.
     *
     * @return The demixing matrix as a ByteArray
     */
    actual fun getDemixingMatrix(): ByteArray {
        return nativeGetDemixingMatrix(handle)
            ?: throw IllegalStateException("Failed to get demixing matrix")
    }

    actual companion object {
        actual fun createAmbisonics(
            sampleRate: Int,
            channels: Int,
            mappingFamily: Int,
            application: OpusApplication
        ): ProjectionEncoderResult {
            OpusLoader.load()
            val result = nativeCreateAmbisonicsStatic(sampleRate, channels, mappingFamily, application.value)
                ?: throw IllegalStateException("nativeCreateAmbisonics failed")

            val handle = result[0]
            val streams = result[1].toInt()
            val coupledStreams = result[2].toInt()

            return ProjectionEncoderResult(
                encoder = OpusProjectionEncoder(handle, sampleRate, channels, streams, coupledStreams, application),
                streams = streams,
                coupledStreams = coupledStreams
            )
        }

        @JvmStatic
        private external fun nativeCreateAmbisonicsStatic(
            sampleRate: Int,
            channels: Int,
            mappingFamily: Int,
            application: Int
        ): LongArray?
    }

    private external fun nativeDestroy(handle: Long)

    private external fun nativeEncodeShortOffset(
        handle: Long,
        pcm: ShortArray,
        pcmOffset: Int,
        frameSize: Int,
        out: ByteArray,
        outOffset: Int,
        maxBytes: Int
    ): Int

    private external fun nativeEncodeFloatOffset(
        handle: Long,
        pcm: FloatArray,
        pcmOffset: Int,
        frameSize: Int,
        out: ByteArray,
        outOffset: Int,
        maxBytes: Int
    ): Int

    private external fun nativeEncode24Offset(
        handle: Long,
        pcm: IntArray,
        pcmOffset: Int,
        frameSize: Int,
        out: ByteArray,
        outOffset: Int,
        maxBytes: Int
    ): Int

    private external fun nativeCtl(handle: Long, request: Int, value: Int): Int
    private external fun nativeCtlQuery(handle: Long, request: Int): Int
    private external fun nativeGetDemixingMatrix(handle: Long): ByteArray?
}
