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



// TODO: zasymolwać długotrwały zapis do pliku, żeby pokazać działanie korutyn
// TODO: Poczyścić kod
// TODO: dodać ViewModel do zarządzania stanem (architektura MVVM)
class MainActivity : ComponentActivity() {
    var permissionGranted by mutableStateOf(false)
    var showPermissionRationale by mutableStateOf(false)
    var currentIntent by mutableStateOf<Intent?>(null)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionGranted = true
        } else {
            // Check if the user has permanently denied the permission. This will happen if the user
            // denied the permission and selected the "Don't ask again" option. This is reflected by
            // shouldShowRequestPermissionRationale() returning false.
            showPermissionRationale=
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    )
                } else false

           
        }
    }
    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if the permission is granted after returning from app settings
        checkNotificationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()

        setContent {
            ReminderShowcaseTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(currentIntent) {
                    currentIntent?.let {
                        Log.d("Navigation", "handleDeepLink uri=${it.data} action=${it.action}")
                        val handled = navController.handleDeepLink(it)
                        Log.d("Navigation", "handleDeepLink result=$handled")
                    }
                }

                LaunchedEffect(permissionGranted) {
                    if (!permissionGranted) {
                        val result = snackbarHostState.showSnackbar(
                            message = "Notification permission is required for reminders",
                            actionLabel = "Open settings"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                            appSettingsLauncher.launch(intent)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navController = navController)
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
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
            } else {
                permissionGranted = true
                showPermissionRationale = false
                NotificationHelper.createNotificationChannel(this)
                
            }

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("Navigation", "onNewIntent: ${intent.data}")
        currentIntent = intent
    }

    override fun onResume() {
        super.onResume()
        // Check if the permission is granted when returning to the activity
        checkNotificationPermission()
    }
}
