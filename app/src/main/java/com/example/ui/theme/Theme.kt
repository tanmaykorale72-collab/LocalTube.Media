package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SageTeal,
    onPrimary = Color(0xFF0F1715),
    primaryContainer = SageTealContainer,
    onPrimaryContainer = OnSageTealContainer,
    secondary = WarmBronze,
    onSecondary = Color(0xFF1F180E),
    secondaryContainer = WarmBronzeContainer,
    onSecondaryContainer = OnWarmBronzeContainer,
    tertiary = ProgressRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A1515),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = CarbonBackground,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHighest = CarbonElevated,
    outline = OutlineDark,
    error = ErrorCoral
)

private val LightColorScheme = lightColorScheme(
    primary = DeepSageTeal,
    onPrimary = Color.White,
    primaryContainer = DeepSageContainer,
    onPrimaryContainer = OnDeepSageContainer,
    secondary = DeepWarmBronze,
    onSecondary = Color.White,
    secondaryContainer = DeepBronzeContainer,
    onSecondaryContainer = OnDeepBronzeContainer,
    tertiary = ProgressRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHighest = LightElevated,
    outline = OutlineLight,
    error = ErrorCoral
)

@Composable
fun CineFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CineFlowTheme(darkTheme = darkTheme, content = content)
}
