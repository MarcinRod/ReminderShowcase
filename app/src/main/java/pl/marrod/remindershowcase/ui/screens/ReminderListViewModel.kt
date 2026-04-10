package pl.marrod.remindershowcase.ui.screens

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.ReminderShowcaseApplication
import pl.marrod.remindershowcase.data.Reminder
import pl.marrod.remindershowcase.data.RemindersRepository
import pl.marrod.remindershowcase.utils.TimeWithUnit
import pl.marrod.remindershowcase.utils.UiText
import kotlin.collections.firstOrNull


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
    val nextReminder: Reminder? = null, // TODO: aktualizacja przy edycji
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
    private val repository: RemindersRepository
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
     * Przechowuje wszystkie przypomnienia załadowane z magazynu jako StateFlow. Jest automatycznie aktualizowana przy każdej zmianie (dodanie, edycja, usunięcie przypomnienia).
     * Używana do obliczania listy przypomnień do wyświetlenia (z uwzględnieniem ustawienia showPastReminders) oraz do obliczania następnego przypomnienia.
     * W odróżnieniu od rozwiązania z zapisem danych w pliku json tutaj nasłuchujemy zmian we Flow z bazy danych i "zbieramy" je za pomocą collect,
     * które jest zainicjowane w bloku init.
     *
     * Do stworzenia StateFlow ze zwykłego Flow używamy operatora stateIn, który pozwala na określenie zakresu życia (viewModelScope),
     * strategii udostępniania (SharingStarted.WhileSubscribed(5000)) oraz wartości początkowej (emptyList()).
     * Nie możemy wykorzystać bezpośrednio Flow z repozytorium, ponieważ musimy mieć dostęp do stanu i wartości z Flow czego sam Flow nie udostępnia */
    private var allReminders = repository.allReminders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        // W odróżnieniu od rozwiązania z zapisem danych w pliku json,
        // tutaj nasłuchujemy zmian we Flow z bazy danych i "zbieramy"
        // je za pomocą collect. Nie ma "ręcznego" ładowania danych tylko obserwacja zmian w bazie danych,
        // co jest dużą zaletą i upraszcza zarządzanie stanem UI, ponieważ nie musimy martwić się o ręczne
        // odświeżanie danych po każdej zmianie.
        viewModelScope.launch {
            // Symulacja dłuższego ładowania — w tym czasie wyświetlany jest ekran Loading.
            delay(3000)

            // Zbieranie danych z StateFlow allReminders rozpoczęte z opóźnieniem.
            // Za każdym razem, gdy baza danych emituje nową listę przypomnień
            // (np. po dodaniu, edycji lub usunięciu przypomnienia),
            allReminders.collect { reminders ->
                // Każda zmiana powoduje wykonanie tego bloku, w którym aktualizujemy stan UI.
                val checkTime = System.currentTimeMillis()
                val nextReminder = getNextReminder(reminders, checkTime)

                if (_screenState.value is ScreenUiState.Loading) {
                    // Przy pierwszej emisji z Room — inicjalizujemy stan ekranu
                    // z pobranymi danymi
                    _screenState.update {
                        ScreenUiState.Success(
                            ReminderListUiState(
                                reminders = buildDisplayedReminders(true, checkTime, reminders)
                            ).withNextReminder(nextReminder)
                        )
                    }
                    // Dajemy Compose czas na narysowanie elementów w stanie "niewidoczne"
                    // zanim uruchomimy animację wejścia
                    delay(50)
                    updateSuccess { it.copy(animatedVisibility = true) }
                    if (nextReminder != null) restartTimer()
                } else {
                    // Kolejne emisje — aktualizacja listy po zapisie, edycji lub usunięciu
                    updateSuccess { uiState ->
                        uiState.copy(
                            reminders = buildDisplayedReminders(
                                uiState.showPastReminders,
                                checkTime,
                                reminders
                            )
                        ).withNextReminder(nextReminder)
                    }
                    if (nextReminder != null) restartTimer() else timerJob?.cancel()
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
     *
     * Zmiana w porównaniu do poprzedniej implementacji polega na
     * dodaniu parametru reminders, który pozwala na przekazanie listy przypomnień do przefiltrowania i posortowania,
     * nie możemy korzystać bezpośrednio z allReminders.value, ponieważ ta wartość jest aktualizowana asynchronicznie
     * i może nie być aktualna w momencie wywołania tej funkcji,
     */
    private fun buildDisplayedReminders(
        showPast: Boolean,
        checkTime: Long,
        reminders: List<Reminder>
    ): List<Reminder> {
        val future = getFutureReminders(reminders, checkTime)
        return if (showPast) future + getPastReminders(reminders, checkTime) else future
    }

    /**
     * Funkcja do ponownego uruchomienia timera, który aktualizuje czas do następnego przypomnienia co sekundę.
     */
    fun restartTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch { updateJob() }
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
        // Posortowane w zapytaniu .sortedBy { it.timestamp }
    }

    /**
     * Funkcja pomocnicza do filtrowania i sortowania przypomnień, aby uzyskać listę przeszłych przypomnień (timestamp < checkTime),
     * posortowanych malejąco po czasie (najbliższe przeszłe przypomnienie jako pierwsze).
     * Ta funkcja jest używana do wyświetlania listy przypomnień, gdy ustawienie showPastReminders jest true.
     */
    private fun getPastReminders(reminders: List<Reminder>, checkTime: Long): List<Reminder> {
        return reminders.filter { it.timestamp < checkTime }.reversed()
        // .sortedByDescending { it.timestamp }
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
                            checkTime,
                            allReminders.value
                        )
                    )
                        .withNextReminder(getNextReminder(allReminders.value, checkTime))
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
     * Funkcja do zapisywania przypomnienia — zarówno nowego, jak i edytowanego.
     *
     * W odróżnieniu od wersji z plikiem JSON, nie ma tu ręcznej aktualizacji listy w pamięci
     * ani ręcznego wywołania [restartTimer] — o to dba kolektor [allReminders] w bloku `init`,
     * który reaguje na każdą zmianę w bazie Room. Dzięki temu jedyną operacją na wątku głównym
     * jest zamknięcie BottomSheet, a cały ciężar synchronizacji UI przejmuje reaktywny Flow.
     *
     * Uwaga: UI wskazuje sukces operacji przed jej faktycznym zakończeniem — w razie błędu
     * zapisu do bazy lub planowania powiadomienia użytkownik nie otrzyma informacji zwrotnej.
     *
     * @param newReminder Nowe lub zaktualizowane przypomnienie do zapisania w bazie danych.
     */
    fun saveReminder(newReminder: Reminder) {
        val reminderToEdit = (screenState.value as? ScreenUiState.Success)?.uiState?.reminderToEdit

        // Zamykamy BottomSheet natychmiast — lista zaktualizuje się gdy Room wyemituje nowe dane
        updateSuccess { uiState ->
            uiState.copy(
                showBottomSheet = false,
                reminderToEdit = null,
                reminderToDelete = null
            )
        }

        viewModelScope.launch() {
            val context = getApplication<ReminderShowcaseApplication>()
            newReminder.scheduleNotification(context)
            reminderToEdit?.let { old ->
                old.cancelNotification(context)
                repository.updateReminder(newReminder)
            } ?: repository.insertReminder(newReminder)
            // kolektor allReminders automatycznie przebuduje listę i zrestartuje timer
        }
    }

    /**
     * Funkcja do usuwania przypomnienia.
     *
     * W odróżnieniu od wersji z plikiem JSON, nie ma tu ręcznego filtrowania listy w pamięci
     * ani ręcznego wywołania [restartTimer] — o to dba kolektor [allReminders] w bloku `init`,
     * który reaguje na każdą zmianę w bazie Room. Jedyną operacją na wątku głównym jest
     * schowanie wizualnego wskazania usunięcia, a resztą zajmuje się reaktywny Flow.
     *
     * Uwaga: UI wskazuje sukces operacji przed jej faktycznym zakończeniem — w razie błędu
     * usunięcia z bazy lub anulowania powiadomienia użytkownik nie otrzyma informacji zwrotnej.
     *
     * @param reminder Przypomnienie do usunięcia z bazy danych.
     */
    fun deleteReminder(reminder: Reminder) {
        // Chowamy wskaźnik usunięcia natychmiast — lista zaktualizuje się gdy Room wyemituje nowe dane
        updateSuccess { uiState -> uiState.copy(reminderToDelete = null) }

        viewModelScope.launch() {
            val context = getApplication<ReminderShowcaseApplication>()
            reminder.cancelNotification(context)
            repository.deleteReminder(reminder)
            // kolektor allReminders automatycznie przebuduje listę i zrestartuje timer
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
                reminders = buildDisplayedReminders(checked, checkTime, allReminders.value)
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