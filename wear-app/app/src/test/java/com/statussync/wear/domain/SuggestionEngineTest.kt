package com.statussync.wear.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {
    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val free = Status.free(now)

    private fun block(state: AvailabilityState, startMin: Long, endMin: Long) =
        ScheduleBlock(state, now.plus(Duration.ofMinutes(startMin)), now.plus(Duration.ofMinutes(endMin)))

    private fun quiet(minutesAgoStart: Long, windowMinutes: Int = 5, stillness: Float = 1f) =
        MotionFeature(now.minus(Duration.ofMinutes(minutesAgoStart)), windowMinutes, 0, stillness)

    @Test
    fun activeBlockSuggestsItsStateUntilItEnds() {
        val s = SuggestionEngine.suggest(listOf(block(AvailabilityState.Class, -5, 40)), emptyList(), now, free)!!

        assertEquals(AvailabilityState.Class, s.state)
        assertEquals(40, s.durationMin)
        assertEquals(SuggestionReason.ScheduleNow, s.reason)
        assertEquals(SuggestionSource.Calendar, s.source)
    }

    @Test
    fun blockStartingSoonHasLowerConfidenceThanOneInProgress() {
        val soon = SuggestionEngine.suggest(listOf(block(AvailabilityState.Call, 8, 38)), emptyList(), now, free)!!
        val active = SuggestionEngine.suggest(listOf(block(AvailabilityState.Call, -1, 30)), emptyList(), now, free)!!

        assertEquals(SuggestionReason.ScheduleSoon, soon.reason)
        assertTrue(active.confidence > soon.confidence)
    }

    @Test
    fun ignoresBlocksFarAwayAlreadyOverOrFree() {
        val blocks = listOf(
            block(AvailabilityState.Class, 30, 60),
            block(AvailabilityState.Focus, -60, -10),
            block(AvailabilityState.Free, -5, 30),
        )

        assertNull(SuggestionEngine.suggest(blocks, emptyList(), now, free))
    }

    @Test
    fun neverOverridesARunningStatus() {
        val busy = Status(AvailabilityState.Focus, now, now.plus(Duration.ofMinutes(20)))

        assertNull(SuggestionEngine.suggest(listOf(block(AvailabilityState.Class, -5, 40)), emptyList(), now, busy))
    }

    @Test
    fun anExpiredStatusCountsAsFree() {
        val expired = Status(AvailabilityState.Focus, now.minusSeconds(900), now.minusSeconds(1))

        assertEquals(
            AvailabilityState.Class,
            SuggestionEngine.suggest(listOf(block(AvailabilityState.Class, 0, 40)), emptyList(), now, expired)?.state,
        )
    }

    @Test
    fun longStillnessSuggestsFocus() {
        val s = SuggestionEngine.suggest(emptyList(), listOf(quiet(10), quiet(5)), now, free)!!

        assertEquals(AvailabilityState.Focus, s.state)
        assertEquals(SuggestionReason.Stillness, s.reason)
        assertEquals(SuggestionSource.Motion, s.source)
    }

    @Test
    fun shortStillnessOrMovementSuggestsNothing() {
        assertNull(SuggestionEngine.suggest(emptyList(), listOf(quiet(5)), now, free))
        assertNull(SuggestionEngine.suggest(emptyList(), listOf(quiet(10, stillness = 0.2f), quiet(5)), now, free))
    }

    @Test
    fun staleOrNonContiguousWindowsDoNotCount() {
        assertNull(SuggestionEngine.suggest(emptyList(), listOf(quiet(40), quiet(35)), now, free))
        assertNull(SuggestionEngine.suggest(emptyList(), listOf(quiet(30), quiet(5)), now, free))
    }

    @Test
    fun scheduleBeatsMotionWhenBothApply() {
        val s = SuggestionEngine.suggest(
            listOf(block(AvailabilityState.Class, -1, 45)),
            listOf(quiet(10), quiet(5)),
            now,
            free,
        )!!

        assertEquals(SuggestionSource.Calendar, s.source)
    }

    @Test
    fun classifiesCalendarTitles() {
        assertEquals(AvailabilityState.Class, ScheduleClassifier.classify("Clase de Diseño"))
        assertEquals(AvailabilityState.Call, ScheduleClassifier.classify("Llamada con cliente"))
        assertEquals(AvailabilityState.Focus, ScheduleClassifier.classify("Entrega proyecto"))
        assertEquals(AvailabilityState.Focus, ScheduleClassifier.classify(null))
    }
}
