/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package eu.buney.kopus

/**
 * Thin wrapper over the native Opus Repacketizer API.
 *
 * The repacketizer allows merging multiple Opus packets into a single packet
 * (up to 120ms total duration) or splitting a multi-frame packet into
 * individual frame packets.
 *
 * This class mirrors the behavior of the C functions:
 * - `opus_repacketizer_create` for creation
 * - `opus_repacketizer_init` for resetting state
 * - `opus_repacketizer_cat` for adding packets
 * - `opus_repacketizer_out` / `opus_repacketizer_out_range` for extracting packets
 * - `opus_repacketizer_get_nb_frames` for querying frame count
 * - `opus_repacketizer_destroy` for cleanup
 *
 * Usage:
 * 1. Create a repacketizer
 * 2. Add packets with [cat] (must have same configuration)
 * 3. Extract merged packet with [out] or [outRange]
 * 4. Call [init] to reset for next batch
 *
 * IMPORTANT: All packets added via [cat] must have the same coding mode,
 * audio bandwidth, frame size, and channel count.
 *
 * Example - Merging packets:
 * ```kotlin
 * val repacketizer = OpusRepacketizer()
 * repacketizer.cat(packet1, packet1.size)
 * repacketizer.cat(packet2, packet2.size)
 * repacketizer.cat(packet3, packet3.size)
 *
 * val merged = ByteArray(4000)
 * val mergedLen = repacketizer.out(merged)
 * // merged now contains all three packets combined
 *
 * repacketizer.close()
 * ```
 *
 * Example - Splitting a multi-frame packet:
 * ```kotlin
 * val repacketizer = OpusRepacketizer()
 * repacketizer.cat(multiFramePacket, multiFramePacket.size)
 *
 * val nbFrames = repacketizer.getNbFrames()
 * for (i in 0 until nbFrames) {
 *     val frame = ByteArray(1500)
 *     val frameLen = repacketizer.outRange(i, i + 1, frame)
 *     // Process individual frame
 * }
 *
 * repacketizer.close()
 * ```
 */
expect class OpusRepacketizer() : AutoCloseable {

    /**
     * Resets the repacketizer state for a new batch of packets.
     *
     * Must be called before adding packets with a different configuration,
     * or when the 120ms maximum duration has been reached.
     *
     * Mirrors the C function `opus_repacketizer_init`.
     */
    fun init()

    /**
     * Adds a packet to the repacketizer.
     *
     * The packet must match the configuration of any previously added packets
     * (same coding mode, audio bandwidth, frame size, and channel count).
     * The total duration of all added packets must not exceed 120ms.
     *
     * Mirrors the C function `opus_repacketizer_cat`.
     *
     * @param data The Opus packet data
     * @param len The number of bytes in the packet
     * @return [OPUS_OK] on success, or an error code:
     *         - [OPUS_INVALID_PACKET]: Invalid TOC, incompatible configuration,
     *           or would exceed 120ms total duration
     */
    fun cat(data: ByteArray, len: Int = data.size): Int

    /**
     * Gets the number of frames currently stored in the repacketizer.
     *
     * Mirrors the C function `opus_repacketizer_get_nb_frames`.
     *
     * @return The number of frames added since the last [init]
     */
    fun getNbFrames(): Int

    /**
     * Constructs a new packet from a range of frames.
     *
     * Mirrors the C function `opus_repacketizer_out_range`.
     *
     * @param begin Index of the first frame to include (0-based)
     * @param end One past the index of the last frame to include
     * @param data Output buffer for the new packet
     * @param maxLen Maximum bytes to write
     * @return The size of the output packet on success, or an error code:
     *         - [OPUS_BAD_ARG]: Invalid frame range
     *         - [OPUS_BUFFER_TOO_SMALL]: Output buffer too small
     */
    fun outRange(begin: Int, end: Int, data: ByteArray, maxLen: Int = data.size): Int

    /**
     * Constructs a new packet containing all frames.
     *
     * Equivalent to `outRange(0, getNbFrames(), data, maxLen)`.
     *
     * Mirrors the C function `opus_repacketizer_out`.
     *
     * @param data Output buffer for the new packet
     * @param maxLen Maximum bytes to write
     * @return The size of the output packet on success, or [OPUS_BUFFER_TOO_SMALL]
     */
    fun out(data: ByteArray, maxLen: Int = data.size): Int

    /**
     * Releases native resources.
     *
     * Mirrors the C function `opus_repacketizer_destroy`.
     */
    override fun close()
}
