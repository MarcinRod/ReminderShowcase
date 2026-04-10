# ReminderShowcase
**Readme stworzone przez AI (zweryfikowane przez człowieka)**  

Aplikacja Android stworzona w celach edukacyjnych, demonstrująca popularne wzorce i technologie stosowane w nowoczesnym rozwoju aplikacji na Androida z Kotlin i Jetpack Compose.

## Opis projektu

ReminderShowcase to w pełni działająca aplikacja do zarządzania przypomnieniami. Projekt służy jako materiał dydaktyczny prezentujący:

- architekturę **MVVM** (Model–View–ViewModel),
- lokalną bazę danych z **Room**,
- reaktywne zarządzanie stanem za pomocą **StateFlow** i **Flow**,
- planowanie powiadomień systemowych przez **AlarmManager**,
- nawigację między ekranami z **Navigation Compose**,
- obsługę **deep linków** z powiadomień,
- **obsługę uprawnień** w czasie działania (runtime permissions),
- animacje widoczności elementów w Compose.

---

## Funkcje

| Funkcja | Opis |
|---|---|
| Lista przypomnień | Wyświetla przyszłe i przeszłe przypomnienia z możliwością filtrowania |
| Dodawanie przypomnienia | Formularz w BottomSheet z wyborem tytułu, opisu i daty/godziny |
| Edycja przypomnienia | Ponowne otwarcie formularza z wypełnionymi danymi |
| Usuwanie przypomnienia | Swipe w lewo na elemencie listy |
| Odliczanie czasu | Dynamiczny licznik czasu do następnego przypomnienia (aktualizowany co sekundę) |
| Powiadomienia | Systemowe powiadomienia planowane przez `AlarmManager` z budzeniem urządzenia |
| Uprawnienia | Prośba o uprawnienie `POST_NOTIFICATIONS` w czasie działania, Snackbar z akcją „Otwórz ustawienia" po odmowie |
| Szczegóły przypomnienia | Osobny ekran z pełnymi danymi, otwierany też przez deep link z powiadomienia |
| Stan ładowania | Ekran ładowania z symulowanym opóźnieniem i animacją wejścia listy |

---

## Architektura

Projekt stosuje architekturę **MVVM** z jednym kierunkiem przepływu danych (Unidirectional Data Flow):

```
UI (Compose) ──► ViewModel ──► Repository ──► Room DAO
     ▲                │
     └── StateFlow ◄──┘
```

### Warstwy

- **`data/`** – encja `Reminder` (Room), `RemindersDao`, `RemindersDb`, `RemindersRepository`
- **`ui/screens/`** – ekrany aplikacji z powiązanymi ViewModelami (`ReminderListScreen`, `ReminderDetailScreen`)
- **`ui/reminder/`** – komponenty formularza (`ReminderForm`, `ReminderBottomSheet`, `ReminderItem`, `ReminderFormViewModel`)
- **`ui/navigation/`** – konfiguracja nawigacji (`AppNavHost`, `Destination`)
- **`notifications/`** – `NotificationHelper`, `ReminderReceiver` (BroadcastReceiver)
- **`utils/`** – formatowanie tekstu i czasu
- **`factory/`** – `AppWideViewModelProvider` (fabryka ViewModeli)

---

## Struktura projektu

```
app/src/main/java/pl/marrod/remindershowcase/
├── data/
│   ├── Reminder.kt                   # Encja Room + logika powiadomień
│   ├── RemindersDao.kt               # Interfejs dostępu do danych (DAO)
│   ├── RemindersDb.kt                # Baza danych Room
│   └── RemindersRepository.kt        # Repozytorium (warstwa abstrakcji)
├── factory/
│   └── AppWideViewModelProvider.kt   # Fabryka ViewModeli
├── notifications/
│   ├── NotificationHelper.kt         # Tworzenie kanału i wyświetlanie powiadomień
│   └── ReminderReceiver.kt           # BroadcastReceiver odbierający alarmy
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt             # Graf nawigacji
│   │   └── Screen.kt                 # Definicje destynacji (Destination)
│   ├── reminder/
│   │   ├── ReminderBottomSheet.kt    # BottomSheet z formularzem
│   │   ├── ReminderForm.kt           # Formularz przypomnienia
│   │   ├── ReminderFormViewModel.kt  # ViewModel formularza
│   │   └── ReminderItem.kt           # Element listy przypomnień
│   ├── screens/
│   │   ├── ReminderDetailScreenWithViewModel.kt
│   │   ├── ReminderDetailViewModel.kt
│   │   ├── ReminderListScreenWithViewModel.kt
│   │   └── ReminderListViewModel.kt
│   └── theme/                        # Konfiguracja motywu Material 3
├── utils/
│   ├── Text.kt                       # Rozszerzenia formatowania tekstu
│   └── Time.kt                       # Rozszerzenia formatowania czasu
├── MainActivity.kt
└── ReminderShowcaseApplication.kt
```

---

## Jak działają powiadomienia

1. Przy zapisaniu przypomnienia wywoływana jest `Reminder.scheduleNotification(context)`.
2. `AlarmManager.setExactAndAllowWhileIdle` planuje alarm na wskazany `timestamp` (działa nawet w trybie uśpienia (Doze)).
3. Gdy alarm się wyzwoli, system uruchamia `ReminderReceiver` (BroadcastReceiver).
4. `ReminderReceiver` przekazuje dane do `NotificationHelper.showReminder(...)`, który wyświetla systemowe powiadomienie.
5. Kliknięcie powiadomienia otwiera `MainActivity` z deep linkiem `remindershowcase://reminder/{reminderId}`, który nawiguje do ekranu szczegółów.

---

## Deep linki

Format URI: `remindershowcase://reminder/{reminderId}`

Deep linki są zdefiniowane w `AndroidManifest.xml` oraz w `AppNavHost` (destynacja `Destination.Details`). Umożliwiają bezpośrednie otwarcie ekranu szczegółów przypomnienia z powiadomienia lub zewnętrznego źródła.

---

## Uprawnienia

Aplikacja wymaga następujących uprawnień (zdefiniowanych w `AndroidManifest.xml`):

- `POST_NOTIFICATIONS` – wyświetlanie powiadomień (Android 13+)
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` – precyzyjne planowanie alarmów
- `RECEIVE_BOOT_COMPLETED` – wznowienie alarmów po restarcie urządzenia *(jeśli zaimplementowane)*

---

## Omawiane koncepty

Projekt jest przeznaczony jako materiał do nauki i ilustruje m.in.:

- **Sealed class vs sealed interface** jako modele stanu UI (`ScreenUiState`)
- **`StateFlow` vs `Flow`** — kiedy używać którego
- **`stateIn`** — przekształcanie `Flow` na `StateFlow` w ViewModelu
- **`SharingStarted.WhileSubscribed`** — optymalizacja subskrypcji
- **`viewModelFactory` / `initializer`** — niestandardowe fabryki ViewModeli z parametrami
- **`AndroidViewModel`** — dostęp do kontekstu aplikacji w ViewModelu
- **Room `@Entity`, `@PrimaryKey`, `@ColumnInfo`** — mapowanie klasy na tabelę
- **`PendingIntent`** — jak działa i dlaczego jest potrzebny
- **`AlarmManager.setExactAndAllowWhileIdle`** — dokładne alarmy a tryb Doze
- **Navigation Compose z type-safe routes** (`@Serializable` destynacje)
- **Deep linki** w nawigacji Compose
- **`BroadcastReceiver`** — odbieranie systemowych zdarzeń
- **Runtime permissions** — żądanie uprawnień w czasie działania aplikacji:
  - `ActivityResultContracts.RequestPermission()` — launcher do żądania uprawnienia `POST_NOTIFICATIONS`
  - `ContextCompat.checkSelfPermission` — sprawdzanie aktualnego stanu uprawnienia
  - obsługa Androida 13+ (API 33) vs. starszych wersji (automatyczne przyznanie uprawnienia podczas instalacji)
  - `onResume()` — ponowne sprawdzanie uprawnień po powrocie z tła
  - `ActivityResultContracts.StartActivityForResult()` — launcher otwierający ustawienia aplikacji (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`)
  - `Snackbar` z akcją „Otwórz ustawienia" — informowanie użytkownika o braku uprawnienia i umożliwienie jego ręcznego nadania
  - `mutableStateOf` do reaktywnego propagowania stanu uprawnień do UI (Compose)

---






