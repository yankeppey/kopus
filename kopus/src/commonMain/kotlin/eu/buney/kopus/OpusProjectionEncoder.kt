/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package eu.buney.kopus

/**
 * Result of creating a projection encoder for ambisonics.
 * Contains the encoder and the computed stream configuration.
 *
 * @property encoder The created projection encoder
 * @property streams Total number of elementary Opus streams
 * @property coupledStreams Number of stereo (coupled) streams
 */
data class ProjectionEncoderResult(
    val encoder: OpusProjectionEncoder,
    val streams: Int,
    val coupledStreams: Int
)

/**
 * Thin, idiomatic wrapper over the native *OpusProjectionEncoder* C API for ambisonics encoding.
 *
 * This class provides a Kotlin interface to the Opus projection encoder, which is specifically
 * designed for encoding ambisonics audio. It uses a demixing matrix to convert ambisonics
 * channels (spherical harmonics) to the internal coupled/uncoupled stream representation.
 *
 * Key concepts:
 * - **Ambisonics**: Full-sphere surround sound technique using spherical harmonics
 * - **Mapping family 3**: Ambisonics (ACN channel ordering, implementation-specific projection)
 * - **Demixing matrix**: Converts ambisonics input to internal stream representation
 *
 * Ambisonics orders and channel counts:
 * - First-order (FOA): 4 channels
 * - Second-order: 9 channels
 * - Third-order: 16 channels
 *
 * This class mirrors the behavior of the C functions:
 * - `opus_projection_ambisonics_encoder_create` for initialization
 * - `opus_projection_encode` / `opus_projection_encode_float` for encoding audio
 * - `opus_projection_encoder_ctl` for controlling encoder parameters
 * - `opus_projection_encoder_destroy` for cleanup
 *
 * @see OpusProjectionDecoder for decoding ambisonics audio
 */
expect class OpusProjectionEncoder : AutoCloseable {

    val sampleRate: Int
    val channels: Int
    val streams: Int
    val coupledStreams: Int
    val application: OpusApplication

    /**
     * Encodes a projection Opus frame, putting the output into a specified data buffer.
     *
     * Mirrors the C function `opus_projection_encode`.
     *
     * @param inPcm 16-bit input signal (interleaved ambisonics channels). Length should be at least frameSize * channels
     * @param inPcmOffset Offset to use when reading the [inPcm] buffer
     * @param frameSize The number of samples _per channel_ in the input signal
     * @param outData Destination buffer for the output payload
     * @param outDataOffset The offset to use when writing to the output data buffer
     * @param maxDataBytes The maximum amount of space allocated for the output payload
     * @return The length of the encoded packet (in bytes) on success or a negative error code on failure
     */
    fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int = DEFAULT_MAX_BYTES
    ): Int

    /**
     * Encodes a projection Opus frame from floating point input.
     *
     * Mirrors the C function `opus_projection_encode_float`.
     *
     * @param inPcm Float input signal (interleaved ambisonics channels), with a normal range of +/-1.0
     * @param inPcmOffset Offset to use when reading the [inPcm] buffer
     * @param frameSize The number of samples _per channel_ in the input signal
     * @param outData Destination buffer for the output payload
     * @param outDataOffset The offset to use when writing to the output data buffer
     * @param maxDataBytes The maximum amount of space allocated for the output payload
     * @return The length of the encoded packet (in bytes) on success or a negative error code on failure
     */
    fun encode(
        inPcm: FloatArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int = DEFAULT_MAX_BYTES
    ): Int

    /**
     * Encodes a projection Opus frame from 24-bit integer input.
     *
     * Mirrors the C function `opus_projection_encode24`.
     *
     * @param inPcm 24-bit input signal stored in 32-bit integers (interleaved ambisonics channels).
     *              Values should be in the range of 24-bit signed integers (-8388608 to 8388607).
     * @param inPcmOffset Offset to use when reading the [inPcm] buffer
     * @param frameSize The number of samples _per channel_ in the input signal
     * @param outData Destination buffer for the output payload
     * @param outDataOffset The offset to use when writing to the output data buffer
     * @param maxDataBytes The maximum amount of space allocated for the output payload
     * @return The length of the encoded packet (in bytes) on success or a negative error code on failure
     */
    fun encode24(
        inPcm: IntArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int = DEFAULT_MAX_BYTES
    ): Int

    /**
     * Releases resources associated with this encoder.
     *
     * Mirrors the C function `opus_projection_encoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the encoder to set a parameter value.
     *
     * @param request A control request code from the `OPUS_SET_*` constants
     * @param value The value to set for the requested parameter
     * @return [OPUS_OK] on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the encoder to query a parameter value.
     *
     * @param request A control request code from the `OPUS_GET_*` constants
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int

    /**
     * Gets the demixing matrix from the encoder.
     *
     * This matrix can be passed to [OpusProjectionDecoder] to decode the audio.
     * The matrix encodes how ambisonics channels are mapped to the internal
     * stream representation.
     *
     * @return The demixing matrix as a ByteArray
     */
    fun getDemixingMatrix(): ByteArray

    companion object {
        /**
         * Creates a projection encoder for ambisonics input.
         *
         * This function uses `opus_projection_ambisonics_encoder_create()` which automatically
         * configures the stream count, coupled stream count, and demixing matrix based on
         * the mapping family.
         *
         * @param sampleRate Sampling rate of input signal (Hz). Must be 8000, 12000, 16000, 24000, or 48000
         * @param channels Number of ambisonics channels:
         *   - First-order ambisonics (FOA): 4 channels
         *   - Second-order ambisonics: 9 channels
         *   - Third-order ambisonics: 16 channels
         * @param mappingFamily Ambisonics mapping family:
         *   - 3: Ambisonics (ACN ordering, implementation-specific projection)
         * @param application Target application (Voip, Audio, or RestrictedLowDelay)
         * @return [ProjectionEncoderResult] containing the encoder and computed configuration
         */
        fun createAmbisonics(
            sampleRate: Int,
            channels: Int,
            mappingFamily: Int,
            application: OpusApplication = OpusApplication.Audio
        ): ProjectionEncoderResult
    }
}
