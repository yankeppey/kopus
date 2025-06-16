/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */

@file:OptIn(ExperimentalForeignApi::class)

package eu.buney.kopus

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import opus.c.*

/* ---------- Packet helpers ---------- */
object OpusPacket {
    fun bandwidth(packet: UByteArray) = opus_packet_get_bandwidth(packet.refTo(0))
    fun samplesPerFrame(packet: UByteArray, Fs: Int) =
        opus_packet_get_samples_per_frame(packet.refTo(0), Fs)

    fun channels(packet: UByteArray) = opus_packet_get_nb_channels(packet.refTo(0))
    fun frames(packet: UByteArray) = opus_packet_get_nb_frames(packet.refTo(0), packet.size)
    fun samples(packet: UByteArray, Fs: Int) =
        opus_packet_get_nb_samples(packet.refTo(0), packet.size, Fs)
}