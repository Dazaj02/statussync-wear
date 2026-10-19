package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.GroupMember
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockGroupRepositoryTest {
    private val scheduler = TestCoroutineScheduler()
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(scheduler.currentTime)
    }

    @Test
    fun startsWithFiveMembersAndTwoFree() = runTest(scheduler) {
        val first = MockGroupRepository(clock, Random(1)).members().take(1).toList().single()

        assertEquals(5, first.size)
        assertEquals(2, first.count { it.status.state == AvailabilityState.Free })
    }

    @Test
    fun statusesChangeOverTimeWithoutLosingMembers() = runTest(scheduler) {
        val emissions = MockGroupRepository(clock, Random(7), changeEveryTicks = 1).members().take(12).toList()

        assertTrue(emissions.all { it.size == 5 })
        assertTrue(emissions.map { it.snapshot() }.distinct().size > 1)
    }

    private fun List<GroupMember>.snapshot() = map { it.userId to it.status.state }
}
