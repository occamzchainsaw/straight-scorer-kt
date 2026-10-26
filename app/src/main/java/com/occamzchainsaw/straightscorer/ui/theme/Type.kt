package com.occamzchainsaw.straightscorer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.occamzchainsaw.straightscorer.R

val Montserrat = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_semibold, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

private val Default = Typography()

private fun TextStyle.montserrat() = copy(fontFamily = Montserrat)

/** M3 type scale with Montserrat in place of Roboto. */
val Typography = Typography(
    displayLarge = Default.displayLarge.montserrat(),
    displayMedium = Default.displayMedium.montserrat(),
    displaySmall = Default.displaySmall.montserrat(),
    headlineLarge = Default.headlineLarge.montserrat(),
    headlineMedium = Default.headlineMedium.montserrat(),
    headlineSmall = Default.headlineSmall.montserrat(),
    titleLarge = Default.titleLarge.montserrat().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.montserrat(),
    titleSmall = Default.titleSmall.montserrat(),
    bodyLarge = Default.bodyLarge.montserrat(),
    bodyMedium = Default.bodyMedium.montserrat(),
    bodySmall = Default.bodySmall.montserrat(),
    labelLarge = Default.labelLarge.montserrat(),
    labelMedium = Default.labelMedium.montserrat(),
    labelSmall = Default.labelSmall.montserrat()
)

/** Tabular figures so changing scores don't shift sideways. */
fun TextStyle.tabularNumbers() = copy(fontFeatureSettings = "tnum")
