package pl.marrod.remindershowcase

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import pl.marrod.remindershowcase.notifications.NotificationHelper
import pl.marrod.remindershowcase.ui.navigation.AppNavHost
import pl.marrod.remindershowcase.ui.theme.ReminderShowcaseTheme


class MainActivity : ComponentActivity() {
    /** Flaga do kontrolowania wyświetlania komunikatu o braku uprawnień.
     * Zmienna typu mutableStateOf pozwala na automatyczne odświeżanie UI po zmianie jej wartości.
     */
    var permissionGranted by mutableStateOf(false)

    /**
     * Przechowuje aktualną intencję w MainActivity, która może być używana do obsługi
     * nawigacji bezpośrednio z powiadomienia przez Deep link.
     */
    var currentIntent by mutableStateOf<Intent?>(null)

    /**
     * Rejestruje launcher do żądania uprawnienia POST_NOTIFICATIONS.
     * Po wywołaniu launch() z odpowiednim uprawnieniem, system wyświetli dialog z prośbą o przyznanie uprawnienia.
     * Po udzieleniu lub odmowie uprawnienia, lambda przekazana jako drugi argument zostanie
     * wywołana z wynikiem (isGranted = true/false), co pozwala na aktualizację stanu permissionGranted.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    /**
     * Rejestruje launcher do otwierania ustawień aplikacji. Po wywołaniu launch() z odpowiednią intencją,
     * system otworzy ekran ustawień aplikacji, gdzie użytkownik może ręcznie przyznać uprawnienie do powiadomień.
     * Po powrocie z ustawień ponownie sprawdzamy stan uprawnień (choć jest to też realizowane w onResume())
     */
    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Sprawdzamy ponownie uprawnienia po powrocie z ustawień, choć jest to też realizowane w onResume()
        checkNotificationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            ReminderShowcaseTheme {
                // inicjalizacja NavController do zarządzania nawigacją w aplikacji
                val navController = rememberNavController()
                // Inicjalizacja SnackbarHostState do zarządzania stanem Snackbar,
                // który będzie używany do wyświetlania komunikatów o braku uprawnień.
                val snackbarHostState = remember { SnackbarHostState() }

                // LaunchedEffect jest tzw. side effect, który jest uruchamiany w zależności od
                // parametrów podanych w wywołaniu. W przypadku tego efektu, jest on uruchamiany
                // przy pierwszej kompozycji oraz za każdym razem, gdy zmienia się wartość permissionGranted.
                // Gdyby zamiast permissionGranted użyć Unit, to efekt byłby uruchamiany tylko raz,
                // przy pierwszej kompozycji.
                // LaunchedEffect pozwala na wykonywanie operacji asynchronicznych lub innych działań,
                // które nie powinny być wykonywane bezpośrednio w funkcji kompozycyjnej.
                // Posiada swój własny CoroutineScope, więc można w nim używać funkcji suspend,
                // takich jak showSnackbar.
                LaunchedEffect(permissionGranted) {
                    if (!permissionGranted) {
                        val result = snackbarHostState.showSnackbar(
                            message = "Notification permission is required for reminders",
                            // Do snackbar można dodać akcję z etykietą, która będzie wyświetlana
                            // jako przycisk w snackbarze. Po kliknięciu tego przycisku, showSnackbar
                            // zwróci SnackbarResult.ActionPerformed, co pozwala na obsłużenie tej akcji.
                            actionLabel = "Open settings"
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            // Do uruchomienia ustawień aplikacji potrzebujemy intencji
                            // z odpowiednim URI, które wskazuje na ekran ustawień tej konkretnej aplikacji.
                            // Jest to szablonowy kod do otwierania ustawień aplikacji,
                            // który wykorzystuje Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            // oraz Uri.fromParts, aby zbudować URI z pakietem aplikacji.
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                            appSettingsLauncher.launch(intent)
                        }
                    }
                }



                // LaunchedEffect do obsługi nawigacji z powiadomienia.
                // Gdy currentIntent się zmieni (np. po kliknięciu powiadomienia),
                // ten efekt zostanie ponownie uruchomiony, a navController.handleDeepLink()
                // spróbuje obsłużyć intencję i nawigować do odpowiedniego ekranu.
                LaunchedEffect(currentIntent) {
                    currentIntent?.let {
                        Log.d("Navigation", "handleDeepLink uri=${it.data} action=${it.action}")
                        val handled = navController.handleDeepLink(it)
                        Log.d("Navigation", "handleDeepLink result=$handled")
                    }
                }


                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navController = navController, permissionGranted = permissionGranted)
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

    }

    /**
     * Sprawdza, czy aplikacja ma uprawnienie do wysyłania powiadomień. Jeśli nie, to uruchamia
     * requestPermissionLauncher, który wyświetli dialog z prośbą o przyznanie uprawnienia.
     * Jeśli uprawnienie jest już przyznane, to ustawia flagę permissionGranted na true i tworzy kanał
     * powiadomień. Funkcję można wywoływać wielokrotnie, np. w onResume(), aby upewnić się,
     * że stan uprawnień jest aktualny, zwłaszcza po powrocie z ustawień aplikacji.
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Tylko dla Androida 13 (API 33) i nowszych, ponieważ w tych wersjach wprowadzono nowe uprawnienie POST_NOTIFICATIONS.
            // Sprawdzamy, czy uprawnienie POST_NOTIFICATIONS jest przyznane. Jeśli nie, to uruchamiamy requestPermissionLauncher,
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                permissionGranted = true
                NotificationHelper.createNotificationChannel(this)
            }

        } else {
            // Dla starszych wersji Androida, uprawnienie do powiadomień jest przyznawane automatycznie podczas instalacji, więc ustawiamy flagę na true.
            permissionGranted = true
            NotificationHelper.createNotificationChannel(this)
        }
    }

    /**
     * Funkcja onNewIntent jest wywoływana, gdy aktywność jest już uruchomiona,
     * a system próbuje ponownie ją uruchomić z nową intencją (np. po kliknięciu powiadomienia).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("Navigation", "onNewIntent: ${intent.data}")
        currentIntent = intent
    }

    override fun onResume() {
        super.onResume()
        // Sprawdzamy uprawnienia do powiadomień za każdym razem, gdy aktywność staje się widoczna
        checkNotificationPermission()
    }
}
