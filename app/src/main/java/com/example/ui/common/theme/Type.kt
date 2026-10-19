package com.example.ui.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun createAppTypography(fontFamily: FontFamily = FontFamily.Default): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.25).sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    )
}

