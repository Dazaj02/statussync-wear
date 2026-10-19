package com.statussync.wear.ui.format

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test
    fun clockUsesMinutesAndSecondsUnderOneHour() {
        assertEquals("24:05", Duration.ofSeconds(24 * 60 + 5).formatClock())
        assertEquals("00:00", Duration.ZERO.formatClock())
    }

    @Test
    fun clockAddsHoursFromOneHour() {
        assertEquals("1:30:00", Duration.ofMinutes(90).formatClock())
    }

    @Test
    fun clockRoundsPartialSecondsUp() {
        assertEquals("00:01", Duration.ofMillis(200).formatClock())
    }

    @Test
    fun minutesAreWrittenInSpanishUnits() {
        assertEquals("25 min", formatMinutes(25))
        assertEquals("1 h", formatMinutes(60))
        assertEquals("1 h 30 min", formatMinutes(90))
    }
}
