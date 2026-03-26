package pl.marrod.remindershowcase.ui.navigation

import kotlinx.serialization.Serializable
import pl.marrod.remindershowcase.BuildConfig

sealed interface Destination {

    @Serializable
    data object List : Destination
    @Serializable
    data class Details(val reminderId: String) : Destination{
        companion object {
            const val DEEP_LINK_URI =  "${BuildConfig.DEEP_LINK_SCHEME}://${BuildConfig.DEEP_LINK_HOST}/{reminderId}"
        }
    }


}
