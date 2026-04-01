package pl.marrod.remindershowcase.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class RemindersRepository(private val dao: RemindersDao) {
    val allReminders = dao.getAllReminders()

    suspend fun insertReminder(reminder: Reminder) = withContext(Dispatchers.IO){
        dao.insertReminder(reminder)
    }
    suspend fun deleteReminder(reminder: Reminder) = withContext(Dispatchers.IO)
    {
        dao.deleteReminder(reminder)
    }
    suspend fun updateReminder(reminder: Reminder) = withContext(Dispatchers.IO){
        dao.updateReminder(reminder)
    }

    /**
     * Wersja 1 — natywny Flow z Room.
     * Room wewnętrznie obserwuje tabelę i emituje nową wartość za każdym razem,
     * gdy wiersz o podanym [id] zostanie zmieniony. Prawdziwie reaktywna.
     */
    fun getReminderByIdFlow(id: String): Flow<Reminder?> = dao.getReminderByIdFlow(id)

    /**
     * Wersja 2 — suspend fun opakowana w flow builder.
     * Emituje wartość jednorazowo i kończy flow. Nie reaguje na późniejsze zmiany w bazie.
     * Odpowiednik Rozwiązania A, ale z interfejsem Flow zamiast suspend fun.
     */
    fun getReminderByIdAsFlow(id: String): Flow<Reminder?> = flow {
        emit(dao.getReminderById(id))
    }
}