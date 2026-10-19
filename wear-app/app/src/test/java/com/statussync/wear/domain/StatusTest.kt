package com.statussync.wear.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusTest {
    private val start = Instant.EPOCH
    private val focus = Status(AvailabilityState.Focus, start, start.plus(Duration.ofMinutes(10)))

    @Test
    fun remainingShrinksAndNeverGoesNegative() {
        assertEquals(Duration.ofMinutes(10), focus.remaining(start))
        assertEquals(Duration.ofMinutes(4), focus.remaining(start.plus(Duration.ofMinutes(6))))
        assertEquals(Duration.ZERO, focus.remaining(start.plus(Duration.ofMinutes(20))))
    }

    @Test
    fun expiresExactlyAtUntil() {
        assertFalse(focus.isExpired(start.plus(Duration.ofSeconds(599))))
        assertTrue(focus.isExpired(start.plus(Duration.ofMinutes(10))))
    }

    @Test
    fun progressGoesFromOneToZero() {
        assertEquals(1f, focus.progress(start), 0.001f)
        assertEquals(0.5f, focus.progress(start.plus(Duration.ofMinutes(5))), 0.001f)
        assertEquals(0f, focus.progress(start.plus(Duration.ofMinutes(30))), 0.001f)
    }

    @Test
    fun freeHasNoTimerAndFullProgress() {
        val free = Status.free(start)
        assertNull(free.remaining(start))
        assertFalse(free.isExpired(start.plus(Duration.ofDays(1))))
        assertEquals(1f, free.progress(start), 0f)
    }
}
