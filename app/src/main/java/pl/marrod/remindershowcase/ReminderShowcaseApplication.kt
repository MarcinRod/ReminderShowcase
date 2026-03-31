package pl.marrod.remindershowcase

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import pl.marrod.remindershowcase.data.ReminderStorage

/**
 * Własna klasa Application, która inicjalizuje ReminderStorage i udostępnia go dla całej aplikacji.
 */
class ReminderShowcaseApplication() : Application() {

    /** zmienna tylko do odczytu (private set), która przechowuje instancję ReminderStorage.
     * Jest oznaczona jako lateinit, co oznacza, że zostanie zainicjalizowana później, w funkcji onCreate. */
    lateinit var storage: ReminderStorage
        private set

    // Funkcja onCreate jest wywoływana, gdy aplikacja jest uruchamiana.
    // Inicjalizujemy ReminderStorage, który będzie używany w całej aplikacji do zarządzania przypomnieniami.
    override fun onCreate() {
        super.onCreate()
        storage = ReminderStorage(this)
    }
}

