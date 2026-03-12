package pl.marrod.remindershowcase.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.ui.screens.ReminderFormScreen
import pl.marrod.remindershowcase.ui.screens.ReminderListScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    onShareClick: (Reminder) -> Unit
) {
    NavHost(navController = navController, startDestination = Routes.list) {

        composable(Routes.list) {
            ReminderListScreen(
                innerPadding = innerPadding,
                onShareClick = onShareClick,
                onEditClick  = { reminder -> navController.navigate(Routes.editRoute(reminder.id)) }
            )
        }

        composable(Routes.add) {
            ReminderFormScreen(
                innerPadding = innerPadding,
                onSaved      = { navController.popBackStack() }
                // reminderId defaults to null → Add mode
            )
        }

        // The route template and how the argument is extracted both change with USE_PATH_ARG:
        //   path segment → "edit/{reminderId}"   → backStackEntry.arguments
        //   query param  → "edit?reminderId=…"   → backStackEntry.arguments (same API, different template)
        composable(
            route     = Routes.edit,
            arguments = listOf(
                navArgument("reminderId") {
                    type = NavType.StringType
                    // Query params can be absent; provide an empty default so the
                    // type stays non-nullable and the screen handles the missing case.
                    if (!USE_PATH_ARG) defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId") ?: ""
            ReminderFormScreen(
                innerPadding = innerPadding,
                onSaved      = { navController.popBackStack() },
                reminderId   = reminderId.ifBlank { null }   // blank query-param default → treat as null
            )
        }
    }
}
