package com.cleaningos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleaningos.presentation.theme.*

/**
 * OceanTopBar — App header with Dark Ocean + Cyan glow aesthetic.
 * Matches the GradHeaderWidget from the Kivy prototype.
 */
@Composable
fun OceanTopBar(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(FrostedGlass, FrostedGlassDark)
                )
            )
    ) {
        // Cyan glow orb — top-right decorative element
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlowCyan20, GlowCyan10, androidx.compose.ui.graphics.Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = CyanMint,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            actions()
        }

        // Bottom cyan glow border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.BottomCenter)
                .background(GlowCyan35)
        )
    }
}
