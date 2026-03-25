package pl.marrod.remindershowcase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.ui.screens.ReminderListScreen

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = Destination.List) {

        composable<Destination.List> {
            val context = LocalContext.current

            val storage = remember { ReminderStorage(context) }
            var reminders by remember { mutableStateOf(storage.loadReminders()) }
            ReminderListScreen(
                reminders = reminders,
                onDelete= { reminder ->
                    storage.deleteReminder(reminder.id)
                    reminders = storage.loadReminders()
                    // TODO: usuwanie powiadomienia z systemu, jeśli jest ustawione
                },
                onSave = { reminder ->
                    storage.addReminder(reminder)
                    reminders = storage.loadReminders()
                },
                onUpdate = { reminder ->
                    storage.deleteReminder(reminder.id)
                    storage.addReminder(reminder)
                    reminders = storage.loadReminders()
                },
            )
        }

        composable<Destination.Details>(
        ) { backStackEntry ->
            val details = backStackEntry.toRoute<Destination.Details>()
            val reminderId = details.reminderId

        }
    }
}
