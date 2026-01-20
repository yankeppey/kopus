/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("unused")
package eu.buney.kopus

/**
 * Extension functions for OpusMultistreamEncoder to provide easy access to encoder control parameters.
 *
 * These functions wrap calls to the OpusMultistreamEncoder.ctl and OpusMultistreamEncoder.ctlQuery methods,
 * which internally use the `opus_multistream_encoder_ctl` C function.
 */

/**
 * Configures the encoder's intended application.
 * @param application One of:
 *  - [OPUS_APPLICATION_VOIP]: Process signal for improved speech intelligibility
 *  - [OPUS_APPLICATION_AUDIO]: Favor faithfulness to the original input
 *  - [OPUS_APPLICATION_RESTRICTED_LOWDELAY]: Configure the minimum possible coding delay
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setApplication(application: Int): Int = ctl(OPUS_SET_APPLICATION_REQUEST, application)

/**
 * Gets the encoder's configured application.
 * @return One of [OPUS_APPLICATION_VOIP], [OPUS_APPLICATION_AUDIO], or [OPUS_APPLICATION_RESTRICTED_LOWDELAY]
 */
fun OpusMultistreamEncoder.getApplication(): Int = ctlQuery(OPUS_GET_APPLICATION_REQUEST)

/**
 * Configures the bitrate in the encoder.
 * @param bitrate Bitrate in bits per second. The default is determined based on channels and sampling rate.
 *                Use [OPUS_AUTO] or [OPUS_BITRATE_MAX] for automatic configuration.
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setBitrate(bitrate: Int): Int = ctl(OPUS_SET_BITRATE_REQUEST, bitrate)

/**
 * Gets the encoder's bitrate configuration.
 * @return The bitrate in bits per second
 */
fun OpusMultistreamEncoder.getBitrate(): Int = ctlQuery(OPUS_GET_BITRATE_REQUEST)

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
fun OpusMultistreamEncoder.setBandwidth(bandwidth: Int): Int = ctl(OPUS_SET_BANDWIDTH_REQUEST, bandwidth)

/**
 * Gets the encoder's configured bandwidth setting.
 * @return One of the OPUS_BANDWIDTH_* constants or OPUS_AUTO
 */
fun OpusMultistreamEncoder.getBandwidth(): Int = ctlQuery(OPUS_GET_BANDWIDTH_REQUEST)

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
fun OpusMultistreamEncoder.setMaxBandwidth(maxBandwidth: Int): Int = ctl(OPUS_SET_MAX_BANDWIDTH_REQUEST, maxBandwidth)

/**
 * Gets the encoder's configured maximum allowed bandwidth.
 * @return One of the OPUS_BANDWIDTH_* constants
 */
fun OpusMultistreamEncoder.getMaxBandwidth(): Int = ctlQuery(OPUS_GET_MAX_BANDWIDTH_REQUEST)

/**
 * Configures the type of signal being encoded.
 * This is a hint which helps the encoder's mode selection.
 * @param signal One of:
 *  - [OPUS_AUTO]: (default)
 *  - [OPUS_SIGNAL_VOICE]: Bias thresholds towards choosing LPC or Hybrid modes
 *  - [OPUS_SIGNAL_MUSIC]: Bias thresholds towards choosing MDCT modes
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setSignal(signal: Int): Int = ctl(OPUS_SET_SIGNAL_REQUEST, signal)

/**
 * Gets the encoder's configured signal type.
 * @return One of [OPUS_AUTO], [OPUS_SIGNAL_VOICE], or [OPUS_SIGNAL_MUSIC]
 */
fun OpusMultistreamEncoder.getSignal(): Int = ctlQuery(OPUS_GET_SIGNAL_REQUEST)

/**
 * Configures the encoder's computational complexity.
 * @param complexity Value from 0-10, with 10 representing the highest complexity.
 *                   Higher complexity produces better quality at a given bitrate.
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setComplexity(complexity: Int): Int = ctl(OPUS_SET_COMPLEXITY_REQUEST, complexity)

/**
 * Gets the encoder's complexity configuration.
 * @return A value from 0-10
 */
fun OpusMultistreamEncoder.getComplexity(): Int = ctlQuery(OPUS_GET_COMPLEXITY_REQUEST)

/**
 * Enables or disables variable bitrate (VBR) in the encoder.
 * @param vbr 0 = Hard CBR, 1 = VBR (default)
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setVBR(vbr: Int): Int = ctl(OPUS_SET_VBR_REQUEST, vbr)

/**
 * Determine if variable bitrate (VBR) is enabled in the encoder.
 * @return 0 = Hard CBR, 1 = VBR (default)
 */
fun OpusMultistreamEncoder.getVBR(): Int = ctlQuery(OPUS_GET_VBR_REQUEST)

/**
 * Enables or disables constrained VBR in the encoder.
 * @param cvbr 0 = Unconstrained VBR, 1 = Constrained VBR (default)
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setVBRConstraint(cvbr: Int): Int = ctl(OPUS_SET_VBR_CONSTRAINT_REQUEST, cvbr)

/**
 * Determine if constrained VBR is enabled in the encoder.
 * @return 0 = Unconstrained VBR, 1 = Constrained VBR (default)
 */
fun OpusMultistreamEncoder.getVBRConstraint(): Int = ctlQuery(OPUS_GET_VBR_CONSTRAINT_REQUEST)

/**
 * Configures the encoder's use of inband forward error correction (FEC).
 * @param fec 0 = Disable FEC (default), 1 = Enable FEC
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setInbandFEC(fec: Int): Int = ctl(OPUS_SET_INBAND_FEC_REQUEST, fec)

/**
 * Gets encoder's configured use of inband forward error correction.
 * @return 0 = FEC disabled, 1 = FEC enabled
 */
fun OpusMultistreamEncoder.getInbandFEC(): Int = ctlQuery(OPUS_GET_INBAND_FEC_REQUEST)

/**
 * Configures the encoder's expected packet loss percentage.
 * @param packetLossPerc Loss percentage in the range 0-100 (default: 0)
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setPacketLossPerc(packetLossPerc: Int): Int = ctl(OPUS_SET_PACKET_LOSS_PERC_REQUEST, packetLossPerc)

/**
 * Gets the encoder's configured packet loss percentage.
 * @return The configured loss percentage in the range 0-100
 */
fun OpusMultistreamEncoder.getPacketLossPerc(): Int = ctlQuery(OPUS_GET_PACKET_LOSS_PERC_REQUEST)

/**
 * Configures mono/stereo forcing in the encoder.
 * @param forceChannels [OPUS_AUTO] = Not forced (default), 1 = Forced mono, 2 = Forced stereo
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setForceChannels(forceChannels: Int): Int = ctl(OPUS_SET_FORCE_CHANNELS_REQUEST, forceChannels)

/**
 * Gets the encoder's forced channel configuration.
 * @return [OPUS_AUTO] = Not forced (default), 1 = Forced mono, 2 = Forced stereo
 */
fun OpusMultistreamEncoder.getForceChannels(): Int = ctlQuery(OPUS_GET_FORCE_CHANNELS_REQUEST)

/**
 * Configures the encoder's use of discontinuous transmission (DTX).
 * @param dtx 0 = Disable DTX (default), 1 = Enable DTX
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setDTX(dtx: Int): Int = ctl(OPUS_SET_DTX_REQUEST, dtx)

/**
 * Gets the encoder's configured use of discontinuous transmission.
 * @return 0 = DTX disabled, 1 = DTX enabled
 */
fun OpusMultistreamEncoder.getDTX(): Int = ctlQuery(OPUS_GET_DTX_REQUEST)

/**
 * Gets the DTX state of the encoder.
 * @return 0 = The encoder is not in DTX, 1 = The encoder is in DTX
 */
fun OpusMultistreamEncoder.getInDTX(): Int = ctlQuery(OPUS_GET_IN_DTX_REQUEST)

/**
 * Configures the depth of signal being encoded.
 * @param lsbDepth Input precision in bits, between 8 and 24 (default: 24)
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setLSBDepth(lsbDepth: Int): Int = ctl(OPUS_SET_LSB_DEPTH_REQUEST, lsbDepth)

/**
 * Gets the encoder's configured signal depth.
 * @return Input precision in bits, between 8 and 24 (default: 24)
 */
fun OpusMultistreamEncoder.getLSBDepth(): Int = ctlQuery(OPUS_GET_LSB_DEPTH_REQUEST)

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
fun OpusMultistreamEncoder.setExpertFrameDuration(frameSize: Int): Int = ctl(OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, frameSize)

/**
 * Gets the encoder's configured use of variable duration frames.
 * @return One of the OPUS_FRAMESIZE_* constants
 */
fun OpusMultistreamEncoder.getExpertFrameDuration(): Int = ctlQuery(OPUS_GET_EXPERT_FRAME_DURATION_REQUEST)

/**
 * Configures use of prediction in the encoder.
 * @param disabled 0 = Enable prediction (default), 1 = Disable prediction
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setPredictionDisabled(disabled: Int): Int = ctl(OPUS_SET_PREDICTION_DISABLED_REQUEST, disabled)

/**
 * Gets the encoder's configured prediction status.
 * @return 0 = Prediction enabled (default), 1 = Prediction disabled
 */
fun OpusMultistreamEncoder.getPredictionDisabled(): Int = ctlQuery(OPUS_GET_PREDICTION_DISABLED_REQUEST)

/**
 * Configures use of phase inversion for intensity stereo.
 * @param disabled 0 = Enable phase inversion (default), 1 = Disable phase inversion
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.setPhaseInversionDisabled(disabled: Int): Int = ctl(OPUS_SET_PHASE_INVERSION_DISABLED_REQUEST, disabled)

/**
 * Gets the encoder's configured phase inversion status.
 * @return 0 = Phase inversion enabled (default), 1 = Phase inversion disabled
 */
fun OpusMultistreamEncoder.getPhaseInversionDisabled(): Int = ctlQuery(OPUS_GET_PHASE_INVERSION_DISABLED_REQUEST)

/**
 * Gets the total samples of delay added by the entire codec.
 * @return Number of lookahead samples
 */
fun OpusMultistreamEncoder.getLookahead(): Int = ctlQuery(OPUS_GET_LOOKAHEAD_REQUEST)

/**
 * Gets the final state of the codec's entropy coder.
 * @return Entropy coder state
 */
fun OpusMultistreamEncoder.getFinalRange(): Int = ctlQuery(OPUS_GET_FINAL_RANGE_REQUEST)

/**
 * Gets the sampling rate the encoder was initialized with.
 * @return Sampling rate of encoder in Hz
 */
fun OpusMultistreamEncoder.getSampleRate(): Int = ctlQuery(OPUS_GET_SAMPLE_RATE_REQUEST)

/**
 * Gets the pitch of the last decoded frame, if available.
 * @return Pitch period at 48 kHz (or 0 if not available)
 */
fun OpusMultistreamEncoder.getPitch(): Int = ctlQuery(OPUS_GET_PITCH_REQUEST)

/**
 * Resets the codec state to be equivalent to a freshly initialized state.
 * @return OPUS_OK on success
 */
fun OpusMultistreamEncoder.resetState(): Int = ctl(OPUS_RESET_STATE, 0)
