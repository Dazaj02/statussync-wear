package com.statussync.wear.domain

import java.time.Duration
import java.time.Instant

/**
 * Estado de disponibilidad actual. `state` es siempre uno de los 4 del contrato; `custom` solo cambia
 * cómo se muestra en el reloj. `until` es null mientras no haya temporizador (Libre).
 */
data class Status(
    val state: AvailabilityState,
    val startedAt: Instant,
    val until: Instant?,
    val custom: CustomState? = null,
) {
    fun remaining(now: Instant): Duration? =
        until?.let { Duration.between(now, it).coerceAtLeast(Duration.ZERO) }

    fun isExpired(now: Instant): Boolean = until != null && !now.isBefore(until)

    /** 1f = bloque completo, 0f = terminado. Sin temporizador siempre es 1f. */
    fun progress(now: Instant): Float {
        val end = until ?: return 1f
        val total = Duration.between(startedAt, end).toMillis()
        if (total <= 0) return 0f
        return (Duration.between(now, end).toMillis().toFloat() / total).coerceIn(0f, 1f)
    }

    companion object {
        fun free(now: Instant) = Status(AvailabilityState.Free, now, null)
    }
}
