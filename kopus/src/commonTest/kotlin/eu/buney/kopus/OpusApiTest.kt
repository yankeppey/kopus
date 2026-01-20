/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of API tests from test_opus_api.c
 * in the native libopus library.
 *
 * C test coverage mapping:
 * - test_opus_api.c:test_msdec_api() lines 347-713  → Multistream decoder tests
 * - test_opus_api.c:test_enc_api() lines 1073-1453  → Encoder tests (partial)
 * - test_opus_api.c:test_repacketizer_api() lines 1456-1775 → Repacketizer tests
 * - Kopus-original → Surround encoder tests, Boolean CTL roundtrip tests
 */
package eu.buney.kopus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * API tests for Opus encoder, decoder, multistream, and repacketizer.
 *
 * These tests verify error handling for invalid parameters and basic functionality,
 * matching the tests in test_opus_api.c from the native libopus library.
 */
class OpusApiTest {

    companion object {
        private val VALID_SAMPLE_RATES = listOf(8000, 12000, 16000, 24000, 48000)
        private const val SAMPLE_RATE = 48000
        private const val CHANNELS = 1
        private const val FRAME_SIZE = 480  // 10ms at 48kHz
        private const val MAX_PACKET = 1500
    }

    //region Multistream decoder tests - test_opus_api.c:test_msdec_api() lines 347-713

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

    //endregion

    //region Encoder tests - test_opus_api.c:test_enc_api() lines 1073-1453

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

    //endregion

    //region Repacketizer tests - test_opus_api.c:test_repacketizer_api() lines 1456-1775

    /**
     * Helper to generate silence PCM data.
     */
    private fun generateSilence(samples: Int, channels: Int): ShortArray {
        return ShortArray(samples * channels)
    }

    /**
     * Helper to encode a single frame.
     */
    private fun encodeFrame(encoder: OpusEncoder, frameSize: Int): ByteArray {
        val pcm = generateSilence(frameSize, encoder.channels)
        val packet = ByteArray(MAX_PACKET)
        val len = encoder.encode(pcm, 0, frameSize, packet, 0, MAX_PACKET)
        assertTrue(len > 0, "Encoding failed: $len")
        return packet.copyOf(len)
    }

    /**
     * Tests basic repacketizer creation and destruction.
     * Ported from test_opus_api.c lines 1484-1487.
     */
    @Test
    fun testRepacketizerCreateAndDestroy() {
        val rp = OpusRepacketizer()
        rp.close()
        println("    opus_repacketizer_create ..................... OK.")
    }

    /**
     * Tests that initial frame count is zero.
     * Ported from test_opus_api.c lines 1489-1491.
     */
    @Test
    fun testRepacketizerInitialFrameCountIsZero() {
        val rp = OpusRepacketizer()
        try {
            assertEquals(0, rp.getNbFrames())
            println("    opus_repacketizer_get_nb_frames .............. OK.")
        } finally {
            rp.close()
        }
    }

    /**
     * Tests adding a single packet to the repacketizer.
     * Ported from test_opus_api.c line 1522.
     */
    @Test
    fun testRepacketizerAddSinglePacket() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            val packet = encodeFrame(encoder, FRAME_SIZE)
            val result = rp.cat(packet, packet.size)
            assertEquals(OPUS_OK, result, "cat() failed: $result")
            assertEquals(1, rp.getNbFrames())
        } finally {
            rp.close()
            encoder.close()
        }
    }

    /**
     * Tests merging multiple packets into one.
     * Kopus-original test using real encoded audio.
     */
    @Test
    fun testRepacketizerMergeMultiplePackets() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val decoder = OpusDecoder(SAMPLE_RATE, CHANNELS)
        val rp = OpusRepacketizer()
        try {
            // Encode 3 separate 10ms packets
            val packet1 = encodeFrame(encoder, FRAME_SIZE)
            val packet2 = encodeFrame(encoder, FRAME_SIZE)
            val packet3 = encodeFrame(encoder, FRAME_SIZE)

            // Add to repacketizer
            assertEquals(OPUS_OK, rp.cat(packet1))
            assertEquals(OPUS_OK, rp.cat(packet2))
            assertEquals(OPUS_OK, rp.cat(packet3))
            assertEquals(3, rp.getNbFrames())

            // Output merged packet
            val merged = ByteArray(MAX_PACKET)
            val mergedLen = rp.out(merged)
            assertTrue(mergedLen > 0, "out() failed: $mergedLen")

            // Verify merged packet can be decoded
            val outPcm = ShortArray(FRAME_SIZE * 3 * CHANNELS)
            val samples = decoder.decode(merged, 0, mergedLen, outPcm, 0, FRAME_SIZE * 3)
            assertEquals(FRAME_SIZE * 3, samples, "Decoded $samples samples, expected ${FRAME_SIZE * 3}")
        } finally {
            rp.close()
            decoder.close()
            encoder.close()
        }
    }

    /**
     * Tests splitting a multi-frame packet into individual frames.
     * Kopus-original test using real encoded audio.
     */
    @Test
    fun testRepacketizerSplitMultiFramePacket() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            encoder.setExpertFrameDuration(OPUS_FRAMESIZE_60_MS)  // 3x 20ms frames

            // Encode 60ms of audio (3 frames at 20ms each)
            val frameSize = SAMPLE_RATE / 1000 * 60  // 60ms = 2880 samples at 48kHz
            val pcm = generateSilence(frameSize, CHANNELS)
            val packet = ByteArray(MAX_PACKET)
            val len = encoder.encode(pcm, 0, frameSize, packet, 0, MAX_PACKET)
            assertTrue(len > 0, "Encoding failed: $len")

            // Add to repacketizer
            val catResult = rp.cat(packet, len)
            assertEquals(OPUS_OK, catResult, "cat() failed: $catResult")

            // Should have 3 frames
            val nbFrames = rp.getNbFrames()
            assertEquals(3, nbFrames, "Expected 3 frames in 60ms packet, got $nbFrames")

            // Extract each frame individually
            for (i in 0 until nbFrames) {
                val frame = ByteArray(MAX_PACKET)
                val frameLen = rp.outRange(i, i + 1, frame)
                assertTrue(frameLen > 0, "outRange($i, ${i + 1}) failed: $frameLen")

                // Verify each extracted frame has 1 frame
                val extractedFrames = OpusPacket.getNbFrames(frame, frameLen)
                assertEquals(1, extractedFrames, "Extracted packet should have 1 frame, got $extractedFrames")
            }
        } finally {
            rp.close()
            encoder.close()
        }
    }

    /**
     * Tests that init() resets the repacketizer state.
     * Ported from test_opus_api.c lines 1478-1482, 1529.
     */
    @Test
    fun testRepacketizerInitResetsState() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            // Add a packet
            val packet = encodeFrame(encoder, FRAME_SIZE)
            assertEquals(OPUS_OK, rp.cat(packet))
            assertEquals(1, rp.getNbFrames())

            // Reset
            rp.init()

            // Frame count should be 0
            assertEquals(0, rp.getNbFrames())

            // Can add packet again
            assertEquals(OPUS_OK, rp.cat(packet))
            assertEquals(1, rp.getNbFrames())

            println("    opus_repacketizer_init ....................... OK.")
        } finally {
            rp.close()
            encoder.close()
        }
    }

    /**
     * Tests that incompatible packets are rejected.
     * Ported from test_opus_api.c line 1525: Change in TOC.
     */
    @Test
    fun testRepacketizerIncompatiblePacketsFail() {
        val encoder48k = OpusEncoder(48000, 1, OpusApplication.Audio)
        val encoder8k = OpusEncoder(8000, 1, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            // Force different bandwidths to ensure incompatibility
            encoder48k.setMaxBandwidth(OPUS_BANDWIDTH_FULLBAND)
            encoder8k.setMaxBandwidth(OPUS_BANDWIDTH_NARROWBAND)

            // Encode packets with different configurations
            val packet48k = encodeFrame(encoder48k, 960)  // 20ms at 48kHz
            val packet8k = encodeFrame(encoder8k, 160)    // 20ms at 8kHz

            // First packet should succeed
            assertEquals(OPUS_OK, rp.cat(packet48k))

            // Second packet should fail due to incompatible configuration
            val result = rp.cat(packet8k)
            assertEquals(OPUS_INVALID_PACKET, result, "Expected OPUS_INVALID_PACKET, got $result")

            // Frame count should still be 1
            assertEquals(1, rp.getNbFrames())
        } finally {
            rp.close()
            encoder8k.close()
            encoder48k.close()
        }
    }

    /**
     * Tests partial frame extraction with outRange().
     * Kopus-original test.
     */
    @Test
    fun testRepacketizerOutRangePartialExtraction() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            // Add 4 packets
            repeat(4) {
                val packet = encodeFrame(encoder, FRAME_SIZE)
                assertEquals(OPUS_OK, rp.cat(packet))
            }
            assertEquals(4, rp.getNbFrames())

            // Extract frames 1-3 (middle two frames)
            val partial = ByteArray(MAX_PACKET)
            val partialLen = rp.outRange(1, 3, partial)
            assertTrue(partialLen > 0, "outRange(1, 3) failed: $partialLen")

            // Verify extracted packet has 2 frames
            val nbFrames = OpusPacket.getNbFrames(partial, partialLen)
            assertEquals(2, nbFrames, "Expected 2 frames in extracted packet, got $nbFrames")
        } finally {
            rp.close()
            encoder.close()
        }
    }

    /**
     * Tests OPUS_BUFFER_TOO_SMALL error.
     * Ported from test_opus_api.c lines 1588-1596.
     */
    @Test
    fun testRepacketizerBufferTooSmall() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            val packet = encodeFrame(encoder, FRAME_SIZE)
            assertEquals(OPUS_OK, rp.cat(packet))

            // Try to output with tiny buffer
            val tinyBuffer = ByteArray(1)
            val result = rp.out(tinyBuffer, 1)
            assertEquals(OPUS_BUFFER_TOO_SMALL, result, "Expected OPUS_BUFFER_TOO_SMALL, got $result")
        } finally {
            rp.close()
            encoder.close()
        }
    }

    /**
     * Tests OPUS_BAD_ARG for invalid frame range.
     * Ported from test_opus_api.c line 1597.
     */
    @Test
    fun testRepacketizerInvalidFrameRange() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            val packet = encodeFrame(encoder, FRAME_SIZE)
            assertEquals(OPUS_OK, rp.cat(packet))
            assertEquals(1, rp.getNbFrames())

            // Try invalid range (beyond available frames)
            val output = ByteArray(MAX_PACKET)
            val result = rp.outRange(0, 5, output)  // Only 1 frame available
            assertEquals(OPUS_BAD_ARG, result, "Expected OPUS_BAD_ARG for invalid range, got $result")
        } finally {
            rp.close()
            encoder.close()
        }
    }

    /**
     * Tests zero-length packet rejection.
     * Ported from test_opus_api.c line 1495: Zero len.
     */
    @Test
    fun testRepacketizerMalformedZeroLength() {
        val rp = OpusRepacketizer()
        try {
            val packet = ByteArray(10)
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 0), "Zero-length packet should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 1 with odd payload rejection.
     * Ported from test_opus_api.c line 1498: Odd payload code 1.
     * Code 1 (bits 01) means 2 frames of equal size, so payload must be even.
     */
    @Test
    fun testRepacketizerMalformedCode1OddPayload() {
        val rp = OpusRepacketizer()
        try {
            val packet = byteArrayOf(0x01, 0x00)  // TOC with code 1, 1 byte payload (odd)
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 2), "Code 1 with odd payload should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 2 overflow (missing length byte).
     * Ported from test_opus_api.c line 1501: Code 2 overflow one.
     * Code 2 (bits 10) needs a length byte, but packet is only 1 byte.
     */
    @Test
    fun testRepacketizerMalformedCode2OverflowOne() {
        val rp = OpusRepacketizer()
        try {
            val packet = byteArrayOf(0x02)  // TOC with code 2, no length byte
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 1), "Code 2 with missing length byte should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 3 without count byte.
     * Ported from test_opus_api.c line 1504: Code 3 no count.
     * Code 3 (bits 11) needs a frame count byte, but packet is only 1 byte.
     */
    @Test
    fun testRepacketizerMalformedCode3NoCount() {
        val rp = OpusRepacketizer()
        try {
            val packet = byteArrayOf(0x03)  // TOC with code 3, no count byte
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 1), "Code 3 with missing count byte should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 2 with length overflow.
     * Ported from test_opus_api.c line 1508: Code 2 overflow two.
     * Code 2 with length=255 but only 2 bytes total (TOC + length).
     */
    @Test
    fun testRepacketizerMalformedCode2OverflowTwo() {
        val rp = OpusRepacketizer()
        try {
            val packet = byteArrayOf(0x02, 0xFF.toByte())  // TOC with code 2, length claims 255
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 2), "Code 2 with length overflow should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 2 with 2-byte length encoding overflow.
     * Ported from test_opus_api.c line 1512: Code 2 overflow three.
     * Code 2 with 2-byte length encoding that overflows.
     */
    @Test
    fun testRepacketizerMalformedCode2OverflowThree() {
        val rp = OpusRepacketizer()
        try {
            val packet = ByteArray(251)
            packet[0] = 0x02  // TOC with code 2
            packet[1] = 0xFA.toByte()  // Length = 250 (requires 2-byte encoding continuation)
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 251), "Code 2 with 2-byte length overflow should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 3 with frame count = 0.
     * Ported from test_opus_api.c line 1516: Code 3 m=0.
     * Code 3 with frame count = 0 (invalid, must be 1-48).
     */
    @Test
    fun testRepacketizerMalformedCode3FrameCountZero() {
        val rp = OpusRepacketizer()
        try {
            val packet = byteArrayOf(0x03, 0x00)  // TOC with code 3, m=0
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 2), "Code 3 with m=0 should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 3 with frame count = 49 (too high).
     * Ported from test_opus_api.c line 1519: Code 3 m=49.
     * Code 3 with frame count = 49 (invalid, max is 48).
     */
    @Test
    fun testRepacketizerMalformedCode3FrameCountTooHigh() {
        val rp = OpusRepacketizer()
        try {
            val packet = ByteArray(100)
            packet[0] = 0x03  // TOC with code 3
            packet[1] = 49    // m=49 (invalid)
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet, 100), "Code 3 with m=49 should return OPUS_INVALID_PACKET")
            assertEquals(0, rp.getNbFrames())
        } finally {
            rp.close()
        }
    }

    /**
     * Tests TOC change rejection.
     * Ported from test_opus_api.c line 1525: Change in TOC.
     * Adding a packet with different TOC after a valid one should fail.
     */
    @Test
    fun testRepacketizerMalformedTocChange() {
        val rp = OpusRepacketizer()
        try {
            // First packet: valid code 0 packet (TOC byte 0x00)
            val packet1 = byteArrayOf(0x00, 0x00, 0x00)  // TOC=0x00, 2 bytes payload
            assertEquals(OPUS_OK, rp.cat(packet1, 3), "First valid packet should succeed")
            assertEquals(1, rp.getNbFrames())

            // Second packet: different TOC (0x04 = different config)
            val packet2 = byteArrayOf(0x04, 0x00, 0x00)  // Different TOC
            assertEquals(OPUS_INVALID_PACKET, rp.cat(packet2, 3), "Packet with different TOC should return OPUS_INVALID_PACKET")
            assertEquals(1, rp.getNbFrames(), "Frame count should remain 1 after rejected packet")
        } finally {
            rp.close()
        }
    }

    /**
     * Tests code 3 with various invalid frame counts (0, 49-63).
     * Ported from test_opus_api.c - the frame count field in code 3 uses bits 0-5 (0-63),
     * but only 1-48 are valid.
     */
    @Test
    fun testRepacketizerMalformedCode3FrameCountsOutOfRange() {
        val rp = OpusRepacketizer()
        try {
            val invalidCounts = listOf(0, 49, 50, 55, 60, 63)
            for (m in invalidCounts) {
                rp.init()  // Reset between tests
                val packet = ByteArray(100)
                packet[0] = 0x03  // TOC with code 3
                packet[1] = m.toByte()
                assertEquals(
                    OPUS_INVALID_PACKET,
                    rp.cat(packet, 100),
                    "Code 3 with m=$m should return OPUS_INVALID_PACKET"
                )
                assertEquals(0, rp.getNbFrames())
            }
        } finally {
            rp.close()
        }
    }

    /**
     * Tests packet padding and unpadding.
     * Ported from test_opus_api.c lines 1572-1578.
     */
    @Test
    fun testRepacketizerPadAndUnpad() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        try {
            val pcm = generateSilence(FRAME_SIZE, CHANNELS)
            val packet = ByteArray(2000)  // Larger buffer for padding
            val originalLen = encoder.encode(pcm, 0, FRAME_SIZE, packet, 0, MAX_PACKET)
            assertTrue(originalLen > 0, "Encoding failed: $originalLen")

            // Pad to larger size
            val paddedLen = originalLen + 100
            val padResult = OpusPacket.pad(packet, originalLen, paddedLen)
            assertEquals(OPUS_OK, padResult, "pad() failed: $padResult")

            // Unpad back
            val unpaddedLen = OpusPacket.unpad(packet, paddedLen)
            assertEquals(originalLen, unpaddedLen, "unpad() returned $unpaddedLen, expected $originalLen")
        } finally {
            encoder.close()
        }
    }

    /**
     * Tests pad() with invalid arguments.
     * Ported from test_opus_api.c lines 1725-1726.
     */
    @Test
    fun testRepacketizerPadInvalidArgs() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        try {
            val pcm = generateSilence(FRAME_SIZE, CHANNELS)
            val packet = ByteArray(MAX_PACKET)
            val len = encoder.encode(pcm, 0, FRAME_SIZE, packet, 0, MAX_PACKET)
            assertTrue(len > 0, "Encoding failed")

            // Try to pad to smaller size (invalid)
            val result = OpusPacket.pad(packet, len, len - 1)
            assertEquals(OPUS_BAD_ARG, result, "Expected OPUS_BAD_ARG for newLen < len")
        } finally {
            encoder.close()
        }
    }

    /**
     * Tests multistream packet padding and unpadding.
     * Ported from test_opus_api.c lines 1580-1587.
     */
    @Test
    fun testRepacketizerMultistreamPadAndUnpad() {
        val streams = 1
        val coupledStreams = 1
        val mapping = byteArrayOf(0, 1)

        val encoder = OpusMultistreamEncoder(
            sampleRate = SAMPLE_RATE,
            channels = 2,
            streams = streams,
            coupledStreams = coupledStreams,
            mapping = mapping,
            application = OpusApplication.Audio
        )
        try {
            val pcm = generateSilence(FRAME_SIZE, 2)
            val packet = ByteArray(2000)
            val originalLen = encoder.encode(pcm, 0, FRAME_SIZE, packet, 0, MAX_PACKET)
            assertTrue(originalLen > 0, "Encoding failed: $originalLen")

            // Pad multistream packet
            val paddedLen = originalLen + 50
            val padResult = OpusPacket.padMultistream(packet, originalLen, paddedLen, streams)
            assertEquals(OPUS_OK, padResult, "padMultistream() failed: $padResult")

            // Unpad multistream packet
            val unpaddedLen = OpusPacket.unpadMultistream(packet, paddedLen, streams)
            assertEquals(originalLen, unpaddedLen, "unpadMultistream() returned $unpaddedLen, expected $originalLen")
        } finally {
            encoder.close()
        }
    }

    /**
     * Tests multiple rounds of repacketizing.
     * Kopus-original test.
     */
    @Test
    fun testRepacketizerMultipleRounds() {
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.Audio)
        val rp = OpusRepacketizer()
        try {
            // Round 1: merge 2 packets
            repeat(2) {
                val packet = encodeFrame(encoder, FRAME_SIZE)
                assertEquals(OPUS_OK, rp.cat(packet))
            }
            assertEquals(2, rp.getNbFrames())

            val merged1 = ByteArray(MAX_PACKET)
            val len1 = rp.out(merged1)
            assertTrue(len1 > 0)

            // Reset for round 2
            rp.init()
            assertEquals(0, rp.getNbFrames())

            // Round 2: merge 3 packets
            repeat(3) {
                val packet = encodeFrame(encoder, FRAME_SIZE)
                assertEquals(OPUS_OK, rp.cat(packet))
            }
            assertEquals(3, rp.getNbFrames())

            val merged2 = ByteArray(MAX_PACKET)
            val len2 = rp.out(merged2)
            assertTrue(len2 > 0)

            println("                        All repacketizer tests passed.")
        } finally {
            rp.close()
            encoder.close()
        }
    }

    //endregion

    //region Surround encoder tests - Kopus-original

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

    //endregion

    //region Boolean CTL roundtrip tests - Kopus-original

    /**
     * Tests Boolean CTL parameter roundtrip for all boolean-ish encoder CTLs.
     */
    @Test
    fun testEncoderBooleanCtlRoundtrip() {
        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)

        try {
            // Test VBR
            encoder.setVBR(false)
            assertEquals(false, encoder.getVBR(), "VBR should be false")
            encoder.setVBR(true)
            assertEquals(true, encoder.getVBR(), "VBR should be true")

            // Test VBR Constraint
            encoder.setVBRConstraint(false)
            assertEquals(false, encoder.getVBRConstraint(), "VBRConstraint should be false")
            encoder.setVBRConstraint(true)
            assertEquals(true, encoder.getVBRConstraint(), "VBRConstraint should be true")

            // Test Inband FEC
            encoder.setInbandFEC(false)
            assertEquals(false, encoder.getInbandFEC(), "InbandFEC should be false")
            encoder.setInbandFEC(true)
            assertEquals(true, encoder.getInbandFEC(), "InbandFEC should be true")

            // Test DTX
            encoder.setDTX(false)
            assertEquals(false, encoder.getDTX(), "DTX should be false")
            encoder.setDTX(true)
            assertEquals(true, encoder.getDTX(), "DTX should be true")

            // Test Prediction Disabled
            encoder.setPredictionDisabled(false)
            assertEquals(false, encoder.getPredictionDisabled(), "PredictionDisabled should be false")
            encoder.setPredictionDisabled(true)
            assertEquals(true, encoder.getPredictionDisabled(), "PredictionDisabled should be true")

            // Test Phase Inversion Disabled
            encoder.setPhaseInversionDisabled(false)
            assertEquals(false, encoder.getPhaseInversionDisabled(), "PhaseInversionDisabled should be false")
            encoder.setPhaseInversionDisabled(true)
            assertEquals(true, encoder.getPhaseInversionDisabled(), "PhaseInversionDisabled should be true")

            println("    Encoder Boolean CTL roundtrip ...................... OK.")

        } finally {
            encoder.close()
        }
    }

    /**
     * Tests Boolean CTL parameter roundtrip for all boolean-ish decoder CTLs.
     */
    @Test
    fun testDecoderBooleanCtlRoundtrip() {
        val decoder = OpusDecoder(48000, 2)

        try {
            // Test Phase Inversion Disabled
            decoder.setPhaseInversionDisabled(false)
            assertEquals(false, decoder.getPhaseInversionDisabled(), "PhaseInversionDisabled should be false")
            decoder.setPhaseInversionDisabled(true)
            assertEquals(true, decoder.getPhaseInversionDisabled(), "PhaseInversionDisabled should be true")

            // Test Ignore Extensions
            decoder.setIgnoreExtensions(false)
            assertEquals(false, decoder.getIgnoreExtensions(), "IgnoreExtensions should be false")
            decoder.setIgnoreExtensions(true)
            assertEquals(true, decoder.getIgnoreExtensions(), "IgnoreExtensions should be true")

            println("    Decoder Boolean CTL roundtrip ...................... OK.")

        } finally {
            decoder.close()
        }
    }

    //endregion
}
