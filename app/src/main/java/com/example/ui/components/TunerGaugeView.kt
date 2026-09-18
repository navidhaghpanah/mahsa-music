package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.DetectedNote
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TunerGaugeView(
    detectedNote: DetectedNote?,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val cents = detectedNote?.cents ?: 0f
    val isInTune = detectedNote?.isInTune ?: false

    val animatedCents by animateFloatAsState(
        targetValue = if (isListening) cents else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tunerNeedle"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tuner_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dial Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 130.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.size(240.dp, 130.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h)
                    val radius = w * 0.42f

                    // Background Arc (180 degrees from -180 to 0)
                    drawArc(
                        color = Color(0x3394A3B8),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // In-tune Sweet Spot (Center -10 to +10 degrees)
                    // 180 to 360 mapped from -50 cents to +50 cents -> Center is 270 deg
                    // +/- 10 cents = +/- 18 degrees
                    drawArc(
                        color = Color(0xFF22C55E).copy(alpha = 0.7f),
                        startAngle = 270f - 18f,
                        sweepAngle = 36f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Tick lines
                    for (c in -50..50 step 10) {
                        val angleDeg = 270f + (c / 50f) * 80f
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val tickLen = if (c == 0) 16.dp.toPx() else if (c % 25 == 0) 12.dp.toPx() else 8.dp.toPx()
                        val tickColor = if (c == 0) Color(0xFF22C55E) else Color(0x66CBD5E1)

                        val startX = (center.x + (radius - tickLen) * cos(angleRad)).toFloat()
                        val startY = (center.y + (radius - tickLen) * sin(angleRad)).toFloat()
                        val endX = (center.x + (radius + 2.dp.toPx()) * cos(angleRad)).toFloat()
                        val endY = (center.y + (radius + 2.dp.toPx()) * sin(angleRad)).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (c == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Needle
                    if (isListening && detectedNote != null && detectedNote.frequency > 40f) {
                        val needleAngle = 270f + (animatedCents / 50f) * 80f
                        val needleRad = Math.toRadians(needleAngle.toDouble())
                        val needleLength = radius * 0.95f

                        val needleX = (center.x + needleLength * cos(needleRad)).toFloat()
                        val needleY = (center.y + needleLength * sin(needleRad)).toFloat()

                        val needleColor = if (isInTune) Color(0xFF22C55E) else Color(0xFFEF4444)

                        drawLine(
                            color = needleColor,
                            start = center,
                            end = Offset(needleX, needleY),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Pivot Center Dot
                    drawCircle(
                        color = if (isInTune) Color(0xFF22C55E) else primaryColor,
                        radius = 7.dp.toPx(),
                        center = center
                    )
                }

                // Cents labels below arc
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-50", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("0", color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("+50", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Note Name Badge
            val noteBadgeColor = when {
                !isListening -> MaterialTheme.colorScheme.surface
                isInTune -> Color(0xFF15803D)
                cents < 0 -> Color(0xFF9A3412)
                else -> Color(0xFF991B1B)
            }

            Box(
                modifier = Modifier
                    .background(noteBadgeColor, RoundedCornerShape(16.dp))
                    .border(2.dp, if (isInTune) Color(0xFF86EFAC) else Color.Transparent, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isListening && detectedNote != null && detectedNote.frequency > 40f) {
                            detectedNote.noteNameFa
                        } else if (isListening) {
                            "در حال شنیدن..."
                        } else {
                            "میکروفن غیرفعال است"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isListening && detectedNote != null && detectedNote.frequency > 40f) {
                        Text(
                            text = detectedNote.noteNameEn,
                            color = Color(0xFFFEF08A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Frequency and Status row
            if (isListening && detectedNote != null && detectedNote.frequency > 40f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.1f Hz", detectedNote.frequency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "•",
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    val statusText = when {
                        isInTune -> "کوک دقیق (ژوست)"
                        cents < 0 -> "بم است (${String.format("%.0f", cents)} سنت)"
                        else -> "زیر است (+${String.format("%.0f", cents)} سنت)"
                    }
                    val statusColor = if (isInTune) Color(0xFF16A34A) else Color(0xFFDC2626)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // Matched Santoor string info
                detectedNote.matchedSantoorString?.let { str ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "خرک ${str.kharak} (${str.register.labelFa})",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isListening) {
                Text(
                    text = "سیم سنتور را بنوازید تا نت و خرک تشخیص داده شود",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
