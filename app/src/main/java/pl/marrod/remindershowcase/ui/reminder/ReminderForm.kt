package pl.marrod.remindershowcase.ui.reminder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.data.Reminder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Formularz do dodawania lub edycji przypomnienia.
 * Jeśli [reminder] jest null, formularz działa w trybie "Dodaj",
 * a jeśli [reminder] jest nie-null, formularz działa w trybie "Edytuj" i wstępnie wypełnia
 * pola danymi z istniejącego przypomnienia. W tej wersji wykorzystano ViewModel
 * do zarządzania stanem formularza, co pozwala na łatwiejsze zarządzanie formularzem i jego ewntualne rozszerzenie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderForm(
    onSaved: (Reminder) -> Unit,
    modifier: Modifier = Modifier,
    reminder: Reminder? = null
) {

    val viewModelKey = remember(reminder?.id){ reminder?.id ?: UUID.randomUUID().toString() }
    // Używamy klucza viewModelKey, żeby dla każdego przypomnienia
    // tworzony był osobny ViewModel — nowy dla dodawania, inny dla każdej edycji.
    val viewModel: ReminderFormViewModel = viewModel(
        key = viewModelKey,
        factory = ReminderFormViewModel.factory(reminder)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showDateTimePicker) {
        DateTimePickerDialog(
            timestamp = uiState.timestamp,
            properties = DialogProperties(dismissOnClickOutside = true),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
            onDismiss = { viewModel.hideDateTimePicker() },
            colors = DatePickerDefaults.colors(),
            onConfirm = { viewModel.onTimestampChange(it) }
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.onTitleChange(it) },
            label = { Text(stringResource(R.string.title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.onDescriptionChange(it) },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showDateTimePicker() }
        ) {
            OutlinedTextField(
                value = uiState.formattedDateTime,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.time_of_reminder)) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.showDateTimePicker() }) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = "Edit reminder time"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                viewModel.buildReminder()?.let { onSaved(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSaveEnabled
        ) {
            Text(
                if (uiState.isEditMode) stringResource(R.string.save_changes)
                else stringResource(R.string.save_schedule_reminder)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    timestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties,
    shape: Shape,
    tonalElevation: Dp,
    colors: DatePickerColors,
) {
    var page by rememberSaveable { mutableStateOf(0) }
    val initialDateTime = remember(timestamp) {
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    val todayMidnight = remember {
        LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = timestamp,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMidnight
            override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = true
    )
    val containerHeight = 568.0.dp
    val containerWidth = 360.0.dp
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.wrapContentHeight(),
        properties = properties,
    ) {
        Surface(
            modifier =
                Modifier
                    .requiredWidth(containerWidth)
                    .heightIn(max = containerHeight),
            shape = shape,
            color = colors.containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (page == 0)
                    DatePicker(state = datePickerState, modifier = Modifier.weight(1f))
                else {
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(modifier = Modifier.weight(1f))
                    if (page == 0) {
                        TextButton(onClick = {
                            page = 1
                        }) { Text(stringResource(R.string.next)) }
                    } else {

                        TextButton(onClick = {
                            page = 0
                        }) { Text(stringResource(R.string.back)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            val selectedDateMillis =
                                datePickerState.selectedDateMillis ?: timestamp
                            // DatePicker returns UTC-midnight millis — extract the date in UTC,
                            // then attach the chosen local time and convert back to epoch millis.
                            val pickedDate = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val pickedDateTime =
                                pickedDate.atTime(timePickerState.hour, timePickerState.minute)
                            onConfirm(
                                pickedDateTime.atZone(ZoneId.systemDefault())
                                    .toEpochSecond() * 1000L
                            )
                        }) { Text(stringResource(R.string.ok)) }

                    }
                }
            }
        }
    }
}
