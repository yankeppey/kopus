/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of test_opus_encode.c from the
 * native libopus library, originally written by Gregory Maxwell.
 */
package eu.buney.kopus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Basic encode→decode roundtrip tests ported from native libopus test_opus_encode.c
 *
 * These tests verify that the Opus encoder and decoder work correctly together
 * by encoding synthetic audio and decoding it back, verifying:
 * 1. The sample counts match
 * 2. The entropy coder states match (FINAL_RANGE verification)
 *
 * FINAL_RANGE verification ensures bit-exact codec operation by comparing
 * the encoder and decoder's internal entropy coder states after each frame.
 */
class OpusEncodeDecodeTest {

    companion object {
        // Test constants matching the original C test
        private const val MAX_PACKET = 1500
        private const val SAMPLES = 48000 * 30  // 30 seconds at 48kHz
        private const val SSAMPLES = SAMPLES / 3
        private const val MAX_FRAME_SAMP = 5760  // 120ms at 48kHz

        // Pseudo-random number generator state (matching original C implementation)
        private var Rz: UInt = 0u
        private var Rw: UInt = 0u
    }

    /**
     * Fast pseudo-random number generator matching the original C implementation.
     * Uses multiply-with-carry algorithm for reproducible results.
     */
    private fun fastRand(): UInt {
        Rz = 36969u * (Rz and 65535u) + (Rz shr 16)
        Rw = 18000u * (Rw and 65535u) + (Rw shr 16)
        return (Rz shl 16) + Rw
    }

    /**
     * Initialize the random seed for reproducible tests.
     */
    private fun initSeed(seed: UInt) {
        Rz = seed
        Rw = seed
    }

    /**
     * Formats a FINAL_RANGE value as hex for error messages.
     */
    private fun toHex(value: Int): String = "0x${value.toUInt().toString(16).padStart(8, '0')}"

    /**
     * Verifies that encoder and decoder FINAL_RANGE values match.
     * This is the standard Opus verification method - matching entropy coder states
     * proves bit-exact codec operation.
     */
    private fun verifyFinalRange(
        encoder: OpusEncoder,
        decoder: OpusDecoder,
        frameInfo: String
    ) {
        val encFinalRange = encoder.getFinalRange()
        val decFinalRange = decoder.getFinalRange()
        assertEquals(
            encFinalRange,
            decFinalRange,
            "$frameInfo: FINAL_RANGE mismatch - encoder=${toHex(encFinalRange)}, decoder=${toHex(decFinalRange)}"
        )
    }

    /**
     * Generates synthetic "music" for testing, matching the original C implementation.
     *
     * This produces a deterministic audio signal that exercises the codec
     * with varying frequency content and amplitude. The algorithm:
     * - First 60ms is silence (2880 samples at 48kHz stereo)
     * - Remainder is procedurally generated using bit manipulation
     * - Applies simple filtering for smoothness
     * - Output is interleaved stereo (L, R, L, R, ...)
     *
     * @param buf Output buffer for interleaved stereo samples
     * @param len Number of stereo sample pairs to generate
     */
    private fun generateMusic(buf: ShortArray, len: Int) {
        var a1 = 0
        var b1 = 0
        var a2 = 0
        var b2 = 0
        var c1 = 0
        var c2 = 0
        var d1 = 0
        var d2 = 0
        var j = 0

        // 60ms silence at start (2880 samples for stereo at 48kHz)
        for (i in 0 until minOf(2880, len)) {
            buf[i * 2] = 0
            buf[i * 2 + 1] = 0
        }

        // Generate procedural audio
        for (i in 2880 until len) {
            // Procedural "music" generation using bit manipulation
            // This creates a varied signal that exercises different codec paths
            val baseValue = ((j * ((j shr 12) xor ((j shr 10 or j shr 12) and 26 and j shr 7))) and 128) + 128
            var v1 = baseValue shl 15
            var v2 = baseValue shl 15

            // Add randomness
            var r = fastRand()
            v1 += (r and 65535u).toInt()
            v1 -= (r shr 16).toInt()
            r = fastRand()
            v2 += (r and 65535u).toInt()
            v2 -= (r shr 16).toInt()

            // Apply simple IIR filtering for smoothness
            b1 = v1 - a1 + ((b1 * 61 + 32) shr 6)
            a1 = v1
            b2 = v2 - a2 + ((b2 * 61 + 32) shr 6)
            a2 = v2
            c1 = (30 * (c1 + b1 + d1) + 32) shr 6
            d1 = b1
            c2 = (30 * (c2 + b2 + d2) + 32) shr 6
            d2 = b2

            // Scale and clip to 16-bit range
            var out1 = (c1 + 128) shr 8
            var out2 = (c2 + 128) shr 8
            out1 = out1.coerceIn(-32768, 32767)
            out2 = out2.coerceIn(-32768, 32767)

            buf[i * 2] = out1.toShort()
            buf[i * 2 + 1] = out2.toShort()

            if (i % 6 == 0) j++
        }
    }

    /**
     * Tests basic encode→decode roundtrip.
     *
     * This test:
     * 1. Generates synthetic audio
     * 2. Encodes it in chunks using the encoder
     * 3. Decodes each chunk using the decoder
     * 4. Verifies the decoded sample count matches the frame size
     * 5. Verifies FINAL_RANGE matches between encoder and decoder
     *
     * Ported from test_encode() in test_opus_encode.c
     */
    @Test
    fun testBasicEncodeDecodeRoundtrip() {
        // Initialize random seed for reproducibility
        initSeed(42u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960  // 20ms at 48kHz

        // Create encoder and decoder
        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            // Generate input audio
            val numSamples = SSAMPLES / 2
            val inBuf = ShortArray(numSamples * channels)
            generateMusic(inBuf, numSamples)

            // Allocate output buffers
            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            // Encode and decode in chunks
            while (sampCount < numSamples - MAX_FRAME_SAMP) {
                // Encode one frame
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "opus_encode() returned error: $len")
                assertTrue(len <= MAX_PACKET, "Encoded packet too large: $len > $MAX_PACKET")

                // Decode the packet
                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                if (outSamples != frameSize) {
                    fail("opus_decode() returned $outSamples samples, expected $frameSize")
                }

                // Verify entropy coder states match (FINAL_RANGE verification)
                verifyFinalRange(encoder, decoder, "Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("Successfully encoded and decoded $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests encode→decode roundtrip with different sample rates.
     * Verifies FINAL_RANGE matches for each sample rate.
     */
    @Test
    fun testEncodeDecodeAllSampleRates() {
        initSeed(12345u)

        val sampleRates = listOf(8000, 12000, 16000, 24000, 48000)

        for (sampleRate in sampleRates) {
            val channels = 1
            val frameSize = sampleRate / 50  // 20ms frame

            val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Voip)
            val decoder = OpusDecoder(sampleRate, channels)

            try {
                // Generate mono audio (reuse stereo generator, take every other sample)
                val numSamples = sampleRate * 2  // 2 seconds
                val stereoTemp = ShortArray(numSamples * 2)
                generateMusic(stereoTemp, numSamples)

                // Convert to mono
                val inBuf = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    inBuf[i] = stereoTemp[i * 2]
                }

                val packet = ByteArray(MAX_PACKET)
                val outBuf = ShortArray(frameSize * channels)

                // Encode and decode a few frames
                var sampCount = 0
                var framesEncoded = 0
                val maxFrames = 10

                while (sampCount < numSamples - frameSize && framesEncoded < maxFrames) {
                    val len = encoder.encode(
                        inPcm = inBuf,
                        inPcmOffset = sampCount,
                        frameSize = frameSize,
                        outData = packet,
                        outDataOffset = 0,
                        maxDataBytes = MAX_PACKET
                    )

                    assertTrue(len > 0, "[$sampleRate Hz] opus_encode() returned error: $len")

                    val outSamples = decoder.decode(
                        inData = packet,
                        inDataOffset = 0,
                        len = len,
                        outPcm = outBuf,
                        outPcmOffset = 0,
                        frameSize = frameSize,
                        decodeFec = false
                    )

                    if (outSamples != frameSize) {
                        fail("[$sampleRate Hz] opus_decode() returned $outSamples, expected $frameSize")
                    }

                    // Verify entropy coder states match
                    verifyFinalRange(encoder, decoder, "[$sampleRate Hz] Frame $framesEncoded")

                    sampCount += frameSize
                    framesEncoded++
                }

                println("[$sampleRate Hz] OK - $framesEncoded frames with FINAL_RANGE verification")

            } finally {
                encoder.close()
                decoder.close()
            }
        }
    }

    /**
     * Tests encode→decode with different application modes.
     * Verifies FINAL_RANGE matches for each application mode.
     */
    @Test
    fun testEncodeDecodeAllApplicationModes() {
        initSeed(99999u)

        val applications = listOf(
            OpusApplication.Voip to "VOIP",
            OpusApplication.Audio to "Audio",
            OpusApplication.RestrictedLowDelay to "RestrictedLowDelay"
        )

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960  // 20ms

        for ((application, name) in applications) {
            val encoder = OpusEncoder(sampleRate, channels, application)
            val decoder = OpusDecoder(sampleRate, channels)

            try {
                val numSamples = sampleRate  // 1 second
                val inBuf = ShortArray(numSamples * channels)
                generateMusic(inBuf, numSamples)

                val packet = ByteArray(MAX_PACKET)
                val outBuf = ShortArray(frameSize * channels)

                // Encode and decode 5 frames
                var sampCount = 0
                repeat(5) { frameNum ->
                    val len = encoder.encode(
                        inPcm = inBuf,
                        inPcmOffset = sampCount * channels,
                        frameSize = frameSize,
                        outData = packet,
                        outDataOffset = 0,
                        maxDataBytes = MAX_PACKET
                    )

                    assertTrue(len > 0, "[$name] opus_encode() failed: $len")

                    val outSamples = decoder.decode(
                        inData = packet,
                        inDataOffset = 0,
                        len = len,
                        outPcm = outBuf,
                        outPcmOffset = 0,
                        frameSize = frameSize,
                        decodeFec = false
                    )

                    if (outSamples != frameSize) {
                        fail("[$name] opus_decode() returned $outSamples, expected $frameSize")
                    }

                    // Verify entropy coder states match
                    verifyFinalRange(encoder, decoder, "[$name] Frame $frameNum")

                    sampCount += frameSize
                }

                println("[$name] OK with FINAL_RANGE verification")

            } finally {
                encoder.close()
                decoder.close()
            }
        }
    }

    /**
     * Tests encode→decode with float API.
     * Verifies FINAL_RANGE matches for float encoding/decoding.
     */
    @Test
    fun testEncodeDecodeFloat() {
        initSeed(77777u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            // Generate float input in range [-1.0, 1.0]
            val numSamples = sampleRate
            val inBuf = FloatArray(numSamples * channels)

            // Simple sine wave for float test
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val sample = kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * t).toFloat() * 0.5f
                inBuf[i * channels] = sample
                inBuf[i * channels + 1] = sample
            }

            val packet = ByteArray(MAX_PACKET)
            val outBuf = FloatArray(frameSize * channels)

            // Encode and decode 5 frames
            var sampCount = 0
            repeat(5) { frameNum ->
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "[Float] Frame $frameNum: opus_encode_float() failed: $len")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = false
                )

                if (outSamples != frameSize) {
                    fail("[Float] Frame $frameNum: opus_decode_float() returned $outSamples, expected $frameSize")
                }

                // Verify entropy coder states match
                verifyFinalRange(encoder, decoder, "[Float] Frame $frameNum")

                sampCount += frameSize
            }

            println("[Float API] OK with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }
}
