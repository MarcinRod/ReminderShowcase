package pl.marrod.remindershowcase.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.ReminderStorage
import pl.marrod.remindershowcase.ui.theme.ReminderShowcaseTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ReminderListScreen(
    innerPadding: PaddingValues,
    onShareClick: (Reminder) -> Unit,
    onEditClick: (Reminder) -> Unit
) {
    val context = LocalContext.current
    val storage = remember { ReminderStorage(context) }
    var reminders by remember { mutableStateOf(storage.loadReminders()) }

    ReminderListContent(
        reminders = reminders,
        innerPadding = innerPadding,
        onDelete = { reminder ->
            storage.deleteReminder(reminder.id)
            reminders = storage.loadReminders()
        },
        onShareClick = onShareClick,
        onEditClick = onEditClick
    )
}

@Composable
fun ReminderListContent(
    reminders: List<Reminder>,
    innerPadding: PaddingValues,
    onDelete: (Reminder) -> Unit,
    onShareClick: (Reminder) -> Unit,
    onEditClick: (Reminder) -> Unit
) {
    if (reminders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No reminders yet. Tap + to add one!", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        var sortedReminders = remember(reminders) { reminders.sortedBy { it.timestamp } }
        var currentReminder by remember { mutableStateOf(reminders.firstOrNull()) }
        Column(modifier = Modifier.padding(innerPadding)) {
            Spacer(modifier = Modifier.height(16.dp))
            // TODO: update the reminder when the next one becomes the "soonest"
            currentReminder?.let { it ->
                ReminderListHeader(reminder = it) {
                    sortedReminders = sortedReminders.drop(1)
                    currentReminder = sortedReminders.firstOrNull()

                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onDelete = { onDelete(reminder) },
                        onShare = { onShareClick(reminder) },
                        onEdit = { onEditClick(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderListHeader(
    reminder: Reminder,
    modifier: Modifier = Modifier,
    onReminderFinish: () -> Unit = { }
) {
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    var timeLeft by remember { mutableStateOf(reminder.timestamp - System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val totalDuration = reminder.timestamp - reminder.createdAtTimestamp
            var elapsed = System.currentTimeMillis() - reminder.createdAtTimestamp
            while (elapsed < totalDuration) {
                progress = (elapsed / totalDuration.toFloat()).coerceIn(0f, 1f)
                delay(1000L) // Update every second
                elapsed = System.currentTimeMillis() - reminder.createdAtTimestamp
                timeLeft = reminder.timestamp - System.currentTimeMillis()
            }
            progress = 1f // Ensure it reaches 100% when done
            timeLeft = 0L
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

                val time: Pair<String, String> = timeLeft.milliseconds.toComponents { days, hours, minutes, seconds, _ ->
                    when {
                        days > 0 -> "$days" to "d"
                        hours > 0 -> "$hours" to "h"
                        minutes > 0 -> "$minutes" to "m"
                        else -> "$seconds" to "s"
                    }
                }
                Box(modifier = Modifier.fillMaxHeight()) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        text = time.first,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = time.second,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp)
                    )

                }


            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(
                        Date(
                            reminder.timestamp
                        )
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(reminder.timestamp))

    val density = LocalDensity.current
    val revealWidthPx = with(density) { 56.dp.toPx() }
    val dismissThresholdPx = revealWidthPx * 2.5f

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    // TODO: isRevelaed should be shared between all items,
    //  not per-item (currently if you open one, then open another,
    //  the first one will not "lose" its state and snap back closed and it should)
    var isRevealed by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }

    val snapToRevealed: () -> Unit = {
        scope.launch {
            offsetX.animateTo(-revealWidthPx, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
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
                .let  { mod ->
                    val settle: () -> Unit = {
                        val current = offsetX.value
                        when {
                            current < -dismissThresholdPx -> {
                                isRevealed = false
                                scope.launch {
                                    offsetX.animateTo(-3000f, tween(300, easing = FastOutLinearInEasing))
                                    onDelete()
                                }
                            }
                            current > -(revealWidthPx / 2f) -> {
                                isRevealed = false
                                scope.launch {
                                    offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                                }
                            }
                            else -> snapToRevealed() // Snap back to revealed if it's past the halfway point but not far enough to dismiss
                        }
                    }
                    // Block 1: long press → reveal, then drag in the SAME gesture
                    mod
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    if (!isRevealed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isRevealed = true
                                        snapToRevealed()
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo((offsetX.value + dragAmount.x).coerceAtMost(0f))
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
                                isRevealed = false
                                scope.launch {
                                    offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
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
                            text = dateString,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true, showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ReminderListScreenPreview() {
    val sampleReminders = listOf(
        Reminder("1", "Buy milk", "Don't forget the low-fat one", System.currentTimeMillis() + 6*60*1000 + 5000),
        Reminder("2", "Call Mom", "", System.currentTimeMillis() + 3600000),
        Reminder("3", "Gym", "Leg day!", System.currentTimeMillis() + 7200000)
    )
    ReminderShowcaseTheme {
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
            ReminderListContent(
                reminders = sampleReminders,
                innerPadding = PaddingValues(0.dp),
                onDelete = {},
                onShareClick = {},
                onEditClick = {}
            )
        }
    }
}
