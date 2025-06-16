/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */
package eu.buney.kopus

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles loading of the Opus JNI native library.
 * Uses thread-safe singleton pattern to ensure library is loaded exactly once.
 */
object OpusLoader {
    // Thread-safe flag to track whether library has been loaded
    private val isLoaded = AtomicBoolean(false)

    /**
     * Loads the Opus JNI library if it hasn't been loaded already.
     * Safe to call multiple times - subsequent calls will be no-ops.
     */
    fun load() {
        // Return immediately if already loaded
        if (!isLoaded.compareAndSet(false, true)) {
            return
        }

        println("Loading Opus JNI library...")
        // For Android: rely on standard jniLibs packaging.
        // For JVM: rely on java.library.path or extract from JAR.
        try {
            System.loadLibrary("opus_jni")
        } catch (e: UnsatisfiedLinkError) {
            // fallback for JVM if needed (e.g., extract to /tmp & System.load)
            loadFromJar()
        }
    }

    private fun loadFromJar() {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val archPath = if (arch.contains("aarch") || arch.contains("arm")) "arm64" else "x86_64"
        val (osPath, libName) = when {
            os.contains("mac") -> "macos" to "libopus_jni.dylib"
            os.contains("linux") -> "linux" to "libopus_jni.so"
            os.contains("windows") -> "windows" to "opus_jni.dll"
            else -> throw RuntimeException("Unsupported OS: $os")
        }

        val resourcePath = "/native/$osPath/$archPath/$libName"
        val extracted = extractToTemp(resourcePath)
        System.load(extracted.absolutePath)
    }

    private fun extractToTemp(path: String): File {
        val temp = createTempFile("libopus_jni")
        OpusLoader::class.java.getResourceAsStream(path)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: throw RuntimeException("Could not find $path in resources!")
        temp.deleteOnExit()
        return temp
    }
}
