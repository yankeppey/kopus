/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

/**
 * Packet inspection utilities for analyzing Opus packets without decoding.
 *
 * These functions allow examination of Opus packet properties such as bandwidth,
 * channel count, frame count, and sample count directly from the packet data.
 * This is useful for buffer allocation, packet validation, and debugging.
 */
expect object OpusPacket {

    /**
     * Gets the bandwidth of an Opus packet.
     *
     * Mirrors the C function `opus_packet_get_bandwidth`.
     *
     * @param data Opus packet data (at least 1 byte)
     * @return One of [OPUS_BANDWIDTH_NARROWBAND], [OPUS_BANDWIDTH_MEDIUMBAND],
     *         [OPUS_BANDWIDTH_WIDEBAND], [OPUS_BANDWIDTH_SUPERWIDEBAND],
     *         [OPUS_BANDWIDTH_FULLBAND], or [OPUS_INVALID_PACKET] if corrupted
     */
    fun getBandwidth(data: ByteArray): Int

    /**
     * Gets the number of samples per frame from an Opus packet.
     *
     * Mirrors the C function `opus_packet_get_samples_per_frame`.
     *
     * @param data Opus packet data (at least 1 byte)
     * @param sampleRate Sampling rate in Hz (must be a multiple of 400)
     * @return Number of samples per frame
     */
    fun getSamplesPerFrame(data: ByteArray, sampleRate: Int): Int

    /**
     * Gets the number of channels from an Opus packet.
     *
     * Mirrors the C function `opus_packet_get_nb_channels`.
     *
     * @param data Opus packet data (at least 1 byte)
     * @return Number of channels (1 or 2), or [OPUS_INVALID_PACKET] if corrupted
     */
    fun getNbChannels(data: ByteArray): Int

    /**
     * Gets the number of frames in an Opus packet.
     *
     * Mirrors the C function `opus_packet_get_nb_frames`.
     *
     * @param packet Opus packet data
     * @param len Length of the packet in bytes
     * @return Number of frames, or [OPUS_BAD_ARG] if insufficient data,
     *         or [OPUS_INVALID_PACKET] if corrupted
     */
    fun getNbFrames(packet: ByteArray, len: Int = packet.size): Int

    /**
     * Gets the number of samples of an Opus packet.
     *
     * Mirrors the C function `opus_packet_get_nb_samples`.
     *
     * @param packet Opus packet data
     * @param len Length of the packet in bytes
     * @param sampleRate Sampling rate in Hz (must be a multiple of 400)
     * @return Number of samples, or [OPUS_BAD_ARG] if insufficient data,
     *         or [OPUS_INVALID_PACKET] if corrupted
     */
    fun getNbSamples(packet: ByteArray, len: Int = packet.size, sampleRate: Int): Int

    /**
     * Checks whether an Opus packet has LBRR (Low Bit-Rate Redundancy).
     *
     * Mirrors the C function `opus_packet_has_lbrr`.
     *
     * @param packet Opus packet data
     * @param len Length of the packet in bytes
     * @return true if LBRR is present, false otherwise.
     *         Returns false for invalid packets.
     */
    fun hasLbrr(packet: ByteArray, len: Int = packet.size): Boolean

    /**
     * Pads an Opus packet to a larger size.
     *
     * This modifies the packet in place. The buffer must have room for [newLen] bytes.
     *
     * Mirrors the C function `opus_packet_pad`.
     *
     * @param data The packet buffer (must have capacity for newLen bytes)
     * @param len Current packet size (must be >= 1)
     * @param newLen Desired packet size after padding (must be >= len)
     * @return [OPUS_OK] on success, or an error code:
     *         - [OPUS_BAD_ARG]: len < 1 or newLen < len
     *         - [OPUS_INVALID_PACKET]: Invalid Opus packet
     */
    fun pad(data: ByteArray, len: Int, newLen: Int): Int

    /**
     * Removes all padding from an Opus packet.
     *
     * This modifies the packet in place, rewriting the TOC to minimize space.
     *
     * Mirrors the C function `opus_packet_unpad`.
     *
     * @param data The packet buffer
     * @param len Current packet size (must be >= 1)
     * @return The new packet size on success, or an error code:
     *         - [OPUS_BAD_ARG]: len < 1
     *         - [OPUS_INVALID_PACKET]: Invalid Opus packet
     */
    fun unpad(data: ByteArray, len: Int): Int

    /**
     * Pads a multistream Opus packet to a larger size.
     *
     * Mirrors the C function `opus_multistream_packet_pad`.
     *
     * @param data The packet buffer (must have capacity for newLen bytes)
     * @param len Current packet size (must be >= 1)
     * @param newLen Desired packet size after padding (must be >= len)
     * @param nbStreams Number of streams in the packet (must be >= 1)
     * @return [OPUS_OK] on success, or an error code
     */
    fun padMultistream(data: ByteArray, len: Int, newLen: Int, nbStreams: Int): Int

    /**
     * Removes all padding from a multistream Opus packet.
     *
     * Mirrors the C function `opus_multistream_packet_unpad`.
     *
     * @param data The packet buffer
     * @param len Current packet size (must be >= 1)
     * @param nbStreams Number of streams in the packet (must be >= 1)
     * @return The new packet size on success, or an error code
     */
    fun unpadMultistream(data: ByteArray, len: Int, nbStreams: Int): Int
}
