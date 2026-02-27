package com.cleaningos.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleaningos.presentation.theme.*

enum class ButtonStyle {
    Primary, Success, Danger, Warning, Purple, Ghost
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )

    val (bgColor, borderColor, textColor) = when (style) {
        ButtonStyle.Primary -> Triple(CyanMint,        CyanMint.copy(.5f),       OceanDeep)
        ButtonStyle.Success -> Triple(SemanticSuccess, SemanticSuccess.copy(.5f), OceanDeep)
        ButtonStyle.Danger  -> Triple(SemanticError,   SemanticError.copy(.5f),   Color.White)
        ButtonStyle.Warning -> Triple(SemanticWarning, SemanticWarning.copy(.5f), OceanDeep)
        ButtonStyle.Purple  -> Triple(SemanticPurple,  SemanticPurple.copy(.5f),  Color.White)
        ButtonStyle.Ghost   -> Triple(FrostedGlass,    CyanMint.copy(.35f),       TextPrimary)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(52.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(bgColor.copy(alpha = 0.95f), bgColor.copy(alpha = 0.80f))
                )
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(25.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}
