package pl.marrod.remindershowcase.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import pl.marrod.remindershowcase.MainActivity
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.ui.navigation.Destination

/**
 * Obiekt pomocniczy do zarządzania kanałem powiadomień i wyświetlania powiadomień przypomnień.
 * Zawiera funkcje do tworzenia kanału powiadomień oraz wyświetlania powiadomień z odpowiednimi
 * danymi i intencją otwarcia szczegółów przypomnienia po kliknięciu.
 */
object NotificationHelper {

    // Każde powiadomienie musi należeć do kanału o unikalnym ID,
    // który jest używany do grupowania powiadomień i zarządzania ich ustawieniami.
    private const val CHANNEL_ID = "reminder_channel"

    /**
     * Tworzy kanał powiadomień dla przypomnień, jeśli jeszcze nie istnieje. Kanał jest wymagany
     * do wyświetlania powiadomień na Androidzie 8.0 (API 26) i nowszych. Funkcję można wywoływać wielokrotnie,
     * ale kanał zostanie utworzony tylko raz.
     */
    fun createNotificationChannel(context: Context) {
        // Za obsługę powiadomień odpowiada usługa systemowa  NotificationManager,
        // która umożliwia m.in. tworzenie kanałów i wyświetlanie powiadomień.
        // Dostęp do tej usługi uzyskujemy za pomocą metody getSystemService z odpowiednim argumentem
        // (podobnie jak do innych usług systemowych).
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kanał tworzymy za pomocą NotificationChannel, który wymaga unikalnego ID,
        // nazwy (widocznej dla użytkownika) oraz poziomu ważności (IMPORTANCE). Ważność kanału
        // wpływa na sposób wyświetlania powiadomień (np. czy są dźwiękowe, czy pojawiają się na ekranie blokady itp.).
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            // Opcjonalnie można ustawić dodatkowe właściwości kanału, takie jak opis, dźwięk, światła itp.
            this.description = "Channel for reminder notifications"
        }
        // Tworzymy kanał za pomocą metody createNotificationChannel.
        // Jeśli kanał o tym ID już istnieje, to nic się nie stanie.
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Wyświetla powiadomienie przypomnienia z podanymi danymi.
     * Po kliknięciu powiadomienia otwiera się ekran szczegółów przypomnienia.
     * Funkcja tworzy intencję z odpowiednim URI.
     * Funkcja jest wywoływana przez [ReminderReceiver], który otrzymuje dane przypomnienia jako extras w intencji z AlarmManager
     * zobacz: [pl.marrod.remindershowcase.data.Reminder.scheduleNotification].
     * @param context Kontekst potrzebny do uzyskania dostępu do systemowych usług (NotificationManager) i tworzenia Intencji.
     * @param id Unikalny identyfikator przypomnienia, używany do odnalezienia szczegółów po kliknięciu powiadomienia.
     * @param title Tytuł przypomnienia, wyświetlany jako nagłówek powiadomienia.
     * @param description Opis przypomnienia, wyświetlany jako treść powiadomienia.
     * Uwaga: Funkcja nie przyjmuje całego obiektu Reminder, aby uniknąć konieczności serializacji obiektu w intencji.
     */
    fun showReminder(context: Context, id: String, title: String, description: String) {

        // Ponownie uzyskujemy dostęp do NotificationManager, aby móc wyświetlić powiadomienie.
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tworzymy intencję, która zostanie wywołana po kliknięciu powiadomienia. Intencja jest typu VIEW,
        // co oznacza, że będzie otwierać ekran (Activity) z odpowiednimi danymi.
        // URI jest budowane za pomocą funkcji buildUri z obiektu Destination.Details.
        // Wywołanie intencji z akcją VIEW i odpowiednim URI pozwala na wykorzystanie
        // NavController do nawigacji do ekranu szczegółów przypomnienia poprzez tzw. deep link.
        val tapIntent = Intent(
            Intent.ACTION_VIEW,
            Destination.Details.buildUri(id).toUri(),
            context,
            MainActivity::class.java
        ).apply {
            // Flaga FLAG_ACTIVITY_SINGLE_TOP zapewnia, że jeśli MainActivity jest już otwarta,
            // to zostanie użyta istniejąca instancja zamiast tworzenia nowej.
            // Podobne ustawienie jest stosowane w manifeście dla MainActivity (android:launchMode="singleTop").
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Tworzymy PendingIntent, który pozwoli systemowi na wykonanie tej intencji w imieniu naszej aplikacji,
        // gdy użytkownik kliknie powiadomienie. Jest to podobne do tworzenia PendingIntent dla AlarmManager,
        // ale tym razem jest to intencja typu activity, ponieważ chcemy otworzyć aktywność.
        // Używamy unikalnego requestCode (id.hashCode()) dla każdego przypomnienia,
        // aby umożliwić zarządzanie poszczególnymi powiadomieniami (np. aktualizację lub anulowanie).
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Do budowania powiadomienia używamy NotificationCompat.Builder, który jest częścią biblioteki AndroidX
        // i zapewnia kompatybilność z różnymi wersjami Androida.
        // Argumentami konstruktora są kontekst i ID kanału, do którego należy powiadomienie.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.placeholder) // ikona
            .setContentTitle(title) // tytuł
            .setContentText(description) // opis
            .setAutoCancel(true) // sprawia, że powiadomienie zniknie po kliknięciu
            .setContentIntent(tapPendingIntent) // intencja otwierająca szczegóły przypomnienia po kliknięciu
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // kategoria powiadomienia, która pomaga systemowi zarządzać powiadomieniami (np. priorytetem, grupowaniem itp.)
            .build()

        // Wyświetlamy powiadomienie za pomocą metody notify, podając unikalny ID (id.hashCode())
        // i zbudowane powiadomienie.
        notificationManager.notify(id.hashCode(), notification)
    }
}

