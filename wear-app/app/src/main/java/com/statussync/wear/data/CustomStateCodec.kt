package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState

/** Formato de una línea por estado: `id \t nombre \t base \t color \t 15,30,60`. Ignora líneas dañadas. */
object CustomStateCodec {
    private const val FIELD = '\t'

    fun encode(states: List<CustomState>): String = states.joinToString("\n") { s ->
        listOf(clean(s.id), clean(s.name), s.base.wireName, s.colorArgb.toString(), s.durationsMin.joinToString(","))
            .joinToString(FIELD.toString())
    }

    fun decode(raw: String?): List<CustomState> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val f = line.split(FIELD)
            if (f.size != 5 || f[0].isBlank() || f[1].isBlank()) return@mapNotNull null
            val base = AvailabilityState.fromWire(f[2]) ?: return@mapNotNull null
            val color = f[3].toLongOrNull() ?: return@mapNotNull null
            val durations = if (f[4].isBlank()) emptyList() else f[4].split(",").map { it.toIntOrNull() ?: return@mapNotNull null }
            CustomState(f[0], f[1], base, color, durations)
        }
    }

    private fun clean(text: String) = text.replace(Regex("[\\t\\r\\n]"), " ").trim()
}
