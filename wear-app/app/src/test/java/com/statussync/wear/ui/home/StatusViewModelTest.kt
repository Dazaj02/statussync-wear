package com.statussync.wear.ui.home

import com.statussync.wear.data.InMemoryCustomStateRepository
import com.statussync.wear.data.InMemoryStatusRepository
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatusViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    /** Reloj que sigue el tiempo virtual de la prueba. */
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(scheduler.currentTime)
    }

    private var nextId = 0

    private fun viewModel(tickMillis: Long = 1_000) = StatusViewModel(
        repository = InMemoryStatusRepository(clock),
        customRepository = InMemoryCustomStateRepository(),
        clock = clock,
        tickMillis = tickMillis,
        newId = { "id${nextId++}" },
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsFreeWithFullRing() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        assertEquals(AvailabilityState.Free, viewModel.uiState.value.state)
        assertNull(viewModel.uiState.value.remaining)
        assertEquals(1f, viewModel.uiState.value.progress, 0f)
    }

    @Test
    fun countsDownWhileTheBlockRuns() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        viewModel.select(AvailabilityState.Focus, Duration.ofMinutes(25))
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(AvailabilityState.Focus, viewModel.uiState.value.state)
        assertEquals(Duration.ofMinutes(24), viewModel.uiState.value.remaining)
    }

    @Test
    fun returnsToFreeAndNotifiesWhenTheTimerEnds() = runTest(dispatcher) {
        val viewModel = viewModel()
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }

        viewModel.select(AvailabilityState.Call, Duration.ofMinutes(15))
        advanceTimeBy(Duration.ofMinutes(15).toMillis() + 1_000)
        runCurrent()

        assertEquals(AvailabilityState.Free, viewModel.uiState.value.state)
        assertEquals(listOf<HomeEvent>(HomeEvent.TimerFinished), events)
    }

    @Test
    fun choosingFreeCancelsTheTimerWithoutNotifying() = runTest(dispatcher) {
        val viewModel = viewModel()
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }

        viewModel.select(AvailabilityState.Focus, Duration.ofMinutes(50))
        advanceTimeBy(10_000)
        viewModel.select(AvailabilityState.Free)
        advanceTimeBy(Duration.ofMinutes(60).toMillis())
        runCurrent()

        assertEquals(AvailabilityState.Free, viewModel.uiState.value.state)
        assertEquals(emptyList<HomeEvent>(), events)
    }

    @Test
    fun addedCustomStateAppearsInTheList() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(dispatcher) { viewModel.customStates.collect {} }

        assertTrue(viewModel.addCustomState("  Estudiando ", AvailabilityState.Focus, 0xFF3B82F6, listOf(25, 50)))

        val created = viewModel.customStates.value.single()
        assertEquals("Estudiando", created.name)
        assertEquals(AvailabilityState.Focus, created.base)
        assertEquals("id0", created.id)
    }

    @Test
    fun selectingACustomStateKeepsItsBaseForSharing() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.addCustomState("Estudiando", AvailabilityState.Focus, 0xFF3B82F6, listOf(25))
        val custom = viewModel.customStates.value.single()

        viewModel.selectCustom(custom, Duration.ofMinutes(25))

        assertEquals(AvailabilityState.Focus, viewModel.uiState.value.state)
        assertEquals(custom, viewModel.uiState.value.custom)
        assertEquals(Duration.ofMinutes(25), viewModel.uiState.value.remaining)
    }

    @Test
    fun removingTheActiveCustomStateReturnsToFree() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.addCustomState("Estudiando", AvailabilityState.Focus, 0xFF3B82F6, listOf(25))
        val custom = viewModel.customStates.value.single()
        viewModel.selectCustom(custom, Duration.ofMinutes(25))

        viewModel.removeCustomState(custom.id)

        assertTrue(viewModel.customStates.value.isEmpty())
        assertEquals(AvailabilityState.Free, viewModel.uiState.value.state)
        assertNull(viewModel.uiState.value.custom)
    }

    @Test
    fun removingAnotherCustomStateDoesNotTouchTheCurrentStatus() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.addCustomState("A", AvailabilityState.Focus, 0xFF3B82F6, listOf(25))
        viewModel.addCustomState("B", AvailabilityState.Call, 0xFFEC4899, listOf(15))
        val (a, b) = viewModel.customStates.value
        viewModel.selectCustom(a, Duration.ofMinutes(25))

        viewModel.removeCustomState(b.id)

        assertEquals(listOf(a), viewModel.customStates.value)
        assertEquals(a, viewModel.uiState.value.custom)
    }

    @Test
    fun cannotCreateMoreThanTheLimit() = runTest(dispatcher) {
        val viewModel = viewModel()
        repeat(CustomState.MAX_COUNT) {
            assertTrue(viewModel.addCustomState("S$it", AvailabilityState.Class, 0xFF14B8A6, listOf(45)))
        }
        assertFalse(viewModel.addCustomState("extra", AvailabilityState.Class, 0xFF14B8A6, listOf(45)))
        assertEquals(CustomState.MAX_COUNT, viewModel.customStates.value.size)
    }
}
