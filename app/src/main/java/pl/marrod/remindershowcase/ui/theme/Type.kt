package pl.marrod.remindershowcase.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pl.marrod.remindershowcase.R


private val IbmMono = FontFamily(
    Font(R.font.ibm_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_mono_medium,  FontWeight.Medium),
    Font(R.font.ibm_mono_bold,    FontWeight.Bold),
)

// Set of Material typography styles to start with
val Typography = Typography().run {
    copy(
        displayLarge   = displayLarge.copy(fontFamily = IbmMono),
        displayMedium  = displayMedium.copy(fontFamily = IbmMono),
        displaySmall   = displaySmall.copy(fontFamily = IbmMono),
        headlineLarge  = headlineLarge.copy(fontFamily = IbmMono),
        headlineMedium = headlineMedium.copy(fontFamily = IbmMono),
        headlineSmall  = headlineSmall.copy(fontFamily = IbmMono),
        titleLarge     = titleLarge.copy(fontFamily = IbmMono),
        titleMedium    = titleMedium.copy(fontFamily = IbmMono),
        titleSmall     = titleSmall.copy(fontFamily = IbmMono),
        bodyLarge      = bodyLarge.copy(fontFamily = IbmMono),
        bodyMedium     = bodyMedium.copy(fontFamily = IbmMono),
        bodySmall      = bodySmall.copy(fontFamily = IbmMono),
        labelLarge     = labelLarge.copy(fontFamily = IbmMono),
        labelMedium    = labelMedium.copy(fontFamily = IbmMono),
        labelSmall     = labelSmall.copy(fontFamily = IbmMono),
    )
}

