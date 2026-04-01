package pl.marrod.remindershowcase.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Reminder::class],
    version = 1,
    exportSchema = false)
abstract class RemindersDb: RoomDatabase() {
    abstract fun remindersDao(): RemindersDao
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}