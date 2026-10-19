package com.statussync.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvailabilityStateTest {
    @Test
    fun wireNamesMatchTheContract() {
        assertEquals(listOf("free", "focus", "class", "call"), AvailabilityState.entries.map { it.wireName })
    }

    @Test
    fun fromWireResolvesKnownAndRejectsUnknownValues() {
        assertEquals(AvailabilityState.Focus, AvailabilityState.fromWire("focus"))
        assertNull(AvailabilityState.fromWire("sleeping"))
    }

    @Test
    fun onlyFreeHasNoTimerAndDefaultsFollowTheContract() {
        assertNull(AvailabilityState.Free.defaultDurationMin)
        assertEquals(50, AvailabilityState.Focus.defaultDurationMin)
        assertEquals(30, AvailabilityState.Call.defaultDurationMin)
    }
}
