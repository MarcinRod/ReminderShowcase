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
import pl.marrod.remindershowcase.utils.TimeWithUnit
import pl.marrod.remindershowcase.utils.UiText
import kotlin.collections.firstOrNull

import kotlin.time.Duration.Companion.milliseconds


/**
 * Klasa reprezentująca stan UI dla ekranu listy przypomnień. Zawiera trzy stany:
 * - Loading: reprezentuje stan ładowania danych, gdy przypomnienia są wczytywane z magazynu. W tym stanie UI może wyświetlać np. animację ładowania.
 * - Success: reprezentuje stan, gdy przypomnienia zostały załadowane
 * - Error: reprezentuje stan, gdy wystąpił błąd podczas ładowania danych. Zawiera komunikat błędu, który można wyświetlić w UI.
 * (nie jest wykorzystany w tej implementacji, ale przewidziany na przyszłość, np. do obsługi błędów podczas zapisu danych).
 *
 *
 * W odróżnieniu od ReminderDetailUiState, tutaj używamy sealed class zamiast sealed interface,
 * ponieważ chcemy mieć możliwość definiowania stanów bez dodatkowych właściwości (np. Loading),
 * które nie potrzebują implementacji wspólnych właściwości (takich jak title w ReminderDetailUiState).
 */
sealed class ScreenUiState {

    object Loading : ScreenUiState()
    data class Success(val uiState: ReminderListUiState) : ScreenUiState()
    data class Error(val message: UiText) : ScreenUiState()
}

/**
 * Pomocnicza klasa danych reprezentująca stan UI dla ekranu listy przypomnień, zawierająca
 * wszystkie informacje potrzebne do wyświetlenia listy przypomnień,
 * czasu do następnego przypomnienia, stanu widoczności elementów interfejsu itp.
 */
data class ReminderListUiState(
    /**
     * Lista przypomnień do wyświetlenia w UI. Może zawierać zarówno przyszłe, jak i przeszłe przypomnienia,
     * w zależności od ustawienia showPastReminders.
     */
    val reminders: List<Reminder> = emptyList(),
    /**
     * Następne przypomnienie, które ma się pojawić.
     * Może być null, jeśli nie ma żadnych przyszłych przypomnień
     */
    val nextReminder: Reminder? = null,
    /**
     * Zapamiętuje przypomnienie do edycji
     */
    val reminderToEdit: Reminder? = null,
    /**
     * Zapamiętuje przypomnienie, które jest oznaczone do usunięcia (przez przesunięcie wpisu w lewo).
     */
    val reminderToDelete: Reminder? = null,
    /**
     * Flaga (stan przełącznika) określająca, czy pokazywać przeszłe przypomnienia (te, których czas już minął) na liście.
     */
    val showPastReminders: Boolean = true,
    /**
     * Tekst reprezentujący czas pozostały do następnego przypomnienia, np. "5 m", "2 h", "1 d".
     * Obliczany na podstawie znacznika czasowego (timestamp) następnego przypomnienia i aktualnego czasu.
     */
    val timeUntilNextReminder: TimeWithUnit = TimeWithUnit("0", "s"),
    /**
     * Wartość od 0 do 1 reprezentująca postęp czasu do następnego przypomnienia
     */
    val progressUntilNextReminder: Float = 0f,
    /**
     * Flaga określająca, czy pokazywać dolny arkusz (BottomSheet) z formularzem dodawania/edycji przypomnienia.
     */
    val showBottomSheet: Boolean = false,
    /**
     * Flaga określająca widoczność elementów ekranu w stanie Success używana w celu animacji
     * wejścia elementów po załadowaniu danych.
     */
    val animatedVisibility: Boolean = false,
) {
    /**
     * Właściwość pomocnicza, która zwraca true, jeśli lista przypomnień jest pusta i
     * jednocześnie ustawienie showPastReminders jest true. Oznacza to, że nie ma żadnych przypomnień
     * do wyświetlenia, nawet po uwzględnieniu przeszłych przypomnień.
     */
    val isEmpty: Boolean get() = reminders.isEmpty() && showPastReminders

    /**
     * Funkcja pomocnicza do aktualizacji informacji o następnym przypomnieniu.
     * Przyjmuje nowe przypomnienie jako argument i zwraca kopię zaktualizowaną o nowe informacje
     */
    fun withNextReminder(next: Reminder?) = copy(
        nextReminder = next,
        timeUntilNextReminder = TimeWithUnit.fromTimestamp(next?.calculateTimeUntil()),
        progressUntilNextReminder = next?.calculateProgressUntil() ?: 0f
    )
}



class ReminderListViewModel(
    application: android.app.Application,
    private val storage: pl.marrod.remindershowcase.data.ReminderStorage
) : AndroidViewModel(application) {



    /** MutableStateFlow przechowuje aktualny stan UI, który jest emitowany przez [screenState] do obserwujących go komponentów UI.
     * Inicjalizowany jest stanem Loading, a następnie aktualizowany do Success po załadowaniu danych z magazynu.
     * W przypadku błędu (niezaimplementowane w tej wersji) można by ustawić stan na Error z odpowiednim komunikatem.
     *
     * Podobnie jak w ReminderDetailViewModel używamy enkapsulacji MutableStateFlow, udostępniając tylko
     * niemodyfikowalny StateFlow do obserwacji stanu UI przez komponenty UI.
     */
    private val _screenState = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val screenState = _screenState.asStateFlow()

    /**
     * Zadanie (Job) odpowiedzialne za aktualizację czasu do następnego przypomnienia co sekundę.
     * Jest przechowywane jako właściwość klasy, aby można było je anulować i ponownie uruchomić
     * (np. po dodaniu nowego przypomnienia, które jest bliżej niż poprzednie następne przypomnienie).
     */
    private var timerJob: Job? = null

    /**
     * Przechowuje wszystkie przypomnienia załadowane z magazynu. Jest aktualizowana przy każdej zmianie (dodanie, edycja, usunięcie przypomnienia).
      Używana do obliczania listy przypomnień do wyświetlenia (z uwzględnieniem ustawienia showPastReminders) oraz do obliczania następnego przypomnienia.
       Przechowywanie tej listy w pamięci pozwala na szybkie aktualizacje UI bez konieczności ponownego ładowania danych z magazynu przy każdej zmianie.
     */
    private var allReminders: List<Reminder> = emptyList()

    init {
        // W zakresie viewModelScope uruchamiamy korutynę, która ładuje przypomnienia z magazynu.
        viewModelScope.launch {
            // Sztuczne opóźnienie, aby pokazać ekran ładowania.
            delay(3000)
            loadReminders().let { resultState ->
                //
                _screenState.update { resultState }
                if (resultState is ScreenUiState.Success) {
                    delay(50)
                    updateSuccess { it.copy(animatedVisibility = true) }
                    resultState.uiState.nextReminder?.let { restartTimer() }
                }
            }
        }

    }


    /**
     * Funkcja pomocnicza do aktualizacji stanu UI w przypadku, gdy obecny stan jest Success.
     * Przyjmuje blok aktualizujący ReminderListUiState i aktualizuje tylko wtedy, gdy obecny stan jest Success.
     * Dzięki temu eliminujemy konieczność sprawdzania typu stanu i rzutowania za każdym razem, gdy chcemy zaktualizować UI w przypadku sukcesu.
     * W innych stanach (Loading, Error) funkcja po prostu zwraca niezmieniony stan, co jest bezpieczne,
     * ponieważ te stany nie powinny być aktualizowane tym blokiem.
     *
     * Użycie tej funkcji pozwala na bardziej zwięzły i czytelny kod podczas aktualizacji stanu UI
     * w różnych miejscach ViewModel, bez powtarzania logiki sprawdzania typu stanu.
     */
    private inline fun updateSuccess(block: (ReminderListUiState) -> ReminderListUiState) {
        _screenState.update { state ->
            if (state is ScreenUiState.Success) state.copy(uiState = block(state.uiState))
            else state
        }
    }

    /**
     * Funkcja pomocnicza do budowania listy przypomnień do wyświetlenia w UI,
     * na podstawie wszystkich przypomnień i ustawienia showPastReminders.
     */
    private fun buildDisplayedReminders(showPast: Boolean, checkTime: Long): List<Reminder> {
        val future = getFutureReminders(allReminders, checkTime)
        return if (showPast) future + getPastReminders(allReminders, checkTime) else future
    }

    /**
     * Funkcja do ponownego uruchomienia timera, który aktualizuje czas do następnego przypomnienia co sekundę.
     */
    fun restartTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch { updateJob() }
    }

    /**
     * Funkcja do ładowania przypomnień z magazynu. Jest oznaczona jako suspend, ponieważ operacja
     * wczytywania danych z dysku może być czasochłonna i nie powinna blokować UI. Wykorzystuje withContext(Dispatchers.IO)
     * do wykonania operacji wczytywania w kontekście IO, co jest odpowiednie dla operacji dyskowych.
     * Po załadowaniu przypomnień aktualizuje stan UI na Success z odpowiednimi danymi.
     * W przypadku błędu (niezaimplementowane w tej wersji) można by zwrócić stan Error z odpowiednim komunikatem.
     */
    private suspend fun loadReminders(): ScreenUiState = withContext(Dispatchers.IO) {
        allReminders = storage.loadReminders()
        val checkTime = System.currentTimeMillis()
        val nextReminder = getNextReminder(allReminders, checkTime)
        // Funkcja load reminders jest wywoływana tylko raz,
        // podczas inicjalizacji ViewModel, domyślne ustawienie showPastReminders to true,
        // więc można od razu zbudować listę przypomnień do wyświetlenia.
        ScreenUiState.Success(
            ReminderListUiState(
                reminders = buildDisplayedReminders(showPast = true, checkTime),
            ).withNextReminder(nextReminder)
        )
    }

    /**
     * Funkcja pomocnicza do znalezienia następnego przypomnienia, które ma się pojawić,
     * na podstawie listy wszystkich przypomnień i aktualnego czasu.Zwraca pierwsze przypomnienie z
     * przyszłości (timestamp >= checkTime) posortowane po czasie, lub null jeśli nie ma żadnych przyszłych przypomnień.
     */
    private fun getNextReminder(reminders: List<Reminder>, checkTime: Long): Reminder? {
        return getFutureReminders(reminders, checkTime).firstOrNull()
    }

    /**
     * Funkcja pomocnicza do filtrowania i sortowania przypomnień, aby uzyskać listę przyszłych przypomnień (timestamp >= checkTime),
     * posortowanych rosnąco po czasie (najbliższe przypomnienie jako pierwsze). Ta funkcja jest używana zarówno do wyświetlania listy
     * przypomnień, jak i do znalezienia następnego przypomnienia.
     */
    private fun getFutureReminders(reminders: List<Reminder>, checkTime: Long): List<Reminder> {
        return reminders.filter { it.timestamp >= checkTime }
            .sortedBy { it.timestamp }
    }

    /**
     * Funkcja pomocnicza do filtrowania i sortowania przypomnień, aby uzyskać listę przeszłych przypomnień (timestamp < checkTime),
     * posortowanych malejąco po czasie (najbliższe przeszłe przypomnienie jako pierwsze).
     * Ta funkcja jest używana do wyświetlania listy przypomnień, gdy ustawienie showPastReminders jest true.
     */
    private fun getPastReminders(reminders: List<Reminder>, checkTime: Long): List<Reminder> {
        return reminders.filter { it.timestamp < checkTime }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Funkcja odpowiedzialna za aktualizację czasu do następnego przypomnienia co sekundę.
     * Jest uruchamiana w osobnym zadaniu (Job) i aktualizuje stan UI, dopóki istnieje następne przypomnienie.
     * Jeśli czas do następnego przypomnienia minie, aktualizuje stan UI, aby pokazać to przypomnienie jako przeszłe
     * (jeśli showPastReminders jest true) i znajduje kolejne następne przypomnienie. Jeśli nie ma już żadnych przyszłych przypomnień,
     * zatrzymuje aktualizację czasu.
     *
     * Ta funkcja jest kluczowa dla dynamicznej aktualizacji UI, pokazując użytkownikowi,
     * ile czasu pozostało do następnego przypomnienia i automatycznie aktualizując tę informację co sekundę.
     */
    private suspend fun updateJob() {

        while (true) {
            delay(1000)
            // jeśli stan nie jest Success, przerywamy aktualizację czasu, ponieważ nie ma danych do aktualizacji
            // nie powinno się to zdarzyć, ponieważ timer jest uruchamiany tylko wtedy, gdy mamy stan Success z
            // następnym przypomnieniem, ale zabezpieczamy się na wypadek nieoczekiwanych zmian stanu
            val currentState = screenState.value as? ScreenUiState.Success ?: break
            val reminder = currentState.uiState.nextReminder

            // Nie ma sensu aktualizować czasu, jeśli nie ma żadnego następnego przypomnienia, więc przerywamy aktualizację czasu.
            reminder ?: break

            val checkTime = System.currentTimeMillis()
            val timeUntilNext = reminder.timestamp - checkTime

            if (timeUntilNext <= 0) {
                // Czas do następnego przypomnienia minął, więc aktualizujemy UI,
                // aby pokazać to przypomnienie jako przeszłe (jeśli showPastReminders jest true)
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
                // Aktualizujemy czas do następnego przypomnienia i postęp,
                updateSuccess { uiState ->
                    uiState.copy(
                        timeUntilNextReminder = TimeWithUnit.fromTimestamp(timeUntilNext),
                        progressUntilNextReminder = reminder.calculateProgressUntil()
                    )
                }
            }

        }
    }

    /**
     * Funkcja do zapisywania przypomnienia, zarówno nowego, jak i edytowanego. Aktualizuje stan UI natychmiast po zmianie,
     * a następnie wykonuje operację zapisu w tle, aby nie blokować UI. Dzięki temu użytkownik od razu widzi efekt dodania
     * lub edycji przypomnienia, a operacje dyskowe i związane z powiadomieniami są wykonywane asynchronicznie.
     * Jest to pewne zagrożenie w przypadku, gdy operacja zapisu do magazynu lub planowania powiadomienia zakończy się niepowodzeniem,
     * ponieważ UI już pokazuje, że przypomnienie zostało dodane lub edytowane.
     */
    fun saveReminder(newReminder: Reminder) {
        // Sprawdzamy, czy obecnie edytujemy jakieś przypomnienie, sprawdzając czy stan UI zawiera przypomnienie do edycji.
        val reminderToEdit = (screenState.value as? ScreenUiState.Success)?.uiState?.reminderToEdit

        // Aktualizacja listy w pamięci przez usunięcie starego przypomnienia (jeśli edytujemy) (operacja filter)
        // i dodanie nowego, a następnie posortowanie listy po czasie.
        allReminders = (allReminders.filter { it.id != reminderToEdit?.id } + newReminder)
            .sortedBy { it.timestamp }

        val checkTime = System.currentTimeMillis()
        // Aktualizujemy stan UI
        updateSuccess { uiState ->
            uiState.copy(
                reminders = buildDisplayedReminders(uiState.showPastReminders, checkTime),
                showBottomSheet = false,
                reminderToEdit = null,
                reminderToDelete = null
            ).withNextReminder(getNextReminder(allReminders, checkTime))
        }
        // restartujemy timer, aby zaczął aktualizować czas do następnego przypomnienia na podstawie nowej listy przypomnień
        restartTimer()

        // Faktyczny zapis nowej listy przypomnień do magazynu wykonujemy w tle, aby nie blokować UI.
        // Tutaj również zaplanowen jest powiadomienie
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<ReminderShowcaseApplication>()
            // Jeżeli edytujemy - usuwamy z pamięci i magazynu stare przypomnienie,
            // a następnie anulujemy jego powiadomienie, aby nie było duplikatów.
            reminderToEdit?.let { old ->
                storage.deleteReminder(old.id)
                old.cancelNotification(context)
            }
            storage.addReminder(newReminder)
            newReminder.scheduleNotification(context)
        }
    }

    /**
     * Funkcja do usuwania przypomnienia. Aktualizuje stan UI natychmiast po zmianie, a następnie
     * wykonuje operację usunięcia w tle, aby nie blokować UI. Usuwa przypomnienie z listy w pamięci,
     * aktualizuje stan UI, restartuje timer, a następnie usuwa przypomnienie z magazynu i anuluje jego powiadomienie.
     * Dzięki temu użytkownik od razu widzi efekt usunięcia przypomnienia, a operacje dyskowe i związane z powiadomieniami są wykonywane asynchronicznie.
     * Jest to pewne zagrożenie w przypadku, gdy operacja usunięcia z magazynu lub anulowania powiadomienia zakończy się niepowodzeniem,
     * ponieważ UI już pokazuje, że przypomnienie zostało usunięte.
     */
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

    /**
     * Funkcja do rozpoczęcia edycji przypomnienia. Aktualizuje stan UI, ustawiając reminderToEdit na przekazane przypomnienie
     * i pokazując dolny arkusz (BottomSheet) z formularzem edycji.
     */
    fun editReminder(reminder: Reminder) {
        updateSuccess { it.copy(reminderToEdit = reminder, showBottomSheet = true) }
    }

    /**
     * Funkcja do zamknięcia dolnego arkusza (BottomSheet) z formularzem dodawania/edycji przypomnienia.
     * Aktualizuje stan UI, ustawiając showBottomSheet na false
     */
    fun hideBottomSheet() {
        updateSuccess { it.copy(showBottomSheet = false, reminderToEdit = null) }
    }

    /**
     * Funkcja do oznaczenia przypomnienia do usunięcia (przez przesunięcie wpisu w lewo). Aktualizuje stan UI, ustawiając reminderToDelete na przekazane przypomnienie.
     *  UI wykorzystuje tę informację do pokazania wizualnego wskazania, że dane przypomnienie jest oznaczone do usunięcia.
     */
    fun toggleDeleteIndication(reminder: Reminder?) {
        updateSuccess { it.copy(reminderToDelete = reminder) }
    }

    /**
     * Funkcja do ustawienia, czy pokazywać przeszłe przypomnienia (te, których czas już minął) na liście.
     * Aktualizuje stan UI, ustawiając showPastReminders na przekazaną wartość i aktualizując
     * listę przypomnień do wyświetlenia na podstawie nowego ustawienia i aktualnego czasu.
     */
    fun setShowPastReminders(checked: Boolean) {
        updateSuccess { uiState ->
            val checkTime = System.currentTimeMillis()
            uiState.copy(
                showPastReminders = checked,
                reminders = buildDisplayedReminders(checked, checkTime)
            )
        }
    }

    /**
     * Funkcja do pokazania dolnego arkusza (BottomSheet) z formularzem dodawania nowego przypomnienia.
     */
    fun showAddBottomDialog() {
        updateSuccess { it.copy(showBottomSheet = true) }
    }
}