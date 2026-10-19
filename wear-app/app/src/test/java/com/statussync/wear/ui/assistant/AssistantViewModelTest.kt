package com.statussync.wear.ui.assistant

import com.statussync.wear.data.InMemoryAssistantSettingsRepository
import com.statussync.wear.data.InMemoryStatusRepository
import com.statussync.wear.data.SimulatedInputs
import com.statussync.wear.domain.AssistantSettings
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.MotionFeature
import com.statussync.wear.domain.MotionSource
import com.statussync.wear.domain.ScheduleBlock
import com.statussync.wear.domain.ScheduleSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    private val status = InMemoryStatusRepository(clock)
    private val settings = InMemoryAssistantSettingsRepository()
    private val simulation = SimulatedInputs(clock)
    private var blocks = emptyList<ScheduleBlock>()
    private var released = false

    private val schedule = object : ScheduleSource {
        override suspend fun upcoming(now: Instant) = blocks
    }
    private val motion = object : MotionSource {
        override suspend fun recent(now: Instant) = simulation.recent(now)
        override fun release() { released = true }
    }

    private fun viewModel() = AssistantViewModel(status, settings, schedule, motion, simulation, clock)

    private fun classBlock() = ScheduleBlock(AvailabilityState.Class, now.minusSeconds(60), now.plus(Duration.ofMinutes(45)))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun doesNothingWhileEverythingIsOff() = runTest(dispatcher) {
        blocks = listOf(classBlock())
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

        vm.evaluate()

        assertNull(vm.uiState.value.pending)
        assertEquals(AvailabilityState.Free, status.status.value.state)
    }

    @Test
    fun suggestsWithoutChangingTheStatusUntilAccepted() = runTest(dispatcher) {
        settings.update { it.copy(calendar = true) }
        blocks = listOf(classBlock())
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

        vm.evaluate()

        assertEquals(AvailabilityState.Class, vm.uiState.value.pending?.state)
        assertEquals(AvailabilityState.Free, status.status.value.state)

        vm.accept()

        assertEquals(AvailabilityState.Class, status.status.value.state)
        assertNull(vm.uiState.value.pending)
    }

    @Test
    fun dismissedSuggestionDoesNotComeBack() = runTest(dispatcher) {
        settings.update { it.copy(calendar = true) }
        blocks = listOf(classBlock())
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

        vm.evaluate()
        vm.dismiss()
        vm.evaluate()

        assertNull(vm.uiState.value.pending)
    }

    @Test
    fun autoApplyChangesTheStatusAndCanBeUndone() = runTest(dispatcher) {
        settings.update { AssistantSettings(calendar = true, autoApply = true) }
        blocks = listOf(classBlock())
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

        vm.evaluate()

        assertEquals(AvailabilityState.Class, status.status.value.state)
        assertNotNull(vm.uiState.value.undo)

        vm.undoAuto()
        vm.evaluate()

        assertEquals(AvailabilityState.Free, status.status.value.state)
        assertNull(vm.uiState.value.undo)
        assertNull(vm.uiState.value.pending)
    }

    @Test
    fun aRunningStatusIsNeverOverridden() = runTest(dispatcher) {
        settings.update { AssistantSettings(calendar = true, autoApply = true) }
        blocks = listOf(classBlock())
        status.setState(AvailabilityState.Call, Duration.ofMinutes(15))
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

        vm.evaluate()

        assertEquals(AvailabilityState.Call, status.status.value.state)
        assertNull(vm.uiState.value.undo)
    }

    @Test
    fun simulatingStillnessTurnsOnMotionAndSuggestsFocus() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

        vm.simulateStillness()

        assertTrue(settings.settings.value.motion)
        assertEquals(AvailabilityState.Focus, vm.uiState.value.pending?.state)
    }

    @Test
    fun pauseReleasesTheSensors() {
        viewModel().pause()

        assertTrue(released)
    }
}
