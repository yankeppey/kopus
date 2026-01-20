/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("unused")
package eu.buney.kopus

/**
 * Extension functions for OpusMultistreamDecoder to provide easy access to decoder control parameters.
 *
 * These functions wrap calls to the OpusMultistreamDecoder.ctl and OpusMultistreamDecoder.ctlQuery methods,
 * which internally use the `opus_multistream_decoder_ctl` C function.
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
fun OpusMultistreamDecoder.setGain(gain: Int): Int = ctl(OPUS_SET_GAIN_REQUEST, gain)

/**
 * Gets the decoder's configured gain adjustment.
 * @return Amount to scale PCM signal by in Q8 dB units.
 */
fun OpusMultistreamDecoder.getGain(): Int = ctlQuery(OPUS_GET_GAIN_REQUEST)

/**
 * If set to true, disables the use of phase inversion for intensity stereo,
 * improving the quality of mono downmixes, but slightly reducing normal
 * stereo quality.
 * @param disabled true = Disable phase inversion, false = Enable phase inversion (default)
 * @return OPUS_OK on success
 */
fun OpusMultistreamDecoder.setPhaseInversionDisabled(disabled: Boolean): Int = ctl(OPUS_SET_PHASE_INVERSION_DISABLED_REQUEST, if (disabled) 1 else 0)

/**
 * Gets the decoder's configured phase inversion status.
 * @return true = Phase inversion disabled, false = Phase inversion enabled (default)
 */
fun OpusMultistreamDecoder.getPhaseInversionDisabled(): Boolean = ctlQuery(OPUS_GET_PHASE_INVERSION_DISABLED_REQUEST) == 1

/**
 * Gets the duration (in samples) of the last packet successfully decoded or concealed.
 * @return Number of samples (at current sampling rate)
 */
fun OpusMultistreamDecoder.getLastPacketDuration(): Int = ctlQuery(OPUS_GET_LAST_PACKET_DURATION_REQUEST)

/**
 * Gets the pitch of the last decoded frame, if available.
 * This can be used for any post-processing algorithm requiring the use of pitch,
 * e.g. time stretching/shortening.
 * @return Pitch period at 48 kHz (or 0 if not available)
 */
fun OpusMultistreamDecoder.getPitch(): Int = ctlQuery(OPUS_GET_PITCH_REQUEST)

/**
 * Gets the sampling rate the decoder was initialized with.
 * @return Sampling rate of decoder in Hz
 */
fun OpusMultistreamDecoder.getSampleRate(): Int = ctlQuery(OPUS_GET_SAMPLE_RATE_REQUEST)

/**
 * Gets the final state of the codec's entropy coder.
 * This is used for testing purposes.
 * The encoder and decoder state should be identical after coding a payload
 * (assuming no data corruption or software bugs).
 * @return Entropy coder state
 */
fun OpusMultistreamDecoder.getFinalRange(): Int = ctlQuery(OPUS_GET_FINAL_RANGE_REQUEST)

/**
 * Gets the decoder's last bandpass value.
 * @return One of:
 *  - [OPUS_BANDWIDTH_NARROWBAND]: 4 kHz passband
 *  - [OPUS_BANDWIDTH_MEDIUMBAND]: 6 kHz passband
 *  - [OPUS_BANDWIDTH_WIDEBAND]: 8 kHz passband
 *  - [OPUS_BANDWIDTH_SUPERWIDEBAND]: 12 kHz passband
 *  - [OPUS_BANDWIDTH_FULLBAND]: 20 kHz passband
 */
fun OpusMultistreamDecoder.getBandwidth(): Int = ctlQuery(OPUS_GET_BANDWIDTH_REQUEST)

/**
 * Resets the codec state to be equivalent to a freshly initialized state.
 * This should be called when switching streams in order to prevent
 * the back to back decoding from giving different results from
 * one at a time decoding.
 * @return OPUS_OK on success
 */
fun OpusMultistreamDecoder.resetState(): Int = ctl(OPUS_RESET_STATE, 0)

/**
 * If set to true, the decoder will ignore all extensions found in the padding area
 * (does not affect DRED, which is decoded separately).
 * @param ignore true = Ignore extensions, false = Process extensions (default)
 * @return OPUS_OK on success
 */
fun OpusMultistreamDecoder.setIgnoreExtensions(ignore: Boolean): Int = ctl(OPUS_SET_IGNORE_EXTENSIONS_REQUEST, if (ignore) 1 else 0)

/**
 * Gets whether the decoder is ignoring extensions.
 * @return true = Ignoring extensions, false = Processing extensions
 */
fun OpusMultistreamDecoder.getIgnoreExtensions(): Boolean = ctlQuery(OPUS_GET_IGNORE_EXTENSIONS_REQUEST) == 1

/**
 * Decodes a complete multistream Opus packet into PCM.
 *
 * This is a simplified version that creates an output buffer of an appropriate size based on the frame size.
 * The decode_fec parameter is set to false, so this function won't perform packet loss concealment.
 *
 * @param data The encoded Opus packet
 * @param frameSize The maximum frame size to decode into (samples per channel)
 * @return The decoded PCM data as a ShortArray
 */
fun OpusMultistreamDecoder.decode(data: ByteArray, frameSize: Int): ShortArray {
    val output = ShortArray(frameSize * channels)
    decode(data, 0, data.size, output, 0, frameSize, false)
    return output
}

/**
 * Decodes a complete multistream Opus packet into floating point PCM.
 *
 * This is a simplified version that creates an output buffer of an appropriate size based on the frame size.
 * The decode_fec parameter is set to false, so this function won't perform packet loss concealment.
 *
 * @param data The encoded Opus packet
 * @param frameSize The maximum frame size to decode into (samples per channel)
 * @return The decoded PCM data as a FloatArray
 */
fun OpusMultistreamDecoder.decodeFloat(data: ByteArray, frameSize: Int): FloatArray {
    val output = FloatArray(frameSize * channels)
    decode(data, 0, data.size, output, 0, frameSize, false)
    return output
}
