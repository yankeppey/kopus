/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of test_opus_custom.c from the
 * native libopus library.
 */
package eu.buney.kopus

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Custom encoder tests ported from native libopus test_opus_custom.c
 *
 * These tests verify:
 * 1. 24-bit encode→decode roundtrip with FINAL_RANGE verification
 * 2. Multistream 24-bit encode/decode
 * 3. Cross-format roundtrip (16→16, 16→24, 24→16, 24→24)
 */
class OpusCustomTest {

    companion object {
        private const val MAX_PACKET = 1500
        private const val MAX_FRAME_SAMP = 5760  // 120ms at 48kHz

        // Sine sweep parameters from test_opus_custom.c
        private const val SINE_SWEEP_AMPLITUDE = 0.5
        private const val SINE_SWEEP_DURATION_S = 60.0
    }

    // Pseudo-random number generator state
    private var Rz: UInt = 0u
    private var Rw: UInt = 0u

    private fun fastRand(): UInt {
        Rz = 36969u * (Rz and 65535u) + (Rz shr 16)
        Rw = 18000u * (Rw and 65535u) + (Rw shr 16)
        return (Rz shl 16) + Rw
    }

    private fun initSeed(seed: UInt) {
        Rz = seed
        Rw = seed
    }

    private fun toHex(value: Int): String = "0x${value.toUInt().toString(16).padStart(8, '0')}"

    /**
     * Generates a 16-bit sine sweep for testing, matching the generate_sine_sweep function
     * in test_opus_custom.c.
     *
     * @param amplitude Amplitude of the sine sweep (0.0 to 1.0)
     * @param sampleRate Sample rate in Hz
     * @param channels Number of channels
     * @param durationSeconds Duration of the sweep in seconds
     * @return ShortArray containing 16-bit samples
     */
    private fun generateSineSweep16(
        amplitude: Double,
        sampleRate: Int,
        channels: Int,
        durationSeconds: Double
    ): ShortArray {
        val startFreq = 100.0
        val endFreq = sampleRate / 2.0
        val numSamples = floor(0.5 + durationSeconds * sampleRate).toInt()
        val maxSampleValue = (1L shl 15) - 1  // 16-bit max value

        val outputBuffer = ShortArray(numSamples * channels)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val b = ln((endFreq + startFreq) / startFreq) / durationSeconds
            val a = startFreq / b
            val sample = amplitude * sin(2 * PI * a * exp(b * t) - (b * t) - 1)

            val shortSample = floor(0.5 + sample * maxSampleValue).toInt().toShort()
            outputBuffer[i * channels] = shortSample
            if (channels == 2) {
                outputBuffer[i * channels + 1] = shortSample
            }
        }

        return outputBuffer
    }

    /**
     * Generates a 24-bit sine sweep for testing, matching the generate_sine_sweep function
     * in test_opus_custom.c.
     *
     * @param amplitude Amplitude of the sine sweep (0.0 to 1.0)
     * @param sampleRate Sample rate in Hz
     * @param channels Number of channels
     * @param durationSeconds Duration of the sweep in seconds
     * @return IntArray containing 24-bit samples stored in 32-bit integers
     */
    private fun generateSineSweep24(
        amplitude: Double,
        sampleRate: Int,
        channels: Int,
        durationSeconds: Double
    ): IntArray {
        val startFreq = 100.0
        val endFreq = sampleRate / 2.0
        val numSamples = floor(0.5 + durationSeconds * sampleRate).toInt()
        val maxSampleValue = (1L shl 23) - 1  // 24-bit max value

        val outputBuffer = IntArray(numSamples * channels)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val b = ln((endFreq + startFreq) / startFreq) / durationSeconds
            val a = startFreq / b
            val sample = amplitude * sin(2 * PI * a * exp(b * t) - (b * t) - 1)

            val intSample = floor(0.5 + sample * maxSampleValue).toInt()
            outputBuffer[i * channels] = intSample
            if (channels == 2) {
                outputBuffer[i * channels + 1] = intSample
            }
        }

        return outputBuffer
    }

    /**
     * Verifies that encoder and decoder FINAL_RANGE values match.
     */
    private fun verifyFinalRange(encoder: OpusEncoder, decoder: OpusDecoder, context: String) {
        val encFinalRange = encoder.getFinalRange()
        val decFinalRange = decoder.getFinalRange()
        assertEquals(
            encFinalRange, decFinalRange,
            "$context: FINAL_RANGE mismatch - encoder: ${toHex(encFinalRange)}, decoder: ${toHex(decFinalRange)}"
        )
    }

    /**
     * Verifies that multistream encoder and decoder FINAL_RANGE values match.
     */
    private fun verifyFinalRange(encoder: OpusMultistreamEncoder, decoder: OpusMultistreamDecoder, context: String) {
        val encFinalRange = encoder.getFinalRange()
        val decFinalRange = decoder.getFinalRange()
        assertEquals(
            encFinalRange, decFinalRange,
            "$context: FINAL_RANGE mismatch - encoder: ${toHex(encFinalRange)}, decoder: ${toHex(decFinalRange)}"
        )
    }

    /**
     * Tests 24-bit encode→decode roundtrip for single-stream encoder/decoder.
     * Port of test_encode() from test_opus_custom.c with encoder_bit_depth=24, decoder_bit_depth=24.
     */
    @Test
    fun testEncode24Decode24Roundtrip() {
        initSeed(242424u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960  // 20ms at 48kHz

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            // Generate sine sweep input (shorter duration for test speed)
            val testDuration = 1.0  // 1 second
            val inBuf = generateSineSweep24(SINE_SWEEP_AMPLITUDE, sampleRate, channels, testDuration)
            val numSamples = inBuf.size / channels

            val packet = ByteArray(MAX_PACKET)
            val outBuf = IntArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount + frameSize <= numSamples && framesEncoded < 10) {
                // Encode using 24-bit API
                val len = encoder.encode24(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "[24-bit] opus_encode24() returned error: $len")

                // Decode using 24-bit API
                val outSamples = decoder.decode24(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "[24-bit] Decoded sample count mismatch")

                // Verify FINAL_RANGE
                verifyFinalRange(encoder, decoder, "[24-bit] Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[24-bit Single-stream] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests 24-bit encode→decode roundtrip for multistream encoder/decoder.
     * Port of test_encode() from test_opus_custom.c with multistream and 24-bit.
     */
    @Test
    fun testMultistreamEncode24Decode24Roundtrip() {
        val sampleRate = 48000
        val channels = 2
        val streams = 1
        val coupledStreams = 1
        val mapping = byteArrayOf(0, 1)
        val frameSize = 960

        val encoder = OpusMultistreamEncoder(
            sampleRate = sampleRate,
            channels = channels,
            streams = streams,
            coupledStreams = coupledStreams,
            mapping = mapping,
            application = OpusApplication.Audio
        )
        val decoder = OpusMultistreamDecoder(
            sampleRate = sampleRate,
            channels = channels,
            streams = streams,
            coupledStreams = coupledStreams,
            mapping = mapping
        )

        try {
            // Generate sine sweep input
            val testDuration = 0.5  // 0.5 second
            val inBuf = generateSineSweep24(SINE_SWEEP_AMPLITUDE, sampleRate, channels, testDuration)
            val numSamples = inBuf.size / channels

            val packet = ByteArray(MAX_PACKET)
            val outBuf = IntArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount + frameSize <= numSamples && framesEncoded < 5) {
                val len = encoder.encode24(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "[24-bit MS] encode failed: $len")

                val outSamples = decoder.decode24(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "[24-bit MS] Decoded sample count mismatch")
                verifyFinalRange(encoder, decoder, "[24-bit MS] Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[24-bit Multistream] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests cross-format encode→decode roundtrip combinations.
     * Port of test_opus_custom.c which randomly selects encoder_bit_depth and decoder_bit_depth
     * independently, testing all combinations: 16→16, 16→24, 24→16, 24→24.
     */
    @Test
    fun testCrossFormatRoundtrip() {
        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            val testDuration = 0.5
            val inBuf16 = generateSineSweep16(SINE_SWEEP_AMPLITUDE, sampleRate, channels, testDuration)
            val inBuf24 = generateSineSweep24(SINE_SWEEP_AMPLITUDE, sampleRate, channels, testDuration)
            val numSamples = inBuf16.size / channels

            val packet = ByteArray(MAX_PACKET)
            val outBuf16 = ShortArray(MAX_FRAME_SAMP * channels)
            val outBuf24 = IntArray(MAX_FRAME_SAMP * channels)

            data class FormatCombo(
                val encodeBits: Int,
                val decodeBits: Int,
                val name: String
            )

            val combinations = listOf(
                FormatCombo(16, 16, "16→16"),
                FormatCombo(16, 24, "16→24"),
                FormatCombo(24, 16, "24→16"),
                FormatCombo(24, 24, "24→24")
            )

            for (combo in combinations) {
                // Reset encoder/decoder state between combinations
                encoder.resetState()
                decoder.resetState()

                var sampCount = 0
                var framesEncoded = 0

                while (sampCount + frameSize <= numSamples && framesEncoded < 5) {
                    // Encode with selected bit depth
                    val len = if (combo.encodeBits == 16) {
                        encoder.encode(
                            inPcm = inBuf16,
                            inPcmOffset = sampCount * channels,
                            frameSize = frameSize,
                            outData = packet,
                            outDataOffset = 0,
                            maxDataBytes = MAX_PACKET
                        )
                    } else {
                        encoder.encode24(
                            inPcm = inBuf24,
                            inPcmOffset = sampCount * channels,
                            frameSize = frameSize,
                            outData = packet,
                            outDataOffset = 0,
                            maxDataBytes = MAX_PACKET
                        )
                    }

                    assertTrue(len > 0, "[${combo.name}] encode failed: $len")

                    // Decode with selected bit depth
                    val outSamples = if (combo.decodeBits == 16) {
                        decoder.decode(
                            inData = packet,
                            inDataOffset = 0,
                            len = len,
                            outPcm = outBuf16,
                            outPcmOffset = 0,
                            frameSize = MAX_FRAME_SAMP,
                            decodeFec = false
                        )
                    } else {
                        decoder.decode24(
                            inData = packet,
                            inDataOffset = 0,
                            len = len,
                            outPcm = outBuf24,
                            outPcmOffset = 0,
                            frameSize = MAX_FRAME_SAMP,
                            decodeFec = false
                        )
                    }

                    assertEquals(frameSize, outSamples, "[${combo.name}] Decoded sample count mismatch")
                    verifyFinalRange(encoder, decoder, "[${combo.name}] Frame $framesEncoded")

                    sampCount += frameSize
                    framesEncoded++
                }

                assertTrue(framesEncoded > 0, "[${combo.name}] No frames were encoded")
                println("[Cross-format ${combo.name}] OK - $framesEncoded frames with FINAL_RANGE verification")
            }

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests cross-format encode→decode roundtrip for multistream encoder/decoder.
     * Tests all combinations: 16→16, 16→24, 24→16, 24→24.
     */
    @Test
    fun testMultistreamCrossFormatRoundtrip() {
        val sampleRate = 48000
        val channels = 2
        val streams = 1
        val coupledStreams = 1
        val mapping = byteArrayOf(0, 1)
        val frameSize = 960

        val encoder = OpusMultistreamEncoder(
            sampleRate = sampleRate,
            channels = channels,
            streams = streams,
            coupledStreams = coupledStreams,
            mapping = mapping,
            application = OpusApplication.Audio
        )
        val decoder = OpusMultistreamDecoder(
            sampleRate = sampleRate,
            channels = channels,
            streams = streams,
            coupledStreams = coupledStreams,
            mapping = mapping
        )

        try {
            val testDuration = 0.5
            val inBuf16 = generateSineSweep16(SINE_SWEEP_AMPLITUDE, sampleRate, channels, testDuration)
            val inBuf24 = generateSineSweep24(SINE_SWEEP_AMPLITUDE, sampleRate, channels, testDuration)
            val numSamples = inBuf16.size / channels

            val packet = ByteArray(MAX_PACKET)
            val outBuf16 = ShortArray(MAX_FRAME_SAMP * channels)
            val outBuf24 = IntArray(MAX_FRAME_SAMP * channels)

            data class FormatCombo(
                val encodeBits: Int,
                val decodeBits: Int,
                val name: String
            )

            val combinations = listOf(
                FormatCombo(16, 16, "16→16"),
                FormatCombo(16, 24, "16→24"),
                FormatCombo(24, 16, "24→16"),
                FormatCombo(24, 24, "24→24")
            )

            for (combo in combinations) {
                encoder.resetState()
                decoder.resetState()

                var sampCount = 0
                var framesEncoded = 0

                while (sampCount + frameSize <= numSamples && framesEncoded < 5) {
                    val len = if (combo.encodeBits == 16) {
                        encoder.encode(
                            inPcm = inBuf16,
                            inPcmOffset = sampCount * channels,
                            frameSize = frameSize,
                            outData = packet,
                            outDataOffset = 0,
                            maxDataBytes = MAX_PACKET
                        )
                    } else {
                        encoder.encode24(
                            inPcm = inBuf24,
                            inPcmOffset = sampCount * channels,
                            frameSize = frameSize,
                            outData = packet,
                            outDataOffset = 0,
                            maxDataBytes = MAX_PACKET
                        )
                    }

                    assertTrue(len > 0, "[MS ${combo.name}] encode failed: $len")

                    val outSamples = if (combo.decodeBits == 16) {
                        decoder.decode(
                            inData = packet,
                            inDataOffset = 0,
                            len = len,
                            outPcm = outBuf16,
                            outPcmOffset = 0,
                            frameSize = MAX_FRAME_SAMP,
                            decodeFec = false
                        )
                    } else {
                        decoder.decode24(
                            inData = packet,
                            inDataOffset = 0,
                            len = len,
                            outPcm = outBuf24,
                            outPcmOffset = 0,
                            frameSize = MAX_FRAME_SAMP,
                            decodeFec = false
                        )
                    }

                    assertEquals(frameSize, outSamples, "[MS ${combo.name}] Decoded sample count mismatch")
                    verifyFinalRange(encoder, decoder, "[MS ${combo.name}] Frame $framesEncoded")

                    sampCount += frameSize
                    framesEncoded++
                }

                assertTrue(framesEncoded > 0, "[MS ${combo.name}] No frames were encoded")
                println("[MS Cross-format ${combo.name}] OK - $framesEncoded frames with FINAL_RANGE verification")
            }

        } finally {
            encoder.close()
            decoder.close()
        }
    }
}
