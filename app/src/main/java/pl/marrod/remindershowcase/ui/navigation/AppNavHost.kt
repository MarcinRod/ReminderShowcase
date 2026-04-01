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
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.ui.screens.ReminderDetailScreen
import pl.marrod.remindershowcase.ui.screens.ReminderListScreen



private const val TAG = "Navigation"
private const val USE_VIEW_MODEL = true

@Composable
fun AppNavHost(
    navController: NavHostController,
    permissionGranted: Boolean
) {
    NavHost(navController = navController, startDestination = Destination.List) {
        if (USE_VIEW_MODEL) {
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
        } else {
            // Nawigacja z ekranami przechowującymi swój własny stan (bez ViewModel)
            composable<Destination.List> {
               // Wszystkie zmienne stanu i logika związana z przechowywaniem i aktualizacją listy
                // przypomnień jest zarządzana bezpośrednio w komponencie ReminderListScreen lub w rodzicu (tutaj AppNavHost).
                val context = LocalContext.current
                val storage = remember { ReminderStorage(context) }
                // Ładowanie przypomnień z pamięci przy pierwszym uruchomieniu w głównym wątku -
                // nie jest to najlepsza praktyka, ale dla prostoty tego przykładu jest to akceptowalne.
                // W prawdziwej aplikacji powinniśmy to zrobić asynchronicznie, aby nie blokować UI.
                var reminders by remember { mutableStateOf(storage.loadReminders()) }

                // Tworzymy scope (zakres) dla korutyn, który będzie używany do uruchamiania
                // operacji asynchronicznych (np. zapisu do bazy danych) bez blokowania UI.
                // W kontekście Compose, rememberCoroutineScope() tworzy scope, który jest automatycznie anulowany,
                // gdy funkcja kompozycyjna opuszcza hierarchię kompozycji, co pomaga uniknąć wycieków pamięci i niepotrzebnych operacji po zniszczeniu UI.
                val scope = rememberCoroutineScope()

                ReminderListScreen(
                    reminders = reminders,
                    onDelete = { reminder ->
                        reminder.cancelNotification(context)
                        storage.deleteReminder(reminder.id)
                        runBlocking {
                            delay(5_000) // Symulacja długotrwałej operacji wczytywania, aby przetestować wpływ na UI
                        }
                        reminders = storage.loadReminders()
                    },
                    onSave = { reminder ->
                        // Zapis nowego przypomnienia
                        Log.i(
                            "AppNavHost",
                            "Saving reminder: $reminder @ ${System.currentTimeMillis()}"
                        )

                        // Symulacja blokującej operacji zapisu, która zajmuje dużo czasu, aby przetestować wpływ na UI
                        // storage.addReminderBlocking(reminder)

                        // Uruchomienie operacji zapisu w tle (metoda launch), aby nie blokować UI. Dzięki temu, nawet jeśli operacja zapisu zajmuje dużo czasu,
                        // UI pozostaje responsywne, a użytkownik może nadal korzystać z aplikacji podczas zapisywania przypomnienia.
                        scope.launch(Dispatchers.IO) {
                            // funkcja addReminder jest oznaczona jako suspend, więc musi być wywołana z korutyny.
                            storage.addReminder(reminder)
                            Log.i(
                                "AppNavHost",
                                "Finished saving reminder: $reminder @ ${System.currentTimeMillis()}"
                            )
                            // Po zakończeniu zapisu, ponownie ładujemy listę przypomnień z pamięci, aby zaktualizować UI.
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
                        // W takiej konfiguracji, najpierw uruchamiamy operację zapisu w tle,
                        // ale powiadomienie jest już planowane zanim operacja zapisu się zakończy.
                        // Nie jest to idealne rozwiązanie bo nie bierze pod uwagę ewentualnych błędów podczas zapisu,
                        // ale pokazuje na co należy zwracać uwagę przy operacjach asynchronicznych.
                        if (permissionGranted)
                            reminder.scheduleNotification(context)


                    },
                    onUpdate = { reminder ->
                        // Aktualziacja istniejącego przypomnienia
                        reminder.cancelNotification(context) // Anulowanie starego powiadomienia, jeśli było zaplanowane
                        storage.deleteReminder(reminder.id) // Usunięcie starego przypomnienia z pamięci, aby zastąpić je zaktualizowanym
                        Log.i(
                            "AppNavHost",
                            "Updating reminder: $reminder @ ${System.currentTimeMillis()}"
                        )
                        // W procesie aktualizacji pokazujemy, jak można użyć async/await do uruchomienia
                        // operacji zapisu w tle i oczekiwania na jej wynik, zanim przejdziemy do kolejnych kroków (np. planowania powiadomienia).
                        scope.launch {
                            val job = async(Dispatchers.IO) {
                                storage.addReminder(reminder)
                                Log.i(
                                    "AppNavHost",
                                    "Finished Updating reminder: $reminder @ ${System.currentTimeMillis()}"
                                )
                                reminder
                            }
                            // Tutaj potencjalnie możemy wykonywać operacje równoległe do zapisu,
                            // ale w tym przypadku po prostu czekamy na jego zakończenie,
                            // aby mieć pewność, że przypomnienie zostało zaktualizowane w pamięci przed planowaniem powiadomienia.

                            val result = job.await()
                            Log.i(
                                "AppNavHost",
                                "$result"
                            )
                            Log.i(
                                "AppNavHost",
                                "Finished loading reminders after update: ${reminders.size} reminders @ ${System.currentTimeMillis()}"
                            )
                            reminders = storage.loadReminders()

                            Log.i(
                                "AppNavHost",
                                "Scheduling updated reminder: $reminder @ ${System.currentTimeMillis()} "
                            )
                            // W tym przypadku planowanie nowego powiadomienia odbywa się dopiero
                            // po zakończeniu operacji zapisu
                            if (permissionGranted)
                                reminder.scheduleNotification(context)

                        }

                    },
                )
            }


            composable<Destination.Details>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = Destination.Details.DEEP_LINK_URI }
                )
            ) { backStackEntry ->
                // Odczytujemy identyfkator przypomnienia z argumentów nawigacji, które są dostępne w backStackEntry.
                Log.i(TAG, backStackEntry.destination.toString())
                // konwersja backStackEntry na konkretny typ Destination, aby łatwo odczytać argumenty
                val details = backStackEntry.toRoute<Destination.Details>()
                val reminderId = details.reminderId

                ReminderDetailScreen(
                    reminderId = reminderId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

