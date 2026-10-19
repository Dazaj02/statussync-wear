package com.statussync.wear.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSamplerTest {
    private val t0 = Instant.parse("2026-01-01T10:00:00Z")

    @Test
    fun closesAOneMinuteWindowWithTheStepDelta() {
        val sampler = MotionSampler()
        sampler.record(t0, 1000)
        sampler.record(t0.plusSeconds(30), 1000)
        assertTrue(sampler.features().isEmpty())

        sampler.record(t0.plusSeconds(60), 1000)

        val window = sampler.features().single()
        assertEquals(0, window.steps)
        assertEquals(1, window.windowMinutes)
        assertEquals(1f, window.stillnessRatio, 0f)
    }

    @Test
    fun walkingLowersStillness() {
        val sampler = MotionSampler()
        sampler.record(t0, 0)
        sampler.record(t0.plusSeconds(60), 90)

        assertEquals(0f, sampler.features().single().stillnessRatio, 0f)
    }

    @Test
    fun aLongGapRestartsInsteadOfClaimingStillness() {
        val sampler = MotionSampler()
        sampler.record(t0, 0)
        sampler.record(t0.plusSeconds(600), 0)

        assertTrue(sampler.features().isEmpty())
    }

    @Test
    fun counterResetCountsAsZeroSteps() {
        val sampler = MotionSampler()
        sampler.record(t0, 5000)
        sampler.record(t0.plusSeconds(60), 10)

        assertEquals(0, sampler.features().single().steps)
    }
}
