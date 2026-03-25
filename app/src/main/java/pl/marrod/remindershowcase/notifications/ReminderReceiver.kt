package pl.marrod.remindershowcase.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.marrod.remindershowcase.data.Reminder

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showReminder(
            context,
            id          = intent.getStringExtra(Reminder.Companion.Extras.ID.name)          ?: "",
            title       = intent.getStringExtra(Reminder.Companion.Extras.TITLE.name)       ?: "Reminder",
            description = intent.getStringExtra(Reminder.Companion.Extras.DESCRIPTION.name) ?: ""
        )
    }
}

