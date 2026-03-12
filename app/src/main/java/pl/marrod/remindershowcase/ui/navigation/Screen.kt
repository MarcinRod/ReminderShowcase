package pl.marrod.remindershowcase.ui.navigation

import androidx.annotation.StringRes
import pl.marrod.remindershowcase.R

// ============================================================================
// SWITCH 1 – Route definition style
//   true  → sealed class  (Approach 1)
//   false → enum          (Approach 2)
// ============================================================================
const val USE_SEALED_CLASS = true

// ============================================================================
// SWITCH 2 – Argument passing style for the Edit route
//   true  → path segment  → "edit/{reminderId}"         navigate("edit/abc")
//   false → query param   → "edit?reminderId={…}"       navigate("edit?reminderId=abc")
// ============================================================================
const val USE_PATH_ARG = true

// ----------------------------------------------------------------------------
// Approach 1 – Sealed class
// Each destination is a typed object. Compiler guarantees exhaustive
// when-expressions and there is no risk of typos in route strings.
// ----------------------------------------------------------------------------
sealed class Screen(val route: String, @param:StringRes val titleRes: Int) {
    data object List : Screen("list", R.string.screen_title_list)
    data object Add  : Screen("add",  R.string.screen_title_add)
    // path-segment variant:  "edit/{reminderId}"
    // query-param variant:   "edit?reminderId={reminderId}"
    data object Edit : Screen(
        if (USE_PATH_ARG) "edit/{reminderId}" else "edit?reminderId={reminderId}",
        R.string.screen_title_edit
    )

    companion object {
        private val all get() = listOf(List, Add, Edit)

        @StringRes
        fun titleRes(route: String?): Int =
            all.firstOrNull { it.route == route }?.titleRes ?: List.titleRes
    }
}

// ----------------------------------------------------------------------------
// Approach 2 – Enum
// Enums are useful when you need to iterate over destinations (e.g. a
// bottom-nav bar) or want ordinal-based logic. Route strings live in the
// constructor just like the sealed class, so the trade-offs are mostly
// stylistic at this level.
// ----------------------------------------------------------------------------
enum class EnumScreen(val route: String, @param:StringRes val titleRes: Int) {
    LIST("list", R.string.screen_title_list),
    ADD("add",   R.string.screen_title_add),
    // path-segment variant:  "edit/{reminderId}"
    // query-param variant:   "edit?reminderId={reminderId}"
    EDIT(
        if (USE_PATH_ARG) "edit/{reminderId}" else "edit?reminderId={reminderId}",
        R.string.screen_title_edit
    );

    companion object {
        @StringRes
        fun titleRes(route: String?): Int =
            entries.firstOrNull { it.route == route }?.titleRes ?: LIST.titleRes
    }
}

// ----------------------------------------------------------------------------
// Routes – single source of truth used by AppNavHost and MainActivity.
// Flip USE_SEALED_CLASS above to switch the entire app between the two styles.
// ----------------------------------------------------------------------------
object Routes {
    val list: String get() = if (USE_SEALED_CLASS) Screen.List.route else EnumScreen.LIST.route
    val add:  String get() = if (USE_SEALED_CLASS) Screen.Add.route  else EnumScreen.ADD.route
    val edit: String get() = if (USE_SEALED_CLASS) Screen.Edit.route else EnumScreen.EDIT.route

    @StringRes
    fun titleRes(route: String?): Int =
        if (USE_SEALED_CLASS) Screen.titleRes(route) else EnumScreen.titleRes(route)

    /** Concrete navigation call – inserts the real id into the route template. */
    fun editRoute(reminderId: String): String =
        if (USE_PATH_ARG) "edit/$reminderId"
        else              "edit?reminderId=$reminderId"
}
