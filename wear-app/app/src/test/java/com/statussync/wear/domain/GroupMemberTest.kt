package com.statussync.wear.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupMemberTest {
    private val now = Instant.parse("2026-01-01T10:00:00Z")

    private fun busy(id: String, state: AvailabilityState, minutes: Long) =
        GroupMember(id, id, Status(state, now, now.plus(Duration.ofMinutes(minutes))))

    private fun free(id: String) = GroupMember(id, id, Status.free(now))

    @Test
    fun freeMembersComeFirstThenByEarliestEnd() {
        val sorted = listOf(busy("b", AvailabilityState.Class, 40), free("z"), busy("a", AvailabilityState.Focus, 10), free("m"))
            .sortedForDisplay(now)

        assertEquals(listOf("m", "z", "a", "b"), sorted.map { it.userId })
    }

    @Test
    fun expiredBlockCountsAsFree() {
        val expired = GroupMember("e", "e", Status(AvailabilityState.Call, now.minusSeconds(600), now.minusSeconds(1)))

        val sorted = listOf(busy("a", AvailabilityState.Focus, 10), expired).sortedForDisplay(now)

        assertEquals(listOf("e", "a"), sorted.map { it.userId })
        assertEquals(AvailabilityState.Free, sorted.first().status.state)
    }
}
