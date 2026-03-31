package pl.marrod.remindershowcase.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import pl.marrod.remindershowcase.notifications.ReminderReceiver
import pl.marrod.remindershowcase.utils.toDisplayDateTime
import pl.marrod.remindershowcase.utils.toDisplayTime

/**
 * Klasa reprezentująca pojedyncze przypomnienie, zawierająca wszystkie niezbędne informacje do jego wyświetlenia i obsługi.
 * @param id Unikalny identyfikator przypomnienia, np. UUID.
 * @param title Tytuł przypomnienia, krótki tekst opisujący jego cel.
 * @param description Szczegółowy opis przypomnienia, może zawierać dodatkowe informacje.
 * @param timestamp Czas przypomnienia w formie znacznika czasu (milliseconds since epoch), określający, kiedy przypomnienie ma się pojawić.
 * @param createdAtTimestamp Czas utworzenia przypomnienia, używany do obliczania postępu przypomnienia.
 * @param isNotificationScheduled Flaga wskazująca, czy powiadomienie dla tego przypomnienia zostało już zaplanowane,
 * aby uniknąć wielokrotnego planowania tego samego przypomnienia.
 */
data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    var isNotificationScheduled: Boolean = false
){
    /**
     * Obiekt towarzyszący zawierający stałe i pomocnicze funkcje związane z klasą Reminder.
     */
    companion object {
        /** Enum definiujący klucze używane do przekazywania danych przypomnienia w Intencji z powiadomienia
         */
       enum class Extras{
           ID, TITLE, DESCRIPTION
       }
    }
    /** Właściwości pomocnicze do formatowania czasu przypomnienia do wyświetlenia w UI
     * Przyjęty krótki format zgodny z lokalnymi ustawieniami użytkownika, np. "14:30" lub "2:30 PM"
     */
    val displayTime: String
        get() = timestamp.toDisplayTime()
    /** Formatowanie daty i czasu przypomnienia do wyświetlenia w UI.
     * Przyjęty krótki format zgodny z lokalnymi ustawienia użytkownika, np. 31.03.2026 14:30*/
    val displayDateTime: String
        get() = timestamp.toDisplayDateTime()

    /**
     * Oblicza czas pozostały do przypomnienia w milisekundach, zwracając 0, jeśli przypomnienie już minęło.
     */
    fun calculateTimeUntil(): Long {
        return (timestamp - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Oblicza postęp przypomnienia jako wartość od 0 do 1, gdzie 0 oznacza, że przypomnienie jest
     * dopiero co utworzone, a 1 oznacza, że przypomnienie już minęło.
     */
    fun calculateProgressUntil(): Float {
        val totalDuration = timestamp - createdAtTimestamp
        val elapsedDuration = System.currentTimeMillis() - createdAtTimestamp
        return if (totalDuration > 0) {
            (elapsedDuration.toFloat() / totalDuration).coerceIn(0f, 1f)
        } else {
            1f
        }
    }

    /**
     * Planowanie powiadomienia dla tego przypomnienia za pomocą AlarmManager, jeśli jeszcze nie zostało zaplanowane.
     * @param context Kontekst potrzebny do uzyskania dostępu do systemowych usług (AlarmManager) i tworzenia Intencji.
     */
    @SuppressLint("MissingPermission") // Pozwolenie na użycie AlarmManager bez dodatkowych uprawnień,
    // ponieważ jest to dozwolone dla alarmów typu RTC_WAKEUP w aplikacjach typu reminder
    fun scheduleNotification(context: Context) {
        // Unikamy wielokrotnego planowania tego samego przypomnienia
        if (isNotificationScheduled) return
        // Uzyskujemy dostęp do systemowego AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Tworzymy Intencję, która zostanie wywołana przez AlarmManager, gdy nadejdzie czas przypomnienia
        // wraz z danymi przypomnienia przekazywanymi jako pola extras. Odbiorcą intencji jest ReminderReceiver,
        // specjalny obiekt typu BroadcastReceiver, który obsłuży wyświetlenie powiadomienia.
        // Tego typu odbiornik musi być zarejestrowany w AndroidManifest.xml, aby system mógł go znaleźć i wywołać.
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(Extras.ID.name, id)
            putExtra(Extras.DESCRIPTION.name, description)
            putExtra(Extras.TITLE.name, title)
        }

        // Tworzymy PendingIntent, który jest specjalnym rodzajem Intencji, pozwalającym innym aplikacjom (w tym przypadku systemowi AlarmManager)
        // na wykonanie tej intencji w przyszłości w imieniu naszej aplikacji.
        // Jest to intencja typu broadcast, ponieważ będzie wysyłana do BroadcastReceiver (ReminderReceiver).
        // Używamy unikalnego requestCode (id.hashCode()) dla każdego przypomnienia,
        // aby umożliwić zarządzanie poszczególnymi przypomnieniami (np. anulowanie). Flagi FLAG_UPDATE_CURRENT
        // i FLAG_IMMUTABLE zapewniają, że jeśli istnieje już PendingIntent z tym samym requestCode, to zostanie on zaktualizowany o nowe dane.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Używamy metody setExactAndAllowWhileIdle, aby zaplanować alarm, który zostanie wywołany
        //  o określonym czasie (timestamp) nawet jeśli urządzenie jest w trybie oszczędzania baterii (doze mode).
        //  Typ alarmu RTC_WAKEUP oznacza, że alarm będzie oparty na czasie rzeczywistym i obudzi urządzenie, jeśli jest uśpione.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timestamp,
            pendingIntent
        )
        // Oznaczamy, że powiadomienie zostało zaplanowane, aby uniknąć wielokrotnego planowania tego samego przypomnienia
        isNotificationScheduled = true
    }

    /**
     * Anuluje zaplanowane powiadomienie dla tego przypomnienia, jeśli było wcześniej zaplanowane.
     * @param context Kontekst potrzebny do uzyskania dostępu do systemowych usług (AlarmManager) i tworzenia Intencji.
     */
    fun cancelNotification(context: Context) {
        // W tym przypadku nie musimy sprawdzać, czy powiadomienie było wcześniej zaplanowane,
        // ponieważ metoda cancel jest bezpieczna do wywołania nawet jeśli nie ma zaplanowanego alarmu.

        // Uzyskujemy dostęp do systemowego AlarmManager.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Tworzymy tę samą Intencję i PendingIntent, które zostały użyte do zaplanowania powiadomienia,
        // ponieważ AlarmManager identyfikuje alarmy na podstawie PendingIntent. Używamy tych samych danych (id.hashCode() jako requestCode) i flag,
        // aby upewnić się, że anulujemy właściwy alarm.
        // System na podstawie tego PendingIntent znajdzie i usunie zaplanowany alarm, który odpowiada temu przypomnieniu.
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Anulujemy zaplanowany alarm za pomocą AlarmManager, przekazując ten sam PendingIntent, który został użyty do jego zaplanowania.
        alarmManager.cancel(pendingIntent)
        // Oznaczamy, że powiadomienie nie jest już zaplanowane, aby umożliwić ponowne planowanie
        // tego przypomnienia w przyszłości, jeśli zajdzie taka potrzeba.
        isNotificationScheduled = false
    }


}


