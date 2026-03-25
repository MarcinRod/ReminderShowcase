package pl.marrod.remindershowcase.ui.navigation

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import pl.marrod.remindershowcase.R

sealed interface Destination {

    @Serializable
    data object List : Destination
    @Serializable
    data class Details(val reminderId: String) : Destination


}
