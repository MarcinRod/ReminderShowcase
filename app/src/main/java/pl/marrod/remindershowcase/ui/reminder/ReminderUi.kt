package pl.marrod.remindershowcase.ui.reminder

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.utils.toDisplayDateTime
import java.util.*
import kotlin.math.roundToInt

/**
 *   Formularz do dodawania lub edycji przypomnienia.
 *   Jeśli [reminderId] jest null, formularz działa w trybie "Dodaj",
 *   a jeśli [reminderId] jest nie-null, formularz działa w trybie "Edytuj" i wstępnie wypełnia
 *   pola danymi z istniejącego przypomnienia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderForm(
    onSaved: (Reminder) -> Unit,
    modifier: Modifier = Modifier,
    reminderId: String? = null
) {
    val context = LocalContext.current
    val storage = remember { ReminderStorage(context) }

    val existing = remember(reminderId) {
        reminderId?.let { id -> storage.loadReminders().find { it.id == id } }
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }

    var timestamp by remember {
        mutableStateOf(
            existing?.timestamp ?: (System.currentTimeMillis() + 1 * 60 * 60 * 1000L)
        )
    }
    var reminderDateTime by remember { mutableStateOf(timestamp.toDisplayDateTime()) }

    var showTimePicker by remember { mutableStateOf(false) }


    if (showTimePicker) {
        DateTimePickerDialog(
            timestamp = timestamp,
            properties = DialogProperties(dismissOnClickOutside = true),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
            onDismiss = { showTimePicker = false },
            colors = DatePickerDefaults.colors(),
            onConfirm = {
                timestamp = it
                reminderDateTime = timestamp.toDisplayDateTime()
                showTimePicker = false
            }
        )

    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = true,
                    onClick = { showTimePicker = true }
                )) {
            OutlinedTextField(
                value = reminderDateTime,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.time_of_reminder)) },
                trailingIcon = {
                    IconButton(
                        onClick = { showTimePicker = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = "Edit reminder time"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),

                )
        }

        Button(
            onClick = {
                if (title.isNotBlank()) {


                    val reminder = (existing ?: Reminder(
                        id = UUID.randomUUID().toString(),
                        title = "",
                        description = "",
                        timestamp = 0
                    )).copy(title = title, description = description, timestamp = timestamp)
                    onSaved(reminder)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank()
        ) {
            Text(
                if (existing == null) stringResource(R.string.save_schedule_reminder) else stringResource(
                    R.string.save_changes
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    onDismiss: () -> Unit,
    reminder: Reminder?,
    onSave: (Reminder) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // sheetState pozwala nam kontrolować zachowanie bottom sheetu,
        // np. czy może być częściowo rozsunięty (skipPartiallyExpanded)
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderForm(
                onSaved = { onSave(it) },
                reminderId = reminder?.id
            )
        }
    }
}

@Composable
fun ReminderItemSimple(
    reminder: Reminder,
    isFromPast: Boolean,
    isRevealed: Boolean,
    onReveal: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {

    val density = LocalDensity.current
    val revealWidthPx = with(density) { 56.dp.toPx() }
    val dismissThresholdPx = revealWidthPx * 3f
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val containerColor =
        if (!isFromPast) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val textColor =
        if (!isFromPast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.6f
        )

    fun snapToRevealed() {
        scope.launch {
            offsetX.animateTo(
                -revealWidthPx,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            )
        }
        onReveal(true)
    }
    fun snapToHidden() {
        scope.launch {
            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    fun settle() {
        // zapisz aktualny stan przesunięcia i zdecyduj,
        // czy karta powinna zostać usunięta, wrócić do pozycji początkowej, czy pozostać odsłonięta
        val current = offsetX.value
        when {
            current < -dismissThresholdPx -> {
                // Przekroczyliśmy próg usunięcia, animuj kartę poza ekran i wywołaj onDelete
                scope.launch {
                    offsetX.animateTo(-3000f, tween(300, easing = FastOutLinearInEasing))
                    onDelete()
                    onReveal(false)
                }
            }

            current > -(revealWidthPx / 2f) -> {
                // Przesunięcie jest zbyt małe, wróć do pozycji początkowej
                onReveal(false)
                snapToHidden()
            }
            // Przesunięcie jest wystarczające, ale nie przekracza progu usunięcia
            else -> snapToRevealed() // powrót karty do pozycji z odsłoniętą ikoną "Usuń"
        }
    }



    // Efekt pozwalający zmienić stan odsłonięcia karty w reakcji na zmianę wartości isRevealed.
    // Jeśli isRevealed stanie się false, karta zostanie automatycznie przesunięta z powrotem
    // do pozycji ukrytej. W tej aplikacji zdarzy się to:
    // - gdy użytkownik odsłoni kartę jednego przypomnienia, a następnie spróbuje odsłonić inną kartę
    // - gdy użytkownik zacznie przesuwać listę
    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            snapToHidden()
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        // Czerwone tło z ikoną "Usuń", które jest widoczne, gdy karta jest odsłonięta
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CardDefaults.shape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.TwoTone.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) { // specjalny modyfikator do obsługi gestów dotykowych
                    // Gesty możliwe do wykrycia rozpoczynają się od przedrostka detect,
                    // np. detectTapGestures, detectDragGestures, detectHorizontalDragGestures itp.
                    detectHorizontalDragGestures(  // Wykrywamy tylko gesty poziomego przeciągania,
                        // aby uniknąć konfliktów z przewijaniem listy
                        onDragStart = { _ ->
                            // rozpoczęcie przeciągania
                            // W tym kodzie tylko logujemy, ale można tu dodać dodatkową logikę,
                            // np. informowanie rodzica o rozpoczęciu przeciągania
                            Log.i(
                                "ReminderItemSimple",
                                "Drag started for reminder with id ${reminder.id}, isRevealed = $isRevealed"
                            )
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // podczas przeciągania, aktualizujemy offsetX o wartość dragAmount
                            // (jest to różnica w pikselach od ostatniego zdarzenia przeciągania)
                            Log.i(
                                "ReminderItemSimple",
                                "Dragging reminder with id ${reminder.id}, dragAmount = $dragAmount"
                            )
                            // consume() informuje system, że ten gest został obsłużony i nie powinien być przekazywany dalej
                            change.consume()
                            // Aktualizujemy offsetX, ale ograniczamy go do wartości 0 (nie pozwalamy przesuwać karty w prawo)
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceAtMost(0f))
                            }
                        },
                        onDragEnd = {
                            // zakończenie przeciągania, decydujemy, czy karta powinna zostać usunięta,
                            // wrócić do pozycji początkowej, czy pozostać odsłonięta.
                            // Wystąpi, gdy użytkownik puści kartę po przeciągnięciu.
                            Log.i(
                                "ReminderItemSimple",
                                "Drag ended for reminder with id ${reminder.id}, settling..."
                            )
                            settle()
                        },
                        onDragCancel = {
                            // przeciąganie zostało anulowane (np. przez system lub inny gest),
                            // karta powinna wrócić do pozycji początkowej
                            Log.i(
                                "ReminderItemSimple",
                                "Drag cancelled for reminder with id ${reminder.id}, settling..."
                            )
                            settle()
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = reminder.displayDateTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,

                            )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.TwoTone.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
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
    var page by remember { mutableStateOf(0) }
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

// ──────────────────────────── Previews ────────────────────────────

@Preview(showBackground = true, name = "ReminderForm – Add mode")
@Composable
private fun ReminderFormAddPreview() {
    ReminderForm(
        onSaved = {}
    )
}

@Preview(showBackground = true, name = "ReminderForm – Edit mode")
@Composable
private fun ReminderFormEditPreview() {
    ReminderForm(
        onSaved = {},
        reminderId = "preview-id"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "ReminderBottomSheet")
@Composable
private fun ReminderBottomSheetPreview() {
    ReminderBottomSheet(
        onDismiss = {},
        onSave = {},
        reminder = Reminder(
            id = "preview-id",
            title = "Buy groceries",
            description = "Milk, eggs, bread, and coffee",
            timestamp = System.currentTimeMillis() + 60_000L
        ),
    )
}
