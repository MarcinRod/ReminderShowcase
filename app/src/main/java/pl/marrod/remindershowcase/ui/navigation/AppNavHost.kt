package pl.marrod.remindershowcase.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.ui.screens.ReminderDetailScreen
import pl.marrod.remindershowcase.ui.screens.ReminderListScreen

// TODO: Skończyć dokumentacje UI. Pamiętać o rozróżnieniu rozwiązania z ViewModel i bez niego,

private const val TAG = "Navigation"
private const val USE_VIEW_MODEL = true

@Composable
fun AppNavHost(
    navController: NavHostController,
    permissionGranted: Boolean
) {
    NavHost(navController = navController, startDestination = Destination.List) {

        composable<Destination.List> {
            if (USE_VIEW_MODEL) {
                ReminderListScreen()
            } else {
                val context = LocalContext.current
                val storage = remember { ReminderStorage(context) }
                var reminders by remember { mutableStateOf(storage.loadReminders()) }
                val scope = rememberCoroutineScope()
                ReminderListScreen(
                    reminders = reminders,
                    onDelete = { reminder ->
                        reminder.cancelNotification(context)
                        storage.deleteReminder(reminder.id)
                        reminders = storage.loadReminders()
                    },
                    onSave = { reminder ->
                        Log.i(
                            "AppNavHost",
                            "Saving reminder: $reminder @ ${System.currentTimeMillis()}"
                        )
                       // storage.addReminderBlocking(reminder)
                        scope.launch(Dispatchers.IO) {
                            storage.addReminder(reminder)
                            Log.i(
                                "AppNavHost",
                                "Finished saving reminder: $reminder @ ${System.currentTimeMillis()}"
                            )
                            reminders = storage.loadReminders()
                            Log.i(
                                "AppNavHost",
                                "Finished loading reminders after save: ${reminders.size} reminders @ ${System.currentTimeMillis()}"
                            )
                        }
                        Log.i(
                            "AppNavHost",
                            "Scheduling added reminder: $reminder @ ${System.currentTimeMillis()}"
                        )
                        if (permissionGranted)
                            reminder.scheduleNotification(context)


                    },
                    onUpdate = { reminder ->
                        reminder.cancelNotification(context)
                        storage.deleteReminder(reminder.id)
                        Log.i(
                            "AppNavHost",
                            "Updating reminder: $reminder @ ${System.currentTimeMillis()}"
                        )
                        scope.launch {
                            val job = async(Dispatchers.IO) {
                                storage.addReminder(reminder)
                                Log.i(
                                    "AppNavHost",
                                    "Finished Updating reminder: $reminder @ ${System.currentTimeMillis()}"
                                )
                                reminder
                            }


                            val result = job.await()
                            Log.i(
                                "AppNavHost",
                                "$result"
                            )
                            reminders = storage.loadReminders()
                            Log.i(
                                "AppNavHost",
                                "Finished loading reminders after update: ${reminders.size} reminders @ ${System.currentTimeMillis()}"
                            )

                        }
                        Log.i(
                            "AppNavHost",
                            "Scheduling updated reminder: $reminder @ ${System.currentTimeMillis()} "
                        )
                        if (permissionGranted)
                            reminder.scheduleNotification(context)

                    },
                )
            }
        }

        composable<Destination.Details>(
            deepLinks = listOf(
                navDeepLink { uriPattern = Destination.Details.DEEP_LINK_URI }
            )
        ) { backStackEntry ->
            Log.i(TAG, backStackEntry.destination.toString())
            val details = backStackEntry.toRoute<Destination.Details>()
            val reminderId = details.reminderId

            ReminderDetailScreen(
                reminderId = reminderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
