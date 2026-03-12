package pl.marrod.remindershowcase.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showReminder(
            context,
            id          = intent.getStringExtra("id")          ?: "",
            title       = intent.getStringExtra("title")       ?: "Reminder",
            description = intent.getStringExtra("description") ?: ""
        )
    }
}

