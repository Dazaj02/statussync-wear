package com.statussync.wear.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.statussync.wear.R

/** Estados del contrato compartido (docs/plan/CONTRATO.md §1). */
enum class AvailabilityState(
    val wireName: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val colorArgb: Long,
    val defaultDurationMin: Int?,
    /** Duraciones que ofrece el selector del reloj. Vacío = sin temporizador. */
    val durationOptionsMin: List<Int>,
) {
    Free("free", R.string.state_free, R.drawable.ic_state_free, 0xFF2E9E5B, null, emptyList()),
    Focus("focus", R.string.state_focus, R.drawable.ic_state_focus, 0xFFD64545, 50, listOf(25, 50, 90)),
    Class("class", R.string.state_class, R.drawable.ic_state_class, 0xFFE0A100, null, listOf(45, 60, 90)), // en W5 dura lo que el bloque de agenda
    Call("call", R.string.state_call, R.drawable.ic_state_call, 0xFF7A4FD6, 30, listOf(15, 30, 60));

    val hasTimer: Boolean get() = durationOptionsMin.isNotEmpty()

    /** Duración al cambiar con un solo toque (Tile): la por defecto, o la primera opción. Null en Libre. */
    val quickDurationMin: Int? get() = defaultDurationMin ?: durationOptionsMin.firstOrNull()

    companion object {
        fun fromWire(value: String): AvailabilityState? = entries.firstOrNull { it.wireName == value }
    }
}
