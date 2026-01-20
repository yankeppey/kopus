/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("unused")
package eu.buney.kopus

/**
 * Extension functions for OpusEncoder to provide easy access to all encoder control parameters.
 *
 * These functions wrap calls to the OpusEncoder.ctl and OpusEncoder.ctlQuery methods,
 * which internally use the `opus_encoder_ctl` C function. Each function corresponds to
 * a specific control request defined in opus_defines.h.
 *
 * WARNING: Many of these functions have not been extensively tested. While they should work
 * correctly as they directly mirror the C API, they should be used with caution and tested
 * thoroughly in your specific application.
 */

/**
 * Configures the encoder's intended application.
 * @param application One of:
 *  - [OPUS_APPLICATION_VOIP]: Process signal for improved speech intelligibility
 *  - [OPUS_APPLICATION_AUDIO]: Favor faithfulness to the original input
 *  - [OPUS_APPLICATION_RESTRICTED_LOWDELAY]: Configure the minimum possible coding delay
 * @return OPUS_OK on success
 */
fun OpusEncoder.setApplication(application: Int): Int = ctl(OPUS_SET_APPLICATION_REQUEST, application)

/**
 * Gets the encoder's configured application.
 * @return One of [OPUS_APPLICATION_VOIP], [OPUS_APPLICATION_AUDIO], or [OPUS_APPLICATION_RESTRICTED_LOWDELAY]
 */
fun OpusEncoder.getApplication(): Int = ctlQuery(OPUS_GET_APPLICATION_REQUEST)

/**
 * Configures the bitrate in the encoder.
 * @param bitrate Bitrate in bits per second. The default is determined based on channels and sampling rate.
 *                Use [OPUS_AUTO] or [OPUS_BITRATE_MAX] for automatic configuration.
 * @return OPUS_OK on success
 */
fun OpusEncoder.setBitrate(bitrate: Int): Int = ctl(OPUS_SET_BITRATE_REQUEST, bitrate)

/**
 * Gets the encoder's bitrate configuration.
 * @return The bitrate in bits per second
 */
fun OpusEncoder.getBitrate(): Int = ctlQuery(OPUS_GET_BITRATE_REQUEST)

/**
 * Sets the encoder's bandpass to a specific value.
 * @param bandwidth One of:
 *  - [OPUS_AUTO]: (default)
 *  - [OPUS_BANDWIDTH_NARROWBAND]: 4 kHz passband
 *  - [OPUS_BANDWIDTH_MEDIUMBAND]: 6 kHz passband
 *  - [OPUS_BANDWIDTH_WIDEBAND]: 8 kHz passband
 *  - [OPUS_BANDWIDTH_SUPERWIDEBAND]: 12 kHz passband
 *  - [OPUS_BANDWIDTH_FULLBAND]: 20 kHz passband
 * @return OPUS_OK on success
 */
fun OpusEncoder.setBandwidth(bandwidth: Int): Int = ctl(OPUS_SET_BANDWIDTH_REQUEST, bandwidth)

/**
 * Gets the encoder's configured bandwidth setting.
 * @return One of the OPUS_BANDWIDTH_* constants or OPUS_AUTO
 */
fun OpusEncoder.getBandwidth(): Int = ctlQuery(OPUS_GET_BANDWIDTH_REQUEST)

/**
 * Configures the maximum bandpass that the encoder will select automatically.
 * @param maxBandwidth One of:
 *  - [OPUS_BANDWIDTH_NARROWBAND]: 4 kHz passband
 *  - [OPUS_BANDWIDTH_MEDIUMBAND]: 6 kHz passband
 *  - [OPUS_BANDWIDTH_WIDEBAND]: 8 kHz passband
 *  - [OPUS_BANDWIDTH_SUPERWIDEBAND]: 12 kHz passband
 *  - [OPUS_BANDWIDTH_FULLBAND]: 20 kHz passband (default)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setMaxBandwidth(maxBandwidth: Int): Int = ctl(OPUS_SET_MAX_BANDWIDTH_REQUEST, maxBandwidth)

/**
 * Gets the encoder's configured maximum allowed bandwidth.
 * @return One of the OPUS_BANDWIDTH_* constants
 */
fun OpusEncoder.getMaxBandwidth(): Int = ctlQuery(OPUS_GET_MAX_BANDWIDTH_REQUEST)

/**
 * Configures the type of signal being encoded.
 * This is a hint which helps the encoder's mode selection.
 * @param signal One of:
 *  - [OPUS_AUTO]: (default)
 *  - [OPUS_SIGNAL_VOICE]: Bias thresholds towards choosing LPC or Hybrid modes
 *  - [OPUS_SIGNAL_MUSIC]: Bias thresholds towards choosing MDCT modes
 * @return OPUS_OK on success
 */
fun OpusEncoder.setSignal(signal: Int): Int = ctl(OPUS_SET_SIGNAL_REQUEST, signal)

/**
 * Gets the encoder's configured signal type.
 * @return One of [OPUS_AUTO], [OPUS_SIGNAL_VOICE], or [OPUS_SIGNAL_MUSIC]
 */
fun OpusEncoder.getSignal(): Int = ctlQuery(OPUS_GET_SIGNAL_REQUEST)

/**
 * Configures the encoder's computational complexity.
 * @param complexity Value from 0-10, with 10 representing the highest complexity.
 *                   Higher complexity produces better quality at a given bitrate.
 * @return OPUS_OK on success
 */
fun OpusEncoder.setComplexity(complexity: Int): Int = ctl(OPUS_SET_COMPLEXITY_REQUEST, complexity)

/**
 * Gets the encoder's complexity configuration.
 * @return A value from 0-10
 */
fun OpusEncoder.getComplexity(): Int = ctlQuery(OPUS_GET_COMPLEXITY_REQUEST)

/**
 * Enables or disables variable bitrate (VBR) in the encoder.
 * @param enabled true = VBR (default), false = Hard CBR
 * @return OPUS_OK on success
 */
fun OpusEncoder.setVBR(enabled: Boolean): Int = ctl(OPUS_SET_VBR_REQUEST, if (enabled) 1 else 0)

/**
 * Determine if variable bitrate (VBR) is enabled in the encoder.
 * @return true = VBR (default), false = Hard CBR
 */
fun OpusEncoder.getVBR(): Boolean = ctlQuery(OPUS_GET_VBR_REQUEST) == 1

/**
 * Enables or disables constrained VBR in the encoder.
 * @param enabled true = Constrained VBR (default), false = Unconstrained VBR
 * @return OPUS_OK on success
 */
fun OpusEncoder.setVBRConstraint(enabled: Boolean): Int = ctl(OPUS_SET_VBR_CONSTRAINT_REQUEST, if (enabled) 1 else 0)

/**
 * Determine if constrained VBR is enabled in the encoder.
 * @return true = Constrained VBR (default), false = Unconstrained VBR
 */
fun OpusEncoder.getVBRConstraint(): Boolean = ctlQuery(OPUS_GET_VBR_CONSTRAINT_REQUEST) == 1

/**
 * Configures the encoder's use of inband forward error correction (FEC).
 * @param enabled true = Enable FEC, false = Disable FEC (default)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setInbandFEC(enabled: Boolean): Int = ctl(OPUS_SET_INBAND_FEC_REQUEST, if (enabled) 1 else 0)

/**
 * Gets encoder's configured use of inband forward error correction.
 * @return true = FEC enabled, false = FEC disabled
 */
fun OpusEncoder.getInbandFEC(): Boolean = ctlQuery(OPUS_GET_INBAND_FEC_REQUEST) == 1

/**
 * Configures the encoder's expected packet loss percentage.
 * @param packetLossPerc Loss percentage in the range 0-100 (default: 0)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setPacketLossPerc(packetLossPerc: Int): Int = ctl(OPUS_SET_PACKET_LOSS_PERC_REQUEST, packetLossPerc)

/**
 * Gets the encoder's configured packet loss percentage.
 * @return The configured loss percentage in the range 0-100
 */
fun OpusEncoder.getPacketLossPerc(): Int = ctlQuery(OPUS_GET_PACKET_LOSS_PERC_REQUEST)

/**
 * Configures mono/stereo forcing in the encoder.
 * @param forceChannels [OPUS_AUTO] = Not forced (default), 1 = Forced mono, 2 = Forced stereo
 * @return OPUS_OK on success
 */
fun OpusEncoder.setForceChannels(forceChannels: Int): Int = ctl(OPUS_SET_FORCE_CHANNELS_REQUEST, forceChannels)

/**
 * Gets the encoder's forced channel configuration.
 * @return [OPUS_AUTO] = Not forced (default), 1 = Forced mono, 2 = Forced stereo
 */
fun OpusEncoder.getForceChannels(): Int = ctlQuery(OPUS_GET_FORCE_CHANNELS_REQUEST)

/**
 * Configures the encoder's use of discontinuous transmission (DTX).
 * @param enabled true = Enable DTX, false = Disable DTX (default)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setDTX(enabled: Boolean): Int = ctl(OPUS_SET_DTX_REQUEST, if (enabled) 1 else 0)

/**
 * Gets the encoder's configured use of discontinuous transmission.
 * @return true = DTX enabled, false = DTX disabled
 */
fun OpusEncoder.getDTX(): Boolean = ctlQuery(OPUS_GET_DTX_REQUEST) == 1

/**
 * Gets the DTX state of the encoder.
 * @return true = The encoder is in DTX, false = The encoder is not in DTX
 */
fun OpusEncoder.getInDTX(): Boolean = ctlQuery(OPUS_GET_IN_DTX_REQUEST) == 1

/**
 * Configures the depth of signal being encoded.
 * @param lsbDepth Input precision in bits, between 8 and 24 (default: 24)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setLSBDepth(lsbDepth: Int): Int = ctl(OPUS_SET_LSB_DEPTH_REQUEST, lsbDepth)

/**
 * Gets the encoder's configured signal depth.
 * @return Input precision in bits, between 8 and 24 (default: 24)
 */
fun OpusEncoder.getLSBDepth(): Int = ctlQuery(OPUS_GET_LSB_DEPTH_REQUEST)

/**
 * Configures the encoder's use of variable duration frames.
 * @param frameSize One of:
 *  - [OPUS_FRAMESIZE_ARG]: Select frame size from the argument (default)
 *  - [OPUS_FRAMESIZE_2_5_MS]: Use 2.5 ms frames
 *  - [OPUS_FRAMESIZE_5_MS]: Use 5 ms frames
 *  - [OPUS_FRAMESIZE_10_MS]: Use 10 ms frames
 *  - [OPUS_FRAMESIZE_20_MS]: Use 20 ms frames
 *  - [OPUS_FRAMESIZE_40_MS]: Use 40 ms frames
 *  - [OPUS_FRAMESIZE_60_MS]: Use 60 ms frames
 *  - [OPUS_FRAMESIZE_80_MS]: Use 80 ms frames
 *  - [OPUS_FRAMESIZE_100_MS]: Use 100 ms frames
 *  - [OPUS_FRAMESIZE_120_MS]: Use 120 ms frames
 * @return OPUS_OK on success
 */
fun OpusEncoder.setExpertFrameDuration(frameSize: Int): Int = ctl(OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, frameSize)

/**
 * Gets the encoder's configured use of variable duration frames.
 * @return One of the OPUS_FRAMESIZE_* constants
 */
fun OpusEncoder.getExpertFrameDuration(): Int = ctlQuery(OPUS_GET_EXPERT_FRAME_DURATION_REQUEST)

/**
 * Configures use of prediction in the encoder.
 * @param disabled true = Disable prediction, false = Enable prediction (default)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setPredictionDisabled(disabled: Boolean): Int = ctl(OPUS_SET_PREDICTION_DISABLED_REQUEST, if (disabled) 1 else 0)

/**
 * Gets the encoder's configured prediction status.
 * @return true = Prediction disabled, false = Prediction enabled (default)
 */
fun OpusEncoder.getPredictionDisabled(): Boolean = ctlQuery(OPUS_GET_PREDICTION_DISABLED_REQUEST) == 1

/**
 * Configures use of phase inversion for intensity stereo.
 * @param disabled true = Disable phase inversion, false = Enable phase inversion (default)
 * @return OPUS_OK on success
 */
fun OpusEncoder.setPhaseInversionDisabled(disabled: Boolean): Int = ctl(OPUS_SET_PHASE_INVERSION_DISABLED_REQUEST, if (disabled) 1 else 0)

/**
 * Gets the encoder's configured phase inversion status.
 * @return true = Phase inversion disabled, false = Phase inversion enabled (default)
 */
fun OpusEncoder.getPhaseInversionDisabled(): Boolean = ctlQuery(OPUS_GET_PHASE_INVERSION_DISABLED_REQUEST) == 1

/**
 * Gets the total samples of delay added by the entire codec.
 * @return Number of lookahead samples
 */
fun OpusEncoder.getLookahead(): Int = ctlQuery(OPUS_GET_LOOKAHEAD_REQUEST)

/**
 * Gets the final state of the codec's entropy coder.
 * @return Entropy coder state
 */
fun OpusEncoder.getFinalRange(): Int = ctlQuery(OPUS_GET_FINAL_RANGE_REQUEST)

/**
 * Resets the codec state to be equivalent to a freshly initialized state.
 * @return OPUS_OK on success
 */
fun OpusEncoder.resetState(): Int = ctl(OPUS_RESET_STATE, 0)

/**
 * Gets the sampling rate the encoder was initialized with.
 * @return Sampling rate of encoder in Hz
 */
fun OpusEncoder.getSampleRate(): Int = ctlQuery(OPUS_GET_SAMPLE_RATE_REQUEST)

/**
 * Gets the pitch of the last decoded frame, if available.
 * @return Pitch period at 48 kHz (or 0 if not available)
 */
fun OpusEncoder.getPitch(): Int = ctlQuery(OPUS_GET_PITCH_REQUEST)

/**
 * Configures Deep Redundancy (DRED) max number of 10-ms redundant frames.
 * @param dredDuration Maximum number of 10-ms redundant frames
 * @return OPUS_OK on success
 */
fun OpusEncoder.setDREDDuration(dredDuration: Int): Int = ctl(OPUS_SET_DRED_DURATION_REQUEST, dredDuration)

/**
 * Gets the encoder's configured Deep Redundancy (DRED) maximum number of frames.
 * @return The maximum number of 10-ms redundant frames
 */
fun OpusEncoder.getDREDDuration(): Int = ctlQuery(OPUS_GET_DRED_DURATION_REQUEST)

/**
 * Enables or disables QEXT (Quality Extension) in the encoder.
 *
 * QEXT is an experimental feature that enables:
 * - 96kHz sampling rate support
 * - Up to 2Mb/s bitrate
 * - 20-bit depth
 *
 * **Warning:** QEXT will *hurt* audio quality unless operating at very high bitrates
 * (typically above 128 kb/s for stereo music).
 *
 * **Note:** This function is only effective in the `kopus-full` artifact.
 * On the base `kopus` artifact, this will return [OPUS_UNIMPLEMENTED].
 *
 * @param enabled true = Enable QEXT, false = Disable QEXT (default)
 * @return [OPUS_OK] on success, [OPUS_UNIMPLEMENTED] if QEXT is not available
 */
fun OpusEncoder.setQext(enabled: Boolean): Int = ctl(OPUS_SET_QEXT_REQUEST, if (enabled) 1 else 0)

/**
 * Gets whether QEXT (Quality Extension) is enabled in the encoder.
 *
 * **Note:** This function is only effective in the `kopus-full` artifact.
 * On the base `kopus` artifact, this will return false.
 *
 * @return true = QEXT enabled, false = QEXT disabled
 */
fun OpusEncoder.getQext(): Boolean = ctlQuery(OPUS_GET_QEXT_REQUEST) == 1

/**
 * A utility extension function that creates a new [ByteArray] each time it is called.
 */
fun OpusEncoder.encode(
    pcm: ShortArray,
): ByteArray {
    val outData = ByteArray(DEFAULT_MAX_BYTES)
    val encodedLen = encode(pcm, 0, pcm.size, outData, 0, DEFAULT_MAX_BYTES)
    return outData.copyOfRange(0, encodedLen)
}

fun OpusEncoder.encode(
    pcm: FloatArray,
): ByteArray {
    val outData = ByteArray(DEFAULT_MAX_BYTES)
    val encodedLen = encode(pcm, 0, pcm.size, outData, 0, DEFAULT_MAX_BYTES)
    return outData.copyOfRange(0, encodedLen)
}
