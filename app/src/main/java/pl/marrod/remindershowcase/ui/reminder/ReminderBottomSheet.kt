package pl.marrod.remindershowcase.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.marrod.remindershowcase.data.Reminder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    onDismiss: () -> Unit,
    reminder: Reminder?,
    onSave: (Reminder) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // sheetState pozwala nam kontrolować zachowanie bottom sheetu,
        // np. czy może być częściowo rozsunięty (skipPartiallyExpanded)
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderForm(
                onSaved = { onSave(it) },
                reminder = reminder
            )
        }
    }
}


