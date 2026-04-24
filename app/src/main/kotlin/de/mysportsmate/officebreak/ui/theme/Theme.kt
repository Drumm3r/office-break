package de.mysportsmate.officebreak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDarkTheme,
    onPrimary = OnGreenPrimaryDark,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = OnGreenContainerDark,
    secondary = GreenPrimaryDarkTheme,
    onSecondary = OnGreenPrimaryDark,
    secondaryContainer = GreenContainerDark,
    onSecondaryContainer = OnGreenContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = OnGreenContainer,
)

@Composable
fun OfficeBreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
