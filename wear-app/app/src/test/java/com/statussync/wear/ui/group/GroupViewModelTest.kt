package com.statussync.wear.ui.group

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.GroupMember
import com.statussync.wear.domain.GroupRepository
import com.statussync.wear.domain.Status
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    private class FakeGroup(initial: List<GroupMember>) : GroupRepository {
        val flow = MutableStateFlow(initial)
        override val groupName = "Equipo"
        override fun members(): Flow<List<GroupMember>> = flow
    }

    private fun busy(id: String, state: AvailabilityState, seconds: Long) =
        GroupMember(id, id, Status(state, now, now.plusSeconds(seconds)))

    private fun free(id: String) = GroupMember(id, id, Status.free(now))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun summarizesFreeMembersAndSortsThemFirst() = runTest(dispatcher) {
        val viewModel = GroupViewModel(FakeGroup(listOf(busy("ana", AvailabilityState.Focus, 600), free("luis"))), clock)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        val ui = viewModel.uiState.value
        assertEquals("Equipo", ui.groupName)
        assertEquals(listOf("luis", "ana"), ui.rows.map { it.userId })
        assertEquals(1, ui.freeCount)
        assertEquals(2, ui.total)
    }

    @Test
    fun remainingMinutesRoundUp() = runTest(dispatcher) {
        val viewModel = GroupViewModel(FakeGroup(listOf(busy("ana", AvailabilityState.Focus, 61), free("luis"))), clock)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        val rows = viewModel.uiState.value.rows
        assertEquals(2L, rows.first { it.userId == "ana" }.remainingMinutes)
        assertNull(rows.first { it.userId == "luis" }.remainingMinutes)
    }

    @Test
    fun followsChangesFromTheRepository() = runTest(dispatcher) {
        val group = FakeGroup(listOf(free("luis")))
        val viewModel = GroupViewModel(group, clock)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        group.flow.value = listOf(busy("luis", AvailabilityState.Call, Duration.ofMinutes(15).seconds))

        assertEquals(0, viewModel.uiState.value.freeCount)
        assertEquals(AvailabilityState.Call, viewModel.uiState.value.rows.single().state)
    }

    @Test
    fun emptyGroupHasNoRows() = runTest(dispatcher) {
        val viewModel = GroupViewModel(FakeGroup(emptyList()), clock)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        assertEquals(0, viewModel.uiState.value.total)
    }
}
