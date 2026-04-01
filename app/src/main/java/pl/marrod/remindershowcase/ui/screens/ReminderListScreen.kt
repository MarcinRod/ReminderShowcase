package pl.marrod.remindershowcase.ui.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.twotone.Alarm
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.ui.reminder.ReminderBottomSheet
import pl.marrod.remindershowcase.ui.reminder.ReminderItemSimple
import pl.marrod.remindershowcase.ui.theme.ReminderShowcaseTheme
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ReminderListScreen(
    reminders: List<Reminder>,
    onDelete: (Reminder) -> Unit,
    onSave: (Reminder) -> Unit,
    onUpdate: (Reminder) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_reminders_title)) },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.TwoTone.Alarm,
                        contentDescription = "App Icon",
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.add_reminder)) },
                onClick = {
                    showBottomSheet = true
                    selectedReminder = null
                },
                icon = { Icon(Icons.Default.AlarmAdd, contentDescription = "Add reminder") }
            )
        }
    ) { innerPadding ->
        ReminderListContent(
            reminders = reminders,
            onDelete = { reminder ->
                Log.i("AppNavHost", "Deleting reminder: ${reminder.title}")
                onDelete(reminder)
            },
            onEditClick = {
                selectedReminder = it
                showBottomSheet = true
            },
            modifier = Modifier.padding(innerPadding)
        )
        if (showBottomSheet) {
            ReminderBottomSheet(
                reminder = selectedReminder,
                onDismiss = { showBottomSheet = false },
                onSave = { newReminder ->
                    if (selectedReminder == null) {
                        onSave(newReminder)
                    } else {
                        onUpdate(newReminder)
                    }
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
fun ReminderListContent(
    reminders: List<Reminder>,
    onDelete: (Reminder) -> Unit,
    onEditClick: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_reminders_msg), style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        var recomputeKey by remember { mutableStateOf(0) }
        var showPast by remember { mutableStateOf(true) }
        var revealedReminderId by remember { mutableStateOf<String?>(null) }

        val sortedFutureReminders = remember(reminders, recomputeKey) {
            reminders.filter { it.timestamp >= System.currentTimeMillis() }
                .sortedBy { it.timestamp }
        }
        val sortedPastReminders = remember(reminders, recomputeKey) {
            reminders.filter { it.timestamp < System.currentTimeMillis() }
                .sortedByDescending { it.timestamp }
        }
        val displayedReminders = remember(showPast, sortedFutureReminders, sortedPastReminders) {
            if (showPast) sortedFutureReminders + sortedPastReminders else sortedFutureReminders
        }
        val currentReminder = remember(sortedFutureReminders) {
            sortedFutureReminders.firstOrNull()
        }



        Column(modifier = modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))
            currentReminder?.let {
                ReminderListHeader(reminder = it) {
                    recomputeKey++
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.show_past_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = showPast,
                    onCheckedChange = {
                        showPast = it
                    },
                    colors = SwitchDefaults.colors(
                             checkedThumbColor = MaterialTheme.colorScheme.primary,
                             uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                             checkedTrackColor = MaterialTheme.colorScheme.surface,
                             uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                        checkedBorderColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            val scrollState = rememberLazyListState()

            LaunchedEffect(scrollState.isScrollInProgress) {
                if (scrollState.isScrollInProgress) {
                    Log.i("ReminderList", "Scroll started, hiding revealed item")
                    revealedReminderId = null
                }
            }

            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(displayedReminders, key = { it.id }) { reminder ->
                    ReminderItemSimple(
                        reminder = reminder,
                        isFromPast = reminder.timestamp < System.currentTimeMillis(),
                        isRevealed = revealedReminderId == reminder.id,
                        onReveal = { isRevealed ->
                            revealedReminderId = if (isRevealed) reminder.id
                            else null
                        },
                        onDelete = { onDelete(reminder) },
                        onEdit = { onEditClick(reminder) },
                        modifier = Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
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
    var progress by remember(reminder) { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    var timeLeft by remember(reminder) { mutableStateOf(reminder.timestamp - System.currentTimeMillis()) }
    LaunchedEffect(reminder) {
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
            onReminderFinish()
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

            val time: Pair<String, String> =
                timeLeft.milliseconds.toComponents { days, hours, minutes, seconds, _ ->
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
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
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
                    text = reminder.displayDateTime,
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


@Preview(
    showBackground = true, showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ReminderListScreenPreview() {
    val sampleReminders = listOf(
        Reminder(
            "1",
            "Buy milk",
            "Don't forget the low-fat one",
            System.currentTimeMillis() + 6 * 60 * 1000 + 5000
        ),
        Reminder("2", "Call Mom", "", System.currentTimeMillis() + 3600000),
        Reminder("3", "Gym", "Leg day!", System.currentTimeMillis() + 7200000)
    )
    ReminderShowcaseTheme {
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
            ReminderListContent(
                reminders = sampleReminders,
                onDelete = {},
                onEditClick = {}
            )
        }
    }
}
