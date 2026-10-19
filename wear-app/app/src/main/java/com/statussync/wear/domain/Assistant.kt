package com.statussync.wear.domain

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

/** Bloque de agenda ya traducido a uno de los 4 estados del contrato. */
data class ScheduleBlock(
    val state: AvailabilityState,
    val startsAt: Instant,
    val endsAt: Instant,
)

/** Agregado de movimiento de una ventana. Nunca datos crudos del sensor. */
data class MotionFeature(
    val windowStart: Instant,
    val windowMinutes: Int,
    val steps: Int,
    /** 1 = completamente quieto. Se estima solo con pasos (sin acelerómetro). */
    val stillnessRatio: Float,
) {
    val windowEnd: Instant get() = windowStart.plus(Duration.ofMinutes(windowMinutes.toLong()))
}

enum class SuggestionReason { ScheduleNow, ScheduleSoon, Stillness }

/** Origen de la sugerencia; equivale a `auto_calendar` / `auto_motion` del contrato. */
enum class SuggestionSource { Calendar, Motion }

data class Suggestion(
    /** Identifica la sugerencia para no repetirla después de descartarla. */
    val key: String,
    val state: AvailabilityState,
    val durationMin: Int,
    val confidence: Float,
    val reason: SuggestionReason,
    val source: SuggestionSource,
    val dismissFor: Duration,
)

data class AssistantSettings(
    val calendar: Boolean = false,
    val motion: Boolean = false,
    val autoApply: Boolean = false,
)

interface AssistantSettingsRepository {
    val settings: StateFlow<AssistantSettings>
    fun update(transform: (AssistantSettings) -> AssistantSettings)
}

interface ScheduleSource {
    /** Bloques activos o que empiezan pronto. Vacío si no hay permiso o datos. */
    suspend fun upcoming(now: Instant): List<ScheduleBlock>
}

interface MotionSource {
    suspend fun recent(now: Instant): List<MotionFeature>

    /** La app dejó de estar visible: suelta sensores. */
    fun release() = Unit
}
