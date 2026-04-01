package pl.marrod.remindershowcase.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.factory.AppWideViewModelProvider
import pl.marrod.remindershowcase.ui.navigation.Destination
import pl.marrod.remindershowcase.ui.theme.ReminderShowcaseTheme
import pl.marrod.remindershowcase.utils.asString
import pl.marrod.remindershowcase.utils.toDisplayDateTime


/**
 * Ekran szczegółów przypomnienia, który korzysta z ViewModel do zarządzania stanem UI.
 */
@Composable
fun ReminderDetailScreen(
    viewModel: ReminderDetailViewModel = viewModel(factory = AppWideViewModelProvider.factory),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pobieramy aktualny stan UI z ViewModelu, korzystając z funkcji collectAsState(),
    // która pozwala na obserwowanie zmian w StateFlow i automatyczne odświeżanie UI, gdy stan się zmienia.
    val uiState by viewModel.uiState.collectAsState()
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title.asString()) },
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
        when(uiState){
            // w uiState przewidujemy dwa stany - Success i Error.
            // Dzięki temu, że uiState jest typu ReminderDetailUiState (sealed interface),
            // kompilator wymusza obsługę wszystkich możliwych stanów, co zwiększa bezpieczeństwo
            // i czytelność kodu.
            is ReminderDetailUiState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = (uiState as ReminderDetailUiState.Error).message.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        textAlign = TextAlign.Center
                    )
                }
            }
            is ReminderDetailUiState.Success -> {
                val reminder = (uiState as ReminderDetailUiState.Success).reminder
                ReminderDetailsContents(
                    reminder = reminder,
                    modifier = modifier.padding(innerPadding)
                )
            }

            ReminderDetailUiState.Loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(innerPadding)
                            .size(96.dp)
                            .align(Alignment.Center)
                    )
                }
            }
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
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f),
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
                Text(text = stringResource(R.string.detail_title_format, reminder.title))
                Text(text = stringResource(R.string.detail_description_format, reminder.description))
                Text(text = stringResource(
                    R.string.detail_time_format,
                    reminder.timestamp.toDisplayDateTime()
                ))
            }
        }
    }
}


