package com.statussync.wear.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.CustomStateRepository
import com.statussync.wear.domain.Status
import com.statussync.wear.domain.StatusRepository
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val state: AvailabilityState,
    val remaining: java.time.Duration?,
    val progress: Float,
    val custom: CustomState? = null,
)

/** Avisos de una sola vez para la UI (vibración). */
sealed interface HomeEvent {
    data object TimerFinished : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatusViewModel(
    private val repository: StatusRepository,
    private val customRepository: CustomStateRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val tickMillis: Long = 1_000,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    val customStates: StateFlow<List<CustomState>> = customRepository.states

    /** Solo corre el reloj interno mientras haya temporizador y la pantalla esté visible. */
    val uiState: StateFlow<HomeUiState> = repository.status
        .flatMapLatest { status ->
            if (status.until == null) flowOf(status.toUi()) else countdown(status)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.status.value.toUi())

    fun select(state: AvailabilityState, duration: Duration? = null) = repository.setState(state, duration)

    fun selectCustom(custom: CustomState, duration: Duration? = null) =
        repository.setState(custom.base, duration, custom)

    /** Crea un estado propio. Devuelve false si ya hay [CustomState.MAX_COUNT]. */
    fun addCustomState(name: String, base: AvailabilityState, colorArgb: Long, durationsMin: List<Int>): Boolean =
        customRepository.add(CustomState(newId(), name.trim(), base, colorArgb, durationsMin))

    /** Elimina un estado propio; si estaba activo, vuelve a Libre. */
    fun removeCustomState(id: String) {
        if (repository.status.value.custom?.id == id) repository.setState(AvailabilityState.Free)
        customRepository.remove(id)
    }

    private fun countdown(status: Status) = flow {
        while (true) {
            if (status.isExpired(clock.instant())) {
                repository.expireIfDue()
                _events.tryEmit(HomeEvent.TimerFinished)
                return@flow
            }
            emit(status.toUi())
            delay(tickMillis)
        }
    }

    private fun Status.toUi(): HomeUiState {
        val now = clock.instant()
        return HomeUiState(state, remaining(now), progress(now), custom)
    }
}
