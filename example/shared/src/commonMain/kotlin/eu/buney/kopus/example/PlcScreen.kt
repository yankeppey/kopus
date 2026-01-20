package eu.buney.kopus.example

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import eu.buney.kopus.Opus
import eu.buney.kopus.OpusApplication
import eu.buney.kopus.OpusDRED
import eu.buney.kopus.OpusDREDDecoder
import eu.buney.kopus.OpusDecoder
import eu.buney.kopus.OpusEncoder
import eu.buney.kopus.encode
import eu.buney.kopus.isDredAvailable
import eu.buney.kopus.setDREDDuration
import eu.buney.kopus.setPacketLossPerc
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import kotlin.math.PI
import kotlin.math.sin

private const val NUM_FRAMES = 5
private val LOST_FRAME_INDICES = setOf(2) // Frame 3 (0-indexed) - test with fewer frames

data class PlcDemoData(
    val originalSignal: List<Double>,
    val decodedWithStandardPLC: List<Double>,
    val decodedWithDredPLC: List<Double>?,
    val frameBoundaries: List<Int>,
    val frameSize: Int,
)

private fun generatePlcDemoData(sampleRate: Int, frameSize: Int): PlcDemoData {
    val totalSamples = frameSize * NUM_FRAMES

    // Generate 5 frames of a chirp signal (frequency sweep from 200Hz to 2000Hz)
    // Chirps are hard for PLC to extrapolate since frequency constantly changes
    val startFreq = 200.0
    val endFreq = 2000.0
    val duration = totalSamples.toDouble() / sampleRate
    val originalPcm = ShortArray(totalSamples) { idx ->
        val t = idx.toDouble() / sampleRate
        // Phase integral of linear frequency sweep from startFreq to endFreq
        val phase = 2.0 * PI * (startFreq * t + (endFreq - startFreq) * t * t / (2.0 * duration))
        (sin(phase) * Short.MAX_VALUE * 0.8).toInt().toShort()
    }

    // Split into frames
    val frames = (0 until NUM_FRAMES).map { frameIdx ->
        originalPcm.copyOfRange(frameIdx * frameSize, (frameIdx + 1) * frameSize)
    }

    // Encode all frames (with DRED enabled if available)
    val encodedPackets = mutableListOf<ByteArray>()
    OpusEncoder(sampleRate, application = OpusApplication.Voip).use { encoder ->
        // IMPORTANT: Must set expected packet loss > 0 for DRED to allocate bits for redundancy
        encoder.setPacketLossPerc(10) // Tell encoder to expect 10% packet loss

        if (Opus.isDredAvailable) {
            val setResult = encoder.setDREDDuration(100) // 100 frames = 1 second of redundancy
            println("DRED enabled on encoder: setDREDDuration(100) returned $setResult")
        } else {
            println("DRED not available")
        }
        frames.forEach { frame ->
            val encoded = encoder.encode(frame)
            println("Encoded frame: ${encoded.size} bytes")
            encodedPackets.add(encoded)
        }
    }

    // Decode with standard PLC for lost frames
    val decodedStandard = ShortArray(totalSamples)
    OpusDecoder(sampleRate).use { decoder ->
        for (i in 0 until NUM_FRAMES) {
            val offset = i * frameSize
            if (i in LOST_FRAME_INDICES) {
                // Frame is "lost" - use standard PLC
                decoder.decode(outPcm = decodedStandard, outPcmOffset = offset, frameSize = frameSize)
            } else {
                decoder.decode(
                    encodedPackets[i], 0, encodedPackets[i].size,
                    decodedStandard, offset, frameSize, false
                )
            }
        }
    }

    // Decode with DRED PLC for the lost frames (if DRED available)
    // Find the first packet after all lost frames to get DRED data
    val firstPacketAfterLoss = (LOST_FRAME_INDICES.maxOrNull() ?: 0) + 1
    val decodedDred: List<Double>? = if (Opus.isDredAvailable) {
        val decoded = ShortArray(totalSamples)
        OpusDecoder(sampleRate).use { decoder ->
            OpusDREDDecoder().use { dredDecoder ->
                OpusDRED().use { dred ->
                    for (i in 0 until NUM_FRAMES) {
                        val offset = i * frameSize
                        if (i in LOST_FRAME_INDICES) {
                            // Frame is "lost" - use DRED from the first packet after all losses
                            if (firstPacketAfterLoss < encodedPackets.size) {
                                val parseResult = dredDecoder.parse(
                                    dred = dred,
                                    data = encodedPackets[firstPacketAfterLoss],
                                    maxDredSamples = frameSize * LOST_FRAME_INDICES.size,
                                    samplingRate = sampleRate
                                )
                                println("DRED parse for frame $i: offset=${parseResult.offset}, dredEnd=${parseResult.dredEnd}")
                                if (parseResult.offset > 0) {
                                    decoder.decodeDred(
                                        dred = dred,
                                        dredOffset = parseResult.offset,
                                        outPcm = decoded,
                                        outPcmOffset = offset,
                                        frameSize = frameSize
                                    )
                                } else {
                                    // No DRED data, fall back to standard PLC
                                    decoder.decode(outPcm = decoded, outPcmOffset = offset, frameSize = frameSize)
                                }
                            } else {
                                decoder.decode(outPcm = decoded, outPcmOffset = offset, frameSize = frameSize)
                            }
                        } else {
                            decoder.decode(
                                encodedPackets[i], 0, encodedPackets[i].size,
                                decoded, offset, frameSize, false
                            )
                        }
                    }
                }
            }
        }
        decoded.map { it.toDouble() }
    } else {
        null
    }

    return PlcDemoData(
        originalSignal = originalPcm.map { it.toDouble() },
        decodedWithStandardPLC = decodedStandard.map { it.toDouble() },
        decodedWithDredPLC = decodedDred,
        frameBoundaries = (1 until NUM_FRAMES).map { it * frameSize },
        frameSize = frameSize
    )
}

@Composable
fun PlcScreen() {
    val sampleRate = remember { 48_000 }
    val frameSize = remember { 960 }

    val plcData = remember {
        generatePlcDemoData(sampleRate, frameSize)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Title and description
        Text(
            text = "Packet Loss Concealment Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Frame 3 is simulated as 'lost'. The decoder reconstructs it using PLC.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Top half: Original signal
        Text(
            text = "Original Signal (5 frames)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ChartWithFrameBoundaries(
            lines = listOf(
                Line(
                    label = "Original",
                    values = plcData.originalSignal,
                    color = SolidColor(Color(0xFF23af92)),
                    firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .3f),
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(1500, easing = EaseInOutCubic),
                    gradientAnimationDelay = 500,
                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                )
            ),
            frameBoundaries = plcData.frameBoundaries,
            totalSamples = plcData.originalSignal.size,
            lostFrameIndices = LOST_FRAME_INDICES,
            frameSize = plcData.frameSize,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom half: Decoded with PLC
        Text(
            text = "Decoded Signal (Frame 3 reconstructed)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ChartWithFrameBoundaries(
            lines = buildList {
                add(
                    Line(
                        label = "Standard PLC",
                        values = plcData.decodedWithStandardPLC,
                        color = SolidColor(Color(0xFFf0a500)),
                        firstGradientFillColor = Color(0xFFf0a500).copy(alpha = .2f),
                        secondGradientFillColor = Color.Transparent,
                        strokeAnimationSpec = tween(1500, easing = EaseInOutCubic),
                        gradientAnimationDelay = 500,
                        drawStyle = DrawStyle.Stroke(width = 2.dp),
                    )
                )
                plcData.decodedWithDredPLC?.let { dredData ->
                    add(
                        Line(
                            label = "DRED PLC",
                            values = dredData,
                            color = SolidColor(Color(0xFF4CAF50)),
                            firstGradientFillColor = Color.Transparent,
                            secondGradientFillColor = Color.Transparent,
                            strokeAnimationSpec = tween(1500, delayMillis = 300, easing = EaseInOutCubic),
                            gradientAnimationDelay = 500,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                        )
                    )
                }
            },
            frameBoundaries = plcData.frameBoundaries,
            totalSamples = plcData.decodedWithStandardPLC.size,
            lostFrameIndices = LOST_FRAME_INDICES,
            frameSize = plcData.frameSize,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFFf0a500), label = "Standard PLC")
            if (plcData.decodedWithDredPLC != null) {
                LegendItem(color = Color(0xFF4CAF50), label = "DRED PLC")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // DRED availability indicator
        if (Opus.isDredAvailable) {
            Text(
                text = "DRED available (kopus-full)",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                text = "DRED not available (requires kopus-full)",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ChartWithFrameBoundaries(
    lines: List<Line>,
    frameBoundaries: List<Int>,
    totalSamples: Int,
    lostFrameIndices: Set<Int>,
    frameSize: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // The chart itself
        LineChart(
            data = lines,
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
            animationMode = AnimationMode.Together(delayBuilder = { it * 200L }),
        )

        // Custom overlay for frame boundaries and lost frame highlight
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            val chartWidth = size.width
            val chartHeight = size.height

            // Highlight all lost frame areas with semi-transparent red
            lostFrameIndices.forEach { lostFrameIndex ->
                val lostFrameStart = lostFrameIndex * frameSize
                val lostFrameEnd = (lostFrameIndex + 1) * frameSize
                val x1 = (lostFrameStart.toFloat() / totalSamples) * chartWidth
                val x2 = (lostFrameEnd.toFloat() / totalSamples) * chartWidth
                drawRect(
                    color = Color.Red.copy(alpha = 0.08f),
                    topLeft = Offset(x1, 0f),
                    size = Size(x2 - x1, chartHeight)
                )
            }

            // Draw vertical dashed lines at frame boundaries
            frameBoundaries.forEach { sampleIndex ->
                val x = (sampleIndex.toFloat() / totalSamples) * chartWidth
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, chartHeight),
                    strokeWidth = 1.5f,
                    pathEffect = pathEffect
                )
            }
        }
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
