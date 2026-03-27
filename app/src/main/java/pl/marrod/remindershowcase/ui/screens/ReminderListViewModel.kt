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
    val isEmpty: Boolean = reminders.isEmpty()
)

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

    private val _showBottomDSheet = MutableStateFlow(false)
    val showBottomSheet = _showBottomDSheet.asStateFlow()

    private val _animatedVisibility = MutableStateFlow(false)
    val animatedVisibility = _animatedVisibility.asStateFlow()
    private var timerJob: Job? = null

    init {

        viewModelScope.launch {
            delay(3000) // Simulate loading delay
            loadReminders().let { resultState ->
                _screenState.update { resultState }
                if (resultState is ScreenUiState.Success) {
                    delay(50) // Small delay to ensure the UI is ready before starting animations
                    _animatedVisibility.update { true }
                    resultState.uiState.nextReminder?.let {
                        restartTimer()
                    }
                }
            }
        }
    }

    fun restartTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch { updateJob() }
    }

    private suspend fun loadReminders(): ScreenUiState = withContext(Dispatchers.IO) {
        val allReminders = storage.loadReminders()
        val checkTime = System.currentTimeMillis()
        val futureReminders = getFutureReminders(allReminders, checkTime)
        val pastReminders = getPastReminders(allReminders, checkTime)
        val nextReminder = getNextReminder(futureReminders, checkTime)

        ScreenUiState.Success(
            ReminderListUiState(
                reminders = futureReminders + pastReminders,
                nextReminder = nextReminder,
                timeUntilNextReminder = TimeWithUnit.fromTimestamp(nextReminder?.calculateTimeUntil()),
                progressUntilNextReminder = nextReminder?.calculateProgressUntil() ?: 0f
            )
        )


    }

    private fun getNextReminder(reminders: List<Reminder>, checkTime: Long): Reminder? {
        return reminders.firstOrNull{
            // in case that we have reminders with past timestamps,
            // we want to show the next upcoming reminder, not the past one
            it.timestamp > checkTime
        }

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
                val futureReminders = getFutureReminders(currentState.uiState.reminders, checkTime)
                val pastReminders = getPastReminders(currentState.uiState.reminders, checkTime)
                val nextReminder =
                    futureReminders.firstOrNull { it.timestamp > checkTime }
                _screenState.update { currentState ->
                    if (currentState is ScreenUiState.Success) {
                        currentState.copy(
                            uiState = currentState.uiState.copy(
                                reminders =
                                    if (currentState.uiState.showPastReminders)
                                        futureReminders + pastReminders
                                    else futureReminders,
                                nextReminder = nextReminder,
                                timeUntilNextReminder = TimeWithUnit.fromTimestamp(nextReminder?.calculateTimeUntil()),
                                progressUntilNextReminder = nextReminder?.calculateProgressUntil()
                                    ?: 0f
                            )
                        )
                    } else currentState
                }

            } else {
                // update the time until next reminder
                _screenState.update { currentState ->
                    if (currentState is ScreenUiState.Success) {
                        currentState.copy(
                            uiState = currentState.uiState.copy(
                                timeUntilNextReminder = TimeWithUnit.fromTimestamp(timeUntilNext),
                                progressUntilNextReminder = reminder.calculateProgressUntil()
                            )
                        )
                    } else currentState
                }
            }

        }
    }

    fun deleteReminder(reminder: Reminder) {
        val currentState = screenState.value as? ScreenUiState.Success ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<ReminderShowcaseApplication>()
            reminder.cancelNotification(context)
            storage.deleteReminder(reminder.id)
            val updatedReminders = storage.loadReminders()
            _screenState.update {
                currentState.copy(
                    uiState = currentState.uiState.copy(
                        reminders = updatedReminders,
                        reminderToDelete = null
                    )
                )
            }
        }
    }

    fun editReminder(reminder: Reminder) {
        val currentState = screenState.value as? ScreenUiState.Success ?: return
        _showBottomDSheet.update { true }
        _screenState.update {
            currentState.copy(
                uiState = currentState.uiState.copy(
                    reminderToEdit = reminder
                )
            )
        }
    }

    fun hideBottomSheet() {
        _showBottomDSheet.update { false }
    }

    fun saveReminder(newReminder: Reminder) {
        hideBottomSheet()

        val currentState = screenState.value as? ScreenUiState.Success ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<ReminderShowcaseApplication>()

            currentState.uiState.reminderToEdit?.let { oldReminder ->
                storage.deleteReminder(oldReminder.id)
                oldReminder.cancelNotification(context)
            }
            storage.addReminder(newReminder)
            newReminder.scheduleNotification(context)
            /** note: this is not very efficient, we should ideally observe the reminders and
             * update the state accordingly, but for simplicity we just load them again after each change
             */
            val updatedReminders =
                storage.loadReminders().sortedBy { it.timestamp }
            val nextReminder = getNextReminder(updatedReminders, System.currentTimeMillis())
            _screenState.update {
                currentState.copy(
                    uiState = currentState.uiState.copy(
                        reminders = updatedReminders,
                        nextReminder = nextReminder,
                        reminderToEdit = null,
                        reminderToDelete = null,
                        timeUntilNextReminder = TimeWithUnit.fromTimestamp(nextReminder?.calculateTimeUntil()),
                        progressUntilNextReminder = nextReminder?.calculateProgressUntil() ?: 0f
                    )
                )
            }
            restartTimer()
        }


    }

    fun toggleDeleteIndication(reminder: Reminder?) {
        val currentState = screenState.value as? ScreenUiState.Success ?: return
        _screenState.update {
            currentState.copy(
                uiState = currentState.uiState.copy(
                    reminderToDelete = reminder
                )
            )
        }

    }

    fun setShowPastReminders(checked: Boolean) {
        val currentState = screenState.value as? ScreenUiState.Success ?: return
        _screenState.update {
            val checkTime = System.currentTimeMillis()
            currentState.copy(
                uiState = currentState.uiState.copy(
                    showPastReminders = checked,
                    reminders = if (checked) {
                        getFutureReminders(
                            currentState.uiState.reminders,
                            checkTime
                        ) +
                                getPastReminders(
                                    currentState.uiState.reminders,
                                    checkTime
                                )
                    } else {
                        getFutureReminders(
                            currentState.uiState.reminders,
                            checkTime
                        )
                    }
                )
            )
        }

    }

    fun showBottomDialog() {
        _showBottomDSheet.update { true }

    }
}