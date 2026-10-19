package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.MotionFeature
import com.statussync.wear.domain.MotionSource
import com.statussync.wear.domain.ScheduleBlock
import com.statussync.wear.domain.ScheduleSource
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Datos de ejemplo para probar el asistente (y la demo) sin depender de calendario ni sensores reales. */
class SimulatedInputs(private val clock: Clock) : ScheduleSource, MotionSource {
    @Volatile private var blocks: List<ScheduleBlock> = emptyList()
    @Volatile private var features: List<MotionFeature> = emptyList()

    /** Una clase de 45 min que empieza en 2 minutos. */
    fun simulateClass() {
        val start = clock.instant().plus(Duration.ofMinutes(2))
        blocks = listOf(ScheduleBlock(AvailabilityState.Class, start, start.plus(Duration.ofMinutes(45))))
    }

    /** 20 minutos sin un solo paso, en ventanas de 5. */
    fun simulateStillness() {
        val now = clock.instant()
        features = (4 downTo 1).map { i ->
            MotionFeature(now.minus(Duration.ofMinutes(i * 5L)), windowMinutes = 5, steps = 0, stillnessRatio = 1f)
        }
    }

    override suspend fun upcoming(now: Instant): List<ScheduleBlock> = blocks

    override suspend fun recent(now: Instant): List<MotionFeature> = features
}

class CompositeScheduleSource(private val sources: List<ScheduleSource>) : ScheduleSource {
    override suspend fun upcoming(now: Instant): List<ScheduleBlock> = sources.flatMap { it.upcoming(now) }
}

class CompositeMotionSource(private val sources: List<MotionSource>) : MotionSource {
    override suspend fun recent(now: Instant): List<MotionFeature> = sources.flatMap { it.recent(now) }

    override fun release() = sources.forEach { it.release() }
}
