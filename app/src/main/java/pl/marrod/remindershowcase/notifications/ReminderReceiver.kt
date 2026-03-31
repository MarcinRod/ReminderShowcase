package pl.marrod.remindershowcase.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.marrod.remindershowcase.data.Reminder

/**
 * Odbiornik powiadomień przesyłanych przez AlarmManagera. Odpowiada za wyświetlenie powiadomienia z przypomnieniem i nic więcej.
 * Odbiornik jest zarejestrowany w AndroidManifest.xml, więc system wie, że ma go uruchomić, gdy nadejdzie czas przypomnienia.
 * W funkcji Reminder.scheduleNotification() przypomnienie jest planowane za pomocą AlarmManagera, który wysyła intencję do tego odbiornika,
 * gdy nadejdzie czas przypomnienia. Odbiornik otrzymuje dane przypomnienia jako extras w intencji, a następnie przekazuje je do NotificationHelper,
 * który tworzy i wyświetla powiadomienie.
 */
class ReminderReceiver : BroadcastReceiver() {
    // We własnej implemntacji klasy dziedziczącej po BroadcastReceiver, musimy nadpisać funkcję onReceive,
    // która jest wywoływana przez system, gdy odbiornik otrzyma intencję.
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showReminder(
            context,
            id          = intent.getStringExtra(Reminder.Companion.Extras.ID.name)          ?: "",
            title       = intent.getStringExtra(Reminder.Companion.Extras.TITLE.name)       ?: "Reminder",
            description = intent.getStringExtra(Reminder.Companion.Extras.DESCRIPTION.name) ?: ""
        )
    }
}

