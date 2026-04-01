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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pl.marrod.remindershowcase.ui.screens.ReminderDetailScreen
import pl.marrod.remindershowcase.ui.screens.ReminderListScreen


private const val TAG = "Navigation"


@Composable
fun AppNavHost(
    navController: NavHostController,
    permissionGranted: Boolean
) {
    NavHost(navController = navController, startDestination = Destination.List) {

        // Nawigacja z ekranami wykorzystującymi ViewModel
        composable<Destination.List> {
            // ViewModel jest inicjalizowany jako argument domyślny w ReminderListScreen, więc nie musimy go tu tworzyć ręcznie
            // Jego fabryka pochodzi z AppWideViewModelProvider,
            ReminderListScreen()
        }
        composable<Destination.Details>(
            deepLinks = listOf(
                navDeepLink { uriPattern = Destination.Details.DEEP_LINK_URI }
            )
        ) { backStackEntry ->
            // Podobnie, ViewModel jest inicjalizowany wewnątrz ReminderDetailScreen, więc nie musimy go tu tworzyć ręcznie
            // argument w postaci identyfikatora przypomnienia jest przekazywany przez SavedStateHandle (specjalny obiekt, który automatycznie otrzymuje argumenty z nawigacji)
            // do ViewModel.
            ReminderDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

    }

}

