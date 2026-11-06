package com.statussync.wear.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.statussync.wear.domain.AvailabilityState

/** Color primario de la app: fondo de todas las pantallas que no muestran un estado. */
val BrandPrimary = Color(0xFF3B82F6)

val OnSurface = Color(0xFFF4F6FB)

fun AvailabilityState.color(): Color = Color(colorArgb)

/**
 * Fondo de la app: el mismo tono del color primario, oscuro y saturado. Nunca negro, blanco ni gris.
 * Con texto claro encima da más de 10:1 de contraste con cualquiera de los cuatro estados.
 */
fun backgroundFor(primary: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    hsv[1] = 0.80f
    hsv[2] = 0.27f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Color de texto/ícono legible sobre un fondo cualquiera (para los colores que elige el usuario). */
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF0B0E12) else Color.White

/** Color del ícono sobre el círculo de estado, con contraste suficiente en cada color. */
fun AvailabilityState.onColor(): Color = when (this) {
    AvailabilityState.Free -> Color(0xFF04140B)
    AvailabilityState.Class -> Color(0xFF1A1200)
    AvailabilityState.Focus, AvailabilityState.Call -> Color.White
}
