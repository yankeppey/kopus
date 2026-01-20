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

import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Encoder tests ported from native libopus test_opus_encode.c
 *
 * These tests verify:
 * 1. Basic encode→decode roundtrip with FINAL_RANGE verification
 * 2. Encoder settings fuzzing (fuzz_encoder_settings)
 * 3. Multistream encode/decode with various channel configurations
 */
class OpusEncodeTest {

    companion object {
        // Test constants matching the original C test
        private const val MAX_PACKET = 1500
        private const val SAMPLES = 48000 * 30  // 30 seconds at 48kHz
        private const val SSAMPLES = SAMPLES / 3
        private const val MAX_FRAME_SAMP = 5760  // 120ms at 48kHz
    }

    // Pseudo-random number generator state (instance-level for test isolation)
    private var Rz: UInt = 0u
    private var Rw: UInt = 0u

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
     * Select a random element from an array.
     */
    private fun <T> randSample(array: Array<T>): T {
        return array[(fastRand() % array.size.toUInt()).toInt()]
    }

    private fun randSample(array: IntArray): Int {
        return array[(fastRand() % array.size.toUInt()).toInt()]
    }

    private fun randSampleBoolean(array: BooleanArray): Boolean {
        return array[(fastRand() % array.size.toUInt()).toInt()]
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
     * Generate synthetic audio for testing with configurable channels.
     */
    private fun generateMusicWithChannels(buf: ShortArray, len: Int, channels: Int) {
        var a1 = 0
        var b1 = 0
        var a2 = 0
        var b2 = 0
        var c1 = 0
        var c2 = 0
        var d1 = 0
        var d2 = 0
        var j = 0

        val silenceSamples = min(2880, len)
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
     * Generates synthetic multi-channel audio for testing.
     */
    private fun generateMultichannelAudio(buf: ShortArray, len: Int, channels: Int) {
        initSeed(42u)

        for (i in 0 until len) {
            for (ch in 0 until channels) {
                val baseFreq = 440.0 + (ch * 100.0)
                val t = i.toDouble() / 48000.0
                val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * baseFreq * t) * 16000).toInt()

                val r = fastRand()
                val noise = ((r and 255u).toInt() - 128)
                val finalSample = (sample + noise).coerceIn(-32768, 32767)

                buf[i * channels + ch] = finalSample.toShort()
            }
        }
    }

    /**
     * Convert frame size in milliseconds (x2 to avoid 2.5ms) to samples.
     */
    private fun frameSizeToSamples(frameSizeMsX2: Int, sampleRate: Int): Int {
        return frameSizeMsX2 * sampleRate / 2000
    }

    /**
     * Get the OPUS_FRAMESIZE_* constant for a given frame size.
     */
    private fun getFrameSizeEnum(frameSize: Int, sampleRate: Int): Int {
        return when (frameSize) {
            sampleRate / 400 -> OPUS_FRAMESIZE_2_5_MS
            sampleRate / 200 -> OPUS_FRAMESIZE_5_MS
            sampleRate / 100 -> OPUS_FRAMESIZE_10_MS
            sampleRate / 50 -> OPUS_FRAMESIZE_20_MS
            sampleRate / 25 -> OPUS_FRAMESIZE_40_MS
            3 * sampleRate / 50 -> OPUS_FRAMESIZE_60_MS
            4 * sampleRate / 50 -> OPUS_FRAMESIZE_80_MS
            5 * sampleRate / 50 -> OPUS_FRAMESIZE_100_MS
            6 * sampleRate / 50 -> OPUS_FRAMESIZE_120_MS
            else -> error("Invalid frame size: $frameSize for sample rate $sampleRate")
        }
    }

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
     * Verifies FINAL_RANGE for multistream encoder/decoder.
     */
    private fun verifyFinalRange(
        encoder: OpusMultistreamEncoder,
        decoder: OpusMultistreamDecoder,
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

    // ==================== Basic Encode/Decode Tests ====================

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

    // ==================== Encoder Settings Fuzzing Tests ====================

    /**
     * Test encode→decode roundtrip with given encoder settings.
     * Returns true on success, false on failure.
     */
    private fun testEncode(
        encoder: OpusEncoder,
        decoder: OpusDecoder,
        channels: Int,
        frameSize: Int,
        inBuf: ShortArray
    ): Boolean {
        val packet = ByteArray(MAX_PACKET)
        val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

        var sampCount = 0
        val maxSamples = inBuf.size / channels - MAX_FRAME_SAMP

        while (sampCount < maxSamples) {
            val len = encoder.encode(
                inPcm = inBuf,
                inPcmOffset = sampCount * channels,
                frameSize = frameSize,
                outData = packet,
                outDataOffset = 0,
                maxDataBytes = MAX_PACKET
            )

            if (len < 0 || len > MAX_PACKET) {
                println("opus_encode() returned $len")
                return false
            }

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
                println("opus_decode() returned $outSamples, expected $frameSize")
                return false
            }

            sampCount += frameSize
        }

        return true
    }

    /**
     * Fuzzes encoder settings by randomly selecting parameters and testing encode/decode.
     *
     * Ported from fuzz_encoder_settings() in test_opus_encode.c
     */
    private fun fuzzEncoderSettings(numEncoders: Int, numSettingChanges: Int) {
        val samplingRates = intArrayOf(8000, 12000, 16000, 24000, 48000)
        val channelOptions = intArrayOf(1, 2)
        val applications = arrayOf(
            OpusApplication.Audio,
            OpusApplication.Voip,
            OpusApplication.RestrictedLowDelay
        )
        val bitrates = intArrayOf(6000, 12000, 16000, 24000, 32000, 48000, 64000, 96000, 510000, OPUS_AUTO, OPUS_BITRATE_MAX)
        val forceChannels = intArrayOf(OPUS_AUTO, OPUS_AUTO, 1, 2)
        val useVbr = booleanArrayOf(false, true, true)
        val vbrConstraints = booleanArrayOf(false, true, true)
        val complexities = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val maxBandwidths = intArrayOf(
            OPUS_BANDWIDTH_NARROWBAND, OPUS_BANDWIDTH_MEDIUMBAND,
            OPUS_BANDWIDTH_WIDEBAND, OPUS_BANDWIDTH_SUPERWIDEBAND,
            OPUS_BANDWIDTH_FULLBAND, OPUS_BANDWIDTH_FULLBAND
        )
        val signals = intArrayOf(OPUS_AUTO, OPUS_AUTO, OPUS_SIGNAL_VOICE, OPUS_SIGNAL_MUSIC)
        val inbandFecs = booleanArrayOf(false, false, true)
        val packetLossPercs = intArrayOf(0, 1, 2, 5)
        val lsbDepths = intArrayOf(8, 24)
        val predictionDisabled = booleanArrayOf(false, false, true)
        val useDtx = booleanArrayOf(false, true)
        val frameSizesMsX2 = intArrayOf(5, 10, 20, 40, 80, 120, 160, 200, 240)

        for (i in 0 until numEncoders) {
            val sampleRate = randSample(samplingRates)
            val numChannels = randSample(channelOptions)
            val application = randSample(applications)

            val encoder = OpusEncoder(sampleRate, numChannels, application)
            val decoder = OpusDecoder(sampleRate, numChannels)

            val numSamples = sampleRate * 2
            val inBuf = ShortArray(numSamples * numChannels)
            generateMusicWithChannels(inBuf, numSamples, numChannels)

            try {
                for (j in 0 until numSettingChanges) {
                    val bitrate = randSample(bitrates)
                    var forceChannel = randSample(forceChannels)
                    val vbr = randSampleBoolean(useVbr)
                    val vbrConstraint = randSampleBoolean(vbrConstraints)
                    val complexity = randSample(complexities)
                    val maxBw = randSample(maxBandwidths)
                    val sig = randSample(signals)
                    val inbandFec = randSampleBoolean(inbandFecs)
                    val pktLoss = randSample(packetLossPercs)
                    val lsbDepth = randSample(lsbDepths)
                    val predDisabled = randSampleBoolean(predictionDisabled)
                    val dtx = randSampleBoolean(useDtx)
                    val frameSizeMsX2 = randSample(frameSizesMsX2)
                    val frameSize = frameSizeToSamples(frameSizeMsX2, sampleRate)
                    val frameSizeEnum = getFrameSizeEnum(frameSize, sampleRate)

                    forceChannel = min(forceChannel, numChannels)

                    assertEquals(0, encoder.setBitrate(bitrate), "setBitrate failed")
                    assertEquals(0, encoder.setForceChannels(forceChannel), "setForceChannels failed")
                    assertEquals(0, encoder.setVBR(vbr), "setVBR failed")
                    assertEquals(0, encoder.setVBRConstraint(vbrConstraint), "setVBRConstraint failed")
                    assertEquals(0, encoder.setComplexity(complexity), "setComplexity failed")
                    assertEquals(0, encoder.setMaxBandwidth(maxBw), "setMaxBandwidth failed")
                    assertEquals(0, encoder.setSignal(sig), "setSignal failed")
                    assertEquals(0, encoder.setInbandFEC(inbandFec), "setInbandFEC failed")
                    assertEquals(0, encoder.setPacketLossPerc(pktLoss), "setPacketLossPerc failed")
                    assertEquals(0, encoder.setLSBDepth(lsbDepth), "setLSBDepth failed")
                    assertEquals(0, encoder.setPredictionDisabled(predDisabled), "setPredictionDisabled failed")
                    assertEquals(0, encoder.setDTX(dtx), "setDTX failed")
                    assertEquals(0, encoder.setExpertFrameDuration(frameSizeEnum), "setExpertFrameDuration failed")

                    if (!testEncode(encoder, decoder, numChannels, frameSize, inBuf)) {
                        fail(
                            "fuzz_encoder_settings failed: ${sampleRate / 1000} kHz, $numChannels ch, " +
                            "app: $application, $bitrate bps, force ch: $forceChannel, vbr: $vbr, " +
                            "vbr constraint: $vbrConstraint, complexity: $complexity, max bw: $maxBw, " +
                            "signal: $sig, inband fec: $inbandFec, pkt loss: $pktLoss%, lsb depth: $lsbDepth, " +
                            "pred disabled: $predDisabled, dtx: $dtx, (${frameSizeMsX2}/2) ms"
                        )
                    }
                }

                println("[Encoder $i] OK - tested $numSettingChanges setting combinations at $sampleRate Hz, $numChannels ch")

            } finally {
                encoder.close()
                decoder.close()
            }
        }
    }

    /**
     * Quick fuzzing test with a small number of iterations.
     * Good for CI/quick validation.
     */
    @Test
    fun testFuzzEncoderSettingsQuick() {
        initSeed(91829182u)
        fuzzEncoderSettings(numEncoders = 3, numSettingChanges = 10)
        println("Quick fuzz test passed (3 encoders x 10 settings)")
    }

    /**
     * Medium fuzzing test matching the default in the original C test.
     * Default: 5 encoders, 40 setting changes each = 200 configurations.
     */
    @Test
    fun testFuzzEncoderSettingsDefault() {
        initSeed(12345678u)
        fuzzEncoderSettings(numEncoders = 5, numSettingChanges = 40)
        println("Default fuzz test passed (5 encoders x 40 settings = 200 configurations)")
    }

    /**
     * Test specific encoder settings that are commonly used.
     */
    @Test
    fun testCommonEncoderSettings() {
        initSeed(55555u)

        data class TestConfig(
            val name: String,
            val sampleRate: Int,
            val channels: Int,
            val application: OpusApplication,
            val bitrate: Int,
            val complexity: Int,
            val vbr: Boolean,
            val frameSize: Int
        )

        val configs = listOf(
            TestConfig("VoIP 16kHz mono", 16000, 1, OpusApplication.Voip, 16000, 5, true, 320),
            TestConfig("Music 48kHz stereo", 48000, 2, OpusApplication.Audio, 128000, 10, true, 960),
            TestConfig("Gaming 48kHz stereo", 48000, 2, OpusApplication.RestrictedLowDelay, 64000, 8, false, 480),
            TestConfig("Voice memo 24kHz mono", 24000, 1, OpusApplication.Voip, 24000, 5, true, 480),
            TestConfig("HQ Music 48kHz stereo", 48000, 2, OpusApplication.Audio, 256000, 10, true, 960),
        )

        for (config in configs) {
            val encoder = OpusEncoder(config.sampleRate, config.channels, config.application)
            val decoder = OpusDecoder(config.sampleRate, config.channels)

            try {
                encoder.setBitrate(config.bitrate)
                encoder.setComplexity(config.complexity)
                encoder.setVBR(config.vbr)

                val numSamples = config.sampleRate
                val inBuf = ShortArray(numSamples * config.channels)
                generateMusicWithChannels(inBuf, numSamples, config.channels)

                assertTrue(
                    testEncode(encoder, decoder, config.channels, config.frameSize, inBuf),
                    "Failed for config: ${config.name}"
                )

                println("[${config.name}] OK")

            } finally {
                encoder.close()
                decoder.close()
            }
        }
    }

    /**
     * Test all valid frame sizes for each sample rate.
     */
    @Test
    fun testAllFrameSizes() {
        initSeed(77777u)

        val sampleRates = listOf(8000, 12000, 16000, 24000, 48000)
        val frameSizesMsX2 = listOf(5, 10, 20, 40, 80, 120, 160, 200, 240)

        for (sampleRate in sampleRates) {
            val encoder = OpusEncoder(sampleRate, 1, OpusApplication.Audio)
            val decoder = OpusDecoder(sampleRate, 1)

            try {
                val numSamples = sampleRate * 2
                val inBuf = ShortArray(numSamples)
                generateMusicWithChannels(inBuf, numSamples, 1)

                for (frameSizeMsX2 in frameSizesMsX2) {
                    val frameSize = frameSizeMsX2 * sampleRate / 2000
                    val frameSizeEnum = getFrameSizeEnum(frameSize, sampleRate)

                    encoder.setExpertFrameDuration(frameSizeEnum)

                    assertTrue(
                        testEncode(encoder, decoder, 1, frameSize, inBuf),
                        "Failed for $sampleRate Hz, frame size ${frameSizeMsX2 / 2.0} ms"
                    )
                }

                println("[$sampleRate Hz] All frame sizes OK")

            } finally {
                encoder.close()
                decoder.close()
            }
        }
    }

    /**
     * Test all complexity levels.
     */
    @Test
    fun testAllComplexityLevels() {
        initSeed(88888u)

        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)
        val decoder = OpusDecoder(48000, 2)

        try {
            val numSamples = 48000
            val inBuf = ShortArray(numSamples * 2)
            generateMusicWithChannels(inBuf, numSamples, 2)

            for (complexity in 0..10) {
                encoder.setComplexity(complexity)

                assertTrue(
                    testEncode(encoder, decoder, 2, 960, inBuf),
                    "Failed for complexity $complexity"
                )
            }

            println("All complexity levels (0-10) OK")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Test all bandwidth settings.
     */
    @Test
    fun testAllBandwidths() {
        initSeed(99999u)

        val bandwidths = listOf(
            OPUS_BANDWIDTH_NARROWBAND to "Narrowband",
            OPUS_BANDWIDTH_MEDIUMBAND to "Mediumband",
            OPUS_BANDWIDTH_WIDEBAND to "Wideband",
            OPUS_BANDWIDTH_SUPERWIDEBAND to "Superwideband",
            OPUS_BANDWIDTH_FULLBAND to "Fullband"
        )

        val encoder = OpusEncoder(48000, 2, OpusApplication.Audio)
        val decoder = OpusDecoder(48000, 2)

        try {
            val numSamples = 48000
            val inBuf = ShortArray(numSamples * 2)
            generateMusicWithChannels(inBuf, numSamples, 2)

            for ((bandwidth, name) in bandwidths) {
                encoder.setMaxBandwidth(bandwidth)

                assertTrue(
                    testEncode(encoder, decoder, 2, 960, inBuf),
                    "Failed for bandwidth $name"
                )

                println("[$name] OK")
            }

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    // ==================== Multistream Encode/Decode Tests ====================

    /**
     * Tests basic stereo encode/decode with explicit mapping.
     * Uses 1 coupled stream for stereo.
     */
    @Test
    fun testMultistreamStereoEncodeDecode() {
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
            val numSamples = sampleRate
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount < numSamples - frameSize && framesEncoded < 10) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "Multistream encode failed: $len")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "Decoded sample count mismatch")
                verifyFinalRange(encoder, decoder, "Stereo Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[Multistream Stereo] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests quad (4-channel) encode/decode.
     * Uses 2 coupled streams for quad audio.
     */
    @Test
    fun testMultistreamQuadChannelEncodeDecode() {
        val sampleRate = 48000
        val channels = 4
        val streams = 2
        val coupledStreams = 2
        val mapping = byteArrayOf(0, 1, 2, 3)
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
            val numSamples = sampleRate / 2
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount < numSamples - frameSize && framesEncoded < 5) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "Quad encode failed: $len")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "Decoded sample count mismatch")
                verifyFinalRange(encoder, decoder, "Quad Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[Multistream Quad] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests 5.1 surround via createSurround factory method.
     * Uses Vorbis channel order (mapping family 1).
     */
    @Test
    fun testMultistreamSurroundEncodeDecode51() {
        val sampleRate = 48000
        val channels = 6
        val mappingFamily = 1
        val frameSize = 960

        val surroundResult = OpusMultistreamEncoder.createSurround(
            sampleRate = sampleRate,
            channels = channels,
            mappingFamily = mappingFamily,
            application = OpusApplication.Audio
        )

        val encoder = surroundResult.encoder
        val decoder = OpusMultistreamDecoder(
            sampleRate = sampleRate,
            channels = channels,
            streams = surroundResult.streams,
            coupledStreams = surroundResult.coupledStreams,
            mapping = surroundResult.mapping
        )

        try {
            println("[5.1 Surround] streams=${surroundResult.streams}, coupledStreams=${surroundResult.coupledStreams}")
            println("[5.1 Surround] mapping=${surroundResult.mapping.map { it.toInt() }}")

            val numSamples = sampleRate / 2
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount < numSamples - frameSize && framesEncoded < 5) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "5.1 encode failed: $len")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "Decoded sample count mismatch")
                verifyFinalRange(encoder, decoder, "5.1 Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[5.1 Surround] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests float encode/decode for multistream.
     */
    @Test
    fun testMultistreamFloatEncodeDecode() {
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
            val numSamples = sampleRate / 2
            val inBuf = FloatArray(numSamples * channels)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val sample = kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * t).toFloat() * 0.5f
                inBuf[i * channels] = sample
                inBuf[i * channels + 1] = sample * 0.8f
            }

            val packet = ByteArray(MAX_PACKET)
            val outBuf = FloatArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount < numSamples - frameSize && framesEncoded < 5) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "Float encode failed: $len")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "Decoded sample count mismatch")
                verifyFinalRange(encoder, decoder, "Float Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[Float Multistream] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests PLC (Packet Loss Concealment) for multistream decoder.
     */
    @Test
    fun testMultistreamPLC() {
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
            val numSamples = sampleRate / 2
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(frameSize * channels)

            val len = encoder.encode(
                inPcm = inBuf,
                inPcmOffset = 0,
                frameSize = frameSize,
                outData = packet,
                outDataOffset = 0,
                maxDataBytes = MAX_PACKET
            )
            assertTrue(len > 0, "Encode failed: $len")

            val outSamples1 = decoder.decode(
                inData = packet,
                inDataOffset = 0,
                len = len,
                outPcm = outBuf,
                outPcmOffset = 0,
                frameSize = frameSize,
                decodeFec = false
            )
            assertEquals(frameSize, outSamples1, "First decode failed")

            val plcSamples = decoder.decode(
                inData = null,
                inDataOffset = 0,
                len = 0,
                outPcm = outBuf,
                outPcmOffset = 0,
                frameSize = frameSize,
                decodeFec = false
            )
            assertEquals(frameSize, plcSamples, "PLC failed - expected $frameSize samples, got $plcSamples")

            println("[Multistream PLC] OK - PLC produced $plcSamples samples")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests discrete channels (mapping family 255) for many channels.
     * Each channel is its own independent mono stream.
     */
    @Test
    fun testMultistreamDiscreteChannels() {
        val sampleRate = 48000
        val channels = 4
        val mappingFamily = 255
        val frameSize = 960

        val surroundResult = OpusMultistreamEncoder.createSurround(
            sampleRate = sampleRate,
            channels = channels,
            mappingFamily = mappingFamily,
            application = OpusApplication.Audio
        )

        val encoder = surroundResult.encoder
        val decoder = OpusMultistreamDecoder(
            sampleRate = sampleRate,
            channels = channels,
            streams = surroundResult.streams,
            coupledStreams = surroundResult.coupledStreams,
            mapping = surroundResult.mapping
        )

        try {
            assertEquals(0, surroundResult.coupledStreams, "Discrete channels should have 0 coupled streams")
            assertEquals(channels, surroundResult.streams, "Discrete channels should have one stream per channel")

            println("[Discrete] streams=${surroundResult.streams}, coupledStreams=${surroundResult.coupledStreams}")

            val numSamples = sampleRate / 4
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(MAX_FRAME_SAMP * channels)

            var sampCount = 0
            var framesEncoded = 0

            while (sampCount < numSamples - frameSize && framesEncoded < 3) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )

                assertTrue(len > 0, "Discrete encode failed: $len")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = MAX_FRAME_SAMP,
                    decodeFec = false
                )

                assertEquals(frameSize, outSamples, "Decoded sample count mismatch")
                verifyFinalRange(encoder, decoder, "Discrete Frame $framesEncoded")

                sampCount += frameSize
                framesEncoded++
            }

            assertTrue(framesEncoded > 0, "No frames were encoded")
            println("[Discrete] OK - $framesEncoded frames with FINAL_RANGE verification")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests OPUS_RESET_STATE for multistream encoder and decoder.
     * Ported from test_opus_encode.c lines 549-550, 670-674.
     */
    @Test
    fun testMultistreamResetState() {
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
            val numSamples = sampleRate / 2
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(frameSize * channels)

            var sampCount = 0
            repeat(3) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )
                assertTrue(len > 0, "Encode failed before reset")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = false
                )
                assertEquals(frameSize, outSamples, "Decode failed before reset")
                sampCount += frameSize
            }

            val encResetResult = encoder.resetState()
            assertTrue(encResetResult >= 0, "Encoder resetState failed: $encResetResult")

            val decResetResult = decoder.resetState()
            assertTrue(decResetResult >= 0, "Decoder resetState failed: $decResetResult")

            repeat(3) { frameNum ->
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )
                assertTrue(len > 0, "Encode failed after reset at frame $frameNum")

                val outSamples = decoder.decode(
                    inData = packet,
                    inDataOffset = 0,
                    len = len,
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = false
                )
                assertEquals(frameSize, outSamples, "Decode failed after reset at frame $frameNum")

                verifyFinalRange(encoder, decoder, "Post-reset Frame $frameNum")

                sampCount += frameSize
            }

            println("[Multistream Reset State] OK - encoder and decoder reset successfully")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests FEC (Forward Error Correction) decoding with decodeFec=true.
     * Ported from test_opus_encode.c line 590.
     */
    @Test
    fun testMultistreamDecodeWithFEC() {
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
            encoder.setInbandFEC(true)
            encoder.setPacketLossPerc(10)

            val numSamples = sampleRate
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packets = mutableListOf<ByteArray>()
            val packetLengths = mutableListOf<Int>()
            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(frameSize * channels)

            var sampCount = 0
            repeat(10) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )
                assertTrue(len > 0, "Encode failed")
                packets.add(packet.copyOf(len))
                packetLengths.add(len)
                sampCount += frameSize
            }

            for (i in 0 until 3) {
                val outSamples = decoder.decode(
                    inData = packets[i],
                    inDataOffset = 0,
                    len = packetLengths[i],
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = false
                )
                assertEquals(frameSize, outSamples, "Normal decode failed at packet $i")
            }

            val fecSamples = decoder.decode(
                inData = packets[4],
                inDataOffset = 0,
                len = packetLengths[4],
                outPcm = outBuf,
                outPcmOffset = 0,
                frameSize = frameSize,
                decodeFec = true
            )
            assertEquals(frameSize, fecSamples, "FEC decode failed - expected $frameSize samples, got $fecSamples")

            val normalSamples = decoder.decode(
                inData = packets[4],
                inDataOffset = 0,
                len = packetLengths[4],
                outPcm = outBuf,
                outPcmOffset = 0,
                frameSize = frameSize,
                decodeFec = false
            )
            assertEquals(frameSize, normalSamples, "Normal decode after FEC failed")

            initSeed(12345u)
            for (i in 5 until packets.size) {
                val useFec = (fastRand() and 3u) != 0u
                val outSamples = decoder.decode(
                    inData = packets[i],
                    inDataOffset = 0,
                    len = packetLengths[i],
                    outPcm = outBuf,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = useFec
                )
                assertEquals(frameSize, outSamples, "Decode with useFec=$useFec failed at packet $i")
            }

            println("[Multistream FEC Decode] OK - FEC decoding with decodeFec=true works")

        } finally {
            encoder.close()
            decoder.close()
        }
    }

    /**
     * Tests combined packet loss simulation with FEC recovery.
     * Ported from test_opus_encode.c line 590.
     */
    @Test
    fun testMultistreamPacketLossWithFEC() {
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
            encoder.setInbandFEC(true)
            encoder.setPacketLossPerc(25)

            val numSamples = sampleRate
            val inBuf = ShortArray(numSamples * channels)
            generateMultichannelAudio(inBuf, numSamples, channels)

            val packet = ByteArray(MAX_PACKET)
            val outBuf = ShortArray(frameSize * channels)

            initSeed(99999u)
            var sampCount = 0
            var framesProcessed = 0
            var lossCount = 0

            while (sampCount < numSamples - frameSize && framesProcessed < 20) {
                val len = encoder.encode(
                    inPcm = inBuf,
                    inPcmOffset = sampCount * channels,
                    frameSize = frameSize,
                    outData = packet,
                    outDataOffset = 0,
                    maxDataBytes = MAX_PACKET
                )
                assertTrue(len > 0, "Encode failed at frame $framesProcessed")

                val loss = (fastRand() and 63u) == 0u
                val useFec = (fastRand() and 3u) != 0u

                val outSamples = if (loss) {
                    lossCount++
                    decoder.decode(
                        inData = packet,
                        inDataOffset = 0,
                        len = 0,
                        outPcm = outBuf,
                        outPcmOffset = 0,
                        frameSize = frameSize,
                        decodeFec = useFec
                    )
                } else {
                    decoder.decode(
                        inData = packet,
                        inDataOffset = 0,
                        len = len,
                        outPcm = outBuf,
                        outPcmOffset = 0,
                        frameSize = frameSize,
                        decodeFec = useFec
                    )
                }

                assertEquals(frameSize, outSamples, "Decode failed at frame $framesProcessed (loss=$loss, fec=$useFec)")

                sampCount += frameSize
                framesProcessed++
            }

            println("[Multistream Packet Loss + FEC] OK - processed $framesProcessed frames, $lossCount simulated losses")

        } finally {
            encoder.close()
            decoder.close()
        }
    }
}
