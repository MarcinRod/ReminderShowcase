package pl.marrod.remindershowcase.ui.reminder

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
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
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.ReminderStorage
import toDisplayDateTime
import java.util.*
import kotlin.math.roundToInt

/**
 * Unified add / edit screen.
 *
 * - [reminderId] == null  →  "Add" mode: all fields start empty, a new Reminder is created on save.
 * - [reminderId] != null  →  "Edit" mode: fields are pre-filled from storage, the existing entry
 *                            is replaced and its alarm is rescheduled on save.
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
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
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
                label = { Text("Time of reminder") },
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
                    ))
                        .copy(title = title, description = description, timestamp = timestamp)

                    //    if (existing != null) storage.deleteReminder(existing.id)
                    // storage.addReminder(reminder)
                    //  reminder.scheduleNotification(context)
                    onSaved(reminder)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank()
        ) {
            Text(if (existing == null) "Save & Schedule Reminder" else "Save Changes")
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
fun ReminderItem(
    reminder: Reminder,
    isRevealed: Boolean,
    onReveal: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    val density = LocalDensity.current
    val revealWidthPx = with(density) { 56.dp.toPx() }
    val dismissThresholdPx = revealWidthPx * 2.5f

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    fun snapToRevealed() {
        scope.launch {
            offsetX.animateTo(
                -revealWidthPx,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            )
        }
    }

    fun snapToHidden() {
        scope.launch {
            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    fun settle() {
        val current = offsetX.value
        when {
            current < -dismissThresholdPx -> {
                Log.i("ReminderItem", "Dismissing reminder with id ${reminder.id}")
                scope.launch {
                    offsetX.animateTo(-3000f, tween(300, easing = FastOutLinearInEasing))
                    Log.i(
                        "ReminderItem",
                        "Finished animation for reminder with id ${reminder.id}, now calling onDelete()"
                    )
                    onDelete()
                    Log.i(
                        "ReminderItem",
                        "Called onDelete() for reminder with id ${reminder.id}, waiting for animation to finish..."
                    )
                    onReveal(false)
                }

            }

            current > -(revealWidthPx / 2f) -> {
                scope.launch {
                    offsetX.animateTo(
                        0f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                    )
                }
                onReveal(false)
            }

            else -> snapToRevealed() // Snap back to revealed if it's past the halfway point but not far enough to dismiss
        }
    }
    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            snapToHidden()
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        // Red delete background revealed behind the card
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CardDefaults.shape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                // Settle helper — shared by both gesture blocks
                .let { thisModifier ->

                    // Block 1: long press → reveal, then drag in the SAME gesture
                    thisModifier
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    if (!isRevealed) {
                                        onReveal(true)
                                        snapToRevealed()
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(
                                            (offsetX.value + dragAmount.x).coerceAtMost(
                                                0f
                                            )
                                        )
                                    }
                                },
                                onDragEnd = { settle() },
                                onDragCancel = { settle() }
                            )
                        }
                        // Block 2: tap to close when already revealed
                        .pointerInput(isRevealed) {
                            if (!isRevealed) return@pointerInput
                            detectTapGestures(onTap = {
                                onReveal(false)
                                scope.launch {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            Spring.DampingRatioMediumBouncy,
                                            Spring.StiffnessMedium
                                        )
                                    )
                                }
                            })
                        }
                        // Block 3: swipe when already revealed (finger lifted after long press, new gesture)
                        .pointerInput(isRevealed) {
                            if (!isRevealed) return@pointerInput
                            detectHorizontalDragGestures(
                                onDragEnd = { settle() },
                                onHorizontalDrag = { change, delta ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo((offsetX.value + delta).coerceAtMost(0f))
                                    }
                                }
                            )
                        }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
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
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reminder.displayDateTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
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
    val containerColor = if (!isFromPast) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val textColor = if (!isFromPast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    fun snapToRevealed() {
        scope.launch {
            offsetX.animateTo(
                -revealWidthPx,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            )
        }
        onReveal(true)
    }

    fun settle() {
        val current = offsetX.value
        when {
            current < -dismissThresholdPx -> {

                scope.launch {
                    offsetX.animateTo(-3000f, tween(300, easing = FastOutLinearInEasing))
                    onDelete()
                    onReveal(false)
                }
            }

            current > -(revealWidthPx / 2f) -> {
                onReveal(false)
                scope.launch {
                    offsetX.animateTo(
                        0f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                    )
                }
            }

            else -> snapToRevealed() // Snap back to revealed if it's past the halfway point but not far enough to dismiss
        }
    }

    fun snapToHidden() {
        scope.launch {
            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            snapToHidden()
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        // Red delete background revealed behind the card
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
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { _ ->
                            Log.i(
                                "ReminderItemSimple",
                                "Drag started for reminder with id ${reminder.id}, isRevealed = $isRevealed"
                            )
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            Log.i(
                                "ReminderItemSimple",
                                "Dragging reminder with id ${reminder.id}, dragAmount = $dragAmount"
                            )
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceAtMost(0f))
                            }
                        },
                        onDragEnd = {
                            Log.i(
                                "ReminderItemSimple",
                                "Drag ended for reminder with id ${reminder.id}, settling..."
                            )
                            settle()
                        },
                        onDragCancel = {
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
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    if (page == 0) {
                        TextButton(onClick = {
                            page = 1
                        }) { Text("Next") }
                    } else {

                        TextButton(onClick = {
                            page = 0
                        }) { Text("Back") }
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
                        }) { Text("OK") }

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
