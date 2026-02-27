package com.cleaningos.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleaningos.presentation.theme.*

data class EdiState(val E: Int = 0, val D: Int = 0, val I: Int = 0)

/**
 * EdiBar — Animated Energy/Demand/Intensity indicator.
 * Three liquid pill indicators with animated fill.
 */
@Composable
fun EdiBar(
    edi: EdiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EdiPill("E", edi.E, SemanticSuccess,  Modifier.weight(1f))
        EdiPill("D", edi.D, SemanticError,    Modifier.weight(1f))
        EdiPill("I", edi.I, SemanticPurple,   Modifier.weight(1f))
    }
}

@Composable
private fun EdiPill(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clampedValue = value.coerceIn(0, 5)
    val targetFraction = clampedValue / 5f

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic),
        label = "edi_$label"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(FrostedGlassDark)
            .border(1.dp, GlowCyan20, shape),
        contentAlignment = Alignment.Center
    ) {
        // Animated fill
        if (animatedFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.15f), color.copy(alpha = 0.30f))
                        )
                    )
            )
        }
        // Label
        Text(
            text = "$label=$clampedValue",
            color = if (animatedFraction > 0.1f) color else TextDisabled,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
