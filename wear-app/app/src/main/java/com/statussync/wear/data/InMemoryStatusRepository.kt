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

class InMemoryStatusRepository(private val clock: Clock = Clock.systemUTC()) : StatusRepository {
    private val _status = MutableStateFlow(Status.free(clock.instant()))
    override val status: StateFlow<Status> = _status.asStateFlow()

    override fun setState(state: AvailabilityState, duration: Duration?, custom: CustomState?) {
        val now = clock.instant()
        val until = if (state == AvailabilityState.Free) null else duration?.let { now.plus(it) }
        _status.value = Status(state, now, until, custom?.takeIf { it.base == state })
    }

    override fun expireIfDue() {
        val now = clock.instant()
        if (_status.value.isExpired(now)) _status.value = Status.free(now)
    }
}
