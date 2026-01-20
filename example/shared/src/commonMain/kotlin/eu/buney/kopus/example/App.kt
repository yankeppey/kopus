@file:OptIn(ExperimentalStdlibApi::class)

package eu.buney.kopus.example

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import eu.buney.kopus.Opus
import eu.buney.kopus.OpusApplication
import eu.buney.kopus.OpusDecoder
import eu.buney.kopus.OpusEncoder
import eu.buney.kopus.encode
import eu.buney.kopus.setBitrate
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.sin

fun reencode(pcm: ShortArray, sampleRate: Int, frameSize: Int, application: OpusApplication): List<Double> {
    val encoded = OpusEncoder(sampleRate, application = application).use {
        it.setBitrate(3000)
        it.encode(pcm)
    }
    return OpusDecoder(sampleRate).use {
        val outPcm = ShortArray(frameSize)
        it.decode(encoded, 0, encoded.size, outPcm, 0, frameSize, false)
        outPcm.map { it.toDouble() }
    }
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Encoding", "PLC", "Surround")

        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .background(Color.White),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> EncodingScreen()
                1 -> PlcScreen()
                2 -> SurroundScreen()
            }
        }
    }
}

@Composable
fun EncodingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val greeting = remember { Opus.getOpusVersion() }
        val sampleRate = remember { 48_000 }
        val frameSize = remember { 960 }
        val pcm = remember {
            ShortArray(frameSize) { idx ->
                (sin(2.0 * PI * 440 * idx / sampleRate) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        val pcmDouble = remember {
            pcm.map { it.toDouble() }
        }
        val reencodedAudio = remember {
            reencode(pcm, sampleRate, frameSize, OpusApplication.Audio)
        }
        val reencodedVoip = remember {
            reencode(pcm, sampleRate, frameSize, OpusApplication.Voip)
        }
        val reencodedLowDelay = remember {
            reencode(pcm, sampleRate, frameSize, OpusApplication.RestrictedLowDelay)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Opus: $greeting")
            LineChart(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
                data = remember {
                    listOf(
                        Line(
                            label = "Original",
                            values = pcmDouble,
                            color = SolidColor(Color(0xFF23af92)),
                            firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                        ),
                        Line(
                            label = "Reencoded (Audio)",
                            values = reencodedAudio,
                            color = SolidColor(Color.Red),
                            firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                        ),
                        Line(
                            label = "Reencoded (VoIP)",
                            values = reencodedVoip,
                            color = SolidColor(Color(0xFFf0a500)),
                            firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                        ),
                        Line(
                            label = "Reencoded (Low Delay)",
                            values = reencodedLowDelay,
                            color = SolidColor(Color(0xFFf05b72)),
                            firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                        ),
                    )
                },
                animationMode = AnimationMode.Together(delayBuilder = {
                    it * 500L
                }),
            )

        }
    }
}
