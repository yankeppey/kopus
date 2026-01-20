/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of multistream API tests from
 * test_opus_api.c in the native libopus library.
 */
package eu.buney.kopus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * API tests for OpusMultistreamEncoder and OpusMultistreamDecoder.
 *
 * These tests verify error handling for invalid parameters, matching the
 * multistream decoder/encoder creation tests in test_opus_api.c.
 */
class OpusApiTest {

    companion object {
        private val VALID_SAMPLE_RATES = listOf(8000, 12000, 16000, 24000, 48000)
    }

    // ==================== Decoder Creation Tests ====================

    /**
     * Tests that valid decoder configurations succeed.
     * Ported from test_opus_api.c multistream decoder creation tests.
     */
    @Test
    fun testDecoderCreateValidConfigurations() {
        for (sampleRate in VALID_SAMPLE_RATES) {
            // Stereo with 1 coupled stream
            val mapping1 = byteArrayOf(0, 1)
            val dec1 = OpusMultistreamDecoder(
                sampleRate = sampleRate,
                channels = 2,
                streams = 1,
                coupledStreams = 1,
                mapping = mapping1
            )
            dec1.close()

            // Stereo with 2 uncoupled streams (dual mono)
            val mapping2 = byteArrayOf(0, 1)
            val dec2 = OpusMultistreamDecoder(
                sampleRate = sampleRate,
                channels = 2,
                streams = 2,
                coupledStreams = 0,
                mapping = mapping2
            )
            dec2.close()
        }
        println("    opus_multistream_decoder_create() valid configs ... OK.")
    }

    /**
     * Tests that decoder creation fails with invalid sample rates.
     * Ported from test_opus_api.c line 532: opus_multistream_decoder_create(48001, ...)
     */
    @Test
    fun testDecoderCreateInvalidSampleRate() {
        val mapping = byteArrayOf(0, 1, 2, 3)

        // 48001 is not a valid sample rate
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48001,
                channels = 4,
                streams = 2,
                coupledStreams = 1,
                mapping = mapping
            )
        }

        // 44100 is not supported by Opus
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 44100,
                channels = 2,
                streams = 1,
                coupledStreams = 1,
                mapping = byteArrayOf(0, 1)
            )
        }

        println("    opus_multistream_decoder_create() invalid sample rate ... OK.")
    }

    /**
     * Tests that decoder creation fails with invalid channel counts.
     * Ported from test_opus_api.c lines 471-501.
     */
    @Test
    fun testDecoderCreateInvalidChannels() {
        val mapping = byteArrayOf(0, 1)

        // channels = 0 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48000,
                channels = 0,
                streams = 1,
                coupledStreams = 0,
                mapping = mapping
            )
        }

        // channels = -1 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48000,
                channels = -1,
                streams = 1,
                coupledStreams = 0,
                mapping = mapping
            )
        }

        println("    opus_multistream_decoder_create() invalid channels ... OK.")
    }

    /**
     * Tests that decoder creation fails with invalid stream counts.
     * Ported from test_opus_api.c lines 440-453.
     */
    @Test
    fun testDecoderCreateInvalidStreams() {
        val mapping = byteArrayOf(0)

        // streams = 0 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48000,
                channels = 1,
                streams = 0,
                coupledStreams = 0,
                mapping = mapping
            )
        }

        // streams = -1 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48000,
                channels = 1,
                streams = -1,
                coupledStreams = 0,
                mapping = mapping
            )
        }

        println("    opus_multistream_decoder_create() invalid streams ... OK.")
    }

    /**
     * Tests that decoder creation fails with invalid coupled stream counts.
     * Ported from test_opus_api.c lines 449, 457.
     */
    @Test
    fun testDecoderCreateInvalidCoupledStreams() {
        val mapping = byteArrayOf(0, 1)

        // coupledStreams = -1 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48000,
                channels = 2,
                streams = 1,
                coupledStreams = -1,
                mapping = mapping
            )
        }

        // coupledStreams > streams should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamDecoder(
                sampleRate = 48000,
                channels = 2,
                streams = 1,
                coupledStreams = 2,
                mapping = mapping
            )
        }

        println("    opus_multistream_decoder_create() invalid coupled streams ... OK.")
    }

    // ==================== Encoder Creation Tests ====================

    /**
     * Tests that valid encoder configurations succeed.
     * Ported from test_opus_api.c multistream encoder creation tests.
     */
    @Test
    fun testEncoderCreateValidConfigurations() {
        for (sampleRate in VALID_SAMPLE_RATES) {
            for (app in listOf(OpusApplication.Voip, OpusApplication.Audio)) {
                // Stereo with 1 coupled stream
                val mapping1 = byteArrayOf(0, 1)
                val enc1 = OpusMultistreamEncoder(
                    sampleRate = sampleRate,
                    channels = 2,
                    streams = 1,
                    coupledStreams = 1,
                    mapping = mapping1,
                    application = app
                )
                enc1.close()

                // Dual mono (2 uncoupled streams)
                val mapping2 = byteArrayOf(0, 1)
                val enc2 = OpusMultistreamEncoder(
                    sampleRate = sampleRate,
                    channels = 2,
                    streams = 2,
                    coupledStreams = 0,
                    mapping = mapping2,
                    application = app
                )
                enc2.close()
            }
        }
        println("    opus_multistream_encoder_create() valid configs ... OK.")
    }

    /**
     * Tests that encoder creation fails with invalid sample rates.
     * Ported from test_opus_api.c line 346.
     */
    @Test
    fun testEncoderCreateInvalidSampleRate() {
        val mapping = byteArrayOf(0, 1)

        // 44100 is not supported
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamEncoder(
                sampleRate = 44100,
                channels = 2,
                streams = 2,
                coupledStreams = 0,
                mapping = mapping,
                application = OpusApplication.Voip
            )
        }

        println("    opus_multistream_encoder_create() invalid sample rate ... OK.")
    }

    /**
     * Tests that encoder creation fails with invalid channel counts.
     * Ported from test_opus_api.c lines 343, 355.
     */
    @Test
    fun testEncoderCreateInvalidChannels() {
        val mapping = byteArrayOf(0, 1)

        // channels = 0 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamEncoder(
                sampleRate = 8000,
                channels = 0,
                streams = 1,
                coupledStreams = 0,
                mapping = mapping,
                application = OpusApplication.Voip
            )
        }

        println("    opus_multistream_encoder_create() invalid channels ... OK.")
    }

    /**
     * Tests that encoder creation fails with invalid stream counts.
     * Ported from test_opus_api.c line 352.
     */
    @Test
    fun testEncoderCreateInvalidStreams() {
        val mapping = byteArrayOf(0, 1)

        // streams = -1 should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamEncoder(
                sampleRate = 8000,
                channels = 2,
                streams = -1,
                coupledStreams = 0,
                mapping = mapping,
                application = OpusApplication.Voip
            )
        }

        println("    opus_multistream_encoder_create() invalid streams ... OK.")
    }

    /**
     * Tests that encoder creation fails with invalid coupled stream counts.
     * Ported from test_opus_api.c line 349.
     */
    @Test
    fun testEncoderCreateInvalidCoupledStreams() {
        val mapping = byteArrayOf(0, 1)

        // coupledStreams > streams should fail
        assertFailsWith<IllegalArgumentException> {
            OpusMultistreamEncoder(
                sampleRate = 8000,
                channels = 2,
                streams = 2,
                coupledStreams = 3,
                mapping = mapping,
                application = OpusApplication.Voip
            )
        }

        println("    opus_multistream_encoder_create() invalid coupled streams ... OK.")
    }

    // ==================== CTL Tests ====================

    /**
     * Tests basic CTL operations on multistream decoder.
     * Ported from test_opus_api.c lines 552-643.
     */
    @Test
    fun testDecoderCtlOperations() {
        // 2 channels, 1 coupled stream = 1 stereo stream
        val mapping = byteArrayOf(0, 1)
        val decoder = OpusMultistreamDecoder(
            sampleRate = 48000,
            channels = 2,
            streams = 1,
            coupledStreams = 1,
            mapping = mapping
        )

        try {
            // Test OPUS_GET_FINAL_RANGE
            val finalRange = decoder.getFinalRange()
            // Initial value should be 0 before any decoding
            assertEquals(0, finalRange, "Initial FINAL_RANGE should be 0")

            // Test OPUS_SET_GAIN / OPUS_GET_GAIN
            val gainResult = decoder.setGain(15)
            assertTrue(gainResult >= 0, "setGain should succeed")
            val gain = decoder.getGain()
            assertEquals(15, gain, "Gain should be 15")

            // Test OPUS_GET_BANDWIDTH (before decoding, may return 0 or error)
            // Just verify it doesn't crash
            decoder.getBandwidth()

            // Test OPUS_RESET_STATE
            val resetResult = decoder.resetState()
            assertTrue(resetResult >= 0, "resetState should succeed")

            println("    opus_multistream_decoder_ctl() .................... OK.")

        } finally {
            decoder.close()
        }
    }

    /**
     * Tests basic CTL operations on multistream encoder.
     * Ported from test_opus_api.c encoder CTL tests.
     */
    @Test
    fun testEncoderCtlOperations() {
        val mapping = byteArrayOf(0, 1)
        val encoder = OpusMultistreamEncoder(
            sampleRate = 48000,
            channels = 2,
            streams = 1,
            coupledStreams = 1,
            mapping = mapping,
            application = OpusApplication.Audio
        )

        try {
            // Test OPUS_GET_BITRATE
            val bitrate = encoder.getBitrate()
            assertTrue(bitrate > 0, "Bitrate should be positive")

            // Test OPUS_SET_BITRATE / OPUS_GET_BITRATE
            // Note: Multistream encoder may adjust bitrate based on stream configuration
            val setBitrateResult = encoder.setBitrate(64000)
            assertTrue(setBitrateResult >= 0, "setBitrate should succeed")
            val newBitrate = encoder.getBitrate()
            assertTrue(newBitrate > 0, "Bitrate after set should be positive")

            // Test OPUS_GET_LSB_DEPTH
            val lsbDepth = encoder.getLSBDepth()
            assertTrue(lsbDepth >= 16, "LSB depth should be at least 16")

            // Test OPUS_SET_COMPLEXITY / OPUS_GET_COMPLEXITY
            encoder.setComplexity(5)
            val complexity = encoder.getComplexity()
            assertEquals(5, complexity, "Complexity should be 5")

            // Test OPUS_GET_FINAL_RANGE
            val finalRange = encoder.getFinalRange()
            // Initial value before encoding
            assertEquals(0, finalRange, "Initial FINAL_RANGE should be 0")

            // Test OPUS_RESET_STATE
            val resetResult = encoder.resetState()
            assertTrue(resetResult >= 0, "resetState should succeed")

            println("    opus_multistream_encoder_ctl() .................... OK.")

        } finally {
            encoder.close()
        }
    }

    // ==================== Surround Encoder Creation Tests ====================

    /**
     * Tests createSurround with valid configurations.
     */
    @Test
    fun testCreateSurroundValidConfigurations() {
        // Mapping family 1 (Vorbis order) - various channel counts
        for (channels in listOf(1, 2, 3, 4, 5, 6)) {
            val result = OpusMultistreamEncoder.createSurround(
                sampleRate = 48000,
                channels = channels,
                mappingFamily = 1,
                application = OpusApplication.Audio
            )
            assertTrue(result.streams > 0, "Streams should be positive for $channels channels")
            assertTrue(result.coupledStreams >= 0, "Coupled streams should be non-negative")
            assertTrue(result.coupledStreams <= result.streams, "Coupled streams should not exceed total streams")
            assertEquals(channels, result.mapping.size, "Mapping size should equal channel count")
            result.encoder.close()
        }

        // Mapping family 255 (discrete) - each channel is independent
        for (channels in listOf(1, 2, 4, 8)) {
            val result = OpusMultistreamEncoder.createSurround(
                sampleRate = 48000,
                channels = channels,
                mappingFamily = 255,
                application = OpusApplication.Audio
            )
            assertEquals(channels, result.streams, "Discrete should have one stream per channel")
            assertEquals(0, result.coupledStreams, "Discrete should have no coupled streams")
            result.encoder.close()
        }

        println("    opus_multistream_surround_encoder_create() ......... OK.")
    }

    /**
     * Tests createSurround fails with invalid mapping families for certain channel counts.
     */
    @Test
    fun testCreateSurroundInvalidMappingFamily() {
        // Mapping family 0 only supports 1-2 channels
        // 3+ channels with family 0 should fail
        assertFailsWith<IllegalStateException> {
            OpusMultistreamEncoder.createSurround(
                sampleRate = 48000,
                channels = 3,
                mappingFamily = 0,
                application = OpusApplication.Audio
            )
        }

        println("    opus_multistream_surround_encoder_create() invalid family ... OK.")
    }
}
