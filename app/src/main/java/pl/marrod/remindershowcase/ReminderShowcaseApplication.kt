package pl.marrod.remindershowcase

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import pl.marrod.remindershowcase.data.ReminderStorage

class ReminderShowcaseApplication() : Application() {

    lateinit var storage: ReminderStorage
        private set

    override fun onCreate() {
        super.onCreate()
        storage = ReminderStorage(this)
    }
}

fun CreationExtras.reminderShowcaseApplication(): ReminderShowcaseApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReminderShowcaseApplication)