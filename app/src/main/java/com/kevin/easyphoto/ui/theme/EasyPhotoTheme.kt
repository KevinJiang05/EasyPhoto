package com.kevin.easyphoto.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EasyPhotoBackground = Color(0xFFF8FAFC)
val EasyPhotoPrimary = Color(0xFF14532D)
val EasyPhotoSecondary = Color(0xFF1D4ED8)
val EasyPhotoText = Color(0xFF111827)
val EasyPhotoDanger = Color(0xFFB91C1C)

private val Colors: ColorScheme = lightColorScheme(
    primary = EasyPhotoPrimary,
    secondary = EasyPhotoSecondary,
    background = EasyPhotoBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = EasyPhotoText,
    onSurface = EasyPhotoText,
    error = EasyPhotoDanger,
)

@Composable
fun EasyPhotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = EasyPhotoTypography,
        content = content,
    )
}
