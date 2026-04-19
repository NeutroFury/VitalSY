package com.jonesys.vitalsy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberLime,
    onPrimary = DeepBlack,
    secondary = ClinicalGreen,
    onSecondary = PureWhite,
    background = DeepBlack,
    onBackground = PureWhite,
    surface = SoftDarkGris,
    onSurface = PureWhite,
    error = ColorBotonRosa,
    onError = PureWhite
)

@Composable
fun VitalSYTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}