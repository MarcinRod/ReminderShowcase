package pl.marrod.remindershowcase.ui.reminder

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.marrod.remindershowcase.data.Reminder
import kotlin.math.roundToInt


@Composable
fun ReminderItem(
    reminder: Reminder,
    isFromPast: Boolean,
    isRevealed: Boolean,
    onReveal: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {

    val density = LocalDensity.current
    val revealWidthPx = with(density) { 56.dp.toPx() }
    val dismissThresholdPx = revealWidthPx * 3f
    val scope = rememberCoroutineScope()
    // Animatable to obiekt, który przechowuje aktualną wartość przesunięcia (offsetX)
    // i pozwala na animowanie tej wartości w czasie. Zasadnie jest to "stan" przechowujący
    // aktualne przesunięcie karty, który może być animowany do nowych wartości
    val offsetX = remember { Animatable(0f) }
    val containerColor =
        if (!isFromPast) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val textColor =
        if (!isFromPast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.6f
        )

    fun snapToRevealed() {
        scope.launch {
            offsetX.animateTo(
                -revealWidthPx,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            )
        }
        onReveal(true)
    }
    fun snapToHidden() {
        scope.launch {
            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    fun settle() {
        // zapisz aktualny stan przesunięcia i zdecyduj,
        // czy karta powinna zostać usunięta, wrócić do pozycji początkowej, czy pozostać odsłonięta
        val current = offsetX.value
        when {
            current < -dismissThresholdPx -> {
                // Przekroczyliśmy próg usunięcia, animuj kartę poza ekran i wywołaj onDelete
                scope.launch {
                    offsetX.animateTo(-3000f, tween(300, easing = FastOutLinearInEasing))
                    onDelete()
                    onReveal(false)
                }
            }

            current > -(revealWidthPx / 2f) -> {
                // Przesunięcie jest zbyt małe, wróć do pozycji początkowej
                onReveal(false)
                snapToHidden()
            }
            // Przesunięcie jest wystarczające, ale nie przekracza progu usunięcia
            else -> snapToRevealed() // powrót karty do pozycji z odsłoniętą ikoną "Usuń"
        }
    }



    // Efekt pozwalający zmienić stan odsłonięcia karty w reakcji na zmianę wartości isRevealed.
    // Jeśli isRevealed stanie się false, karta zostanie automatycznie przesunięta z powrotem
    // do pozycji ukrytej. W tej aplikacji zdarzy się to:
    // - gdy użytkownik odsłoni kartę jednego przypomnienia, a następnie spróbuje odsłonić inną kartę
    // - gdy użytkownik zacznie przesuwać listę
    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            snapToHidden()
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        // Czerwone tło z ikoną "Usuń", które jest widoczne, gdy karta jest odsłonięta
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CardDefaults.shape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.TwoTone.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) { // specjalny modyfikator do obsługi gestów dotykowych
                    // Gesty możliwe do wykrycia rozpoczynają się od przedrostka detect,
                    // np. detectTapGestures, detectDragGestures, detectHorizontalDragGestures itp.
                    detectHorizontalDragGestures(  // Wykrywamy tylko gesty poziomego przeciągania,
                        // aby uniknąć konfliktów z przewijaniem listy
                        onDragStart = { _ ->
                            // rozpoczęcie przeciągania
                            // W tym kodzie tylko logujemy, ale można tu dodać dodatkową logikę,
                            // np. informowanie rodzica o rozpoczęciu przeciągania
                            Log.i(
                                "ReminderItemSimple",
                                "Drag started for reminder with id ${reminder.id}, isRevealed = $isRevealed"
                            )
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // podczas przeciągania, aktualizujemy offsetX o wartość dragAmount
                            // (jest to różnica w pikselach od ostatniego zdarzenia przeciągania)
                            Log.i(
                                "ReminderItemSimple",
                                "Dragging reminder with id ${reminder.id}, dragAmount = $dragAmount"
                            )
                            // consume() informuje system, że ten gest został obsłużony i nie powinien być przekazywany dalej
                            change.consume()
                            // Aktualizujemy offsetX, ale ograniczamy go do wartości 0 (nie pozwalamy przesuwać karty w prawo)
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceAtMost(0f))
                            }
                        },
                        onDragEnd = {
                            // zakończenie przeciągania, decydujemy, czy karta powinna zostać usunięta,
                            // wrócić do pozycji początkowej, czy pozostać odsłonięta.
                            // Wystąpi, gdy użytkownik puści kartę po przeciągnięciu.
                            Log.i(
                                "ReminderItemSimple",
                                "Drag ended for reminder with id ${reminder.id}, settling..."
                            )
                            settle()
                        },
                        onDragCancel = {
                            // przeciąganie zostało anulowane (np. przez system lub inny gest),
                            // karta powinna wrócić do pozycji początkowej
                            Log.i(
                                "ReminderItemSimple",
                                "Drag cancelled for reminder with id ${reminder.id}, settling..."
                            )
                            settle()
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = reminder.displayDateTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,

                            )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.TwoTone.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
        }
    }
}
