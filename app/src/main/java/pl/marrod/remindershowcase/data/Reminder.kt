package pl.marrod.remindershowcase.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import pl.marrod.remindershowcase.notifications.ReminderReceiver
import toDisplayDateTime
import toDisplayTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    var isNotificationScheduled: Boolean = false
){
    companion object {
       enum class Extras{
           ID, TITLE, DESCRIPTION
       }
    }
    val displayTime: String
        get() = timestamp.toDisplayTime()
    val displayDateTime: String
        get() = timestamp.toDisplayDateTime()

    fun scheduleNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(Extras.ID.name, title)
            putExtra(Extras.DESCRIPTION.name, description)
            putExtra(Extras.TITLE.name, id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timestamp,
            pendingIntent
        )
        isNotificationScheduled = true
    }


}


