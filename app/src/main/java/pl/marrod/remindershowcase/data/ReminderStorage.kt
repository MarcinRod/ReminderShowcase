package pl.marrod.remindershowcase.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ReminderStorage(private val context: Context) {
    private val gson = Gson()
    private val fileName = "reminders.json"
    private val file: File get() = File(context.filesDir, fileName)

    private val testFile: File get() = File(context.filesDir, "test.txt")
    fun saveReminders(reminders: List<Reminder>) {
        try {
            val json = gson.toJson(reminders)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveRemindersBlocking(numIterations: Int = 5_000) {
        try {
            val startTime = System.currentTimeMillis()
            Log.i("ReminderStorage", "started blocking save")
            for (i in 1..numIterations) {
                testFile.writeText("Saving reminders... $i")
            }

            Log.i(
                "ReminderStorage",
                "finished blocking save: ${System.currentTimeMillis() - startTime} ms"
            )
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

     fun addReminderBlocking(reminder: Reminder) {

            val reminders = loadReminders().toMutableList()
            reminders.add(reminder)
            saveReminders(reminders)
            saveRemindersBlocking(numIterations = 10_000)

    }
    suspend fun addReminder(reminder: Reminder) {
        withContext(Dispatchers.IO) {
            val reminders = loadReminders().toMutableList()
            reminders.add(reminder)
            saveReminders(reminders)
            saveRemindersBlocking()
        }
    }

    fun deleteReminder(id: String) {
        val reminders = loadReminders().toMutableList()
        reminders.removeAll { it.id == id }
        saveReminders(reminders)
    }


    companion object {
        fun createDummyReminders(number: Int): List<Reminder> {
            val dummyReminders = mutableListOf<Reminder>()
            val now = System.currentTimeMillis()
            for (i in 1..number) {
                dummyReminders.add(
                    Reminder(
                        id = "reminder_$i",
                        title = "Reminder $i",
                        description = "This is a description for reminder $i.",
                        timestamp = now + i * 60 * 60 * 1000L // Each reminder 1 hour apart
                    )
                )
            }
            return dummyReminders
        }
    }
}
