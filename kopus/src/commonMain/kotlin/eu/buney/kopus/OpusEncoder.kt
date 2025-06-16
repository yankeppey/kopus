/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package eu.buney.kopus

import eu.buney.kopus.OpusApplication.Voip

/**
 * Thin, idiomatic wrapper over the native *OpusEncoder* C API.
 *
 * This class provides a Kotlin interface to the Opus audio codec encoder functionality.
 * It mirrors the behavior of the C functions:
 * - `opus_encoder_create` / `opus_encoder_init` for initialization
 * - `opus_encode` / `opus_encode_float` for encoding audio
 * - `opus_encoder_ctl` for controlling encoder parameters
 * - `opus_encoder_destroy` for cleanup
 *
 * Features:
 *  – Creation helpers mirror `opus_encoder_create` / `opus_encoder_init`.
 *  – `encode()` auto-selects int16 or float path.
 *  – All advanced tuning is reachable via [ctl]/[ctlQuery] methods.
 */
expect class OpusEncoder(
    sampleRate: Int = 32_000,
    channels: Int = 1,
    application: OpusApplication = Voip
) : AutoCloseable {

    val sampleRate: Int
    val channels: Int
    val application: OpusApplication

    /**
     * Encodes an Opus frame, putting the output into a specified data buffer.
     *
     * Mirrors the C function `opus_encode`.
     *
     * @param inPcm 16-bit input signal (interleaved if stereo), in a short array. Length should be at least frameSize * channels
     * @param inPcmOffset Offset to use when reading the [inPcm] buffer
     * @param frameSize The number of samples _per channel_ in the input signal. The frame size must be a valid Opus framesize for the given sample rate.
     * For example, at 48kHz the permitted values are 120, 240, 480, 960, 1920, and 2880. Passing in a duration of less than 10ms
     * (480 samples at 48kHz) will prevent the encoder from using FEC, DTX, or hybrid modes.
     * @param outData Destination buffer for the output payload. This must contain at least [maxDataBytes]
     * @param outDataOffset The offset to use when writing to the output data buffer
     * @param maxDataBytes The maximum amount of space allocated for the output payload. This may be used to impose
     * an upper limit on the instant bitrate, but should not be used as the only bitrate control (use setBitrate for that)
     * @return The length of the encoded packet (in bytes) on success or a negative error code on failure
     */
    fun encode(
        inPcm: ShortArray, inPcmOffset: Int, frameSize: Int,
        outData: ByteArray, outDataOffset: Int, maxDataBytes: Int = DEFAULT_MAX_BYTES
    ): Int

    /**
     * Encodes an Opus frame from floating point input, putting the output into a specified data buffer.
     *
     * Mirrors the C function `opus_encode_float`.
     *
     * @param inPcm Float input signal (interleaved if stereo), with a normal range of +/-1.0. Length should be at least frameSize * channels
     * @param inPcmOffset Offset to use when reading the [inPcm] buffer
     * @param frameSize The number of samples _per channel_ in the input signal. The frame size must be a valid Opus framesize for the given sample rate.
     * @param outData Destination buffer for the output payload. This must contain at least [maxDataBytes]
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
     * Mirrors the C function `opus_encoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the encoder to set a parameter value.
     *
     * Mirrors the C function `opus_encoder_ctl` for setting values.
     *
     * @param request A control request code from the OPUS_SET_* constants
     * @param value The value to set for the requested parameter
     * @return OPUS_OK on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the encoder to query a parameter value.
     *
     * Mirrors the C function `opus_encoder_ctl` for getting values.
     *
     * @param request A control request code from the OPUS_GET_* constants
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int

}
