package pl.marrod.remindershowcase.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Klasa reprezentująca bazę danych Room dla przypomnień, zawierająca definicję encji i DAO.
 * Baza danych jest singletonem, co oznacza, że istnieje tylko jedna instancja tej klasy w całej aplikacji,
 * zapewniając spójny dostęp do danych i unikając problemów z wielokrotnym otwieraniem bazy danych.
 * Encja przypomnienia (Reminder) jest zdefiniowana w osobnej klasie, a DAO (RemindersDao) zawiera metody do operacji na tej bazie danych.
 * Klasa oznaczaona adnotacją @Database musi być abstrakcyjna bo jej implementacja jest generowana
 * automatycznie przez Room podczas kompilacji, na podstawie zdefiniowanych encji i DAO.
 */
@Database(
    entities = [Reminder::class], // określa, że ta baza danych będzie zawierać tabelę dla encji Reminder.
    // Można dodać więcej encji, jeśli aplikacja będzie tego wymagać, np. kategorie przypomnień, użytkownicy itp.
    version = 1, // wersja bazy danych, która powinna być zwiększana przy każdej zmianie struktury bazy danych (np. dodanie nowej kolumny, zmiana typu danych itp.)
    exportSchema = false) // opcja exportSchema określa, czy Room ma eksportować schemat bazy danych do pliku JSON podczas kompilacji. Przydatne do śledzenia zmian w strukturze bazy danych, ale można wyłączyć, jeśli nie jest potrzebne.
abstract class RemindersDb: RoomDatabase() {
    abstract fun remindersDao(): RemindersDao

    /** Obiekt towarzyszący zawierający stałe i funkcję do uzyskania instancji bazy danych.
     * Zapewnia implementację wzorca singleton, aby zapewnić, że tylko jedna instancja bazy danych
     * jest tworzona i używana w całej aplikacji. Jest to szablonowy kod tworzenia bazy danych Room,
     * który można łatwo dostosować do innych projektów, zmieniając tylko nazwę klasy i encje itd..
     */
    companion object{
        const val DB_NAME = "reminders_db"
        @Volatile
        private var INSTANCE: RemindersDb? = null
        fun getInstance(context: android.content.Context): RemindersDb {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RemindersDb::class.java,
                    DB_NAME
                )
                    // Dodaj migracje w kolejności: .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)
                    // Room automatycznie wybierze odpowiednią migrację na podstawie aktualnej wersji bazy.
                    // .addMigrations(MIGRATION_1_2)

                    // UWAGA: fallbackToDestructiveMigration() usuwa i odtwarza bazę danych
                    // przy braku migracji — powoduje utratę danych, NIE używać produkcyjnie!
                    // .fallbackToDestructiveMigration()
                    .build()
               INSTANCE = instance
                instance
            }
        }

        /**
         * Przykład migracji z wersji 1 do 2.
         * Migracja jest wymagana gdy zmienia się struktura bazy danych (np. nowa kolumna, tabela itp.)
         * i chcemy zachować dane użytkownika. Każda migracja wykonuje odpowiednie zapytania SQL,
         * które doprowadzają schemat bazy danych do stanu zgodnego z nową wersją encji.
         *
         * Aby użyć tej migracji należy:
         *  1. Zwiększyć version w @Database do 2
         *  2. Dodać nową kolumnę do encji Reminder (np. val priority: Int = 0)
         *  3. Dodać MIGRATION_1_2 do buildera przez .addMigrations(MIGRATION_1_2)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Przykład: dodanie kolumny 'priority' (INTEGER, domyślnie 0) do tabeli reminders
                db.execSQL("ALTER TABLE reminders ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}