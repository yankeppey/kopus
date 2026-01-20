/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package eu.buney.kopus

/**
 * Result of creating a multistream encoder using surround preset configuration.
 * Contains the encoder and the computed stream configuration.
 *
 * @property encoder The created multistream encoder
 * @property streams Total number of elementary Opus streams
 * @property coupledStreams Number of stereo (coupled) streams
 * @property mapping Channel mapping array
 */
data class SurroundEncoderResult(
    val encoder: OpusMultistreamEncoder,
    val streams: Int,
    val coupledStreams: Int,
    val mapping: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SurroundEncoderResult
        return encoder == other.encoder &&
                streams == other.streams &&
                coupledStreams == other.coupledStreams &&
                mapping.contentEquals(other.mapping)
    }

    override fun hashCode(): Int {
        var result = encoder.hashCode()
        result = 31 * result + streams
        result = 31 * result + coupledStreams
        result = 31 * result + mapping.contentHashCode()
        return result
    }
}

/**
 * Thin, idiomatic wrapper over the native *OpusMSEncoder* C API for multistream encoding.
 *
 * This class provides a Kotlin interface to the Opus multistream audio codec encoder,
 * which supports encoding multiple channels (up to 255) as a set of elementary Opus streams.
 * It mirrors the behavior of the C functions:
 * - `opus_multistream_encoder_create` for initialization with explicit mapping
 * - `opus_multistream_surround_encoder_create` for surround preset configuration
 * - `opus_multistream_encode` / `opus_multistream_encode_float` for encoding audio
 * - `opus_multistream_encoder_ctl` for controlling encoder parameters
 * - `opus_multistream_encoder_destroy` for cleanup
 *
 * Key concepts:
 * - **streams**: Total number of elementary Opus streams (1-255)
 * - **coupledStreams**: Number of stereo (2-channel) streams
 * - **mapping**: ByteArray mapping I/O channels to streams
 *
 * For standard surround sound configurations (5.1, 7.1, etc.), use [createSurround].
 */
expect class OpusMultistreamEncoder(
    sampleRate: Int,
    channels: Int,
    streams: Int,
    coupledStreams: Int,
    mapping: ByteArray,
    application: OpusApplication = OpusApplication.Audio
) : AutoCloseable {

    val sampleRate: Int
    val channels: Int
    val streams: Int
    val coupledStreams: Int
    val application: OpusApplication

    /**
     * Encodes a multistream Opus frame, putting the output into a specified data buffer.
     *
     * Mirrors the C function `opus_multistream_encode`.
     *
     * @param inPcm 16-bit input signal (interleaved for multiple channels). Length should be at least frameSize * channels
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
     * Encodes a multistream Opus frame from floating point input.
     *
     * Mirrors the C function `opus_multistream_encode_float`.
     *
     * @param inPcm Float input signal (interleaved for multiple channels), with a normal range of +/-1.0
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
     * Releases resources associated with this encoder.
     *
     * Mirrors the C function `opus_multistream_encoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the encoder to set a parameter value.
     *
     * @param request A control request code from the OPUS_SET_* constants
     * @param value The value to set for the requested parameter
     * @return OPUS_OK on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the encoder to query a parameter value.
     *
     * @param request A control request code from the OPUS_GET_* constants
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int

    companion object {
        /**
         * Creates a multistream encoder configured for standard surround sound channel layouts.
         *
         * This function uses `opus_multistream_surround_encoder_create()` which automatically
         * configures the stream count, coupled stream count, and channel mapping based on
         * the mapping family.
         *
         * @param sampleRate Sampling rate of input signal (Hz). Must be 8000, 12000, 16000, 24000, or 48000
         * @param channels Number of channels (1-255)
         * @param mappingFamily Channel mapping family:
         *   - 0: Custom mapping (1-2 channels only)
         *   - 1: Vorbis surround order (1-8 channels)
         *   - 255: Discrete channels (1-255 channels, no coupling)
         * @param application Target application (Voip, Audio, or RestrictedLowDelay)
         * @return [SurroundEncoderResult] containing the encoder and computed configuration
         */
        fun createSurround(
            sampleRate: Int,
            channels: Int,
            mappingFamily: Int,
            application: OpusApplication = OpusApplication.Audio
        ): SurroundEncoderResult
    }
}
