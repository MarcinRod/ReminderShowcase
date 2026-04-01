package pl.marrod.remindershowcase.factory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.marrod.remindershowcase.ReminderShowcaseApplication
import pl.marrod.remindershowcase.ui.screens.ReminderDetailViewModel
import pl.marrod.remindershowcase.ui.screens.ReminderListViewModel

/**
 * Obiekt dostarczający "fabryki" (factory) dla klas ViewModel.
 * Jest to miejsce, gdzie definiujemy, jak tworzyć instancje naszych klas ViewModel,
 * zwłaszcza gdy wymagają one niestandardowych konstruktorów (np. z parametrami).
 */
object AppWideViewModelProvider {
    /**
     * Fabryka dla klas ViewModel, która wykorzystuje funkcję `viewModelFactory` do tworzenia instancji.
     * viewModelFactory to funkcja pomocnicza, która upraszcza tworzenie fabryk dla ViewModel,
     * zwłaszcza gdy potrzebujemy przekazać niestandardowe parametry do konstruktora.
     */
    val factory = viewModelFactory {
        // każda klasa ViewModel, która jest tworzona za pomocą tej fabryki, będzie inicjalizowana zgodnie z instrukcjami w bloku initializer.
        // W tym przypadku mamy tylko jeden ViewModel, ReminderListViewModel, ale można dodać więcej, jeśli będzie potrzeba.
        initializer {
            // funkcja viewModelFactory zapewnia dostęp do CreationExtras,
            // które zawierają kontekst aplikacji dzięki czemu możemy uzyskać dostęp do innych komponentów aplikacji, takich jak storage.
            // W tym przypadku instancja klasy aplikacji (ReminderShowcaseApplication) jest uzyskiwana
            // za pomocą funkcji rozszerzającej `reminderShowcaseApplication()`, która jest zdefiniowana w ReminderShowcaseApplication.kt.
            ReminderListViewModel(
                application = reminderShowcaseApplication(),
                repository = reminderShowcaseApplication().remindersRepository,
                //storage = reminderShowcaseApplication().storage
            )
        }

        initializer {
            // viewModelFactory pozwala na automatycznie stworzenie SavedStateHandle, które można
            // wykorzystać do odczytywania argumentów z przejść między ekranami (np. z NavController).
            // Dzięki temu możemy łatwo przekazywać dane do ViewModel, które są potrzebne do jego działania.
            ReminderDetailViewModel(
                savedStateHandle = this.createSavedStateHandle(),
                repository = reminderShowcaseApplication().remindersRepository,
                //storage = reminderShowcaseApplication().storage
            )
        }

    }
}
/**
 * Funkcja rozszerzająca dla CreationExtras, która umożliwia łatwe uzyskanie instancji ReminderShowcaseApplication
 * z kontekstu, który jest dostępny w CreationExtras podczas tworzenia ViewModel
 */
fun CreationExtras.reminderShowcaseApplication(): ReminderShowcaseApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReminderShowcaseApplication)