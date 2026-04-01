package pl.marrod.remindershowcase.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Klasa pomocnicza do łatwiejszego zarządzania tekstami w UI.
 * Pozwala na przechowywanie zarówno statycznych tekstów z zasobów,
 * jak i dynamicznych tekstów generowanych w czasie działania aplikacji.
 */
sealed class UiText {
    data class Dynamic(val text: String) : UiText()
    data class Resource(val resId: Int) : UiText()
}
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> text
    is UiText.Resource -> stringResource(resId)
}