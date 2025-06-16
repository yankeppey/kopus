/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import opus.c.opus_get_version_string

@OptIn(ExperimentalForeignApi::class)
actual object Opus {
    actual fun getOpusVersion(): String =
        opus_get_version_string()?.toKString() ?: error("opus_get_version_string returned null")
}
