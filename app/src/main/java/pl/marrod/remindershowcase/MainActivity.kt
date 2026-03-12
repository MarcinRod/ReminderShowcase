package pl.marrod.remindershowcase

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.marrod.remindershowcase.ui.navigation.AppNavHost
import pl.marrod.remindershowcase.ui.navigation.Routes
import pl.marrod.remindershowcase.ui.theme.ReminderShowcaseTheme

// TODO: dodać animacje przejścia między ekranami
// TODO: poprawić UI/UX -dialog przy usuwaniu (usuwanie w stylu swipe to delete)
// TODO: ustawianie czasu przez time picker, a nie ręczne wpisywanie minut
// TODO: kilknięcie w powiadomienie otwiera ekran informacji o reminder

class MainActivity : ComponentActivity() {
    var permissionGranted = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  isGranted ->
        if (isGranted) {
            permissionGranted = true
        } else {
            // Check if the user has permanently denied the permission. This will happen if the user
            // denied the permission and selected the "Don't ask again" option. This is reflected by
            // shouldShowRequestPermissionRationale() returning false.
            val permanentlyDenied =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    )
                } else false

            if (permanentlyDenied) {
                // show toast
                Toast.makeText(
                    this,
                    "Notification permission permanently denied. Please enable it in settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()

        setContent {
            ReminderShowcaseTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                val title = stringResource(Routes.titleRes(currentRoute))
                val showBack = currentRoute == Routes.add || currentRoute == Routes.edit

                @OptIn(ExperimentalMaterial3Api::class)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(title) },
                            navigationIcon = {
                                if (showBack) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (currentRoute == Routes.list) {
                                    IconButton(onClick = {
                                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                    }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        if (currentRoute == Routes.list) {
                            FloatingActionButton(onClick = { navController.navigate(Routes.add) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        innerPadding = innerPadding,
                        onShareClick = { reminder ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Reminder: ${reminder.title}\n${reminder.description}")
                                type = "text/plain"
                            }
                            startActivity(Intent.createChooser(sendIntent, "Share Reminder"))
                        }
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
