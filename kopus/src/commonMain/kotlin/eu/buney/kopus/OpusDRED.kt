/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

/**
 * Holds parsed DRED (Deep Redundancy) data from an Opus packet.
 *
 * DRED is an experimental Opus feature that provides neural network-based
 * packet loss recovery, storing up to ~1 second of redundant audio data.
 *
 * This class wraps the native `OpusDRED` structure. Use [OpusDREDDecoder.parse]
 * to populate this object with DRED data from a packet, then pass it to
 * [OpusDecoder.decodeDred] to recover audio.
 *
 * **Note:** This class is only functional in the `kopus-full` artifact.
 * On the base `kopus` artifact, the constructor will throw [UnsupportedOperationException].
 *
 * @throws UnsupportedOperationException if DRED is not available in this build
 */
expect class OpusDRED() : AutoCloseable {

    /**
     * Releases resources associated with this DRED state.
     *
     * Mirrors the C function `opus_dred_free`.
     */
    override fun close()
}
