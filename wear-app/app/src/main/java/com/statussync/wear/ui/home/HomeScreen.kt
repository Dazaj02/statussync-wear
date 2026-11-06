package com.statussync.wear.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.statussync.wear.R
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.ui.assistant.AssistantUiState
import com.statussync.wear.ui.assistant.assistantItems
import com.statussync.wear.ui.format.formatClock
import com.statussync.wear.ui.theme.color
import com.statussync.wear.ui.theme.onColor
import com.statussync.wear.ui.theme.onColorFor

@Composable
fun HomeScreen(
    ui: HomeUiState,
    customStates: List<CustomState>,
    onFree: () -> Unit,
    onPickTimer: (AvailabilityState) -> Unit,
    onPickCustom: (CustomState) -> Unit,
    onAdd: () -> Unit,
    onRemoveCustom: (CustomState) -> Unit,
    onGroup: () -> Unit,
    assistant: AssistantUiState,
    onAccept: () -> Unit,
    onDismissSuggestion: () -> Unit,
    onUndo: () -> Unit,
    onAssistant: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        // El estado actual abre centrado en pantalla; los botones aparecen al deslizar.
        state = rememberScalingLazyListState(initialCenterItemIndex = 0),
        autoCentering = AutoCenteringParams(itemIndex = 0),
    ) {
        item { CurrentStatus(ui) }
        assistantItems(assistant, onAccept, onDismissSuggestion, onUndo)
        items(AvailabilityState.entries.size) { index ->
            val state = AvailabilityState.entries[index]
            StateButton(
                label = stringResource(state.labelRes),
                color = state.color(),
                onColor = state.onColor(),
                iconRes = state.iconRes,
                selected = ui.custom == null && state == ui.state,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (state.hasTimer) onPickTimer(state) else onFree()
                },
            )
        }
        items(customStates.size) { index ->
            val custom = customStates[index]
            val color = Color(custom.colorArgb)
            StateButton(
                label = custom.name,
                color = color,
                onColor = onColorFor(color),
                iconRes = custom.base.iconRes,
                selected = ui.custom?.id == custom.id,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPickCustom(custom)
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemoveCustom(custom)
                },
                longClickLabel = stringResource(R.string.action_remove_state),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionIcon(R.drawable.ic_assistant, stringResource(R.string.action_assistant), onAssistant)
                ActionIcon(R.drawable.ic_group, stringResource(R.string.action_group), onGroup)
                if (customStates.size < CustomState.MAX_COUNT) {
                    ActionIcon(R.drawable.ic_add, stringResource(R.string.action_add_state), onAdd)
                }
            }
        }
        if (customStates.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.remove_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun CurrentStatus(ui: HomeUiState) {
    val target = ui.custom?.let { Color(it.colorArgb) } ?: ui.state.color()
    val onTarget = ui.custom?.let { onColorFor(target) } ?: ui.state.onColor()
    // Una sola animación: el anillo cambia de color y de avance al cambiar de estado.
    val color by animateColorAsState(target, tween(300), label = "statusColor")
    val onColor by animateColorAsState(onTarget, tween(300), label = "statusOnColor")
    val progress by animateFloatAsState(ui.progress, tween(300), label = "statusProgress")
    val track = MaterialTheme.colorScheme.surfaceContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Box(
                modifier = Modifier.size(52.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(ui.state.iconRes),
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            text = ui.custom?.name ?: stringResource(ui.state.labelRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = ui.remaining?.let { stringResource(R.string.remaining_format, it.formatClock()) }
                ?: stringResource(R.string.free_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun StateButton(
    label: String,
    color: Color,
    onColor: Color,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = longClickLabel,
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        // El estado activo se marca con borde, texto y ícono de verificación, no solo con color.
        border = if (selected) BorderStroke(2.dp, color) else null,
        icon = {
            Box(Modifier.size(36.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (selected) R.drawable.ic_check else iconRes),
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        label = { Text(label) },
        secondaryLabel = if (selected) ({ Text(stringResource(R.string.state_active)) }) else null,
    )
}

/** Acción secundaria: botón redondo de 48 dp con ícono neutro (el color queda reservado para los estados). */
@Composable
private fun ActionIcon(@DrawableRes iconRes: Int, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(IconButtonDefaults.DefaultButtonSize),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = description, modifier = Modifier.size(24.dp))
    }
}
