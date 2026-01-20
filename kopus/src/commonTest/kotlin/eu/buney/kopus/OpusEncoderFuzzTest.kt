/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 *
 * This test is a Kotlin Multiplatform port of fuzz_encoder_settings() from
 * test_opus_encode.c in the native libopus library, originally written by Gregory Maxwell.
 */
package eu.buney.kopus

import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Encoder settings fuzzing tests ported from native libopus test_opus_encode.c
 *
 * These tests verify that the Opus encoder handles all valid parameter combinations
 * correctly by randomly selecting settings and performing encode→decode roundtrips.
 */
class OpusEncoderFuzzTest {

    companion object {
        // Test constants matching the original C test (test_opus_encode.c)
        private const val MAX_PACKET = 1500
        private const val SAMPLES = 48000 * 30       // 30 seconds at 48kHz
        private const val SSAMPLES = SAMPLES / 3
        private const val MAX_FRAME_SAMP = 5760      // 120ms at 48kHz

        // Pseudo-random number generator state (matching original C implementation)
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

    private fun initSeed(seed: UInt) {
        Rz = seed
        Rw = seed
    }

    /**
     * Select a random element from an array.
     */
    private fun <T> randSample(array: Array<T>): T {
        return array[(fastRand() % array.size.toUInt()).toInt()]
    }

    private fun randSample(array: IntArray): Int {
        return array[(fastRand() % array.size.toUInt()).toInt()]
    }

    /**
     * Generate synthetic audio for testing.
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
     *
     * @param numEncoders Number of encoder instances to create and test
     * @param numSettingChanges Number of setting changes per encoder
     */
    private fun fuzzEncoderSettings(numEncoders: Int, numSettingChanges: Int) {
        // Parameters to fuzz - some values are duplicated to increase their probability
        val samplingRates = intArrayOf(8000, 12000, 16000, 24000, 48000)
        val channelOptions = intArrayOf(1, 2)
        val applications = arrayOf(
            OpusApplication.Audio,
            OpusApplication.Voip,
            OpusApplication.RestrictedLowDelay
        )
        val bitrates = intArrayOf(6000, 12000, 16000, 24000, 32000, 48000, 64000, 96000, 510000, OPUS_AUTO, OPUS_BITRATE_MAX)
        val forceChannels = intArrayOf(OPUS_AUTO, OPUS_AUTO, 1, 2)
        val useVbr = intArrayOf(0, 1, 1)
        val vbrConstraints = intArrayOf(0, 1, 1)
        val complexities = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val maxBandwidths = intArrayOf(
            OPUS_BANDWIDTH_NARROWBAND, OPUS_BANDWIDTH_MEDIUMBAND,
            OPUS_BANDWIDTH_WIDEBAND, OPUS_BANDWIDTH_SUPERWIDEBAND,
            OPUS_BANDWIDTH_FULLBAND, OPUS_BANDWIDTH_FULLBAND
        )
        val signals = intArrayOf(OPUS_AUTO, OPUS_AUTO, OPUS_SIGNAL_VOICE, OPUS_SIGNAL_MUSIC)
        val inbandFecs = intArrayOf(0, 0, 1)
        val packetLossPercs = intArrayOf(0, 1, 2, 5)
        val lsbDepths = intArrayOf(8, 24)
        val predictionDisabled = intArrayOf(0, 0, 1)
        val useDtx = intArrayOf(0, 1)
        val frameSizesMsX2 = intArrayOf(5, 10, 20, 40, 80, 120, 160, 200, 240)  // x2 to avoid 2.5 ms

        for (i in 0 until numEncoders) {
            val sampleRate = randSample(samplingRates)
            val numChannels = randSample(channelOptions)
            val application = randSample(applications)

            val encoder = OpusEncoder(sampleRate, numChannels, application)
            val decoder = OpusDecoder(sampleRate, numChannels)

            // Generate test audio
            val numSamples = sampleRate * 2  // 2 seconds of audio
            val inBuf = ShortArray(numSamples * numChannels)
            generateMusic(inBuf, numSamples, numChannels)

            try {
                for (j in 0 until numSettingChanges) {
                    val bitrate = randSample(bitrates)
                    var forceChannel = randSample(forceChannels)
                    val vbr = randSample(useVbr)
                    val vbrConstraint = randSample(vbrConstraints)
                    val complexity = randSample(complexities)
                    val maxBw = randSample(maxBandwidths)
                    val sig = randSample(signals)
                    val inbandFec = randSample(inbandFecs)
                    val pktLoss = randSample(packetLossPercs)
                    val lsbDepth = randSample(lsbDepths)
                    val predDisabled = randSample(predictionDisabled)
                    val dtx = randSample(useDtx)
                    val frameSizeMsX2 = randSample(frameSizesMsX2)
                    val frameSize = frameSizeToSamples(frameSizeMsX2, sampleRate)
                    val frameSizeEnum = getFrameSizeEnum(frameSize, sampleRate)

                    // Force channel can't exceed actual channel count
                    forceChannel = min(forceChannel, numChannels)

                    // Apply all settings
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

                    // Test encode/decode
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
            // VoIP configuration
            TestConfig("VoIP 16kHz mono", 16000, 1, OpusApplication.Voip, 16000, 5, true, 320),
            // Music streaming
            TestConfig("Music 48kHz stereo", 48000, 2, OpusApplication.Audio, 128000, 10, true, 960),
            // Low latency gaming
            TestConfig("Gaming 48kHz stereo", 48000, 2, OpusApplication.RestrictedLowDelay, 64000, 8, false, 480),
            // Voice memo
            TestConfig("Voice memo 24kHz mono", 24000, 1, OpusApplication.Voip, 24000, 5, true, 480),
            // High quality music
            TestConfig("HQ Music 48kHz stereo", 48000, 2, OpusApplication.Audio, 256000, 10, true, 960),
        )

        for (config in configs) {
            val encoder = OpusEncoder(config.sampleRate, config.channels, config.application)
            val decoder = OpusDecoder(config.sampleRate, config.channels)

            try {
                encoder.setBitrate(config.bitrate)
                encoder.setComplexity(config.complexity)
                encoder.setVBR(if (config.vbr) 1 else 0)

                val numSamples = config.sampleRate  // 1 second
                val inBuf = ShortArray(numSamples * config.channels)
                generateMusic(inBuf, numSamples, config.channels)

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
        // Frame sizes in ms: 2.5, 5, 10, 20, 40, 60, 80, 100, 120
        val frameSizesMsX2 = listOf(5, 10, 20, 40, 80, 120, 160, 200, 240)

        for (sampleRate in sampleRates) {
            val encoder = OpusEncoder(sampleRate, 1, OpusApplication.Audio)
            val decoder = OpusDecoder(sampleRate, 1)

            try {
                val numSamples = sampleRate * 2
                val inBuf = ShortArray(numSamples)
                generateMusic(inBuf, numSamples, 1)

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
            generateMusic(inBuf, numSamples, 2)

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
            generateMusic(inBuf, numSamples, 2)

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
}
