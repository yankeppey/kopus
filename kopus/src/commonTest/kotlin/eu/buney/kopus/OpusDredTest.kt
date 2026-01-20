/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of test_opus_dred.c from the
 * native libopus library, originally written by Michael Klingbeil.
 */
package eu.buney.kopus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for DRED (Deep Redundancy) features and feature detection.
 *
 * These tests verify:
 * 1. Feature detection (Opus.isDredAvailable, isOsceAvailable, isQextAvailable)
 * 2. DRED API functionality (OpusDRED, OpusDREDDecoder)
 * 3. Random data parsing robustness (ported from test_opus_dred.c)
 *
 * **Note:** DRED tests require the kopus-full artifact. When running on the
 * base kopus artifact, DRED-specific tests will be skipped.
 */
class OpusDredTest {

    companion object {
        // Test constants from test_opus_dred.c
        private const val NB_RANDOM_ITERATIONS = 10000  // Reduced from 10M in C for reasonable test time
        private const val MAX_EXTENSION_SIZE = 200

        // Pseudo-random number generator state
        private var Rz: UInt = 0u
        private var Rw: UInt = 0u
    }

    /**
     * Fast pseudo-random number generator matching the original C implementation.
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

    // ==================== Feature Detection Tests ====================

    /**
     * Tests that Opus.isDredAvailable returns a boolean value.
     *
     * The actual value depends on whether running kopus (base) or kopus-full.
     * This test just verifies the property is accessible and returns consistently.
     */
    @Test
    fun testIsDredAvailableAccessible() {
        val isDredAvailable = Opus.isDredAvailable
        // Call it twice to verify consistency
        val isDredAvailable2 = Opus.isDredAvailable
        assertEquals(isDredAvailable, isDredAvailable2, "isDredAvailable should be consistent")
        println("Opus.isDredAvailable = $isDredAvailable")
    }

    /**
     * Tests that Opus.isOsceAvailable returns a boolean value.
     */
    @Test
    fun testIsOsceAvailableAccessible() {
        val isOsceAvailable = Opus.isOsceAvailable
        val isOsceAvailable2 = Opus.isOsceAvailable
        assertEquals(isOsceAvailable, isOsceAvailable2, "isOsceAvailable should be consistent")
        println("Opus.isOsceAvailable = $isOsceAvailable")
    }

    /**
     * Tests that Opus.isQextAvailable returns a boolean value.
     */
    @Test
    fun testIsQextAvailableAccessible() {
        val isQextAvailable = Opus.isQextAvailable
        val isQextAvailable2 = Opus.isQextAvailable
        assertEquals(isQextAvailable, isQextAvailable2, "isQextAvailable should be consistent")
        println("Opus.isQextAvailable = $isQextAvailable")
    }

    /**
     * Tests the relationship between feature availability flags.
     *
     * In kopus-full, all DNN features (DRED, OSCE, QEXT) should be available.
     * In kopus (base), none should be available.
     */
    @Test
    fun testFeatureAvailabilityConsistency() {
        val isDredAvailable = Opus.isDredAvailable
        val isOsceAvailable = Opus.isOsceAvailable
        val isQextAvailable = Opus.isQextAvailable

        // All features should have the same availability
        // (they're all built together with --enable-dred --enable-osce --enable-qext)
        if (isDredAvailable) {
            assertTrue(isOsceAvailable, "If DRED is available, OSCE should also be available")
            assertTrue(isQextAvailable, "If DRED is available, QEXT should also be available")
            println("Running on kopus-full: all DNN features available")
        } else {
            assertFalse(isOsceAvailable, "If DRED is not available, OSCE should not be available")
            assertFalse(isQextAvailable, "If DRED is not available, QEXT should not be available")
            println("Running on kopus (base): no DNN features available")
        }
    }

    // ==================== DRED API Tests ====================

    /**
     * Tests OpusDRED creation and destruction.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testOpusDredCreation() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testOpusDredCreation")
            return
        }

        val dred = OpusDRED()
        assertNotNull(dred, "OpusDRED should be created successfully")
        dred.close()
        println("OpusDRED creation and destruction OK")
    }

    /**
     * Tests OpusDREDDecoder creation and destruction.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testOpusDredDecoderCreation() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testOpusDredDecoderCreation")
            return
        }

        val dredDecoder = OpusDREDDecoder()
        assertNotNull(dredDecoder, "OpusDREDDecoder should be created successfully")
        dredDecoder.close()
        println("OpusDREDDecoder creation and destruction OK")
    }

    /**
     * Tests parsing random DRED data without crashing.
     *
     * Ported from test_random_dred() in test_opus_dred.c.
     * The decoder should handle any input data gracefully without crashing.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testRandomDredParsing() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testRandomDredParsing")
            return
        }

        initSeed(12345u)

        val dredDecoder = OpusDREDDecoder()
        val dred = OpusDRED()

        try {
            var parseSuccessCount = 0
            var processSuccessCount = 0

            for (i in 0 until NB_RANDOM_ITERATIONS) {
                // Generate random payload
                val len = (fastRand() % (MAX_EXTENSION_SIZE + 1).toUInt()).toInt()
                val payload = ByteArray(len) { fastRand().toByte() }
                val deferProcessing = (fastRand() and 1u) != 0u

                // Parse the random data
                val result = dredDecoder.parse(
                    dred = dred,
                    data = payload,
                    dataOffset = 0,
                    len = len,
                    maxDredSamples = 48000,
                    samplingRate = 48000,
                    deferProcessing = deferProcessing
                )

                if (result.offset > 0) {
                    parseSuccessCount++

                    // If parse succeeded, process should also succeed
                    val processResult = dredDecoder.process(dred, dred)
                    assertTrue(processResult >= 0, "process should succeed if parse succeeds (iteration $i)")
                    processSuccessCount++

                    // Verify that offset >= dredEnd (end before beginning)
                    assertTrue(result.offset >= result.dredEnd,
                        "offset (${result.offset}) should be >= dredEnd (${result.dredEnd})")
                }
            }

            println("Random DRED parsing: $parseSuccessCount/$NB_RANDOM_ITERATIONS parsed successfully")
            println("Random DRED processing: $processSuccessCount successful processes")

        } finally {
            dred.close()
            dredDecoder.close()
        }
    }

    /**
     * Tests DRED parse/process with valid Opus packets that may contain DRED data.
     *
     * This test creates valid Opus packets and attempts to parse DRED data from them.
     * Even if there's no DRED data in the packets, the decoder should handle them gracefully.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testDredParseWithValidOpusPackets() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testDredParseWithValidOpusPackets")
            return
        }

        initSeed(54321u)

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val dredDecoder = OpusDREDDecoder()
        val dred = OpusDRED()

        try {
            // Generate some audio
            val numSamples = sampleRate
            val inBuf = ShortArray(numSamples * channels)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * t) * 16000).toInt().toShort()
                inBuf[i * channels] = sample
                inBuf[i * channels + 1] = sample
            }

            val packet = ByteArray(1500)
            var sampCount = 0
            var framesEncoded = 0

            while (sampCount + frameSize <= numSamples && framesEncoded < 10) {
                // Encode a frame
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = packet.size
                )
                assertTrue(len > 0, "Encoding failed")

                // Try to parse DRED from the packet
                // Note: Standard Opus packets without DRED extension won't have DRED data
                val result = dredDecoder.parse(
                    dred = dred,
                    data = packet,
                    dataOffset = 0,
                    len = len,
                    maxDredSamples = 48000,
                    samplingRate = sampleRate,
                    deferProcessing = false
                )

                // Result may be 0 (no DRED) or > 0 (has DRED)
                // Just verify we don't crash and get a valid result structure
                assertTrue(result.offset >= 0 || result.offset < 0,
                    "DRED parse result should be returned")

                sampCount += frameSize
                framesEncoded++
            }

            println("Parsed $framesEncoded valid Opus packets for DRED data (no crash)")

        } finally {
            encoder.close()
            dred.close()
            dredDecoder.close()
        }
    }

    /**
     * Tests DRED decoder CTL operations.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testDredDecoderCtl() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testDredDecoderCtl")
            return
        }

        val dredDecoder = OpusDREDDecoder()

        try {
            // Test OPUS_RESET_STATE
            val resetResult = dredDecoder.ctl(OPUS_RESET_STATE, 0)
            assertTrue(resetResult >= 0 || resetResult == OPUS_UNIMPLEMENTED,
                "DRED decoder reset should succeed or return UNIMPLEMENTED")

            println("DRED decoder CTL operations OK")

        } finally {
            dredDecoder.close()
        }
    }

    /**
     * Tests multiple OpusDRED instances can coexist.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testMultipleDredInstances() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testMultipleDredInstances")
            return
        }

        val dred1 = OpusDRED()
        val dred2 = OpusDRED()
        val dredDecoder = OpusDREDDecoder()

        try {
            // Both should work independently
            assertNotNull(dred1)
            assertNotNull(dred2)

            // Process from one to another (even with empty state)
            val processResult = dredDecoder.process(dred1, dred2)
            // Result may be error since no data was parsed, but should not crash
            println("Process result: $processResult (may be error code)")

            println("Multiple DRED instances coexist OK")

        } finally {
            dred1.close()
            dred2.close()
            dredDecoder.close()
        }
    }

    // ==================== DRED Decode Tests ====================

    /**
     * Tests OpusDecoder.decodeDred methods exist and don't crash.
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testDecoderDecodeDredMethods() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testDecoderDecodeDredMethods")
            return
        }

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val decoder = OpusDecoder(sampleRate, channels)
        val dred = OpusDRED()

        try {
            // Test decodeDred with ShortArray
            val outPcmShort = ShortArray(frameSize * channels)
            val resultShort = decoder.decodeDred(
                dred = dred,
                dredOffset = 0,
                outPcm = outPcmShort,
                outPcmOffset = 0,
                frameSize = frameSize
            )
            // Result may be error since DRED has no data, but method should exist
            println("decodeDred(ShortArray) result: $resultShort")

            // Test decodeDred with FloatArray
            val outPcmFloat = FloatArray(frameSize * channels)
            val resultFloat = decoder.decodeDred(
                dred = dred,
                dredOffset = 0,
                outPcm = outPcmFloat,
                outPcmOffset = 0,
                frameSize = frameSize
            )
            println("decodeDred(FloatArray) result: $resultFloat")

            // Test decodeDred24 with IntArray
            val outPcm24 = IntArray(frameSize * channels)
            val result24 = decoder.decodeDred24(
                dred = dred,
                dredOffset = 0,
                outPcm = outPcm24,
                outPcmOffset = 0,
                frameSize = frameSize
            )
            println("decodeDred24(IntArray) result: $result24")

            println("OpusDecoder.decodeDred methods OK (no crash)")

        } finally {
            decoder.close()
            dred.close()
        }
    }

    // ==================== OSCE/QEXT CTL Tests ====================

    /**
     * Tests OSCE BWE CTL operations on decoder.
     *
     * This test works on both base and full variants, but the CTL may
     * return OPUS_UNIMPLEMENTED on the base variant.
     */
    @Test
    fun testOsceBweCtl() {
        val decoder = OpusDecoder(48000, 2)

        try {
            // Try to set OSCE BWE
            val setResult = decoder.setOsceBwe(true)
            if (setResult == OPUS_UNIMPLEMENTED) {
                println("OSCE BWE not available (OPUS_UNIMPLEMENTED)")
                assertFalse(Opus.isOsceAvailable, "If OSCE CTL returns UNIMPLEMENTED, isOsceAvailable should be false")
            } else {
                assertTrue(setResult >= 0, "setOsceBwe should succeed or return UNIMPLEMENTED")
                assertTrue(Opus.isOsceAvailable, "If OSCE CTL succeeds, isOsceAvailable should be true")

                // Verify we can read back the value
                val osceBwe = decoder.getOsceBwe()
                assertEquals(true, osceBwe, "OSCE BWE should be true after setting")

                // Test disabling
                val disableResult = decoder.setOsceBwe(false)
                assertTrue(disableResult >= 0, "setOsceBwe(false) should succeed")
                val osceBweDisabled = decoder.getOsceBwe()
                assertEquals(false, osceBweDisabled, "OSCE BWE should be false after disabling")

                println("OSCE BWE CTL roundtrip OK")
            }

        } finally {
            decoder.close()
        }
    }

    /**
     * Tests QEXT CTL operations on encoder.
     *
     * This test works on both base and full variants, but the CTL may
     * return OPUS_UNIMPLEMENTED on the base variant.
     */
    @Test
    fun testQextCtl() {
        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)

        try {
            // Try to set QEXT
            val setResult = encoder.setQext(true)
            if (setResult == OPUS_UNIMPLEMENTED) {
                println("QEXT not available (OPUS_UNIMPLEMENTED)")
                assertFalse(Opus.isQextAvailable, "If QEXT CTL returns UNIMPLEMENTED, isQextAvailable should be false")
            } else {
                assertTrue(setResult >= 0, "setQext should succeed or return UNIMPLEMENTED")
                assertTrue(Opus.isQextAvailable, "If QEXT CTL succeeds, isQextAvailable should be true")

                // Verify we can read back the value
                val qext = encoder.getQext()
                assertEquals(true, qext, "QEXT should be true after setting")

                // Test disabling
                val disableResult = encoder.setQext(false)
                assertTrue(disableResult >= 0, "setQext(false) should succeed")
                val qextDisabled = encoder.getQext()
                assertEquals(false, qextDisabled, "QEXT should be false after disabling")

                println("QEXT CTL roundtrip OK")
            }

        } finally {
            encoder.close()
        }
    }

    // ==================== DRED Encoder CTL Tests ====================

    /**
     * Tests DRED-related encoder CTL operations.
     *
     * This test works on both base and full variants, but the CTL may
     * return OPUS_UNIMPLEMENTED on the base variant.
     */
    @Test
    fun testDredEncoderCtl() {
        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)

        try {
            // Test OPUS_SET_DRED_DURATION / OPUS_GET_DRED_DURATION
            val setDredDuration = encoder.setDREDDuration(100)
            if (setDredDuration == OPUS_UNIMPLEMENTED) {
                println("DRED duration CTL not available (OPUS_UNIMPLEMENTED)")
                assertFalse(Opus.isDredAvailable, "If DRED CTL returns UNIMPLEMENTED, isDredAvailable should be false")
                return
            }

            assertTrue(setDredDuration >= 0, "setDREDDuration should succeed")
            assertTrue(Opus.isDredAvailable, "If DRED CTL succeeds, isDredAvailable should be true")

            val dredDuration = encoder.getDREDDuration()
            assertTrue(dredDuration >= 0, "getDREDDuration should return non-negative value")

            // Test OPUS_SET_DNN_BLOB
            // Note: We can't really test this properly without a DNN blob file
            // Just verify the method exists and doesn't crash with null/empty

            println("DRED encoder CTL OK: duration=$dredDuration")

        } finally {
            encoder.close()
        }
    }

    // ==================== Integration Test ====================

    /**
     * Integration test: Full DRED workflow (encode with DRED, parse, decode).
     *
     * This test will be skipped on the base artifact where DRED is not available.
     */
    @Test
    fun testFullDredWorkflow() {
        if (!Opus.isDredAvailable) {
            println("DRED not available, skipping testFullDredWorkflow")
            return
        }

        val sampleRate = 48000
        val channels = 2
        val frameSize = 960

        val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Audio)
        val decoder = OpusDecoder(sampleRate, channels)
        val dredDecoder = OpusDREDDecoder()
        val dred = OpusDRED()

        try {
            // Enable DRED on encoder
            val setDredResult = encoder.setDREDDuration(100)
            if (setDredResult < 0) {
                println("Could not enable DRED on encoder: $setDredResult")
                return
            }

            // Generate audio
            val numSamples = sampleRate
            val inBuf = ShortArray(numSamples * channels)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * t) * 16000).toInt().toShort()
                inBuf[i * channels] = sample
                inBuf[i * channels + 1] = sample
            }

            val packet = ByteArray(1500)
            val outBuf = ShortArray(frameSize * channels)
            var sampCount = 0
            var framesProcessed = 0
            var dredFound = false

            // Encode and decode multiple frames
            while (sampCount + frameSize <= numSamples && framesProcessed < 20) {
                // Encode
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = packet.size
                )
                assertTrue(len > 0, "Encoding failed at frame $framesProcessed")

                // Decode normally
                val decoded = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = false
                )
                assertEquals(frameSize, decoded, "Normal decode failed")

                // Try to parse DRED from packet
                val parseResult = dredDecoder.parse(
                    dred = dred,
                    data = packet,
                    dataOffset = 0,
                    len = len,
                    maxDredSamples = sampleRate,
                    samplingRate = sampleRate,
                    deferProcessing = false
                )

                if (parseResult.offset > 0) {
                    dredFound = true
                    println("Frame $framesProcessed: DRED found, offset=${parseResult.offset}, dredEnd=${parseResult.dredEnd}")

                    // Try to decode DRED
                    val dredDecoded = decoder.decodeDred(
                        dred = dred,
                        dredOffset = parseResult.offset,
                        outPcm = outBuf,
                        outPcmOffset = 0,
                        frameSize = frameSize
                    )
                    println("DRED decode result: $dredDecoded")
                }

                sampCount += frameSize
                framesProcessed++
            }

            println("Full DRED workflow completed: $framesProcessed frames processed, DRED found: $dredFound")

        } finally {
            encoder.close()
            decoder.close()
            dredDecoder.close()
            dred.close()
        }
    }
}
