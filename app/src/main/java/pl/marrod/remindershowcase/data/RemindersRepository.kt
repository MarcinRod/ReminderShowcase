package pl.marrod.remindershowcase.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Klasa repozytorium dla przypomnień, która pośredniczy między warstwą danych (DAO) a resztą aplikacji,
 * zapewniając czystą abstrakcję i enkapsulację logiki dostępu. Nie jest niezbędna w tak prostej aplikacji,
 * ale jest wzorzec często stosowany w architekturze MVVM, który ułatwia zarządzanie danymi, testowanie i utrzymanie kodu.
 * Repozytorium może zawierać dodatkową logikę biznesową, np. filtrowanie, sortowanie, łączenie danych z różnych źródeł (np. lokalna baza danych + API) itp.,
 * ale w tym przypadku pełni głównie rolę pośrednika, delegując operacje do DAO z odpowiednim dyspozytorem (IO) i zapewniając interfejs dla reszty aplikacji.
 *
 */
class RemindersRepository(private val dao: RemindersDao) {
    /**
     * Pobiera wszystkie przypomnienia z bazy danych jako Flow, co pozwala na obserwowanie zmian w bazie danych
     * i automatyczne aktualizowanie UI, gdy dane się zmienią.
     * Metoda nie wymaga oznaczenia jako suspend, ponieważ samo wywołanie funkcji natychmiast zwraca
     * obiekt Flow, nie wykonując żadnej pracy. Faktyczne zapytanie do bazy danych jest wykonywane
     * dopiero podczas kolekcji (collect) Flow w kontekście korutyny.
     */
    val allReminders = dao.getAllReminders()

    /**
     * Operacje na bazie danych (wstawianie, aktualizacja, usuwanie) są oznaczone jako suspend, ponieważ wykonują zapytania do bazy danych,
     * które są operacjami blokującymi i powinny być wykonywane w kontekście korutyny. Użycie withContext(Dispatchers.IO) zapewnia, że
     * te operacje są wykonywane na odpowiednim wątku. W przypadku tego repozytorium jego główną
     * rolą jest delegowanie tych operacji do DAO na odpowiednim wątku,
     * dzięki czemu nie musimy martwić się o zarządzanie wątkami w innych częściach aplikacji, które korzystają z tego repozytorium.
     */

    /**
     * Wstawia nowe przypomnienie do bazy danych. Jeśli przypomnienie o tym samym id już istnieje, zostanie zastąpione (REPLACE).
     */
    suspend fun insertReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        dao.insertReminder(reminder)
    }

    /**
     * Usuwa przypomnienie z bazy danych.
     */
    suspend fun deleteReminder(reminder: Reminder) = withContext(Dispatchers.IO)
    {
        dao.deleteReminder(reminder)
    }

    /**
     * Usuwa przypomnienie z bazy danych na podstawie jego unikalnego identyfikatora (id).
     */
    suspend fun updateReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        dao.updateReminder(reminder)
    }

    // Poniżej dwie wersje pobierania pojedynczego przypomnienia jako Flow,
    // pokazujące różnicę między natywnym Flow z Room a opakowaniem suspend fun w flow builder.
    // Zalecana jest pierwsza wersja, która jest prawdziwie reaktywna i automatycznie emituje nowe wartości przy każdej zmianie w bazie danych,
    // podczas gdy druga wersja emituje wartość jednorazowo i nie reaguje na późniejsze zmiany, co może prowadzić do nieaktualnych danych w UI,
    // jeśli przypomnienie zostanie zaktualizowane po pierwszym pobraniu. W praktyce, jeśli potrzebujemy obserwować zmiany pojedynczego przypomnienia,
    // powinniśmy używać natywnego Flow z Room.
    // Druga wersja może być użyteczna w sytuacjach, gdy chcemy pobrać przypomnienie jednorazowo,
    // np. do wyświetlenia szczegółów w UI, ale musimy być świadomi jej ograniczeń w kontekście aktualizacji danych.
    /**
     * Pobiera pojedyncze przypomnienie z bazy danych na podstawie jego unikalnego identyfikatora (id) i zwraca je jako Flow.
     * Wersja 1 — natywny Flow z Room.
     * Room wewnętrznie obserwuje tabelę i emituje nową wartość za każdym razem,
     * gdy wiersz o podanym [id] zostanie zmieniony. Prawdziwie reaktywna wersja.
     */
    fun getReminderByIdFlow(id: String): Flow<Reminder?> = dao.getReminderByIdFlow(id)

    /**
     * Wersja 2 — suspend fun opakowana w flow builder.
     * Emituje wartość jednorazowo i kończy flow. Nie reaguje na późniejsze zmiany w bazie.
     * Odpowiednik Rozwiązania A, ale z interfejsem Flow zamiast suspend fun.
     */
    fun getReminderByIdAsFlow(id: String): Flow<Reminder?> = flow {
        emit(dao.getReminderById(id))
    }
}