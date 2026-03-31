package pl.marrod.remindershowcase.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Prosta klasa do zarządzania przechowywaniem przypomnień w pliku JSON.
 * Używa biblioteki Gson do serializacji i deserializacji danych.
 */
class ReminderStorage(private val context: Context) {
    // Konfiguracja Gson i pliku do przechowywania przypomnień
    private val gson = Gson()
    private val fileName = "reminders.json"
    private val file: File get() = File(context.filesDir, fileName)

    // Dodatkowy plik do symulacji blokującej operacji zapisu
    private val testFile: File get() = File(context.filesDir, "test.txt")

    /**
     * Zapisuje listę przypomnień do pliku JSON.
     *
     *   **Uwaga: Dla prostoty nie jest to funkcja suspend, ale powinna być,
     *   aby uniknąć blokowania UI podczas operacji dyskowych.**
     */
    fun saveReminders(reminders: List<Reminder>) {
        try {
            val json = gson.toJson(reminders)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Symuluje blokującą operację zapisu, która zajmuje dużo czasu.
     * Można dostosować liczbę iteracji, aby zwiększyć lub zmniejszyć czas trwania blokady.
     * Używane do testowania wpływu długotrwałych operacji na UI i responsywność aplikacji.
     */
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

    /**
     * Wczytuje listę przypomnień z pliku JSON. Jeśli plik nie istnieje lub wystąpi błąd, zwraca pustą listę.
     *
     * **Uwaga: Dla prostoty nie jest to funkcja suspend, ale powinna być, aby uniknąć blokowania UI podczas operacji dyskowych.**
     */
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

    /**
     * Dodaje nowe przypomnienie do istniejącej listy i zapisuje ją, a następnie wykonuje blokującą operację zapisu,
     * aby zasymulować długotrwałą operację i jej wpływ na UI. Używane do testowania,
     * jak aplikacja radzi sobie z blokującymi operacjami podczas dodawania przypomnienia.
     */
    fun addReminderBlocking(reminder: Reminder) {
        val reminders = loadReminders().toMutableList()
        reminders.add(reminder)
        saveReminders(reminders)
        saveRemindersBlocking(numIterations = 10_000)

    }

    /**
     * Dodaje nowe przypomnienie do istniejącej listy i zapisuje ją, wykonując operację w tle (oznaczenie suspend),
     * aby nie blokować UI.
     */
    suspend fun addReminder(reminder: Reminder) {
        withContext(Dispatchers.IO) { // Zmiana kontekstu na IO dla operacji dyskowych
            // W tym rozwiązaniu dodawanie przypomnienia wymaga wczytania całej listy, modyfikacji
            // i ponownego zapisu, co jest prostą, ale nieefektywną metodą.
            val reminders = loadReminders().toMutableList()
            reminders.add(reminder)
            saveReminders(reminders)
            // Dodatkowa blokująca operacja zapisu, aby zasymulować długotrwałą operację i jej wpływ na UI.
            saveRemindersBlocking()
        }
    }

    /**
     * Usuwa przypomnienie o podanym ID z listy i zapisuje zaktualizowaną listę.
     *
     *  **Uwaga: Dla prostoty nie jest to funkcja suspend, ale powinna być,
     *   aby uniknąć blokowania UI podczas operacji dyskowych.**
     */
    fun deleteReminder(id: String) {
        val reminders = loadReminders().toMutableList()
        reminders.removeAll { it.id == id }
        saveReminders(reminders)
    }


    companion object {
        /**
         * Funkcja pomocnicza do generowania listy przykładowych przypomnień,
         * używana do testowania i podglądu UI.
         */
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
