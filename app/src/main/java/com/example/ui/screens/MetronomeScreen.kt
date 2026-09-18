package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TempoDetector
import com.example.ui.MahsaMusicViewModel

@Composable
fun MetronomeScreen(
    viewModel: MahsaMusicViewModel,
    modifier: Modifier = Modifier
) {
    val liveTempo by viewModel.liveTempo.collectAsState()
    val isMicActive by viewModel.isMicrophoneActive.collectAsState()

    val metronomeBpm by viewModel.metronomeBpm.collectAsState()
    val metronomeMeter by viewModel.metronomeMeter.collectAsState()
    val isPlaying by viewModel.isMetronomePlaying.collectAsState()
    val currentBeat by viewModel.currentMetronomeBeat.collectAsState()
    val isMuted by viewModel.isMetronomeMuted.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "مهسا موزیک • مترونوم و تشخیص تمپو",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "تشخیص زنده سرعت مضراب سنتور و مترونوم هوشمند",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Microphone Tempo Detector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tempo_detector_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (liveTempo.bpm > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Rhythm icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تشخیص هوشمند تمپوی مضراب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isMicActive) {
                        Text(
                            text = "میکروفن خاموش",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16A34A).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "شنیدن فعال",
                                color = Color(0xFF16A34A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (liveTempo.bpm > 0) {
                            Text(
                                text = "${liveTempo.bpm} BPM",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = liveTempo.categoryFa,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "دقت تشخیص: ${(liveTempo.confidence * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "-- BPM",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isMicActive) "با مضراب سنتور بنوازید تا تمپو محاسبه شود" else "برای تشخیص تمپو، میکروفن را فعال کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Adopt Button
                    if (liveTempo.bpm > 0) {
                        Button(
                            onClick = { viewModel.adoptDetectedTempo() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("adopt_tempo_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تنظیم مترونوم", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Metronome Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("metronome_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Meter selector (2/4, 3/4, 4/4, 6/8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "میزان‌نما (کسر میزان):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 6).forEach { meter ->
                            val isSelected = meter == metronomeMeter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setMetronomeMeter(meter) },
                                label = {
                                    Text(
                                        text = if (meter == 6) "۶/۸ (چهارمضراب)" else "$meter/۴",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beat Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (b in 1..metronomeMeter) {
                        val isCurrent = isPlaying && currentBeat == b
                        val isAccent = b == 1

                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isCurrent && isAccent -> Color(0xFFF59E0B) // Golden accent
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            animationSpec = tween(durationMillis = 80),
                            label = "beatDotColor"
                        )

                        val dotScale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.25f else 1.0f,
                            animationSpec = tween(durationMillis = 80),
                            label = "beatDotScale"
                        )

                        Box(
                            modifier = Modifier
                                .size((18 * dotScale).dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    1.dp,
                                    if (isAccent) Color(0xFFD97706) else Color.Transparent,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big BPM Readout
                Text(
                    text = "$metronomeBpm",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = TempoDetector.getTempoCategory(metronomeBpm).first,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Continuous Slider
                Slider(
                    value = metronomeBpm.toFloat(),
                    onValueChange = { viewModel.setMetronomeBpm(it.toInt()) },
                    valueRange = 40f..240f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bpm_slider")
                )

                // Quick Increment/Decrement Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.setMetronomeBpm(metronomeBpm - 5) },
                        modifier = Modifier.testTag("bpm_minus_5")
                    ) {
                        Text("-۵")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.setMetronomeBpm(metronomeBpm - 1) },
                        modifier = Modifier.testTag("bpm_minus_1")
                    ) {
                        Text("-۱")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.setMetronomeBpm(metronomeBpm + 1) },
                        modifier = Modifier.testTag("bpm_plus_1")
                    ) {
                        Text("+۱")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.setMetronomeBpm(metronomeBpm + 5) },
                        modifier = Modifier.testTag("bpm_plus_5")
                    ) {
                        Text("+۵")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play / Stop and Tool Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tap Tempo Button
                    ElevatedButton(
                        onClick = { viewModel.tapTempo() },
                        modifier = Modifier.testTag("tap_tempo_button")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = "Tap tempo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تَپ تمپو (Tap)")
                    }

                    // Sound Mute Toggle
                    IconButton(
                        onClick = { viewModel.toggleMetronomeMute() },
                        modifier = Modifier.testTag("mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute metronome",
                            tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Play/Stop Primary Button
                    Button(
                        onClick = { viewModel.toggleMetronome() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("metronome_play_stop_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Start"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "توقف" else "پخش")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Iranian Rhythmic Presets
        Text(
            text = "الگوهای سرعتی موسیقی اصیل ایرانی:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        val presets = listOf(
            Triple("سنگین (آوازی)", "۵۸ BPM", 58),
            Triple("روان (پیش‌درآمد)", "۸۸ BPM", 88),
            Triple("چهارمضراب آرام", "۱۰۸ BPM", 108),
            Triple("چهارمضراب روان", "۱۲۶ BPM", 126),
            Triple("رنگ شادمان", "۱۴۴ BPM", 144),
            Triple("چهارمضراب سریع", "۱۶۸ BPM", 168)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (name, label, bpm) ->
                OutlinedButton(
                    onClick = { viewModel.setMetronomeBpm(bpm) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
