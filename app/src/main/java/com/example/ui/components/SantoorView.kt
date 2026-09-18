package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.audio.DastgahTuning
import com.example.audio.SantoorRegister
import com.example.audio.SantoorString

@Composable
fun SantoorDiagramView(
    activeTuning: DastgahTuning,
    highlightedString: SantoorString?,
    onStringSelected: (SantoorString) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("santoor_diagram_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header / Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دیاگرام خرک‌های سنتور (۹ خرک)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeTuning.nameFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Registers Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendChip(color = Color(0xFFF59E0B), label = "سیم‌های زرد (بم)")
                LegendChip(color = Color(0xFF38BDF8), label = "سیم‌های سفید (میانی)")
                LegendChip(color = Color(0xFFA855F7), label = "پشت خرک (زیر)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Santoor Trapezoid Body Representation
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFF2E190E) // Rich Walnut Tone
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Column labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پشت خرک",
                            color = Color(0xFFA855F7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "سفید",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "خرک",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "زرد",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // 9 Kharaks (From top Kharak 9 to bottom Kharak 1)
                    for (kharakIndex in 9 downTo 1) {
                        val posht = activeTuning.strings.find {
                            it.kharak == kharakIndex && it.register == SantoorRegister.POSHT_E_KHARAK
                        }
                        val sefid = activeTuning.strings.find {
                            it.kharak == kharakIndex && it.register == SantoorRegister.SEFID
                        }
                        val zard = activeTuning.strings.find {
                            it.kharak == kharakIndex && it.register == SantoorRegister.ZARD
                        }

                        KharakRow(
                            kharakNum = kharakIndex,
                            zard = zard,
                            sefid = sefid,
                            posht = posht,
                            highlighted = highlightedString,
                            onSelect = onStringSelected
                        )

                        if (kharakIndex > 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KharakRow(
    kharakNum: Int,
    zard: SantoorString?,
    sefid: SantoorString?,
    posht: SantoorString?,
    highlighted: SantoorString?,
    onSelect: (SantoorString) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C110A).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(vertical = 3.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Posht-e Kharak note
        StringCourseButton(
            santoorString = posht,
            isHighlighted = highlighted != null && posht != null &&
                    highlighted.kharak == posht.kharak && highlighted.register == posht.register,
            baseColor = Color(0xFFA855F7),
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Sefid note
        StringCourseButton(
            santoorString = sefid,
            isHighlighted = highlighted != null && sefid != null &&
                    highlighted.kharak == sefid.kharak && highlighted.register == sefid.register,
            baseColor = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Kharak Badge
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(26.dp)
                .background(Color(0xFF451A03), CircleShape)
                .border(1.dp, Color(0xFF78350F), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = kharakNum.toString(),
                color = Color(0xFFFDE68A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Zard note
        StringCourseButton(
            santoorString = zard,
            isHighlighted = highlighted != null && zard != null &&
                    highlighted.kharak == zard.kharak && highlighted.register == zard.register,
            baseColor = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )
    }
}

@Composable
private fun StringCourseButton(
    santoorString: SantoorString?,
    isHighlighted: Boolean,
    baseColor: Color,
    modifier: Modifier = Modifier,
    onSelect: (SantoorString) -> Unit
) {
    if (santoorString == null) {
        Box(modifier = modifier)
        return
    }

    val animatedBg by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFF22C55E) else baseColor.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 200),
        label = "stringBg"
    )

    val animatedBorder by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFF86EFAC) else baseColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "stringBorder"
    )

    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(6.dp))
            .clickable { onSelect(santoorString) }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = santoorString.noteNameFa,
            color = if (isHighlighted) Color.White else baseColor,
            fontSize = 11.sp,
            fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
