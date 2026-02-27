package com.cleaningos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cleaningos.presentation.theme.*

/**
 * GlassCard — Soft Liquid Glassmorphism component.
 *
 * Visual spec:
 *   - Background: semi-transparent FrostedGlass (#1E293B @ ~90%)
 *   - Border: 1.5dp solid CyanMint with glow effect
 *   - Corner radius: 25dp (extreme liquid feel)
 *   - Subtle gradient shimmer overlay
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 25.dp,
    glowColor: Color = CyanMint,
    glowAlpha: Float = 0.28f,
    backgroundColor: Color = FrostedGlass,
    elevation: Dp = 8.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val glowBorder = Color(
        red   = glowColor.red,
        green = glowColor.green,
        blue  = glowColor.blue,
        alpha = glowAlpha
    )

    // Outer glow shadow layer
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = glowColor.copy(alpha = 0.15f),
                spotColor = glowColor.copy(alpha = 0.20f)
            )
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                // Base frosted glass background
                .background(backgroundColor.copy(alpha = 0.92f))
                // Shimmer gradient overlay for glass feel
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardOverlay,
                            Color.Transparent
                        )
                    )
                )
                // Cyan glow border
                .border(
                    width = 1.5.dp,
                    color = glowBorder,
                    shape = shape
                )
                .padding(padding),
            content = content
        )
    }
}

/** Danger variant — red glow for alerts */
@Composable
fun DangerGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(
    modifier = modifier,
    glowColor = SemanticError,
    backgroundColor = Color(0xFF1E0F0F),
    content = content
)

/** Success variant — green glow */
@Composable
fun SuccessGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(
    modifier = modifier,
    glowColor = SemanticSuccess,
    backgroundColor = Color(0xFF0F1E14),
    content = content
)

/** Warning variant — yellow glow */
@Composable
fun WarningGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(
    modifier = modifier,
    glowColor = SemanticWarning,
    backgroundColor = Color(0xFF1E1A0F),
    content = content
)
