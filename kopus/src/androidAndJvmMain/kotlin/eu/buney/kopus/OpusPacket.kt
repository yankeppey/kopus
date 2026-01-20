/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual object OpusPacket {
    init {
        OpusLoader.load()
    }

    actual fun getBandwidth(data: ByteArray): Int =
        nativeGetBandwidth(data)

    actual fun getSamplesPerFrame(data: ByteArray, sampleRate: Int): Int =
        nativeGetSamplesPerFrame(data, sampleRate)

    actual fun getNbChannels(data: ByteArray): Int =
        nativeGetNbChannels(data)

    actual fun getNbFrames(packet: ByteArray, len: Int): Int =
        nativeGetNbFrames(packet, len)

    actual fun getNbSamples(packet: ByteArray, len: Int, sampleRate: Int): Int =
        nativeGetNbSamples(packet, len, sampleRate)

    actual fun hasLbrr(packet: ByteArray, len: Int): Boolean =
        nativeHasLbrr(packet, len)

    actual fun pad(data: ByteArray, len: Int, newLen: Int): Int =
        nativePad(data, len, newLen)

    actual fun unpad(data: ByteArray, len: Int): Int =
        nativeUnpad(data, len)

    actual fun padMultistream(data: ByteArray, len: Int, newLen: Int, nbStreams: Int): Int =
        nativePadMultistream(data, len, newLen, nbStreams)

    actual fun unpadMultistream(data: ByteArray, len: Int, nbStreams: Int): Int =
        nativeUnpadMultistream(data, len, nbStreams)

    private external fun nativeGetBandwidth(data: ByteArray): Int
    private external fun nativeGetSamplesPerFrame(data: ByteArray, sampleRate: Int): Int
    private external fun nativeGetNbChannels(data: ByteArray): Int
    private external fun nativeGetNbFrames(packet: ByteArray, len: Int): Int
    private external fun nativeGetNbSamples(packet: ByteArray, len: Int, sampleRate: Int): Int
    private external fun nativeHasLbrr(packet: ByteArray, len: Int): Boolean
    private external fun nativePad(data: ByteArray, len: Int, newLen: Int): Int
    private external fun nativeUnpad(data: ByteArray, len: Int): Int
    private external fun nativePadMultistream(data: ByteArray, len: Int, newLen: Int, nbStreams: Int): Int
    private external fun nativeUnpadMultistream(data: ByteArray, len: Int, nbStreams: Int): Int
}
