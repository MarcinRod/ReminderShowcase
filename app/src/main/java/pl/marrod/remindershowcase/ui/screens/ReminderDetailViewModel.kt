package pl.marrod.remindershowcase.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.RemindersRepository
import pl.marrod.remindershowcase.ui.navigation.Destination
import pl.marrod.remindershowcase.utils.UiText

/**
 * Interfejs reprezentujący stan UI dla ekranu szczegółów przypomnienia.
 * Zawiera tytuł, który jest wspólny zarówno dla stanu sukcesu, jak i błędu.
 */
sealed interface ReminderDetailUiState {
    /**
     * Tytuł przypomnienia, który będzie wyświetlany w UI. Typ [UiText] pozwala na elastyczne zarządzanie
     * tekstami, zarówno z zasobów, jak i dynamicznymi generowanymi w czasie działania aplikacji.
     */
    val title: UiText

    /**
     * Stan reprezentujący pomyślne załadowanie przypomnienia. Zawiera dane przypomnienia, które można wyświetlić w UI.
     */
    data class Success(val reminder: Reminder) : ReminderDetailUiState
    {
        override val title: UiText
            get() = UiText.Dynamic(reminder.title)
    }

    /**
     * Stan reprezentujący błąd podczas ładowania przypomnienia. Zawiera komunikat błędu, który można wyświetlić w UI.
     */
    data class Error(val message: UiText) : ReminderDetailUiState {
        override val title: UiText
            get() = UiText.Resource(R.string.broken_link)
    }

    /** Stan podczas oczekiwania na pierwsze dane z Flow */
    object Loading : ReminderDetailUiState {
        override val title = UiText.Resource(R.string.loading)
    }
}

/**
 * ViewModel dla ekranu szczegółów przypomnienia.
 * Odpowiada za zarządzanie stanem UI. Ekran szczegółów jest bardzo prosty, więc ViewModel
 * jest odpowiedzialny tylko za ładowanie danych przypomnienia na podstawie ID przekazanego w argumencie nawigacji.
 * */
class ReminderDetailViewModel(
    savedStateHandle: androidx.lifecycle.SavedStateHandle, // SavedStateHandle pozwala na dostęp do argumentów przekazanych podczas nawigacji, takich jak ID przypomnienia.
    private val repository: RemindersRepository
    //storage: ReminderStorage // stara wersja (dla porównania, jak było wcześniej bez repozytorium
) : ViewModel() {
    private val reminderId: String = savedStateHandle.toRoute<Destination.Details>().reminderId

    private val _uiState = MutableStateFlow<ReminderDetailUiState>(ReminderDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        // Aby porbać dane przypomnienia, korzystamy z funkcji getReminderByIdFlow z repozytorium, która zwraca Flow<Reminder?>.
        // Flow pozwala na obserwowanie zmian danych w czasie rzeczywistym, więc jeśli przypomnienie
        // zostanie zaktualizowane lub usunięte, UI zostanie automatycznie odświeżone, co jest dużą
        // zaletą w porównaniu do tradycyjnego podejścia z suspend fun, które zwraca tylko jednorazową wartość.
        viewModelScope.launch {

            repository.getReminderByIdFlow(reminderId).collect { reminder ->
                _uiState.update {
                    if (reminder != null) ReminderDetailUiState.Success(reminder)
                    else ReminderDetailUiState.Error(UiText.Resource(R.string.reminder_not_found))
                }
            }
        }
    }
}