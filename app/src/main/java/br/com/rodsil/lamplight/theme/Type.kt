package br.com.rodsil.lamplight.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

// ponytail: platform serif and sans stand in for the hand lettered display font and humanist sans of PRD 6.1; swap in M3 with the final fonts.
private val DefaultTypography = Typography()

val Typography =
  Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = FontFamily.Serif),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = FontFamily.Serif),
  )
