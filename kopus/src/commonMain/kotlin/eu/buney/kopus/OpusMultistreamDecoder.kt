/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package eu.buney.kopus

/**
 * Thin, idiomatic wrapper over the native *OpusMSDecoder* C API for multistream decoding.
 *
 * This class provides a Kotlin interface to the Opus multistream audio codec decoder,
 * which supports decoding multiple channels (up to 255) from a set of elementary Opus streams.
 * It mirrors the behavior of the C functions:
 * - `opus_multistream_decoder_create` for initialization
 * - `opus_multistream_decode` / `opus_multistream_decode_float` for decoding audio
 * - `opus_multistream_decoder_ctl` for controlling decoder parameters
 * - `opus_multistream_decoder_destroy` for cleanup
 *
 * Key concepts:
 * - **streams**: Total number of elementary Opus streams (1-255)
 * - **coupledStreams**: Number of stereo (2-channel) streams
 * - **mapping**: ByteArray mapping I/O channels to streams
 *
 * For decoding surround sound, use the configuration returned by [OpusMultistreamEncoder.createSurround].
 */
expect class OpusMultistreamDecoder(
    sampleRate: Int,
    channels: Int,
    streams: Int,
    coupledStreams: Int,
    mapping: ByteArray
) : AutoCloseable {

    val sampleRate: Int
    val channels: Int
    val streams: Int
    val coupledStreams: Int

    /**
     * Decodes a multistream Opus packet into PCM audio using 16-bit format.
     *
     * Mirrors the C function `opus_multistream_decode`.
     *
     * @param inData The input payload. Use null to indicate packet loss (PLC)
     * @param inDataOffset The offset to use when reading the input payload
     * @param len The number of bytes in the payload (the packet size)
     * @param outPcm A buffer to put the output PCM (interleaved for multiple channels)
     * @param outPcmOffset The offset to use when writing to the output buffer
     * @param frameSize The number of samples (per channel) of available space in the output PCM buffer
     * @param decodeFec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data
     * @return The number of decoded samples (per channel) on success or a negative error code on failure
     */
    fun decode(
        inData: ByteArray? = null,
        inDataOffset: Int = 0,
        len: Int = 0,
        outPcm: ShortArray,
        outPcmOffset: Int = 0,
        frameSize: Int,
        decodeFec: Boolean = false
    ): Int

    /**
     * Decodes a multistream Opus packet into PCM audio using floating point format.
     *
     * Mirrors the C function `opus_multistream_decode_float`.
     *
     * @param inData The input payload. Use null to indicate packet loss (PLC)
     * @param inDataOffset The offset to use when reading the input payload
     * @param len The number of bytes in the payload (the packet size)
     * @param outPcm A buffer to put the output PCM as floating point values (interleaved for multiple channels)
     * @param outPcmOffset The offset to use when writing to the output buffer
     * @param frameSize The number of samples (per channel) of available space in the output PCM buffer
     * @param decodeFec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data
     * @return The number of decoded samples (per channel) on success or a negative error code on failure
     */
    fun decode(
        inData: ByteArray? = null,
        inDataOffset: Int = 0,
        len: Int = 0,
        outPcm: FloatArray,
        outPcmOffset: Int = 0,
        frameSize: Int,
        decodeFec: Boolean = false
    ): Int

    /**
     * Releases resources associated with this decoder.
     *
     * Mirrors the C function `opus_multistream_decoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the decoder to set a parameter value.
     *
     * @param request A control request code from the OPUS_SET_* constants
     * @param value The value to set for the requested parameter
     * @return OPUS_OK on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the decoder to query a parameter value.
     *
     * @param request A control request code from the OPUS_GET_* constants
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int
}
