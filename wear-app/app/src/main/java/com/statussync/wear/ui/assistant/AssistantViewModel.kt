package com.statussync.wear.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statussync.wear.data.SimulatedInputs
import com.statussync.wear.domain.AssistantSettings
import com.statussync.wear.domain.AssistantSettingsRepository
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.MotionSource
import com.statussync.wear.domain.ScheduleSource
import com.statussync.wear.domain.Status
import com.statussync.wear.domain.StatusRepository
import com.statussync.wear.domain.Suggestion
import com.statussync.wear.domain.SuggestionEngine
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssistantUiState(
    val settings: AssistantSettings = AssistantSettings(),
    /** Sugerencia esperando que el usuario la acepte o la descarte. */
    val pending: Suggestion? = null,
    /** Último cambio automático, mientras se puede deshacer. */
    val undo: Suggestion? = null,
)

class AssistantViewModel(
    private val statusRepository: StatusRepository,
    private val settingsRepository: AssistantSettingsRepository,
    private val schedule: ScheduleSource,
    private val motion: MotionSource,
    private val simulation: SimulatedInputs,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val pending = MutableStateFlow<Suggestion?>(null)
    private val undo = MutableStateFlow<Suggestion?>(null)
    private var statusBeforeAuto: Status? = null
    private val dismissedUntil = mutableMapOf<String, Instant>()

    val uiState: StateFlow<AssistantUiState> = combine(settingsRepository.settings, pending, undo) { s, p, u ->
        AssistantUiState(s, p, u)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssistantUiState(settingsRepository.settings.value))

    /** Lo llama la UI cada pocos segundos mientras la app está visible. */
    suspend fun evaluate() {
        val settings = settingsRepository.settings.value
        statusRepository.expireIfDue()
        if (!settings.calendar && !settings.motion) {
            pending.value = null
            return
        }
        val now = clock.instant()
        val blocks = if (settings.calendar) schedule.upcoming(now) else emptyList()
        val features = if (settings.motion) motion.recent(now) else emptyList()
        val current = statusRepository.status.value

        val suggestion = SuggestionEngine.suggest(blocks, features, now, current)
            ?.takeIf { it.confidence >= MIN_CONFIDENCE }
            ?.takeIf { dismissedUntil[it.key]?.let { until -> now.isAfter(until) } ?: true }

        when {
            suggestion == null -> pending.value = null
            settings.autoApply && suggestion.confidence >= AUTO_CONFIDENCE -> {
                statusBeforeAuto = current
                apply(suggestion)
                pending.value = null
                undo.value = suggestion
            }
            else -> pending.value = suggestion
        }
    }

    fun accept() {
        val suggestion = pending.value ?: return
        apply(suggestion)
        pending.value = null
    }

    fun dismiss() {
        val suggestion = pending.value ?: return
        dismissedUntil[suggestion.key] = clock.instant().plus(suggestion.dismissFor)
        pending.value = null
    }

    /** Vuelve al estado que había antes del cambio automático y no lo repite. */
    fun undoAuto() {
        val suggestion = undo.value ?: return
        val previous = statusBeforeAuto
        dismissedUntil[suggestion.key] = clock.instant().plus(suggestion.dismissFor)
        if (previous == null || previous.state == AvailabilityState.Free) {
            statusRepository.setState(AvailabilityState.Free)
        } else {
            statusRepository.setState(previous.state, previous.remaining(clock.instant()), previous.custom)
        }
        undo.value = null
    }

    fun clearUndo() {
        undo.value = null
    }

    fun setCalendar(enabled: Boolean) = settingsRepository.update { it.copy(calendar = enabled) }

    fun setMotion(enabled: Boolean) = settingsRepository.update { it.copy(motion = enabled) }

    fun setAutoApply(enabled: Boolean) = settingsRepository.update { it.copy(autoApply = enabled) }

    fun simulateClass() {
        simulation.simulateClass()
        settingsRepository.update { it.copy(calendar = true) }
        viewModelScope.launch { evaluate() }
    }

    fun simulateStillness() {
        simulation.simulateStillness()
        settingsRepository.update { it.copy(motion = true) }
        viewModelScope.launch { evaluate() }
    }

    /** La app dejó de estar visible: suelta los sensores. */
    fun pause() = motion.release()

    private fun apply(suggestion: Suggestion) =
        statusRepository.setState(suggestion.state, Duration.ofMinutes(suggestion.durationMin.toLong()))

    private companion object {
        const val MIN_CONFIDENCE = 0.5f
        const val AUTO_CONFIDENCE = 0.8f
    }
}
