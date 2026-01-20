/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

/**
 * Result of parsing DRED data from a packet.
 */
data class DredParseResult(
    /**
     * Offset (in samples) of the first decoded DRED samples.
     * Zero if no DRED is present.
     * Negative values indicate an error code.
     */
    val offset: Int,

    /**
     * Number of non-encoded (silence) samples between the DRED timestamp
     * and the last DRED sample.
     */
    val dredEnd: Int
)

/**
 * Parses and processes DRED (Deep Redundancy) data from Opus packets.
 *
 * DRED is an experimental Opus feature that provides neural network-based
 * packet loss recovery. This decoder extracts DRED redundancy data from
 * encoded packets, which can then be used to recover audio when packets are lost.
 *
 * Typical workflow:
 * 1. Create an [OpusDREDDecoder] instance
 * 2. Create an [OpusDRED] instance to hold the parsed data
 * 3. Call [parse] to extract DRED data from a packet
 * 4. Pass the [OpusDRED] to [OpusDecoder.decodeDred] to recover audio
 *
 * **Note:** This class is only functional in the `kopus-full` artifact.
 * On the base `kopus` artifact, the constructor will throw [UnsupportedOperationException].
 *
 * @throws UnsupportedOperationException if DRED is not available in this build
 */
expect class OpusDREDDecoder() : AutoCloseable {

    /**
     * Parses DRED data from an Opus packet.
     *
     * Mirrors the C function `opus_dred_parse`.
     *
     * @param dred The [OpusDRED] object to populate with parsed data
     * @param data The input packet data containing DRED information
     * @param dataOffset Offset into the data array to start reading from
     * @param len Number of bytes to read from the data array
     * @param maxDredSamples Maximum number of DRED samples that may be needed
     * @param samplingRate Sampling rate used for maxDredSamples argument (needs not match decoder's rate)
     * @param deferProcessing If true, CPU-intensive processing is deferred until [process] is called
     * @return A [DredParseResult] containing the offset of the first decoded DRED sample and
     *         the number of non-encoded samples between DRED timestamp and last DRED sample,
     *         or a negative error code
     */
    fun parse(
        dred: OpusDRED,
        data: ByteArray,
        dataOffset: Int = 0,
        len: Int = data.size,
        maxDredSamples: Int,
        samplingRate: Int,
        deferProcessing: Boolean = false
    ): DredParseResult

    /**
     * Finishes decoding DRED data when [parse] was called with deferProcessing=true.
     *
     * Mirrors the C function `opus_dred_process`.
     *
     * @param src Source DRED state to process
     * @param dst Destination DRED state to store the result (can be the same as src)
     * @return OPUS_OK on success or a negative error code on failure
     */
    fun process(src: OpusDRED, dst: OpusDRED): Int

    /**
     * Releases resources associated with this DRED decoder.
     *
     * Mirrors the C function `opus_dred_decoder_destroy`.
     */
    override fun close()

    /**
     * Performs a CTL (control) operation on the DRED decoder to set a parameter value.
     *
     * Mirrors the C function `opus_dred_decoder_ctl` for setting values.
     *
     * @param request A control request code
     * @param value The value to set for the requested parameter
     * @return OPUS_OK on success or an error code on failure
     */
    fun ctl(request: Int, value: Int): Int

    /**
     * Performs a CTL (control) operation on the DRED decoder to query a parameter value.
     *
     * Mirrors the C function `opus_dred_decoder_ctl` for getting values.
     *
     * @param request A control request code
     * @return The requested parameter value on success or an error code on failure
     */
    fun ctlQuery(request: Int): Int
}
