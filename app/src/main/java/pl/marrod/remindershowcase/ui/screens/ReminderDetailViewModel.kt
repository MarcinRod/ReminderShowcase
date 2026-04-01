package pl.marrod.remindershowcase.ui.screens

import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.marrod.remindershowcase.R
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.ReminderStorage
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

}

/**
 * ViewModel dla ekranu szczegółów przypomnienia.
 * Odpowiada za zarządzanie stanem UI. Ekran szczegółów jest bardzo prosty, więc ViewModel
 * jest odpowiedzialny tylko za ładowanie danych przypomnienia na podstawie ID przekazanego w argumencie nawigacji.
 * */
class ReminderDetailViewModel(
    savedStateHandle: androidx.lifecycle.SavedStateHandle, // SavedStateHandle pozwala na dostęp do argumentów przekazanych podczas nawigacji, takich jak ID przypomnienia.
    storage: ReminderStorage // storage jest potrzebne do załadowania danych przypomnienia z lokalnego magazynu, na podstawie ID.
) : ViewModel() {
    private val detailsFromRoute = savedStateHandle.toRoute<Destination.Details>()
    private val reminderId: String = detailsFromRoute.reminderId
    private val reminder = storage.loadReminders().find { it.id == reminderId } // potencjalne źródło problemu jeżeli operacja wczytywania byłaby długotrwała -
    // w takim przypadku warto rozważyć asynchroniczne ładowanie danych, np. za pomocą viewModelScope.

    /** MutableStateFlow przechowuje aktualny stan UI, który jest emitowany przez [uiState] do obserwujących go komponentów UI.
     * W przypadku tego prostego ekranu, stan jest inicjalizowany bezpośrednio w konstruktorze na podstawie tego,
     * czy udało się znaleźć przypomnienie o podanym ID. Stan UI jest zmieniany tak naprawdę tylko raz, więc nie ma
     * potrzeby emitowania nowych wartości po inicjalizacji. To rozwiązanie jest tutaj bardziej w celach demonstracyjnych.
     */
    private val _uiState = MutableStateFlow<ReminderDetailUiState>(
        if (reminder != null) {
            ReminderDetailUiState.Success(reminder)
        } else {
            ReminderDetailUiState.Error(UiText.Resource(R.string.reminder_not_found))
        }
    )

    /**
     * Rozdzielenie MutableStateFlow i StateFlow pozwala na enkapsulację stanu UI.
     * Komponenty UI mogą obserwować uiState, ale nie mogą go modyfikować bezpośrednio,
     * co zwiększa bezpieczeństwo i kontrolę nad tym, jak stan jest aktualizowany.
     */
    val uiState = _uiState.asStateFlow()


}