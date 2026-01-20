/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

/**
 * Thin, idiomatic wrapper over the native *OpusDecoder* C API.
 *
 * This class provides a Kotlin interface to the Opus audio codec decoder functionality.
 * It mirrors the behavior of the C functions:
 * - `opus_decoder_create` / `opus_decoder_init` for initialization
 * - `opus_decode` / `opus_decode_float` for decoding audio packets
 * - `opus_decoder_ctl` for controlling decoder parameters
 * - `opus_decoder_destroy` for cleanup
 */
expect class OpusDecoder(
    sampleRate: Int = 48_000,
    channels: Int = 1,
) : AutoCloseable {

    val channels: Int

    /**
     * Decodes an Opus packet into PCM audio using 16-bit format.
     *
     * Mirrors the C function `opus_decode`.
     *
     * @param inData The input payload. Use a NULL pointer to indicate packet loss
     * @param inDataOffset The offset to use when reading the input payload. Usually 0
     * @param len The number of bytes in the payload (the packet size)
     * @param outPcm A buffer to put the output PCM, in a short array. The output size is (# of samples) * (# of channels) * 2.
     * You can use the OpusPacketInfo helpers to get a hint of the frame size before you decode the packet if you need exact sizing.
     * @param outPcmOffset The offset to use when writing to the output buffer
     * @param frameSize The number of samples (per channel) of available space in the output PCM buffer.
     * If this is less than the maximum packet duration (120ms; 5760 for 48kHz), this function will
     * not be capable of decoding some packets. In the case of PLC (data == NULL) or FEC (decodeFec == true),
     * then frameSize needs to be exactly the duration of the audio that is missing, otherwise the decoder will
     * not be in an optimal state to decode the next incoming packet. For the PLC and FEC cases, frameSize *must*
     * be a multiple of 2.5 ms.
     * @param decodeFec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data from THIS packet. Using this packet
     * recovery scheme, you will actually decode this packet twice, first with decodeFec TRUE and then again with FALSE. If FEC data is not
     * available in this packet, the decoder will simply generate a best-effort recreation of the lost packet. In that case, the
     * length of frameSize must be EXACTLY the length of the audio that was lost, or else the decoder will be in an inconsistent state.
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
     * Decodes an Opus packet into PCM audio using floating point format.
     *
     * Mirrors the C function `opus_decode_float`.
     *
     * @param inData The input payload. Use a NULL pointer to indicate packet loss
     * @param inDataOffset The offset to use when reading the input payload. Usually 0
     * @param len The number of bytes in the payload (the packet size)
     * @param outPcm A buffer to put the output PCM as floating point values. The output size is (# of samples) * (# of channels) * sizeof(float).
     * @param outPcmOffset The offset to use when writing to the output buffer
     * @param frameSize The number of samples (per channel) of available space in the output PCM buffer
     * @param decodeFec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data from THIS packet
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
     * Decodes an Opus packet into PCM audio using 24-bit integer format.
     *
     * Mirrors the C function `opus_decode24`.
     *
     * @param inData The input payload. Use null to indicate packet loss (PLC)
     * @param inDataOffset The offset to use when reading the input payload. Usually 0
     * @param len The number of bytes in the payload (the packet size)
     * @param outPcm A buffer to put the output PCM as 24-bit values stored in 32-bit integers.
     *               The output size is (# of samples) * (# of channels).
     * @param outPcmOffset The offset to use when writing to the output buffer
     * @param frameSize The number of samples (per channel) of available space in the output PCM buffer
     * @param decodeFec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data from THIS packet
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
     * Mirrors the C function `opus_decoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the decoder to set a parameter value.
     *
     * Mirrors the C function `opus_decoder_ctl` for setting values.
     *
     * @param request A control request code from the OPUS_SET_* constants
     * @param value The value to set for the requested parameter
     * @return OPUS_OK on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the decoder to query a parameter value.
     *
     * Mirrors the C function `opus_decoder_ctl` for getting values.
     *
     * @param request A control request code from the OPUS_GET_* constants
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int
}