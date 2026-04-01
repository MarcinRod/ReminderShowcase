package pl.marrod.remindershowcase.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.milliseconds

/**
 * Klasa pomocnicza reprezentująca czas do następnego przypomnienia w formacie "wartość + jednostka" (np. "5 m", "2 h", "1 d").
 * Zawiera funkcję statyczną fromTimestamp, która konwertuje czas w milisekundach na odpowiednią wartość
 * i jednostkę czasu, wybierając największą jednostkę, która jest mniejsza lub równa podanemu czasowi (np. jeśli czas
 * to 90 sekund, to zostanie zwrócone "1 m", a nie "90 s").
 */
data class TimeWithUnit(val value: String, val unit: String) {
    companion object {
        fun fromTimestamp(timestamp: Long?): TimeWithUnit {
            timestamp ?: return TimeWithUnit("0", "s")
            // Typ Long posiada właściwość rozszerzającą milliseconds,
            // która pozwala na łatwe konwertowanie wartości w milisekundach
            // na różne jednostki czasu (dni, godziny, minuty, sekundy) przez rozłożenie
            // je na komponenty za pomocą funkcji toComponents.
            return timestamp.milliseconds.toComponents { days, hours, minutes, seconds, _ ->
                when {
                    // jeśli jest więcej niż 0 dni, zwróć wartość w dniach
                    days > 0 -> TimeWithUnit(days.toString(), "d")
                    // jeśli jest więcej niż 0 godzin ale mniej niż 1 dzień, zwróć wartość w godzinach
                    hours > 0 -> TimeWithUnit(hours.toString(), "h")
                    // jeśli jest więcej niż 0 minut ale mniej niż 1 godzina, zwróć wartość w minutach
                    minutes > 0 -> TimeWithUnit(minutes.toString(), "m")
                    // w przeciwnym razie zwróć wartość w sekundach
                    else -> TimeWithUnit(seconds.toString(), "s")
                }
            }
        }
    }
}
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

