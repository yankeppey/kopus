/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

actual object Opus {
    init {
        OpusLoader.load()
    }

    actual external fun getOpusVersion(): String
}
