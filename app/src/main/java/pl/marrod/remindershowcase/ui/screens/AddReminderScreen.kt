package pl.marrod.remindershowcase.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.notifications.ReminderReceiver
import java.util.*

/**
 * Unified add / edit screen.
 *
 * - [reminderId] == null  →  "Add" mode: all fields start empty, a new Reminder is created on save.
 * - [reminderId] != null  →  "Edit" mode: fields are pre-filled from storage, the existing entry
 *                            is replaced and its alarm is rescheduled on save.
 */
@Composable
fun ReminderFormScreen(
    innerPadding: PaddingValues,
    onSaved: () -> Unit,
    reminderId: String? = null
) {
    val context = LocalContext.current
    val storage = remember { ReminderStorage(context) }

    val existing = remember(reminderId) {
        reminderId?.let { id -> storage.loadReminders().find { it.id == id } }
    }

    var title         by remember { mutableStateOf(existing?.title       ?: "") }
    var description   by remember { mutableStateOf(existing?.description ?: "") }
    var minutesOffset by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = minutesOffset,
            onValueChange = { if (it.all { char -> char.isDigit() }) minutesOffset = it },
            label = { Text(if (existing == null) "Notify in X minutes" else "Reschedule in X minutes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    val offset      = minutesOffset.toLongOrNull() ?: 1L
                    val triggerTime = System.currentTimeMillis() + (offset * 60 * 1000)

                    val reminder = (existing ?: Reminder(id = UUID.randomUUID().toString(), title = "", description = "", timestamp = 0))
                        .copy(title = title, description = description, timestamp = triggerTime)

                    if (existing != null) storage.deleteReminder(existing.id)
                    storage.addReminder(reminder)
                    scheduleNotification(context, reminder)
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank()
        ) {
            Text(if (existing == null) "Save & Schedule Reminder" else "Save Changes")
        }
    }
}

private fun scheduleNotification(context: Context, reminder: Reminder) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", reminder.title)
        putExtra("description", reminder.description)
        putExtra("id", reminder.id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        reminder.timestamp,
        pendingIntent
    )
}
