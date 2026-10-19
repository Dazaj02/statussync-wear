package com.statussync.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.statussync.wear.domain.AvailabilityState

private val StatusSyncColors = ColorScheme(
    primary = AvailabilityState.Free.color(),
    onPrimary = Surface,
    background = Surface,
    onBackground = OnSurface,
    surfaceContainer = SurfaceContainer,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = AvailabilityState.Focus.color(),
)

@Composable
fun StatusSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StatusSyncColors, content = content)
}
