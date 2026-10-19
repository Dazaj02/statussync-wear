package com.statussync.wear.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.statussync.wear.R
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.ui.format.formatClock
import com.statussync.wear.ui.theme.SurfaceContainer
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
    onRemove: () -> Unit,
    onGroup: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { CurrentStatus(ui) }
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
            )
        }
        item { ActionButton(stringResource(R.string.action_group), R.drawable.ic_group, onGroup) }
        if (customStates.size < CustomState.MAX_COUNT) {
            item { ActionButton(stringResource(R.string.action_add_state), R.drawable.ic_add, onAdd) }
        }
        if (customStates.isNotEmpty()) {
            item { ActionButton(stringResource(R.string.action_remove_state), R.drawable.ic_delete, onRemove) }
        }
    }
}

@Composable
private fun CurrentStatus(ui: HomeUiState) {
    val color = ui.custom?.let { Color(it.colorArgb) } ?: ui.state.color()
    val onColor = ui.custom?.let { onColorFor(color) } ?: ui.state.onColor()
    val glow = remember(color) { Brush.radialGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 9.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawCircle(brush = glow, radius = size.minDimension / 2)
                drawArc(
                    color = SurfaceContainer,
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
                    sweepAngle = 360f * ui.progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Box(
                modifier = Modifier.size(76.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(ui.state.iconRes),
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(40.dp),
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
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        icon = {
            Box(Modifier.size(36.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        label = { Text(label) },
    )
}

@Composable
private fun ActionButton(label: String, @DrawableRes iconRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
        label = { Text(label) },
    )
}
