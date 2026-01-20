/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */

@file:OptIn(ExperimentalForeignApi::class)

package eu.buney.kopus

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import opus.c.opus_multistream_packet_pad
import opus.c.opus_multistream_packet_unpad
import opus.c.opus_packet_get_bandwidth
import opus.c.opus_packet_get_nb_channels
import opus.c.opus_packet_get_nb_frames
import opus.c.opus_packet_get_nb_samples
import opus.c.opus_packet_get_samples_per_frame
import opus.c.opus_packet_has_lbrr
import opus.c.opus_packet_pad
import opus.c.opus_packet_unpad

actual object OpusPacket {

    actual fun getBandwidth(data: ByteArray): Int =
        data.usePinned { pinned ->
            opus_packet_get_bandwidth(pinned.addressOf(0).reinterpret())
        }

    actual fun getSamplesPerFrame(data: ByteArray, sampleRate: Int): Int =
        data.usePinned { pinned ->
            opus_packet_get_samples_per_frame(pinned.addressOf(0).reinterpret(), sampleRate)
        }

    actual fun getNbChannels(data: ByteArray): Int =
        data.usePinned { pinned ->
            opus_packet_get_nb_channels(pinned.addressOf(0).reinterpret())
        }

    actual fun getNbFrames(packet: ByteArray, len: Int): Int =
        packet.usePinned { pinned ->
            opus_packet_get_nb_frames(pinned.addressOf(0).reinterpret(), len)
        }

    actual fun getNbSamples(packet: ByteArray, len: Int, sampleRate: Int): Int =
        packet.usePinned { pinned ->
            opus_packet_get_nb_samples(pinned.addressOf(0).reinterpret(), len, sampleRate)
        }

    actual fun hasLbrr(packet: ByteArray, len: Int): Boolean =
        packet.usePinned { pinned ->
            opus_packet_has_lbrr(pinned.addressOf(0).reinterpret(), len) == 1
        }

    actual fun pad(data: ByteArray, len: Int, newLen: Int): Int =
        data.usePinned { pinned ->
            opus_packet_pad(pinned.addressOf(0).reinterpret(), len, newLen)
        }

    actual fun unpad(data: ByteArray, len: Int): Int =
        data.usePinned { pinned ->
            opus_packet_unpad(pinned.addressOf(0).reinterpret(), len)
        }

    actual fun padMultistream(data: ByteArray, len: Int, newLen: Int, nbStreams: Int): Int =
        data.usePinned { pinned ->
            opus_multistream_packet_pad(pinned.addressOf(0).reinterpret(), len, newLen, nbStreams)
        }

    actual fun unpadMultistream(data: ByteArray, len: Int, nbStreams: Int): Int =
        data.usePinned { pinned ->
            opus_multistream_packet_unpad(pinned.addressOf(0).reinterpret(), len, nbStreams)
        }
}
