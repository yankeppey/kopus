/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("unused")
package eu.buney.kopus

/**
 * Extension functions for OpusDecoder to provide easy access to all decoder control parameters.
 *
 * These functions wrap calls to the OpusDecoder.ctl and OpusDecoder.ctlQuery methods,
 * which internally use the `opus_decoder_ctl` C function. Each function corresponds to
 * a specific control request defined in opus_defines.h.
 *
 * WARNING: Many of these functions have not been extensively tested. While they should work
 * correctly as they directly mirror the C API, they should be used with caution and tested
 * thoroughly in your specific application.
 */

/**
 * Configures decoder gain adjustment.
 * Scales the decoded output by a factor specified in Q8 dB units.
 * This has a maximum range of -32768 to 32767 inclusive.
 *
 * gain = pow(10, x/(20.0*256))
 *
 * @param gain Amount to scale PCM signal by in Q8 dB units.
 * @return OPUS_OK on success
 */
fun OpusDecoder.setGain(gain: Int): Int = ctl(OPUS_SET_GAIN_REQUEST, gain)

/**
 * Gets the decoder's configured gain adjustment.
 * @return Amount to scale PCM signal by in Q8 dB units.
 */
fun OpusDecoder.getGain(): Int = ctlQuery(OPUS_GET_GAIN_REQUEST)

/**
 * If set to 1, disables the use of phase inversion for intensity stereo,
 * improving the quality of mono downmixes, but slightly reducing normal
 * stereo quality.
 * @param disabled 0 = Enable phase inversion (default), 1 = Disable phase inversion
 * @return OPUS_OK on success
 */
fun OpusDecoder.setPhaseInversionDisabled(disabled: Int): Int = ctl(OPUS_SET_PHASE_INVERSION_DISABLED_REQUEST, disabled)

/**
 * Gets the decoder's configured phase inversion status.
 * @return 0 = Phase inversion enabled (default), 1 = Phase inversion disabled
 */
fun OpusDecoder.getPhaseInversionDisabled(): Int = ctlQuery(OPUS_GET_PHASE_INVERSION_DISABLED_REQUEST)

/**
 * Gets the duration (in samples) of the last packet successfully decoded or concealed.
 * @return Number of samples (at current sampling rate)
 */
fun OpusDecoder.getLastPacketDuration(): Int = ctlQuery(OPUS_GET_LAST_PACKET_DURATION_REQUEST)

/**
 * Gets the pitch of the last decoded frame, if available.
 * This can be used for any post-processing algorithm requiring the use of pitch,
 * e.g. time stretching/shortening.
 * @return Pitch period at 48 kHz (or 0 if not available)
 */
fun OpusDecoder.getPitch(): Int = ctlQuery(OPUS_GET_PITCH_REQUEST)

/**
 * Gets the sampling rate the decoder was initialized with.
 * @return Sampling rate of decoder in Hz
 */
fun OpusDecoder.getSampleRate(): Int = ctlQuery(OPUS_GET_SAMPLE_RATE_REQUEST)

/**
 * Gets the final state of the codec's entropy coder.
 * This is used for testing purposes.
 * The encoder and decoder state should be identical after coding a payload
 * (assuming no data corruption or software bugs).
 * @return Entropy coder state
 */
fun OpusDecoder.getFinalRange(): Int = ctlQuery(OPUS_GET_FINAL_RANGE_REQUEST)

/**
 * Gets the decoder's last bandpass value.
 * @return One of:
 *  - [OPUS_BANDWIDTH_NARROWBAND]: 4 kHz passband
 *  - [OPUS_BANDWIDTH_MEDIUMBAND]: 6 kHz passband
 *  - [OPUS_BANDWIDTH_WIDEBAND]: 8 kHz passband
 *  - [OPUS_BANDWIDTH_SUPERWIDEBAND]: 12 kHz passband
 *  - [OPUS_BANDWIDTH_FULLBAND]: 20 kHz passband
 */
fun OpusDecoder.getBandwidth(): Int = ctlQuery(OPUS_GET_BANDWIDTH_REQUEST)

/**
 * Resets the codec state to be equivalent to a freshly initialized state.
 * This should be called when switching streams in order to prevent
 * the back to back decoding from giving different results from
 * one at a time decoding.
 * @return OPUS_OK on success
 */
fun OpusDecoder.resetState(): Int = ctl(OPUS_RESET_STATE, 0)

/**
 * Decodes a complete Opus packet into PCM.
 *
 * This is a simplified version that creates an output buffer of an appropriate size based on the frame size.
 * The decode_fec parameter is set to false, so this function won't perform packet loss concealment.
 *
 * @param data The encoded Opus packet
 * @param frameSize The maximum frame size to decode into (samples per channel)
 * @return The decoded PCM data as a ShortArray
 */
fun OpusDecoder.decode(data: ByteArray, frameSize: Int): ShortArray {
    val output = ShortArray(frameSize * channels)
    decode(data, 0, data.size, output, 0, frameSize, false)
    return output
}

/**
 * Decodes a complete Opus packet into floating point PCM.
 *
 * This is a simplified version that creates an output buffer of an appropriate size based on the frame size.
 * The decode_fec parameter is set to false, so this function won't perform packet loss concealment.
 *
 * @param data The encoded Opus packet
 * @param frameSize The maximum frame size to decode into (samples per channel)
 * @return The decoded PCM data as a FloatArray
 */
fun OpusDecoder.decodeFloat(data: ByteArray, frameSize: Int): FloatArray {
    val output = FloatArray(frameSize * channels)
    decode(data, 0, data.size, output, 0, frameSize, false)
    return output
}
