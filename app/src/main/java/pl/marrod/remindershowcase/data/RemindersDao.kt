package pl.marrod.remindershowcase.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RemindersDao {

        @Query("SELECT * FROM reminders ORDER BY timestamp ASC")
        fun getAllReminders(): Flow<List<Reminder>>
        @Query("SELECT * FROM reminders WHERE id = :id")
        suspend fun getReminderById(id: String): Reminder?

        // Room obserwuje wiersz i emituje nową wartość za każdym razem gdy się zmieni
        @Query("SELECT * FROM reminders WHERE id = :id")
        fun getReminderByIdFlow(id: String): Flow<Reminder?>

        @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
        suspend fun insertReminder(reminder: Reminder)

        @Update(onConflict = androidx.room.OnConflictStrategy.REPLACE)
        suspend fun updateReminder(reminder: Reminder)

        @Delete
        suspend fun deleteReminder(reminder: Reminder)
}