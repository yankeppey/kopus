/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

expect object Opus {
    fun getOpusVersion(): String
}

/**
 * Whether DRED (Deep Redundancy) is available in this build.
 * DRED provides neural network-based packet loss recovery.
 * Only available in the kopus-full artifact.
 */
val Opus.isDredAvailable: Boolean
    get() = IS_FULL_VARIANT

/**
 * Whether OSCE (Opus Speech Coding Enhancement) is available in this build.
 * OSCE provides neural network bandwidth extension (8kHz → 20kHz).
 * Only available in the kopus-full artifact.
 */
val Opus.isOsceAvailable: Boolean
    get() = IS_FULL_VARIANT

/**
 * Whether QEXT (Quality Extension) is available in this build.
 * QEXT enables 96kHz support, up to 2Mb/s bitrate, and 20-bit depth.
 * Only available in the kopus-full artifact.
 */
val Opus.isQextAvailable: Boolean
    get() = IS_FULL_VARIANT

enum class OpusApplication(val value: Int) {
    Voip(2048),
    Audio(2049),
    RestrictedLowDelay(2051),
}