package br.com.rodsil.lamplight.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Always dark: the app is used at night, and the palette is the brand, so no dynamic color.
private val LamplightColorScheme =
  darkColorScheme(
    primary = Honey,
    onPrimary = HoneyInk,
    primaryContainer = HoneyShade,
    onPrimaryContainer = Parchment,
    secondaryContainer = RoseShade,
    onSecondaryContainer = Parchment,
    tertiaryContainer = OliveShade,
    onTertiaryContainer = Parchment,
    secondary = DustyRose,
    onSecondary = Walnut,
    tertiary = FadedOlive,
    onTertiary = Walnut,
    background = Walnut,
    onBackground = Parchment,
    surface = Walnut,
    onSurface = Parchment,
    surfaceVariant = Driftwood,
    onSurfaceVariant = Suede,
    surfaceContainer = Bark,
    surfaceContainerHigh = Driftwood,
    surfaceContainerHighest = Driftwood,
    outline = Umber,
    outlineVariant = Driftwood,
    error = Terracotta,
    onError = Walnut,
  )

@Composable
fun LamplightTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = LamplightColorScheme, typography = Typography, content = content)
}
