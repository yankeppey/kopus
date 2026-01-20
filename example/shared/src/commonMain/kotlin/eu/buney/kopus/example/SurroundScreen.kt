package eu.buney.kopus.example

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import eu.buney.kopus.DEFAULT_MAX_BYTES
import eu.buney.kopus.OpusApplication
import eu.buney.kopus.OpusMultistreamDecoder
import eu.buney.kopus.OpusMultistreamEncoder
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import kotlin.math.PI
import kotlin.math.sin

/**
 * Channel configuration for 5.1 surround sound (Vorbis channel order).
 */
private data class ChannelConfig(
    val name: String,
    val frequency: Float,
    val color: Color
)

private val CHANNEL_CONFIGS = listOf(
    ChannelConfig("Front Left", 440f, Color(0xFF23af92)),      // Green
    ChannelConfig("Center", 330f, Color(0xFF2196F3)),          // Blue
    ChannelConfig("Front Right", 554f, Color(0xFFf44336)),     // Red
    ChannelConfig("Surround Left", 392f, Color(0xFFf0a500)),   // Orange
    ChannelConfig("Surround Right", 494f, Color(0xFF9C27B0)),  // Purple
    ChannelConfig("LFE", 60f, Color(0xFF757575))               // Gray
)

data class SurroundDemoData(
    val originalChannels: List<List<Double>>,
    val decodedChannels: List<List<Double>>,
    val streams: Int,
    val coupledStreams: Int,
    val encodedSize: Int
)

private fun generateSineWave(frequency: Float, sampleRate: Int, frameSize: Int): ShortArray {
    return ShortArray(frameSize) { idx ->
        (sin(2.0 * PI * frequency * idx / sampleRate) * Short.MAX_VALUE * 0.8).toInt().toShort()
    }
}

private fun interleave(channels: List<ShortArray>): ShortArray {
    val numChannels = channels.size
    val frameSize = channels[0].size
    val result = ShortArray(frameSize * numChannels)
    for (i in 0 until frameSize) {
        for (ch in 0 until numChannels) {
            result[i * numChannels + ch] = channels[ch][i]
        }
    }
    return result
}

private fun deinterleave(pcm: ShortArray, numChannels: Int, frameSize: Int): List<ShortArray> {
    return (0 until numChannels).map { ch ->
        ShortArray(frameSize) { i -> pcm[i * numChannels + ch] }
    }
}

private fun generateSurroundDemoData(sampleRate: Int, frameSize: Int): SurroundDemoData {
    // Generate 6 mono sine waves at different frequencies
    val monoChannels = CHANNEL_CONFIGS.map { config ->
        generateSineWave(config.frequency, sampleRate, frameSize)
    }

    // Interleave into 6-channel PCM
    val surroundPcm = interleave(monoChannels)

    // Create surround encoder (mapping family 1 = Vorbis)
    val result = OpusMultistreamEncoder.createSurround(
        sampleRate = sampleRate,
        channels = 6,
        mappingFamily = 1,
        application = OpusApplication.Audio
    )

    val encoded = ByteArray(DEFAULT_MAX_BYTES)
    val encodedLen: Int
    val decodedChannels: List<ShortArray>

    result.encoder.use { encoder ->
        encodedLen = encoder.encode(surroundPcm, 0, frameSize, encoded, 0, DEFAULT_MAX_BYTES)

        // Decode with matching configuration
        OpusMultistreamDecoder(
            sampleRate = sampleRate,
            channels = 6,
            streams = result.streams,
            coupledStreams = result.coupledStreams,
            mapping = result.mapping
        ).use { decoder ->
            val decoded = ShortArray(frameSize * 6)
            decoder.decode(encoded, 0, encodedLen, decoded, 0, frameSize)
            decodedChannels = deinterleave(decoded, 6, frameSize)
        }
    }

    return SurroundDemoData(
        originalChannels = monoChannels.map { ch -> ch.map { it.toDouble() } },
        decodedChannels = decodedChannels.map { ch -> ch.map { it.toDouble() } },
        streams = result.streams,
        coupledStreams = result.coupledStreams,
        encodedSize = encodedLen
    )
}

@Composable
fun SurroundScreen() {
    val sampleRate = remember { 48_000 }
    val frameSize = remember { 960 }

    val surroundData = remember {
        generateSurroundDemoData(sampleRate, frameSize)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Title and description
        Text(
            text = "5.1 Surround Encoding Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "${surroundData.streams} streams (${surroundData.coupledStreams} coupled, ${surroundData.streams - surroundData.coupledStreams} mono) • ${surroundData.encodedSize} bytes",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 6 channel charts in 2-column grid
        // Row 1: Front Left, Front Right
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelChart(
                channelIndex = 0,
                surroundData = surroundData,
                modifier = Modifier.weight(1f)
            )
            ChannelChart(
                channelIndex = 2,
                surroundData = surroundData,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Surround Left, Surround Right
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelChart(
                channelIndex = 3,
                surroundData = surroundData,
                modifier = Modifier.weight(1f)
            )
            ChannelChart(
                channelIndex = 4,
                surroundData = surroundData,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Center, LFE
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelChart(
                channelIndex = 1,
                surroundData = surroundData,
                modifier = Modifier.weight(1f)
            )
            ChannelChart(
                channelIndex = 5,
                surroundData = surroundData,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFF23af92), label = "Original")
            LegendItem(color = Color.Gray, label = "Decoded")
        }
    }
}

@Composable
private fun ChannelChart(
    channelIndex: Int,
    surroundData: SurroundDemoData,
    modifier: Modifier = Modifier
) {
    val config = CHANNEL_CONFIGS[channelIndex]

    Column(modifier = modifier) {
        Text(
            text = "${config.name} (${config.frequency.toInt()} Hz)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LineChart(
            data = listOf(
                Line(
                    label = "Original",
                    values = surroundData.originalChannels[channelIndex],
                    color = SolidColor(Color(0xFF23af92)),
                    firstGradientFillColor = Color.Transparent,
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(1500, easing = EaseInOutCubic),
                    gradientAnimationDelay = 500,
                    drawStyle = DrawStyle.Stroke(width = 1.5.dp),
                ),
                Line(
                    label = "Decoded",
                    values = surroundData.decodedChannels[channelIndex],
                    color = SolidColor(config.color),
                    firstGradientFillColor = Color.Transparent,
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(1500, delayMillis = 300, easing = EaseInOutCubic),
                    gradientAnimationDelay = 500,
                    drawStyle = DrawStyle.Stroke(width = 1.5.dp),
                )
            ),
            modifier = Modifier.fillMaxSize(),
            animationMode = AnimationMode.Together(delayBuilder = { it * 200L }),
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
