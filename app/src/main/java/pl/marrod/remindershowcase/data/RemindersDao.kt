package pl.marrod.remindershowcase.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Interfejs DAO (Data Access Object) dla przypomnień, definiujący metody do operacji na bazie danych Room.
 * DAO jest odpowiedzialny za abstrakcję warstwy dostępu do danych, umożliwiając łatwe wykonywanie
 * operacji CRUD (Create, Read, Update, Delete) na encjach przypomnień.
 */
@Dao
interface RemindersDao {
        /**
         * Wstawia nowe przypomnienie do bazy danych. Jeśli przypomnienie o tym samym id już istnieje, zostanie zastąpione (REPLACE).
         * Operacje na bazie danych muszą być wykonywane w kontekście korutyny, dlatego metoda jest oznaczona jako suspend.
         */
        @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
        suspend fun insertReminder(reminder: Reminder)

        /**
         * Aktualizuje istniejące przypomnienie w bazie danych.
         * Jeśli przypomnienie o tym samym id nie istnieje, operacja nie wprowadzi żadnych zmian.
         */
        @Update()
        suspend fun updateReminder(reminder: Reminder)

        /**
         * Usuwa przypomnienie z bazy danych.
         */
        @Delete
        suspend fun deleteReminder(reminder: Reminder)

        /**
         * Usuwanie można też wykonać za pomocą zapytania SQL
         */
        @Query("DELETE FROM reminders WHERE id = :id")
        suspend fun deleteReminderById(id: String)


        /**
         * Pobiera wszystkie przypomnienia z bazy danych, posortowane rosnąco według czasu przypomnienia (timestamp).
         * Zwraca Flow, co pozwala na obserwowanie zmian w bazie danych i automatyczne aktualizowanie UI, gdy dane się zmienią.
         * Metoda nie wymaga oznaczenia jako suspend, ponieważ samo wywołanie funkcji natychmiast zwraca
         * obiekt Flow, nie wykonując żadnej pracy. Faktyczne zapytanie do bazy danych jest wykonywane
         * dopiero podczas kolekcji (collect) Flow w kontekście korutyny.
         */
        @Query("SELECT * FROM reminders ORDER BY timestamp ASC")
        fun getAllReminders(): Flow<List<Reminder>>

        /**
         * Pobiera pojedyncze przypomnienie z bazy danych na podstawie jego unikalnego identyfikatora (id).
         * Zwraca null, jeśli przypomnienie o podanym id nie istnieje.
         * Metoda jest oznaczona jako suspend, ponieważ wykonuje zapytanie do bazy danych,
         * które jest operacją blokującą i powinno być wykonywane w kontekście korutyny.
         */
        @Query("SELECT * FROM reminders WHERE id = :id")
        suspend fun getReminderById(id: String): Reminder?

        /**
         * Pobiera pojedyncze przypomnienie z bazy danych na podstawie jego unikalnego identyfikatora (id) i zwraca je jako Flow.
         * Jest to alternatywna metoda do getReminderById, która pozwala na obserwowanie zmian w konkretnym przypomnieniu.
         * Ułatwia to automatyczne aktualizowanie UI, gdy dane przypomnienia się zmienią, bez konieczności ponownego wywoływania zapytania.
         */
        @Query("SELECT * FROM reminders WHERE id = :id")
        fun getReminderByIdFlow(id: String): Flow<Reminder?>

}