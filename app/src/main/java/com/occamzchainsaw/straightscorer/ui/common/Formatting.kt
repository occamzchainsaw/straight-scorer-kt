package com.occamzchainsaw.straightscorer.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

fun formatDuration(millis: Long): String {
    val duration = millis.coerceAtLeast(0).milliseconds
    val hours = duration.inWholeHours
    val minutes = duration.inWholeMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "${duration.inWholeMinutes.coerceAtLeast(1)} min"
}

fun formatAverage(average: Float, locale: Locale): String = String.format(locale, "%.1f", average)

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** e.g. "26 Oct 2026, 19:42" in the user's locale. */
fun formatDateTime(epochMillis: Long, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** e.g. "October 2026". */
fun formatMonth(epochMillis: Long, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("LLLL yyyy", locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))
        .replaceFirstChar { it.titlecase(locale) }
