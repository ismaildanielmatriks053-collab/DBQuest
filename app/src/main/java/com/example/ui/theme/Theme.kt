package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DBQuestColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF041C24),
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanPrimary,
    secondary = VioletSecondary,
    onSecondary = Color(0xFF160E2E),
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletSecondary,
    tertiary = AmberGold,
    onTertiary = Color(0xFF281700),
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberGold,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = DeepNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedNavyCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = DeepNavySurface,
    surfaceContainer = ElevatedNavyCard,
    surfaceContainerHigh = HighSurfaceNavy,
    outline = BorderSlate,
    outlineVariant = Color(0xFF202A4A),
    error = RoseLoss,
    onError = Color.White,
    errorContainer = RoseContainer,
    onErrorContainer = Color(0xFFFFB4C0)
)

val DBQuestShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DBQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DBQuestColorScheme,
        typography = DBQuestTypography,
        shapes = DBQuestShapes,
        content = content
    )
}
