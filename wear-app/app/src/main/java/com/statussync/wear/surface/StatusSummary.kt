package com.statussync.wear.surface

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.Status
import java.time.Duration
import java.time.Instant

/** Lo que muestran el Tile y la Complication del estado actual, ya sin depender de Android. */
data class StatusSummary(
    val state: AvailabilityState,
    /** Nombre propio si el estado lo tiene; null = usar el nombre del estado base. */
    val customName: String?,
    val colorArgb: Long,
    /** Minutos que faltan, redondeados hacia arriba. Null si no hay temporizador. */
    val remainingMinutes: Long?,
)

fun Status.toSummary(now: Instant): StatusSummary = StatusSummary(
    state = state,
    customName = custom?.name,
    colorArgb = custom?.colorArgb ?: state.colorArgb,
    remainingMinutes = remaining(now)?.let { ceilMinutes(it) },
)

private fun ceilMinutes(duration: Duration): Long = (duration.seconds + 59) / 60
