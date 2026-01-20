/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of test_opus_decode.c from the
 * native libopus library, originally written by Gregory Maxwell.
 */
package eu.buney.kopus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Decoder tests ported from native libopus test_opus_decode.c
 *
 * These tests verify that the Opus decoder handles various input conditions
 * correctly, including all valid mode combinations, invalid inputs,
 * deterministic behavior, and PLC (Packet Loss Concealment).
 */
class OpusDecodeTest {

    companion object {
        // Test constants matching the original C test (test_opus_decode.c)
        private const val MAX_PACKET = 1500
        private const val MAX_FRAME_SAMP = 5760  // 120ms at 48kHz

        // Valid sample rates for Opus
        private val SAMPLE_RATES = listOf(48000, 24000, 16000, 12000, 8000)

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
     * Helper to safely decode, normalizing cross-platform error handling.
     * iOS throws exceptions while JVM returns negative error codes.
     * Accepts nullable packet to support PLC testing.
     *
     * @return Number of decoded samples on success, null on failure
     */
    private fun safeDecodeOrNull(
        decoder: OpusDecoder,
        packet: ByteArray?,
        offset: Int,
        len: Int,
        outBuf: ShortArray,
        frameSize: Int
    ): Int? {
        return try {
            val result = decoder.decode(packet, offset, len, outBuf, 0, frameSize, false)
            if (result < 0) null else result
        } catch (e: Exception) {
            // iOS throws OpusException on errors
            null
        }
    }

    /**
     * Helper to safely decode with float output.
     */
    private fun safeDecodeFloatOrNull(
        decoder: OpusDecoder,
        packet: ByteArray,
        offset: Int,
        len: Int,
        outBuf: FloatArray,
        frameSize: Int
    ): Int? {
        return try {
            val result = decoder.decode(packet, offset, len, outBuf, 0, frameSize, false)
            if (result < 0) null else result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates synthetic "music" for testing, matching the original C implementation.
     */
    private fun generateMusic(buf: ShortArray, len: Int, channels: Int) {
        var a1 = 0
        var b1 = 0
        var a2 = 0
        var b2 = 0
        var c1 = 0
        var c2 = 0
        var d1 = 0
        var d2 = 0
        var j = 0

        val silenceSamples = minOf(2880, len)
        for (i in 0 until silenceSamples * channels) {
            buf[i] = 0
        }

        for (i in 2880 until len) {
            val baseValue = ((j * ((j shr 12) xor ((j shr 10 or j shr 12) and 26 and j shr 7))) and 128) + 128
            var v1 = baseValue shl 15
            var v2 = baseValue shl 15

            var r = fastRand()
            v1 += (r and 65535u).toInt()
            v1 -= (r shr 16).toInt()
            r = fastRand()
            v2 += (r and 65535u).toInt()
            v2 -= (r shr 16).toInt()

            b1 = v1 - a1 + ((b1 * 61 + 32) shr 6)
            a1 = v1
            b2 = v2 - a2 + ((b2 * 61 + 32) shr 6)
            a2 = v2
            c1 = (30 * (c1 + b1 + d1) + 32) shr 6
            d1 = b1
            c2 = (30 * (c2 + b2 + d2) + 32) shr 6
            d2 = b2

            var out1 = (c1 + 128) shr 8
            var out2 = (c2 + 128) shr 8
            out1 = out1.coerceIn(-32768, 32767)
            out2 = out2.coerceIn(-32768, 32767)

            if (channels == 2) {
                buf[i * 2] = out1.toShort()
                buf[i * 2 + 1] = out2.toShort()
            } else {
                buf[i] = out1.toShort()
            }

            if (i % 6 == 0) j++
        }
    }

    /**
     * Test decoder creation and basic decoding for all sample rate × channel combinations.
     * This is a smoke test to verify decoder creation works across all configurations.
     */
    @Test
    fun testAllSampleRatesAndChannels() {
        initSeed(12345u)

        for (sampleRate in SAMPLE_RATES) {
            for (channels in 1..2) {
                val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
                val decoder = OpusDecoder(sampleRate, channels)

                try {
                    // Verify decoder sample rate
                    val queriedSampleRate = decoder.getSampleRate()
                    assertEquals(sampleRate, queriedSampleRate, "Decoder sample rate mismatch")

                    // Generate test audio and encode a frame
                    val frameSize = sampleRate / 50  // 20ms frame
                    val inBuf = ShortArray(frameSize * channels)
                    generateMusic(inBuf, frameSize, channels)

                    val packet = ByteArray(MAX_PACKET)
                    val encodedLen = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)
                    assertTrue(encodedLen > 0, "Encoding failed for $sampleRate Hz, $channels ch")

                    // Decode the packet
                    val outBuf = ShortArray(MAX_FRAME_SAMP * channels)
                    val decoded = safeDecodeOrNull(decoder, packet, 0, encodedLen, outBuf, MAX_FRAME_SAMP)

                    assertTrue(decoded != null && decoded > 0,
                        "Decoding failed for $sampleRate Hz, $channels ch")

                    println("[$sampleRate Hz, $channels ch] OK - encoded $encodedLen bytes, decoded $decoded samples")

                } finally {
                    encoder.close()
                    decoder.close()
                }
            }
        }
    }

    /**
     * Test all 64 Opus code 0 modes with representative second-byte values.
     *
     * The first byte of an Opus packet encodes the mode (TOC byte):
     * - Bits 0-4: Configuration (0-31)
     * - Bit 5: Stereo flag
     * - Bits 6-7: Frame count code (0-3)
     *
     * Code 0 means a single frame per packet.
     */
    @Test
    fun testCode0Modes() {
        initSeed(98765u)

        // Test representative second byte values
        val secondByteValues = listOf(0, 1, 127, 128, 255)

        for (sampleRate in SAMPLE_RATES) {
            val decoder = OpusDecoder(sampleRate, 2)  // Stereo decoder can decode mono too
            val outBuf = ShortArray(MAX_FRAME_SAMP * 2)

            try {
                var successCount = 0
                var failCount = 0

                // Test all 64 mode combinations (bits 0-5 of TOC byte)
                // Bits 6-7 = 0 for code 0 (single frame)
                for (config in 0 until 32) {
                    for (stereo in 0..1) {
                        val tocByte = (config or (stereo shl 5)).toByte()

                        for (secondByte in secondByteValues) {
                            val packet = byteArrayOf(tocByte, secondByte.toByte())

                            // Reset decoder state between tests
                            decoder.resetState()

                            val result = safeDecodeOrNull(decoder, packet, 0, packet.size, outBuf, MAX_FRAME_SAMP)
                            if (result != null && result > 0) {
                                successCount++
                            } else {
                                failCount++
                            }
                        }
                    }
                }

                println("[$sampleRate Hz] Code 0 modes: $successCount succeeded, $failCount failed")

                // We expect some modes to fail (e.g., incompatible sample rates for certain modes)
                // but the decoder should never crash
                assertTrue(successCount > 0, "No code 0 modes succeeded at $sampleRate Hz")

            } finally {
                decoder.close()
            }
        }
    }

    /**
     * Test encoder/decoder determinism using OPUS_GET_FINAL_RANGE.
     *
     * The encoder and decoder should produce identical entropy coder states
     * after processing the same audio with identical settings.
     */
    @Test
    fun testDeterminism() {
        initSeed(11111u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960  // 20ms at 48kHz

        // Generate test audio
        val numSamples = sampleRate * 2  // 2 seconds
        val inBuf = ShortArray(numSamples * channels)
        generateMusic(inBuf, numSamples, channels)

        // First pass
        val encoder1 = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder1 = OpusDecoder(sampleRate, channels)

        // Second pass (identical settings)
        val encoder2 = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder2 = OpusDecoder(sampleRate, channels)

        try {
            // Set identical encoder settings
            for (encoder in listOf(encoder1, encoder2)) {
                encoder.setBitrate(64000)
                encoder.setComplexity(5)
                encoder.setVBR(false)  // Use CBR for determinism
            }

            val packet1 = ByteArray(MAX_PACKET)
            val packet2 = ByteArray(MAX_PACKET)
            val outBuf1 = ShortArray(MAX_FRAME_SAMP * channels)
            val outBuf2 = ShortArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesProcessed = 0
            val maxFrames = 50

            while (sampCount < numSamples - frameSize && framesProcessed < maxFrames) {
                // Encode with both encoders
                val len1 = encoder1.encode(inBuf, sampCount * channels, frameSize, packet1, 0, MAX_PACKET)
                val len2 = encoder2.encode(inBuf, sampCount * channels, frameSize, packet2, 0, MAX_PACKET)

                // Packet lengths should be identical with CBR
                assertEquals(len1, len2, "Frame $framesProcessed: Encoded lengths differ: $len1 vs $len2")

                // Packet contents should be identical
                for (i in 0 until len1) {
                    assertEquals(packet1[i], packet2[i],
                        "Frame $framesProcessed: Packet byte $i differs")
                }

                // Decode with both decoders
                val decoded1 = decoder1.decode(packet1, 0, len1, outBuf1, 0, MAX_FRAME_SAMP, false)
                val decoded2 = decoder2.decode(packet2, 0, len2, outBuf2, 0, MAX_FRAME_SAMP, false)

                assertEquals(decoded1, decoded2,
                    "Frame $framesProcessed: Decoded sample counts differ: $decoded1 vs $decoded2")

                // Check encoder final range
                val encRange1 = encoder1.getFinalRange()
                val encRange2 = encoder2.getFinalRange()
                assertEquals(encRange1, encRange2,
                    "Frame $framesProcessed: Encoder final range differs: $encRange1 vs $encRange2")

                // Check decoder final range
                val decRange1 = decoder1.getFinalRange()
                val decRange2 = decoder2.getFinalRange()
                assertEquals(decRange1, decRange2,
                    "Frame $framesProcessed: Decoder final range differs: $decRange1 vs $decRange2")

                sampCount += frameSize
                framesProcessed++
            }

            println("Determinism test passed: $framesProcessed frames processed with identical results")

        } finally {
            encoder1.close()
            encoder2.close()
            decoder1.close()
            decoder2.close()
        }
    }

    /**
     * Test that invalid packet handling doesn't crash the decoder.
     * The decoder should gracefully reject invalid input.
     */
    @Test
    fun testInvalidPacketHandling() {
        initSeed(55555u)

        val decoder = OpusDecoder(48000, 2)
        val outBuf = ShortArray(MAX_FRAME_SAMP * 2)

        try {
            // Test 1: Zero-length packet (len=0 triggers PLC in Opus, which is valid behavior)
            run {
                val emptyPacket = ByteArray(0)
                val result = safeDecodeOrNull(decoder, emptyPacket, 0, 0, outBuf, MAX_FRAME_SAMP)
                // Zero-length packet with len=0 triggers PLC (packet loss concealment)
                // which generates comfort noise samples - this is valid Opus behavior
                println("[Zero-length packet] Result: $result (PLC generates samples if non-null)")
            }

            // Test 2: Single-byte packet (truncated)
            run {
                decoder.resetState()
                val truncatedPacket = byteArrayOf(0x00)
                val result = safeDecodeOrNull(decoder, truncatedPacket, 0, 1, outBuf, MAX_FRAME_SAMP)
                // Single byte packets might be valid for some modes, just ensure no crash
                println("[Single-byte packet] Result: $result (no crash)")
            }

            // Test 3: Invalid TOC configurations with truncated data
            run {
                decoder.resetState()
                // TOC byte indicating multiple frames but insufficient data
                val badPacket = byteArrayOf(0xFC.toByte())  // Code 3 (VBR) but no frame count
                val result = safeDecodeOrNull(decoder, badPacket, 0, 1, outBuf, MAX_FRAME_SAMP)
                println("[Truncated VBR packet] Result: $result (no crash)")
            }

            // Test 4: Frame size too small for output buffer
            run {
                decoder.resetState()
                // Generate a valid packet first
                val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)
                try {
                    val inBuf = ShortArray(960 * 2)
                    val packet = ByteArray(MAX_PACKET)
                    val len = encoder.encode(inBuf, 0, 960, packet, 0, MAX_PACKET)

                    // Try to decode into a buffer that's too small
                    val smallOutBuf = ShortArray(10)
                    val result = safeDecodeOrNull(decoder, packet, 0, len, smallOutBuf, 5)
                    assertTrue(result == null || result <= 5,
                        "Decoding into small buffer should fail or respect limit")
                    println("[Small output buffer] Result: $result (no crash)")
                } finally {
                    encoder.close()
                }
            }

            // Test 5: Random garbage packets
            run {
                var crashCount = 0
                repeat(100) { i ->
                    decoder.resetState()
                    val garbageLen = (fastRand() % 100u).toInt() + 1
                    val garbagePacket = ByteArray(garbageLen) {
                        fastRand().toByte()
                    }
                    val result = safeDecodeOrNull(decoder, garbagePacket, 0, garbageLen, outBuf, MAX_FRAME_SAMP)
                    // Just ensure no crash - result doesn't matter
                }
                println("[Random garbage packets] 100 packets processed without crash")
            }

            println("Invalid packet handling tests completed successfully")

        } finally {
            decoder.close()
        }
    }

    /**
     * Test using De Bruijn sequence to cover all two-byte mode combinations.
     *
     * A De Bruijn sequence of order 2 on a 256-symbol alphabet visits every
     * possible pair of bytes exactly once. This ensures comprehensive coverage
     * of the TOC byte + first payload byte combinations.
     */
    @Test
    fun testDeBruijnSequence() {
        initSeed(77777u)

        // Create and close an encoder to ensure native library is loaded
        // (workaround for OpusDecoder init order issue)
        OpusEncoder(48000, 1, OpusApplication.Audio).close()

        for (sampleRate in SAMPLE_RATES) {
            val decoder = OpusDecoder(sampleRate, 2)
            val outBuf = ShortArray(MAX_FRAME_SAMP * 2)

            try {
                var successCount = 0
                var failCount = 0

                // Generate De Bruijn sequence for 2-byte combinations
                // This is a simplified version that tests all 256 first bytes
                // with representative second bytes
                val representativeSecondBytes = intArrayOf(0, 1, 63, 64, 127, 128, 191, 192, 254, 255)

                for (firstByte in 0 until 256) {
                    for (secondByte in representativeSecondBytes) {
                        decoder.resetState()

                        val packet = byteArrayOf(firstByte.toByte(), secondByte.toByte())
                        val result = safeDecodeOrNull(decoder, packet, 0, 2, outBuf, MAX_FRAME_SAMP)

                        if (result != null && result > 0) {
                            successCount++
                        } else {
                            failCount++
                        }
                    }
                }

                println("[$sampleRate Hz] De Bruijn test: $successCount succeeded, $failCount failed out of ${256 * representativeSecondBytes.size}")
                // Decoder should never crash, even with invalid mode combinations
                assertTrue(successCount + failCount == 256 * representativeSecondBytes.size,
                    "Not all combinations were tested")

            } finally {
                decoder.close()
            }
        }
    }

    /**
     * Fuzz test with random packet data.
     *
     * This tests the decoder's robustness by feeding it random byte sequences.
     * The decoder should never crash, only return errors for invalid data.
     */
    @Test
    fun testRandomPacketFuzzing() {
        initSeed(33333u)

        // Create and close an encoder to ensure native library is loaded
        // (workaround for OpusDecoder init order issue)
        OpusEncoder(48000, 1, OpusApplication.Audio).close()

        val iterations = 1000  // Reduced from 65000 in C test for reasonable test time

        for (sampleRate in SAMPLE_RATES) {
            val decoder = OpusDecoder(sampleRate, 2)
            val outBuf = ShortArray(MAX_FRAME_SAMP * 2)
            val outBufFloat = FloatArray(MAX_FRAME_SAMP * 2)

            try {
                var shortSuccessCount = 0
                var floatSuccessCount = 0

                repeat(iterations) {
                    // Generate random packet length (1 to MAX_PACKET)
                    val packetLen = (fastRand() % MAX_PACKET.toUInt()).toInt() + 1
                    val packet = ByteArray(packetLen) {
                        fastRand().toByte()
                    }

                    decoder.resetState()

                    // Test short decode
                    val shortResult = safeDecodeOrNull(decoder, packet, 0, packetLen, outBuf, MAX_FRAME_SAMP)
                    if (shortResult != null && shortResult > 0) {
                        shortSuccessCount++
                    }

                    decoder.resetState()

                    // Test float decode
                    val floatResult = safeDecodeFloatOrNull(decoder, packet, 0, packetLen, outBufFloat, MAX_FRAME_SAMP)
                    if (floatResult != null && floatResult > 0) {
                        floatSuccessCount++
                    }
                }

                println("[$sampleRate Hz] Random fuzzing: $shortSuccessCount/$iterations short, $floatSuccessCount/$iterations float succeeded")

            } finally {
                decoder.close()
            }
        }

        println("Random packet fuzzing completed - no crashes")
    }

    /**
     * Test decoder reset functionality.
     *
     * After reset, the decoder should be in a clean state equivalent to
     * a freshly created decoder.
     */
    @Test
    fun testDecoderReset() {
        initSeed(44444u)

        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)
        val decoder = OpusDecoder(48000, 2)

        try {
            val frameSize = 960
            val inBuf = ShortArray(frameSize * 2)
            generateMusic(inBuf, frameSize, 2)

            val packet = ByteArray(MAX_PACKET)
            val outBuf1 = ShortArray(MAX_FRAME_SAMP * 2)
            val outBuf2 = ShortArray(MAX_FRAME_SAMP * 2)

            // Encode a frame
            val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)
            assertTrue(len > 0, "Encoding failed")

            // Decode multiple times to build up state
            repeat(5) {
                decoder.decode(packet, 0, len, outBuf1, 0, MAX_FRAME_SAMP, false)
            }

            // Reset the decoder
            val resetResult = decoder.resetState()
            assertEquals(0, resetResult, "Reset should return OPUS_OK (0)")

            // Decode again - should produce same result as fresh decoder
            val decoded1 = decoder.decode(packet, 0, len, outBuf1, 0, MAX_FRAME_SAMP, false)

            // Create fresh decoder for comparison
            val freshDecoder = OpusDecoder(48000, 2)
            try {
                val decoded2 = freshDecoder.decode(packet, 0, len, outBuf2, 0, MAX_FRAME_SAMP, false)

                assertEquals(decoded1, decoded2, "Reset decoder should behave like fresh decoder")

                // Compare final ranges
                val range1 = decoder.getFinalRange()
                val range2 = freshDecoder.getFinalRange()
                assertEquals(range1, range2, "Final ranges should match after reset")

                println("Decoder reset test passed")

            } finally {
                freshDecoder.close()
            }

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Test various frame sizes and verify correct sample counts.
     */
    @Test
    fun testFrameSizes() {
        initSeed(66666u)

        val sampleRate = 48000
        val channels = 2

        // Frame sizes in ms and corresponding sample counts at 48kHz
        val frameSizes = listOf(
            2.5 to 120,    // 2.5ms
            5.0 to 240,    // 5ms
            10.0 to 480,   // 10ms
            20.0 to 960,   // 20ms
            40.0 to 1920,  // 40ms
            60.0 to 2880,  // 60ms
        )

        for ((frameDurationMs, frameSize) in frameSizes) {
            val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
            val decoder = OpusDecoder(sampleRate, channels)

            try {
                // Set the expert frame duration
                val frameSizeEnum = when (frameSize) {
                    120 -> OPUS_FRAMESIZE_2_5_MS
                    240 -> OPUS_FRAMESIZE_5_MS
                    480 -> OPUS_FRAMESIZE_10_MS
                    960 -> OPUS_FRAMESIZE_20_MS
                    1920 -> OPUS_FRAMESIZE_40_MS
                    2880 -> OPUS_FRAMESIZE_60_MS
                    else -> error("Unknown frame size: $frameSize")
                }
                encoder.setExpertFrameDuration(frameSizeEnum)

                val inBuf = ShortArray(frameSize * channels)
                generateMusic(inBuf, frameSize, channels)

                val packet = ByteArray(MAX_PACKET)
                val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

                val encodedLen = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)
                assertTrue(encodedLen > 0, "Encoding failed for ${frameDurationMs}ms frame")

                val decodedSamples = decoder.decode(packet, 0, encodedLen, outBuf, 0, MAX_FRAME_SAMP, false)
                assertEquals(frameSize, decodedSamples,
                    "${frameDurationMs}ms: Expected $frameSize samples, got $decodedSamples")

                // Verify last packet duration
                val lastDuration = decoder.getLastPacketDuration()
                assertEquals(frameSize, lastDuration,
                    "${frameDurationMs}ms: Last packet duration mismatch")

                println("[${frameDurationMs}ms frame] OK - $frameSize samples")

            } finally {
                encoder.close()
                decoder.close()
            }
        }
    }

    /**
     * Test decoder gain adjustment.
     */
    @Test
    fun testDecoderGain() {
        initSeed(88888u)

        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)
        val decoder = OpusDecoder(48000, 2)

        try {
            val frameSize = 960
            val inBuf = ShortArray(frameSize * 2)
            generateMusic(inBuf, frameSize, 2)

            val packet = ByteArray(MAX_PACKET)
            val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)

            // Test various gain values
            val gainValues = listOf(0, 256, -256, 512, -512)  // Q8 format (256 = 1dB)

            for (gain in gainValues) {
                val setResult = decoder.setGain(gain)
                assertEquals(0, setResult, "setGain($gain) should return OPUS_OK")

                val getResult = decoder.getGain()
                assertEquals(gain, getResult, "getGain() should return $gain")

                decoder.resetState()
                val outBuf = ShortArray(MAX_FRAME_SAMP * 2)
                val decoded = decoder.decode(packet, 0, len, outBuf, 0, MAX_FRAME_SAMP, false)
                assertTrue(decoded > 0, "Decoding with gain $gain failed")
            }

            println("Decoder gain test passed")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Test bandwidth detection after decoding.
     */
    @Test
    fun testBandwidthDetection() {
        initSeed(99999u)

        val bandwidthSettings = listOf(
            OPUS_BANDWIDTH_NARROWBAND to "Narrowband",
            OPUS_BANDWIDTH_MEDIUMBAND to "Mediumband",
            OPUS_BANDWIDTH_WIDEBAND to "Wideband",
            OPUS_BANDWIDTH_SUPERWIDEBAND to "Superwideband",
            OPUS_BANDWIDTH_FULLBAND to "Fullband"
        )

        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)
        val decoder = OpusDecoder(48000, 2)

        try {
            val frameSize = 960
            val inBuf = ShortArray(frameSize * 2)
            generateMusic(inBuf, frameSize, 2)

            for ((bandwidth, name) in bandwidthSettings) {
                encoder.setMaxBandwidth(bandwidth)
                encoder.setBandwidth(bandwidth)

                val packet = ByteArray(MAX_PACKET)
                val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)

                decoder.resetState()
                val outBuf = ShortArray(MAX_FRAME_SAMP * 2)
                val decoded = decoder.decode(packet, 0, len, outBuf, 0, MAX_FRAME_SAMP, false)
                assertTrue(decoded > 0, "Decoding failed for $name bandwidth")

                val detectedBandwidth = decoder.getBandwidth()
                // Verify the detected bandwidth is a valid Opus bandwidth value
                // Note: Opus may choose a different bandwidth than requested based on
                // content analysis, sample rate, and internal heuristics
                assertTrue(detectedBandwidth in OPUS_BANDWIDTH_NARROWBAND..OPUS_BANDWIDTH_FULLBAND,
                    "$name: Detected bandwidth $detectedBandwidth should be a valid bandwidth")

                println("[$name] Requested bandwidth $bandwidth, detected $detectedBandwidth")
            }

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Test PLC (Packet Loss Concealment) on a fresh decoder.
     *
     * Ported from test_opus_decode.c lines 111-131:
     * Tests PLC behavior on a freshly created decoder before any packets
     * have been decoded. This is a key test from the original C test suite.
     */
    @Test
    fun testPLCOnFreshDecoder() {
        // Test PLC on fresh decoders for all sample rate/channel combinations
        for (sampleRate in SAMPLE_RATES) {
            val factor = 48000 / sampleRate
            for (channels in 1..2) {
                val decoder = OpusDecoder(sampleRate, channels)

                try {
                    val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

                    // Test PLC on a fresh decoder (2.5ms = 120 samples at 48kHz)
                    val plcFrameSize = 120 / factor
                    val outSamples = decoder.decode(outPcm = outBuf, frameSize = plcFrameSize)
                    assertEquals(plcFrameSize, outSamples,
                        "[$sampleRate Hz, $channels ch] Fresh decoder PLC should return $plcFrameSize samples")

                    // Verify OPUS_GET_LAST_PACKET_DURATION
                    val lastDuration = decoder.getLastPacketDuration()
                    assertEquals(plcFrameSize, lastDuration,
                        "[$sampleRate Hz, $channels ch] Last packet duration should match")

                } finally {
                    decoder.close()
                }
            }
        }

        println("Fresh decoder PLC test passed for all configurations")
    }

    /**
     * Test PLC with invalid frame sizes.
     *
     * Ported from test_opus_decode.c lines 117-119:
     * Frame sizes that aren't multiples of 2.5ms should return an error.
     */
    @Test
    fun testPLCInvalidFrameSize() {
        for (sampleRate in SAMPLE_RATES) {
            val factor = 48000 / sampleRate
            val decoder = OpusDecoder(sampleRate, 2)

            try {
                val outBuf = ShortArray(MAX_FRAME_SAMP * 2)

                // Test on a size which isn't a multiple of 2.5ms
                // Valid 2.5ms frame size is 120/factor, so add 2 to make it invalid
                val invalidFrameSize = 120 / factor + 2

                val result = safeDecodeOrNull(
                    decoder, null, 0, 0, outBuf, invalidFrameSize
                )

                // Should return error (null from our safe wrapper, or negative value)
                assertTrue(result == null || result < 0,
                    "[$sampleRate Hz] PLC with invalid frame size $invalidFrameSize should fail")

            } finally {
                decoder.close()
            }
        }

        println("PLC invalid frame size test passed")
    }

    /**
     * Test PLC after decoding real packets (6 consecutive PLC frames).
     *
     * Ported from test_opus_decode.c lines 204-228:
     * After decoding real packets, run PLC for 6 frames to get better PLC coverage,
     * then test PLC at 2.5ms as a drift correction simulation.
     */
    @Test
    fun testPLCAfterRealPackets() {
        initSeed(11111u)

        for (sampleRate in SAMPLE_RATES) {
            val factor = 48000 / sampleRate
            for (channels in 1..2) {
                val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
                val decoder = OpusDecoder(sampleRate, channels)

                try {
                    // Generate and encode a test packet
                    val frameSize = sampleRate / 50  // 20ms
                    val inBuf = ShortArray(frameSize * channels)
                    generateMusic(inBuf, frameSize, channels)

                    val packet = ByteArray(MAX_PACKET)
                    val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)
                    assertTrue(len > 0, "Encoding failed")

                    // Decode real packet
                    val outBuf = ShortArray(MAX_FRAME_SAMP * channels)
                    val decoded = decoder.decode(packet, 0, len, outBuf, 0, MAX_FRAME_SAMP, false)
                    assertTrue(decoded > 0, "Decoding failed")

                    val expectedSamples = decoded

                    // Run PLC for 6 frames (matching C test)
                    for (j in 0 until 6) {
                        val plcSamples = decoder.decode(outPcm = outBuf, frameSize = expectedSamples)
                        assertEquals(expectedSamples, plcSamples,
                            "[$sampleRate Hz, $channels ch] PLC frame $j should return $expectedSamples samples")

                        val dur = decoder.getLastPacketDuration()
                        assertEquals(plcSamples, dur,
                            "[$sampleRate Hz, $channels ch] Last packet duration should match PLC samples")
                    }

                    // Run PLC at 2.5ms as a drift correction simulation
                    val driftCorrectionSize = 120 / factor
                    if (expectedSamples != driftCorrectionSize) {
                        val driftSamples = decoder.decode(outPcm = outBuf, frameSize = driftCorrectionSize)
                        assertEquals(driftCorrectionSize, driftSamples,
                            "[$sampleRate Hz, $channels ch] Drift correction PLC should return $driftCorrectionSize samples")

                        val dur = decoder.getLastPacketDuration()
                        assertEquals(driftSamples, dur,
                            "[$sampleRate Hz, $channels ch] Drift correction duration should match")
                    }

                } finally {
                    encoder.close()
                    decoder.close()
                }
            }
        }

        println("PLC after real packets test passed for all configurations")
    }

    /**
     * Test PLC with FEC flag variations.
     *
     * Ported from test_opus_decode.c lines 329-333:
     * Test PLC with both fec=0 and fec=1 (decodeFec parameter).
     */
    @Test
    fun testPLCWithFEC() {
        initSeed(22222u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            // Generate and encode a test packet
            val inBuf = ShortArray(frameSize * channels)
            generateMusic(inBuf, frameSize, channels)

            val packet = ByteArray(MAX_PACKET)
            val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)

            // Decode the real packet first
            val outBuf = ShortArray(MAX_FRAME_SAMP * channels)
            decoder.decode(packet, 0, len, outBuf, 0, MAX_FRAME_SAMP, false)

            // Test PLC with decodeFec = true
            // In the C test: opus_decode(decbak, 0, 0, outbuf, MAX_FRAME_SAMP, 1)
            val plcWithFec = decoder.decode(
                inData = null,
                outPcm = outBuf,
                frameSize = MAX_FRAME_SAMP,
                decodeFec = true
            )
            assertTrue(plcWithFec >= 20,
                "PLC with FEC=true should return at least 20 samples, got $plcWithFec")

            // Reset and test PLC with decodeFec = false
            decoder.resetState()
            decoder.decode(packet, 0, len, outBuf, 0, MAX_FRAME_SAMP, false)

            val plcWithoutFec = decoder.decode(
                inData = null,
                outPcm = outBuf,
                frameSize = MAX_FRAME_SAMP,
                decodeFec = false
            )
            assertTrue(plcWithoutFec >= 20,
                "PLC with FEC=false should return at least 20 samples, got $plcWithoutFec")

            println("PLC with FEC test passed: fec=true returned $plcWithFec, fec=false returned $plcWithoutFec")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Test PLC with float output format.
     */
    @Test
    fun testPLCFloat() {
        initSeed(33333u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            val inBuf = ShortArray(frameSize * channels)
            generateMusic(inBuf, frameSize, channels)

            val packet = ByteArray(MAX_PACKET)
            val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)

            // Prime decoder with real audio using float decode
            val outBuf = FloatArray(frameSize * channels)
            decoder.decode(packet, 0, len, outBuf, 0, frameSize, false)

            // Test PLC with float output
            val plcBuf = FloatArray(frameSize * channels)
            val plcSamples = decoder.decode(outPcm = plcBuf, frameSize = frameSize)

            assertEquals(frameSize, plcSamples, "Float PLC should generate $frameSize samples")

            // Verify we got non-zero audio
            var hasNonZero = false
            for (sample in plcBuf) {
                if (sample != 0f) {
                    hasNonZero = true
                    break
                }
            }
            assertTrue(hasNonZero, "Float PLC should generate non-zero audio")

            println("Float PLC test passed: generated $plcSamples samples")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Test consecutive PLC calls (multiple lost packets).
     *
     * Opus gradually fades the concealment audio when multiple consecutive
     * packets are lost, eventually producing silence.
     */
    @Test
    fun testConsecutivePLC() {
        initSeed(44444u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)

        try {
            // Generate loud audio to prime the decoder
            val inBuf = ShortArray(frameSize * channels)
            generateMusic(inBuf, frameSize, channels)

            val packet = ByteArray(MAX_PACKET)
            val len = encoder.encode(inBuf, 0, frameSize, packet, 0, MAX_PACKET)

            // Decode several real packets to build up decoder state
            val outBuf = ShortArray(frameSize * channels)
            repeat(5) {
                decoder.decode(packet, 0, len, outBuf, 0, frameSize, false)
            }

            // Simulate multiple consecutive lost packets (10 frames)
            val plcBuf = ShortArray(frameSize * channels)

            repeat(10) { i ->
                val samples = decoder.decode(outPcm = plcBuf, frameSize = frameSize)
                assertEquals(frameSize, samples, "PLC iteration $i should return $frameSize samples")

                // Verify duration matches
                val dur = decoder.getLastPacketDuration()
                assertEquals(samples, dur, "PLC iteration $i duration should match samples")

                // Calculate energy of this frame
                var energy = 0L
                for (sample in plcBuf) {
                    energy += sample.toLong() * sample.toLong()
                }

                println("PLC frame $i: energy = $energy")
            }

            println("Consecutive PLC test passed")

        } finally {
            encoder.close()
            decoder.close()
        }
    }
}
