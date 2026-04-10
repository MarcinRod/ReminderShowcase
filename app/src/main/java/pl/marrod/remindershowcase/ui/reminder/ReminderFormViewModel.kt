package pl.marrod.remindershowcase.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.utils.toDisplayDateTime
import java.util.UUID
import kotlin.collections.copy
import kotlin.toString

/**
 * Stan UI formularza przypomnienia. Przechowuje wszystkie pola formularza oraz
 * stan pomocniczych elementów UI (np. widoczność dialogu wyboru daty/czasu).
 */
data class ReminderFormUiState(
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis() + 60 * 60 * 1000L,
    val showDateTimePicker: Boolean = false,
    val isEditMode: Boolean = false,
) {
    /** Walidacja — przycisk zapisu aktywny tylko gdy tytuł nie jest pusty */
    val isSaveEnabled: Boolean get() = title.isNotBlank()

    /** Sformatowana data i czas do wyświetlenia w polu tekstowym */
    val formattedDateTime: String get() = timestamp.toDisplayDateTime()
}

/**
 * ViewModel dla formularza przypomnienia.
 *
 * Przyjmuje cały obiekt [Reminder] zamiast samego ID, dzięki czemu nie potrzebuje
 * dostępu do bazy — dane są już dostępne w pamięci rodzica (np. ReminderListViewModel).
 *
 * @param reminder Edytowane przypomnienie lub null w trybie dodawania.
 */
class ReminderFormViewModel(
    private val reminder: Reminder?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReminderFormUiState(
            title = reminder?.title ?: "",
            description = reminder?.description ?: "",
            timestamp = reminder?.timestamp ?: (System.currentTimeMillis() + 60 * 60 * 1000L),
            isEditMode = reminder != null,
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onTimestampChange(timestamp: Long) {
        _uiState.update { it.copy(timestamp = timestamp, showDateTimePicker = false) }
    }

    fun showDateTimePicker() {
        _uiState.update { it.copy(showDateTimePicker = true) }
    }

    fun hideDateTimePicker() {
        _uiState.update { it.copy(showDateTimePicker = false) }
    }

    /**
     * Buduje obiekt [Reminder] na podstawie aktualnego stanu formularza.
     * W trybie edycji zachowuje oryginalne [Reminder.id] i [Reminder.createdAtTimestamp].
     * W trybie dodawania generuje nowe UUID.
     * Zwraca null jeśli formularz jest nieprawidłowy (tytuł pusty).
     */
    fun buildReminder(): Reminder? {
        val state = _uiState.value
        if (!state.isSaveEnabled) return null
        val base = reminder ?:
        // W trybie dodawania tworzymy nowy pusty obiekt z wygenerowanym ID i aktualnym czasem utworzenia
        Reminder(
            id = UUID.randomUUID().toString(),
            title = "",
            description = "",
            timestamp = 0L
        )
        //kopiujemy i nadpisujemy tylko pola edytowane w formularzu, reszta (id, createdAtTimestamp) pozostaje bez zmian
        return base.copy(
            title = state.title.trim(),
            description = state.description.trim(),
            timestamp = state.timestamp,
            isNotificationScheduled = false
        )

    }

    companion object {
        /**
         * Tworzy fabrykę ViewModel dla danego [reminder].
         *
         * Użycie: viewModel(key = reminder?.id ?: "unikalny_klucz", factory = ReminderFormViewModel.factory(reminder))
         *
         * Wykorzystujemy viewModelFactory, podobnie jak w przypadku [pl.marrod.remindershowcase.factory.AppWideViewModelProvider],
         * ale aby umożliwić przekazanie całego obiektu [Reminder] do ViewModel musimy stworzyć niestandardową fabrykę,
         * która przyjmuje ten obiekt jako argument.
         */
        fun factory(reminder: Reminder?) = viewModelFactory {
            initializer {
                ReminderFormViewModel(reminder = reminder)
            }
        }
    }
}