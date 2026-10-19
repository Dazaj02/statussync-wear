package com.statussync.wear.domain

import java.time.Duration
import java.time.Instant

/**
 * IA local por reglas, sin red. Misma salida que `POST /suggest` del contrato (estado, confianza, motivo),
 * así en W6 puede quedar como respaldo sin conexión.
 */
object SuggestionEngine {
    const val SOON_MINUTES = 10L
    const val QUIET_MINUTES_NEEDED = 10
    const val MOTION_DISMISS_MINUTES = 30L
    const val FOCUS_MINUTES = 25
    private const val QUIET_RATIO = 0.9f
    private const val STALE_SECONDS = 180L
    private const val GAP_SECONDS = 60L

    /** Un bloque en curso (manual o con temporizador) nunca se pisa: lo manual gana a lo automático. */
    fun suggest(blocks: List<ScheduleBlock>, motion: List<MotionFeature>, now: Instant, current: Status): Suggestion? {
        if (current.state != AvailabilityState.Free && !current.isExpired(now)) return null
        return listOfNotNull(fromSchedule(blocks, now), fromMotion(motion, now)).maxByOrNull { it.confidence }
    }

    private fun fromSchedule(blocks: List<ScheduleBlock>, now: Instant): Suggestion? {
        val soonLimit = now.plus(Duration.ofMinutes(SOON_MINUTES))
        val block = blocks
            .filter { it.state != AvailabilityState.Free && it.endsAt.isAfter(now) && !it.startsAt.isAfter(soonLimit) }
            .minByOrNull { it.startsAt } ?: return null
        val active = !block.startsAt.isAfter(now)
        val minutes = ((Duration.between(now, block.endsAt).seconds + 59) / 60).toInt().coerceAtLeast(1)
        return Suggestion(
            key = "cal:${block.startsAt}",
            state = block.state,
            durationMin = minutes,
            confidence = if (active) 0.9f else 0.8f,
            reason = if (active) SuggestionReason.ScheduleNow else SuggestionReason.ScheduleSoon,
            source = SuggestionSource.Calendar,
            dismissFor = Duration.between(now, block.endsAt),
        )
    }

    private fun fromMotion(motion: List<MotionFeature>, now: Instant): Suggestion? {
        val newestFirst = motion.sortedByDescending { it.windowStart }
        val newest = newestFirst.firstOrNull() ?: return null
        if (newest.windowEnd.isBefore(now.minusSeconds(STALE_SECONDS))) return null

        var quietMinutes = 0
        var previousStart: Instant? = null
        for (window in newestFirst) {
            if (window.stillnessRatio < QUIET_RATIO) break
            val start = previousStart
            if (start != null && Duration.between(window.windowEnd, start).abs().seconds > GAP_SECONDS) break
            quietMinutes += window.windowMinutes
            previousStart = window.windowStart
        }
        if (quietMinutes < QUIET_MINUTES_NEEDED) return null

        return Suggestion(
            key = "motion",
            state = AvailabilityState.Focus,
            durationMin = FOCUS_MINUTES,
            confidence = 0.6f,
            reason = SuggestionReason.Stillness,
            source = SuggestionSource.Motion,
            dismissFor = Duration.ofMinutes(MOTION_DISMISS_MINUTES),
        )
    }
}

/** Traduce el título de un evento de calendario a un estado. Lo que no se reconoce cuenta como Enfoque. */
object ScheduleClassifier {
    private val CLASS_WORDS = listOf("clase", "class", "lecture", "cátedra", "catedra", "seminario")
    private val CALL_WORDS = listOf("llamada", "call", "videollamada", "zoom", "meet")

    fun classify(title: String?): AvailabilityState {
        val text = title?.lowercase().orEmpty()
        return when {
            CLASS_WORDS.any { it in text } -> AvailabilityState.Class
            CALL_WORDS.any { it in text } -> AvailabilityState.Call
            else -> AvailabilityState.Focus
        }
    }
}
