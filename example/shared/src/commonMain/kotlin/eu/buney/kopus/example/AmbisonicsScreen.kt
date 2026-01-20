package eu.buney.kopus.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import eu.buney.kopus.DEFAULT_MAX_BYTES
import eu.buney.kopus.OpusApplication
import eu.buney.kopus.OpusProjectionDecoder
import eu.buney.kopus.OpusProjectionEncoder
import androidx.compose.animation.core.snap
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * First-order ambisonics channel configuration.
 * ACN (Ambisonics Channel Number) ordering with SN3D normalization.
 */
private data class AmbisonicsChannelConfig(
    val name: String,
    val description: String,
    val color: Color
)

private val AMBISONICS_CHANNELS = listOf(
    AmbisonicsChannelConfig("W", "Omnidirectional", Color(0xFF2196F3)),      // Blue
    AmbisonicsChannelConfig("Y", "Left-Right (Y axis)", Color(0xFF4CAF50)),  // Green
    AmbisonicsChannelConfig("Z", "Up-Down (Z axis)", Color(0xFF9C27B0)),     // Purple
    AmbisonicsChannelConfig("X", "Front-Back (X axis)", Color(0xFFf44336))   // Red
)

data class AmbisonicsDemoData(
    val originalChannels: List<List<Double>>,
    val decodedChannels: List<List<Double>>,
    val streams: Int,
    val coupledStreams: Int,
    val encodedSize: Int
)

/**
 * Generates first-order ambisonics audio with a sound source at a fixed azimuth angle.
 *
 * First-order ambisonics encoding (ACN/SN3D):
 * - W = signal * 1.0 (omnidirectional)
 * - Y = signal * sin(azimuth) (left-right)
 * - Z = signal * sin(elevation) = 0 (no elevation)
 * - X = signal * cos(azimuth) (front-back)
 *
 * @param frequency The frequency of the sine wave tone
 * @param sampleRate Sample rate in Hz
 * @param frameSize Number of samples per frame
 * @param azimuthDegrees Azimuth angle in degrees (0=front, 90=left, 180=back, 270=right)
 */
private fun generateFixedAngleAmbisonics(
    frequency: Float,
    sampleRate: Int,
    frameSize: Int,
    azimuthDegrees: Float
): List<ShortArray> {
    val w = ShortArray(frameSize)
    val y = ShortArray(frameSize)
    val z = ShortArray(frameSize)
    val x = ShortArray(frameSize)

    // Convert degrees to radians
    val azimuth = azimuthDegrees * PI / 180.0

    // Pre-calculate the directional coefficients
    val cosAzimuth = cos(azimuth)
    val sinAzimuth = sin(azimuth)

    for (i in 0 until frameSize) {
        // Generate base signal (sine wave)
        val signal = sin(2.0 * PI * frequency * i / sampleRate) * Short.MAX_VALUE * 0.7

        // First-order ambisonics encoding (ACN ordering, SN3D normalization)
        // ACN order: 0=W, 1=Y, 2=Z, 3=X
        w[i] = signal.toInt().toShort()                      // W: omnidirectional
        y[i] = (signal * sinAzimuth).toInt().toShort()       // Y: left-right
        z[i] = 0                                              // Z: up-down (no elevation)
        x[i] = (signal * cosAzimuth).toInt().toShort()       // X: front-back
    }

    return listOf(w, y, z, x)  // ACN order
}

/**
 * Interleaves multiple mono channels into a single interleaved buffer.
 */
private fun interleaveChannels(channels: List<ShortArray>): ShortArray {
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

/**
 * Deinterleaves an interleaved buffer into separate mono channels.
 */
private fun deinterleaveChannels(pcm: ShortArray, numChannels: Int, frameSize: Int): List<ShortArray> {
    return (0 until numChannels).map { ch ->
        ShortArray(frameSize) { i -> pcm[i * numChannels + ch] }
    }
}

private fun generateAmbisonicsDemoData(sampleRate: Int, frameSize: Int, azimuthDegrees: Float): AmbisonicsDemoData {
    // Generate first-order ambisonics with source at fixed angle (440 Hz tone)
    val monoChannels = generateFixedAngleAmbisonics(
        frequency = 440f,
        sampleRate = sampleRate,
        frameSize = frameSize,
        azimuthDegrees = azimuthDegrees
    )

    // Interleave into 4-channel PCM (ACN order: W, Y, Z, X)
    val ambisonicsPcm = interleaveChannels(monoChannels)

    // Create ambisonics projection encoder (mapping family 3 = ambisonics with built-in mixing)
    val result = OpusProjectionEncoder.createAmbisonics(
        sampleRate = sampleRate,
        channels = 4,           // First-order ambisonics
        mappingFamily = 3,      // Ambisonics (implementation-specific projection)
        application = OpusApplication.Audio
    )

    val encoded = ByteArray(DEFAULT_MAX_BYTES)
    val encodedLen: Int
    val decodedChannels: List<ShortArray>

    result.encoder.use { encoder ->
        // Encode the ambisonics audio
        encodedLen = encoder.encode(ambisonicsPcm, 0, frameSize, encoded, 0, DEFAULT_MAX_BYTES)

        // Get the demixing matrix for the decoder
        val demixingMatrix = encoder.getDemixingMatrix()

        // Decode with the projection decoder
        OpusProjectionDecoder(
            sampleRate = sampleRate,
            channels = 4,
            streams = result.streams,
            coupledStreams = result.coupledStreams,
            demixingMatrix = demixingMatrix
        ).use { decoder ->
            val decoded = ShortArray(frameSize * 4)
            decoder.decode(encoded, 0, encodedLen, decoded, 0, frameSize)
            decodedChannels = deinterleaveChannels(decoded, 4, frameSize)
        }
    }

    return AmbisonicsDemoData(
        originalChannels = monoChannels.map { ch -> ch.map { it.toDouble() } },
        decodedChannels = decodedChannels.map { ch -> ch.map { it.toDouble() } },
        streams = result.streams,
        coupledStreams = result.coupledStreams,
        encodedSize = encodedLen
    )
}

@Composable
fun AmbisonicsScreen() {
    val sampleRate = remember { 48_000 }
    val frameSize = remember { 960 }

    var azimuthDegrees by remember { mutableFloatStateOf(0f) }

    val ambisonicsData = remember(azimuthDegrees) {
        generateAmbisonicsDemoData(sampleRate, frameSize, azimuthDegrees)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Ambisonics (Spatial Audio) Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Azimuth control row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compass visualization
            AzimuthCompass(
                azimuthDegrees = azimuthDegrees,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Slider and labels
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Azimuth: ${azimuthDegrees.toInt()}° (${getDirectionLabel(azimuthDegrees)})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = azimuthDegrees,
                    onValueChange = { azimuthDegrees = it },
                    valueRange = 0f..359f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "X: ${String.format("%.2f", cos(azimuthDegrees * PI / 180))}  •  Y: ${String.format("%.2f", sin(azimuthDegrees * PI / 180))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Info text
        Text(
            text = "${ambisonicsData.streams} streams • ${ambisonicsData.encodedSize} bytes encoded",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 4 channel charts in 2x2 grid (ACN order: W, Y, Z, X)
        // Row 1: W (omnidirectional), X (front-back)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmbisonicsChannelChart(
                channelIndex = 0,  // W
                ambisonicsData = ambisonicsData,
                modifier = Modifier.weight(1f)
            )
            AmbisonicsChannelChart(
                channelIndex = 3,  // X
                ambisonicsData = ambisonicsData,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Y (left-right), Z (up-down)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmbisonicsChannelChart(
                channelIndex = 1,  // Y
                ambisonicsData = ambisonicsData,
                modifier = Modifier.weight(1f)
            )
            AmbisonicsChannelChart(
                channelIndex = 2,  // Z
                ambisonicsData = ambisonicsData,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AmbisonicsLegendItem(color = Color(0xFF23af92), label = "Original")
            AmbisonicsLegendItem(color = Color.Gray, label = "Decoded")
        }
    }
}

/**
 * Returns a human-readable direction label for the given azimuth angle.
 */
private fun getDirectionLabel(azimuthDegrees: Float): String {
    val normalized = ((azimuthDegrees % 360) + 360) % 360
    return when {
        normalized < 22.5 || normalized >= 337.5 -> "Front"
        normalized < 67.5 -> "Front-Left"
        normalized < 112.5 -> "Left"
        normalized < 157.5 -> "Back-Left"
        normalized < 202.5 -> "Back"
        normalized < 247.5 -> "Back-Right"
        normalized < 292.5 -> "Right"
        else -> "Front-Right"
    }
}

/**
 * A simple compass visualization showing the sound source position.
 */
@Composable
private fun AzimuthCompass(
    azimuthDegrees: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 8.dp.toPx()

        // Draw circle
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw direction labels positions (as small dots)
        val labelRadius = radius + 4.dp.toPx()
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            val rad = -angle * PI.toFloat() / 180f + PI.toFloat() / 2  // Adjust for screen coordinates
            val x = center.x + labelRadius * 0.85f * cos(rad)
            val y = center.y - labelRadius * 0.85f * sin(rad)
            drawCircle(
                color = Color.Gray,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Draw listener at center
        drawCircle(
            color = Color.DarkGray,
            radius = 6.dp.toPx(),
            center = center
        )

        // Draw sound source position
        // Convert azimuth to screen coordinates (0° = top/front, 90° = left)
        val sourceAngle = (-azimuthDegrees + 90) * PI.toFloat() / 180f
        val sourceX = center.x + radius * 0.7f * cos(sourceAngle)
        val sourceY = center.y - radius * 0.7f * sin(sourceAngle)

        drawCircle(
            color = Color(0xFFf44336),  // Red for sound source
            radius = 10.dp.toPx(),
            center = Offset(sourceX, sourceY)
        )

        // Draw line from center to source
        drawLine(
            color = Color(0xFFf44336).copy(alpha = 0.5f),
            start = center,
            end = Offset(sourceX, sourceY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun AmbisonicsChannelChart(
    channelIndex: Int,
    ambisonicsData: AmbisonicsDemoData,
    modifier: Modifier = Modifier
) {
    val config = AMBISONICS_CHANNELS[channelIndex]

    Column(modifier = modifier) {
        Text(
            text = "${config.name}: ${config.description}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LineChart(
            data = listOf(
                Line(
                    label = "Original",
                    values = ambisonicsData.originalChannels[channelIndex],
                    color = SolidColor(Color(0xFF23af92)),
                    firstGradientFillColor = Color.Transparent,
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = snap(),
                    gradientAnimationSpec = snap(),
                    drawStyle = DrawStyle.Stroke(width = 1.5.dp),
                ),
                Line(
                    label = "Decoded",
                    values = ambisonicsData.decodedChannels[channelIndex],
                    color = SolidColor(config.color),
                    firstGradientFillColor = Color.Transparent,
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = snap(),
                    gradientAnimationSpec = snap(),
                    drawStyle = DrawStyle.Stroke(width = 1.5.dp),
                )
            ),
            modifier = Modifier.fillMaxSize(),
            animationDelay = 0,
            animationMode = AnimationMode.Together(delayBuilder = { 0L }),
        )
    }
}

@Composable
private fun AmbisonicsLegendItem(color: Color, label: String) {
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
