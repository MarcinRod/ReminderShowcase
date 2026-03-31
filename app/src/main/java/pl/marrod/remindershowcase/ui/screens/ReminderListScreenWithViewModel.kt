package pl.marrod.remindershowcase.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.twotone.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.factory.AppWideViewModelProvider
import pl.marrod.remindershowcase.ui.reminder.ReminderBottomSheet
import pl.marrod.remindershowcase.ui.reminder.ReminderItemSimple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderListViewModel = viewModel(factory = AppWideViewModelProvider.factory)
) {
    val screenState by viewModel.screenState.collectAsState()

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My reminders") },
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
            if(screenState is ScreenUiState.Success) {
                ExtendedFloatingActionButton(
                    text = { Text("Add reminder") },
                    onClick = {
                        viewModel.showAddBottomDialog()
                    },
                    icon = { Icon(Icons.Default.AlarmAdd, contentDescription = "Add reminder") }
                )
            }
        }
    ) { innerPadding ->
        when (screenState) {
            is ScreenUiState.Loading -> {

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

            is ScreenUiState.Success -> {

                val uiState = (screenState as ScreenUiState.Success).uiState
           //     val showBottomSheet by viewModel.showBottomSheet.collectAsState()
            //    val animatedVisibility by viewModel.animatedVisibility.collectAsState()
                AnimatedVisibility(
                    visible = uiState.animatedVisibility,
                    enter = fadeIn() +  slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(500)
                    ),
                    exit = fadeOut() +  slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(500)
                    ),
                    modifier = Modifier
                ) {

                    if (uiState.isEmpty) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reminders yet. Tap the button below to add your first reminder!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    } else {
                        ReminderListContent(
                            reminders = uiState.reminders,
                            nextReminder = uiState.nextReminder,
                            progress = uiState.progressUntilNextReminder,
                            timeLeft = uiState.timeUntilNextReminder,
                            showPastReminders = uiState.showPastReminders,
                            reminderToDelete = uiState.reminderToDelete,
                            onShowPastRemindersChange = { viewModel.setShowPastReminders(it) },
                            onToggleDeleteIndication = { reminder ->
                                viewModel.toggleDeleteIndication(reminder)
                            },
                            onDelete = { reminder ->
                                viewModel.deleteReminder(reminder)
                            },
                            onEditClick = { reminder ->
                                viewModel.editReminder(reminder)

                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
                if (uiState.showBottomSheet) {
                    ReminderBottomSheet(
                        reminder = uiState.reminderToEdit,
                        onDismiss = { viewModel.hideBottomSheet() },
                        onSave = { newReminder ->
                            viewModel.saveReminder(newReminder)
                        }
                    )
                }

            }


            is ScreenUiState.Error -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    Text(text = (screenState as ScreenUiState.Error).message)
                }
            }
        }


    }
}


@Composable
fun ReminderListContent(
    reminders: List<Reminder>,
    nextReminder: Reminder?,
    progress: Float,
    timeLeft: TimeWithUnit,
    showPastReminders: Boolean,
    reminderToDelete: Reminder?,
    onShowPastRemindersChange: (Boolean) -> Unit,
    onToggleDeleteIndication: (Reminder?) -> Unit,
    onDelete: (Reminder) -> Unit,
    onEditClick: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {


    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        nextReminder?.let {
            ReminderListHeader(
                reminder = it,
                progress = progress,
                time = timeLeft
            )
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
                text = "Show past reminders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = showPastReminders,
                onCheckedChange = {
                    onShowPastRemindersChange(it)
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
                onToggleDeleteIndication(null)
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderItemSimple(
                    reminder = reminder,
                    isFromPast = reminder.timestamp < System.currentTimeMillis(),
                    isRevealed = reminderToDelete?.equals(reminder.id) ?: false,
                    onReveal = { isRevealed ->
                        onToggleDeleteIndication(if (isRevealed) reminder else null)
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

@Composable
fun ReminderListHeader(
    reminder: Reminder,
    progress: Float,
    time: TimeWithUnit,
    modifier: Modifier = Modifier
) {

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

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
                    text = time.value,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = time.unit,
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
