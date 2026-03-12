package pl.marrod.remindershowcase.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ReminderStorage(private val context: Context) {
    private val gson = Gson()
    private val fileName = "reminders.json"
    private val file: File get() = File(context.filesDir, fileName)

    fun saveReminders(reminders: List<Reminder>) {
        try {
            val json = gson.toJson(reminders)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadReminders(): List<Reminder> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<Reminder>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addReminder(reminder: Reminder) {
        val reminders = loadReminders().toMutableList()
        reminders.add(reminder)
        saveReminders(reminders)
    }

    fun deleteReminder(id: String) {
        val reminders = loadReminders().toMutableList()
        reminders.removeAll { it.id == id }
        saveReminders(reminders)
    }
}
