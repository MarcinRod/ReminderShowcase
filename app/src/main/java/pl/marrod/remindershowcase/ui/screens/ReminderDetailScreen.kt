package pl.marrod.remindershowcase.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.ui.navigation.Destination
import pl.marrod.remindershowcase.ui.theme.ReminderShowcaseTheme
import pl.marrod.remindershowcase.utils.toDisplayDateTime

class ReminderDetailViewModel(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    storage: ReminderStorage
) : ViewModel() {
    var route = savedStateHandle.toRoute<Destination.Details>()
    val reminderId: String = route.reminderId
    val reminder = storage.loadReminders().find { it.id == reminderId }
}

@Composable
fun ReminderDetailScreen(
    reminderId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contxt = LocalContext.current
    val reminder = remember(reminderId) {
        ReminderStorage(contxt).loadReminders().find { it.id == reminderId }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(reminder?.title ?: "Broken link") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.ArrowBack,
                            contentDescription = "Back Icon",
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (reminder == null) {
            Text(
                text = "The reminder with the provided ID could not be found.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                textAlign = TextAlign.Center
            )
        } else {
            ReminderDetailsContents(
                reminder = reminder,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun ReminderDetailsContents(reminder: Reminder, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Title: ${reminder.title}")
                Text(text = "Description: ${reminder.description}")
                Text(text = "Time: ${reminder.timestamp.toDisplayDateTime()}")
            }
        }
    }
}

@Preview
@Composable
fun ReminderDetailScreenPreview() {
    ReminderShowcaseTheme {
        val reminder = Reminder(
            id = "reminder_1",
            title = "Sample Reminder",
            description = "This is a sample reminder for preview purposes.",
            timestamp = System.currentTimeMillis() + 3600000L // 1 hour from now
        )
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(reminder.title) },
                    navigationIcon = {
                        IconButton(
                            onClick = { }
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.ArrowBack,
                                contentDescription = "Back Icon",
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            ReminderDetailsContents(
                reminder = reminder,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}