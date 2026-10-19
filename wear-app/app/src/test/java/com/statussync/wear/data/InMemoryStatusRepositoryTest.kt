package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryStatusRepositoryTest {
    private class MutableClock(var now: Instant = Instant.EPOCH) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    @Test
    fun startsFreeWithoutTimer() {
        val status = InMemoryStatusRepository(MutableClock()).status.value
        assertEquals(AvailabilityState.Free, status.state)
        assertNull(status.until)
    }

    @Test
    fun setStateWithDurationSetsUntil() {
        val clock = MutableClock()
        val repository = InMemoryStatusRepository(clock)

        repository.setState(AvailabilityState.Focus, Duration.ofMinutes(25))

        assertEquals(AvailabilityState.Focus, repository.status.value.state)
        assertEquals(Instant.EPOCH.plus(Duration.ofMinutes(25)), repository.status.value.until)
    }

    @Test
    fun freeNeverKeepsATimer() {
        val repository = InMemoryStatusRepository(MutableClock())
        repository.setState(AvailabilityState.Free, Duration.ofMinutes(10))
        assertNull(repository.status.value.until)
    }

    @Test
    fun expireIfDueReturnsToFreeOnlyAfterTheBlockEnds() {
        val clock = MutableClock()
        val repository = InMemoryStatusRepository(clock)
        repository.setState(AvailabilityState.Call, Duration.ofMinutes(15))

        clock.now = Instant.EPOCH.plus(Duration.ofMinutes(14))
        repository.expireIfDue()
        assertEquals(AvailabilityState.Call, repository.status.value.state)

        clock.now = Instant.EPOCH.plus(Duration.ofMinutes(15))
        repository.expireIfDue()
        assertEquals(AvailabilityState.Free, repository.status.value.state)
    }

    @Test
    fun cancellingWithFreeClearsTheTimer() {
        val repository = InMemoryStatusRepository(MutableClock())
        repository.setState(AvailabilityState.Focus, Duration.ofMinutes(50))
        repository.setState(AvailabilityState.Free)
        assertEquals(AvailabilityState.Free, repository.status.value.state)
        assertNull(repository.status.value.until)
    }
}
