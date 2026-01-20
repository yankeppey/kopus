/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */

@file:OptIn(ExperimentalForeignApi::class)

package eu.buney.kopus

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import opus.c.opus_int16Var
import opus.c.opus_multistream_packet_pad
import opus.c.opus_multistream_packet_unpad
import opus.c.opus_packet_get_bandwidth
import opus.c.opus_packet_get_nb_channels
import opus.c.opus_packet_get_nb_frames
import opus.c.opus_packet_get_nb_samples
import opus.c.opus_packet_get_samples_per_frame
import opus.c.opus_packet_has_lbrr
import opus.c.opus_packet_pad
import opus.c.opus_packet_parse
import opus.c.opus_packet_unpad
import platform.posix.int32_tVar

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

    actual fun parse(packet: ByteArray, len: Int): PacketFrameInfo? {
        if (len < 1) return null

        return packet.usePinned { pinned ->
            memScoped {
                val dataPtr = pinned.addressOf(0).reinterpret<UByteVar>()
                val tocVar = alloc<UByteVar>()
                val framesArray = allocArray<CPointerVar<UByteVar>>(48)
                val sizesArray = allocArray<opus_int16Var>(48)
                val payloadOffsetVar = alloc<int32_tVar>()

                val numFrames = opus_packet_parse(
                    dataPtr,
                    len,
                    tocVar.ptr,
                    framesArray,
                    sizesArray,
                    payloadOffsetVar.ptr
                )

                if (numFrames < 1) {
                    return@usePinned null
                }

                // Convert frame pointers to offsets
                val frameOffsets = IntArray(numFrames) { i ->
                    val framePtr = framesArray[i]
                    if (framePtr != null) {
                        (framePtr.rawValue.toLong() - dataPtr.rawValue.toLong()).toInt()
                    } else {
                        0
                    }
                }

                val frameSizes = IntArray(numFrames) { i ->
                    sizesArray[i].toInt()
                }

                PacketFrameInfo(
                    toc = tocVar.value.toByte(),
                    numFrames = numFrames,
                    frameOffsets = frameOffsets,
                    frameSizes = frameSizes,
                    payloadOffset = payloadOffsetVar.value
                )
            }
        }
    }
}
