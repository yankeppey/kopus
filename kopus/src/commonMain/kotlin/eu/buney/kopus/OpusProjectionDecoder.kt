/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package eu.buney.kopus

/**
 * Thin, idiomatic wrapper over the native *OpusProjectionDecoder* C API for ambisonics decoding.
 *
 * This class provides a Kotlin interface to the Opus projection decoder, which is specifically
 * designed for decoding ambisonics audio. It uses a demixing matrix to convert the internal
 * stream representation back to output channels (speakers, binaural, etc.).
 *
 * The demixing matrix can be obtained from the encoder using [OpusProjectionEncoder.getDemixingMatrix]
 * or generated using ambisonics processing libraries.
 *
 * Key concepts:
 * - **Demixing matrix**: Maps coded streams to output channels
 * - **Output channels**: Can be any speaker configuration or binaural output
 * - **streams/coupledStreams**: Must match the encoded stream configuration
 *
 * This class mirrors the behavior of the C functions:
 * - `opus_projection_decoder_create` for initialization
 * - `opus_projection_decode` / `opus_projection_decode_float` for decoding audio
 * - `opus_projection_decoder_ctl` for controlling decoder parameters
 * - `opus_projection_decoder_destroy` for cleanup
 *
 * @param sampleRate Sampling rate to decode at (Hz). Must be 8000, 12000, 16000, 24000, or 48000
 * @param channels Number of output channels (1-255)
 * @param streams Total number of elementary Opus streams (from encoder configuration)
 * @param coupledStreams Number of stereo (coupled) streams (from encoder configuration)
 * @param demixingMatrix The demixing matrix that maps coded channels to output channels
 *
 * @see OpusProjectionEncoder for encoding ambisonics audio
 */
expect class OpusProjectionDecoder(
    sampleRate: Int,
    channels: Int,
    streams: Int,
    coupledStreams: Int,
    demixingMatrix: ByteArray
) : AutoCloseable {

    val sampleRate: Int
    val channels: Int
    val streams: Int
    val coupledStreams: Int

    /**
     * Decodes a projection Opus packet into PCM audio using 16-bit format.
     *
     * Mirrors the C function `opus_projection_decode`.
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
     * Decodes a projection Opus packet into PCM audio using floating point format.
     *
     * Mirrors the C function `opus_projection_decode_float`.
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
     * Decodes a projection Opus packet into PCM audio using 24-bit integer format.
     *
     * Mirrors the C function `opus_projection_decode24`.
     *
     * @param inData The input payload. Use null to indicate packet loss (PLC)
     * @param inDataOffset The offset to use when reading the input payload
     * @param len The number of bytes in the payload (the packet size)
     * @param outPcm A buffer to put the output PCM as 24-bit values stored in 32-bit integers (interleaved for multiple channels)
     * @param outPcmOffset The offset to use when writing to the output buffer
     * @param frameSize The number of samples (per channel) of available space in the output PCM buffer
     * @param decodeFec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data
     * @return The number of decoded samples (per channel) on success or a negative error code on failure
     */
    fun decode24(
        inData: ByteArray? = null,
        inDataOffset: Int = 0,
        len: Int = 0,
        outPcm: IntArray,
        outPcmOffset: Int = 0,
        frameSize: Int,
        decodeFec: Boolean = false
    ): Int

    /**
     * Releases resources associated with this decoder.
     *
     * Mirrors the C function `opus_projection_decoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the decoder to set a parameter value.
     *
     * @param request A control request code from the `OPUS_SET_*` constants
     * @param value The value to set for the requested parameter
     * @return [OPUS_OK] on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the decoder to query a parameter value.
     *
     * @param request A control request code from the `OPUS_GET_*` constants
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int
}
