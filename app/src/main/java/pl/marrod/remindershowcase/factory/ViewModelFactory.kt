package pl.marrod.remindershowcase.factory

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.marrod.remindershowcase.reminderShowcaseApplication
import pl.marrod.remindershowcase.ui.screens.ReminderListViewModel

object AppWideViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ReminderListViewModel(
                application = reminderShowcaseApplication(),
                storage = reminderShowcaseApplication().storage
            )
        }

    }
}