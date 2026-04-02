package pl.marrod.remindershowcase

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import pl.marrod.remindershowcase.data.RemindersDb
import pl.marrod.remindershowcase.data.RemindersRepository

/**
 * Własna klasa Application, która inicjalizuje ReminderStorage i udostępnia go dla całej aplikacji.
 */
class ReminderShowcaseApplication() : Application() {

    /**(STARA WERSJA DLA PORÓWNANIA) zmienna tylko do odczytu (private set), która przechowuje instancję ReminderStorage.
     * Jest oznaczona jako lateinit, co oznacza, że zostanie zainicjalizowana później, w funkcji onCreate. */
  //  lateinit var storage: ReminderStorage
   //     private set

    /**
     * dostęp do repozytorium przypomnień, które jest inicjalizowane przy użyciu DAO z bazy danych Room.
     * inicjalizacja repozytorium odbywa się w sposób leniwy (lazy),
     * co oznacza, że zostanie utworzone dopiero przy pierwszym dostępie
     * do tej właściwości. Dzięki temu unikamy niepotrzebnej inicjalizacji,
     * jeśli repozytorium nie jest używane od razu po uruchomieniu aplikacji.
     */
    val remindersRepository by lazy { //
         RemindersRepository(
            RemindersDb.getInstance(this).remindersDao()
        )
    }
    // Funkcja onCreate jest wywoływana, gdy aplikacja jest uruchamiana.
    // Inicjalizujemy ReminderStorage, który będzie używany w całej aplikacji do zarządzania przypomnieniami.
    override fun onCreate() {
        super.onCreate()
    //    storage = ReminderStorage(this) // stara wersja (dla porównania, jak było wcześniej bez repozytorium)
    }
}

