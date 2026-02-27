package com.cleaningos.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CleaningTypography = Typography(
    displayLarge  = TextStyle(color = TextPrimary,   fontWeight = FontWeight.Bold,    fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(color = TextPrimary,   fontWeight = FontWeight.Bold,    fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(color = TextPrimary,   fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium= TextStyle(color = TextPrimary,   fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(color = TextPrimary,   fontWeight = FontWeight.Medium,  fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge    = TextStyle(color = TextPrimary,   fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium   = TextStyle(color = TextPrimary,   fontWeight = FontWeight.Medium,  fontSize = 16.sp),
    titleSmall    = TextStyle(color = TextSecondary, fontWeight = FontWeight.Medium,  fontSize = 14.sp),
    bodyLarge     = TextStyle(color = TextPrimary,   fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(color = TextPrimary,   fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(color = TextSecondary, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(color = CyanMint,      fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium   = TextStyle(color = TextSecondary, fontWeight = FontWeight.Medium,  fontSize = 12.sp),
    labelSmall    = TextStyle(color = TextDisabled,  fontWeight = FontWeight.Normal,  fontSize = 11.sp),
)
