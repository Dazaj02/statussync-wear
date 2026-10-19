package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.Status
import com.statussync.wear.domain.StatusRepository
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class InMemoryStatusRepository(
    private val clock: Clock = Clock.systemUTC(),
    initial: Status? = null,
) : StatusRepository {
    private val _status = MutableStateFlow(initial ?: Status.free(clock.instant()))
    override val status: StateFlow<Status> = _status.asStateFlow()

    override fun setState(state: AvailabilityState, duration: Duration?, custom: CustomState?) {
        val now = clock.instant()
        val until = if (state == AvailabilityState.Free) null else duration?.let { now.plus(it) }
        _status.value = Status(state, now, until, custom?.takeIf { it.base == state })
        onChanged(_status.value)
    }

    override fun expireIfDue() {
        val now = clock.instant()
        if (_status.value.isExpired(now)) {
            _status.value = Status.free(now)
            onChanged(_status.value)
        }
    }

    /** Gancho para persistir; la versión en memoria no hace nada. */
    protected open fun onChanged(status: Status) = Unit
}
