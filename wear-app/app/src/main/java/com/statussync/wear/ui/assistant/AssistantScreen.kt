package com.statussync.wear.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.statussync.wear.R
import com.statussync.wear.domain.Suggestion
import com.statussync.wear.domain.SuggestionReason
import com.statussync.wear.ui.theme.color
import com.statussync.wear.ui.theme.onColor
import com.statussync.wear.ui.theme.onColorFor

/** Ajustes del asistente: tres interruptores y botones para probarlo sin datos reales. */
@Composable
fun AssistantScreen(
    ui: AssistantUiState,
    onCalendar: (Boolean) -> Unit,
    onMotion: (Boolean) -> Unit,
    onAuto: (Boolean) -> Unit,
    onSimulateClass: () -> Unit,
    onSimulateStillness: () -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.assistant_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = stringResource(R.string.assistant_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }
        item { Toggle(R.string.assistant_calendar, R.string.assistant_calendar_hint, ui.settings.calendar, onCalendar) }
        item { Toggle(R.string.assistant_motion, R.string.assistant_motion_hint, ui.settings.motion, onMotion) }
        item { Toggle(R.string.assistant_auto, R.string.assistant_auto_hint, ui.settings.autoApply, onAuto) }
        item {
            Text(
                text = stringResource(R.string.assistant_simulate_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        item { SimulateButton(stringResource(R.string.assistant_simulate_class), onSimulateClass) }
        item { SimulateButton(stringResource(R.string.assistant_simulate_still), onSimulateStillness) }
    }
}

@Composable
private fun Toggle(labelRes: Int, hintRes: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        secondaryLabel = { Text(stringResource(hintRes)) },
    )
}

@Composable
private fun SimulateButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        icon = { IconBadge(R.drawable.ic_assistant, Color(0xFF7A4FD6)) },
        label = { Text(label) },
    )
}

/** Tarjeta de sugerencia (Aceptar / Ahora no) y aviso de cambio automático (Deshacer), para la pantalla principal. */
fun ScalingLazyListScope.assistantItems(
    ui: AssistantUiState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
) {
    ui.pending?.let { suggestion ->
        val accent = suggestion.state.color()
        item {
            Text(
                text = stringResource(R.string.suggestion_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = stringResource(
                    R.string.suggestion_switch_format,
                    stringResource(suggestion.state.labelRes),
                    suggestion.durationMin,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = stringResource(suggestion.reason.textRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = suggestion.state.onColor()),
                icon = { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(stringResource(R.string.suggestion_accept)) },
            )
        }
        item {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                icon = { Icon(painterResource(R.drawable.ic_close), contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(stringResource(R.string.suggestion_dismiss)) },
            )
        }
    }
    ui.undo?.let { applied ->
        item {
            Text(
                text = stringResource(R.string.auto_applied_format, stringResource(applied.state.labelRes)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = onUndo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                icon = { IconBadge(R.drawable.ic_undo, Color(0xFFE0A100)) },
                label = { Text(stringResource(R.string.auto_undo)) },
            )
        }
    }
}

@Composable
fun IconBadge(iconRes: Int, accent: Color) {
    Box(Modifier.size(36.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = onColorFor(accent),
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun SuggestionReason.textRes(): Int = when (this) {
    SuggestionReason.ScheduleNow -> R.string.reason_schedule_now
    SuggestionReason.ScheduleSoon -> R.string.reason_schedule_soon
    SuggestionReason.Stillness -> R.string.reason_stillness
}
