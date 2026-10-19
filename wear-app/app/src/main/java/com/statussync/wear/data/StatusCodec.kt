package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.Status
import java.time.Instant

/** Formato de una línea: `estado | inicio en ms | fin en ms (o vacío) | id del estado propio (o vacío)`. */
object StatusCodec {
    private const val SEP = "|"

    fun encode(status: Status): String = listOf(
        status.state.wireName,
        status.startedAt.toEpochMilli().toString(),
        status.until?.toEpochMilli()?.toString().orEmpty(),
        status.custom?.id.orEmpty(),
    ).joinToString(SEP)

    /** Devuelve Libre si no hay nada guardado, está dañado o el bloque ya venció. */
    fun decode(raw: String?, customStates: List<CustomState>, now: Instant): Status {
        val free = Status.free(now)
        val f = raw?.split(SEP) ?: return free
        if (f.size != 4) return free
        val state = AvailabilityState.fromWire(f[0]) ?: return free
        val started = f[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return free
        val until = if (f[2].isBlank()) null else (f[2].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return free)
        val custom = customStates.firstOrNull { it.id == f[3] && it.base == state }
        val status = Status(state, started, until, custom)
        return if (status.isExpired(now)) free else status
    }
}
