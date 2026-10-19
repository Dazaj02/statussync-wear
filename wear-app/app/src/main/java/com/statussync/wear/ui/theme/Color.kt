package com.statussync.wear.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.statussync.wear.domain.AvailabilityState

// Tokens base (pantalla OLED: fondo negro puro ahorra batería).
val Surface = Color(0xFF000000)
val SurfaceContainer = Color(0xFF1B1F24)
val OnSurface = Color(0xFFF2F4F7)
val OnSurfaceVariant = Color(0xFFB4BAC2)

fun AvailabilityState.color(): Color = Color(colorArgb)

/** Color de texto/ícono legible sobre un fondo cualquiera (para los colores que elige el usuario). */
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF0B0E12) else Color.White

/** Color del ícono sobre el círculo de estado, con contraste suficiente en cada color. */
fun AvailabilityState.onColor(): Color = when (this) {
    AvailabilityState.Free -> Color(0xFF04140B)
    AvailabilityState.Class -> Color(0xFF1A1200)
    AvailabilityState.Focus, AvailabilityState.Call -> Color.White
}
