package pl.marrod.remindershowcase.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


/**
 * Konwersja znacznika czasu (w milisekundach) na czytelny dla człowieka formę daty i czasu.
 * Format wyjściowy to krótki format określony przez ustawienia regionalne i preferencje użytkownika, np. "31.03.2026 14:30".
 */
fun Long.toDisplayDateTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))

/**
 * Konwersja znacznika czasu (w milisekundach) na czytelny dla człowieka format czasu.
 * Format wyjściowy to krótki format określony przez ustawienia regionalne i preferencje użytkownika, np. "14:30".
 */
fun Long.toDisplayTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
