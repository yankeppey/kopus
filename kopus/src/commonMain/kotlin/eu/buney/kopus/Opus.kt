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

enum class OpusApplication(val value: Int) {
    Voip(2048),
    Audio(2049),
    RestrictedLowDelay(2051),
}