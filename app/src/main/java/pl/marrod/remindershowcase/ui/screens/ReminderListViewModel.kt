package pl.marrod.remindershowcase.ui.screens

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.marrod.remindershowcase.ReminderShowcaseApplication
import pl.marrod.remindershowcase.data.Reminder
import kotlin.collections.firstOrNull

import kotlin.time.Duration.Companion.milliseconds


sealed class ScreenUiState {
    object Loading : ScreenUiState()
    data class Success(val uiState: ReminderListUiState) : ScreenUiState()
    data class Error(val message: String) : ScreenUiState()
}

data class ReminderListUiState(
    val reminders: List<Reminder> = emptyList(),
    val nextReminder: Reminder? = null,
    val reminderToEdit: Reminder? = null,
    val reminderToDelete: Reminder? = null,
    val showPastReminders: Boolean = true,
    val timeUntilNextReminder: TimeWithUnit = TimeWithUnit("0", "s"),
    val progressUntilNextReminder: Float = 0f,
    val showBottomSheet: Boolean = false,
    val animatedVisibility: Boolean = false,
) {
    val isEmpty: Boolean get() = reminders.isEmpty() && showPastReminders
    fun withNextReminder(next: Reminder?) = copy(
        nextReminder = next,
        timeUntilNextReminder = TimeWithUnit.fromTimestamp(next?.calculateTimeUntil()),
        progressUntilNextReminder = next?.calculateProgressUntil() ?: 0f
    )
}

data class TimeWithUnit(val value: String, val unit: String) {
    companion object {
        fun fromTimestamp(timestamp: Long?): TimeWithUnit {
            timestamp ?: return TimeWithUnit("0", "s")
            return timestamp.milliseconds.toComponents { days, hours, minutes, seconds, _ ->
                when {
                    days > 0 -> TimeWithUnit(days.toString(), "d")
                    hours > 0 -> TimeWithUnit(hours.toString(), "h")
                    minutes > 0 -> TimeWithUnit(minutes.toString(), "m")
                    else -> TimeWithUnit(seconds.toString(), "s")
                }
            }
        }
    }
}

class ReminderListViewModel(
    application: android.app.Application,
    private val storage: pl.marrod.remindershowcase.data.ReminderStorage
) : AndroidViewModel(application) {



    private val _screenState = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val screenState = _screenState.asStateFlow()

    private var timerJob: Job? = null
    private var allReminders: List<Reminder> = emptyList()

    init {


        viewModelScope.launch {
            delay(3000)
            loadReminders().let { resultState ->
                _screenState.update { resultState }
                if (resultState is ScreenUiState.Success) {
                    delay(50)
                    updateSuccess { it.copy(animatedVisibility = true) }
                    resultState.uiState.nextReminder?.let { restartTimer() }
                }
            }
        }

    }

    // Eliminates the if (state is Success) ... else state boilerplate
    private inline fun updateSuccess(block: (ReminderListUiState) -> ReminderListUiState) {
        _screenState.update { state ->
            if (state is ScreenUiState.Success) state.copy(uiState = block(state.uiState))
            else state
        }
    }

    // Eliminates the showPastReminders if-else split
    private fun buildDisplayedReminders(showPast: Boolean, checkTime: Long): List<Reminder> {
        val future = getFutureReminders(allReminders, checkTime)
        return if (showPast) future + getPastReminders(allReminders, checkTime) else future
    }

    fun restartTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch { updateJob() }
    }

    private suspend fun loadReminders(): ScreenUiState = withContext(Dispatchers.IO) {
        allReminders = storage.loadReminders()
        val checkTime = System.currentTimeMillis()
        val nextReminder = getNextReminder(allReminders, checkTime)

        ScreenUiState.Success(
            ReminderListUiState(
                reminders = buildDisplayedReminders(showPast = true, checkTime),
            ).withNextReminder(nextReminder)
        )
    }

    private fun getNextReminder(reminders: List<Reminder>, checkTime: Long): Reminder? {
        return getFutureReminders(reminders, checkTime).firstOrNull()
    }

    private fun getFutureReminders(reminders: List<Reminder>, checkTime: Long): List<Reminder> {
        return reminders.filter { it.timestamp >= checkTime }
            .sortedBy { it.timestamp }
    }

    private fun getPastReminders(reminders: List<Reminder>, checkTime: Long): List<Reminder> {
        return reminders.filter { it.timestamp < checkTime }
            .sortedByDescending { it.timestamp }
    }

    private suspend fun updateJob() {

        while (true) {
            delay(1000)
            val currentState = screenState.value as? ScreenUiState.Success ?: break
            val reminder = currentState.uiState.nextReminder
            reminder ?: break
            val checkTime = System.currentTimeMillis()
            val timeUntilNext = reminder.timestamp - checkTime

            if (timeUntilNext <= 0) {
                // reminder time has passed, update the state to show it as past
                updateSuccess { uiState ->
                    uiState.copy(
                        reminders = buildDisplayedReminders(
                            uiState.showPastReminders,
                            checkTime
                        )
                    )
                        .withNextReminder(getNextReminder(allReminders, checkTime))
                }
            } else {
                // update the time until next reminder
                updateSuccess { uiState ->
                    uiState.copy(
                        timeUntilNextReminder = TimeWithUnit.fromTimestamp(timeUntilNext),
                        progressUntilNextReminder = reminder.calculateProgressUntil()
                    )
                }
            }

        }
    }

    fun saveReminder(newReminder: Reminder) {
        val reminderToEdit = (screenState.value as? ScreenUiState.Success)?.uiState?.reminderToEdit

        // Update in-memory state right now
        allReminders = (allReminders.filter { it.id != reminderToEdit?.id } + newReminder)
            .sortedBy { it.timestamp }
        val checkTime = System.currentTimeMillis()
        updateSuccess { uiState ->
            uiState.copy(
                reminders = buildDisplayedReminders(uiState.showPastReminders, checkTime),
                showBottomSheet = false,
                reminderToEdit = null,
                reminderToDelete = null
            ).withNextReminder(getNextReminder(allReminders, checkTime))
        }
        restartTimer()

        // Persist in the background — UI is already updated
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<ReminderShowcaseApplication>()
            reminderToEdit?.let { old ->
                storage.deleteReminder(old.id)
                old.cancelNotification(context)
            }
            storage.addReminder(newReminder)
            newReminder.scheduleNotification(context)
        }
    }

    fun deleteReminder(reminder: Reminder) {

        allReminders = allReminders.filter { it.id != reminder.id }
        val checkTime = System.currentTimeMillis()
        updateSuccess { uiState ->
            uiState.copy(
                reminders = buildDisplayedReminders(uiState.showPastReminders, checkTime),
                reminderToDelete = null
            ).withNextReminder(getNextReminder(allReminders, checkTime))
        }
        restartTimer()
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<ReminderShowcaseApplication>()
            reminder.cancelNotification(context)
            storage.deleteReminder(reminder.id)
        }
    }

    fun editReminder(reminder: Reminder) {
        updateSuccess { it.copy(reminderToEdit = reminder, showBottomSheet = true) }
    }

    fun hideBottomSheet() {
        updateSuccess { it.copy(showBottomSheet = false, reminderToEdit = null) }
    }


    fun toggleDeleteIndication(reminder: Reminder?) {
        updateSuccess { it.copy(reminderToDelete = reminder) }
    }

    fun setShowPastReminders(checked: Boolean) {
        updateSuccess { uiState ->
            val checkTime = System.currentTimeMillis()
            uiState.copy(
                showPastReminders = checked,
                reminders = buildDisplayedReminders(checked, checkTime)
            )
        }
    }

    fun showAddBottomDialog() {
        updateSuccess { it.copy(showBottomSheet = true) }
    }
}