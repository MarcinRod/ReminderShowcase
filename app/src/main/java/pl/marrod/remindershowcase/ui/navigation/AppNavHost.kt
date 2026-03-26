package pl.marrod.remindershowcase.ui.navigation

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.navigation.NavController
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

private const val TAG = "Navigation"

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            val deepLinkUri = arguments?.let {
                BundleCompat.getParcelable(
                    it,
                    "android-support-nav:controller:deepLinkIntent",
                    Intent::class.java
                )
            }?.data?.let { "(deep link: $it)" } ?: ""
            Log.d(TAG, "→ ${destination.route} $deepLinkUri".trim())
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(navController = navController, startDestination = Destination.List) {

        composable<Destination.List> {
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
                        val result = async(Dispatchers.IO) {
                            storage.addReminder(reminder)
                            Log.i(
                                "AppNavHost",
                                "Finished Updating reminder: $reminder @ ${System.currentTimeMillis()}"
                            )
                        }
                      //  result.await()

                        reminders = storage.loadReminders()
                        Log.i(
                            "AppNavHost",
                            "Finished loading reminders after update: ${reminders.size} reminders @ ${System.currentTimeMillis()}"
                        )
                    }
                    Log.i(
                        "AppNavHost",
                        "Scheduling updated reminder: $reminder @ ${System.currentTimeMillis()}"
                    )
                    reminder.scheduleNotification(context)

                },
            )
        }

        composable<Destination.Details>(
            deepLinks = listOf(
                navDeepLink { uriPattern = Destination.Details.DEEP_LINK_URI }
            )
        ) { backStackEntry ->
            val details = backStackEntry.toRoute<Destination.Details>()
            val reminderId = details.reminderId

            ReminderDetailScreen(
                reminderId = reminderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
