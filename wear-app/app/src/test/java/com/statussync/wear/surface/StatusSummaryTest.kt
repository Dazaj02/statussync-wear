package com.statussync.wear.surface

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.Status
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusSummaryTest {
    private val start = Instant.EPOCH

    @Test
    fun freeHasNoRemainingTime() {
        val summary = Status.free(start).toSummary(start)
        assertEquals(AvailabilityState.Free, summary.state)
        assertNull(summary.remainingMinutes)
        assertNull(summary.customName)
        assertEquals(AvailabilityState.Free.colorArgb, summary.colorArgb)
    }

    @Test
    fun remainingMinutesRoundUpSoTheLastSecondsStillShowOneMinute() {
        val status = Status(AvailabilityState.Focus, start, start.plus(Duration.ofMinutes(10)))
        assertEquals(10L, status.toSummary(start).remainingMinutes)
        assertEquals(10L, status.toSummary(start.plusSeconds(1)).remainingMinutes)
        assertEquals(1L, status.toSummary(start.plusSeconds(10 * 60 - 5)).remainingMinutes)
        assertEquals(0L, status.toSummary(start.plusSeconds(10 * 60)).remainingMinutes)
    }

    @Test
    fun customStateOverridesNameAndColorButKeepsTheBaseState() {
        val custom = CustomState("1", "Estudiando", AvailabilityState.Focus, 0xFF3B82F6, listOf(25))
        val status = Status(AvailabilityState.Focus, start, start.plus(Duration.ofMinutes(25)), custom)

        val summary = status.toSummary(start)

        assertEquals(AvailabilityState.Focus, summary.state)
        assertEquals("Estudiando", summary.customName)
        assertEquals(0xFF3B82F6, summary.colorArgb)
    }

    @Test
    fun quickDurationUsesTheDefaultThenTheFirstOption() {
        assertNull(AvailabilityState.Free.quickDurationMin)
        assertEquals(50, AvailabilityState.Focus.quickDurationMin)
        assertEquals(30, AvailabilityState.Call.quickDurationMin)
        assertEquals(45, AvailabilityState.Class.quickDurationMin)
    }
}
