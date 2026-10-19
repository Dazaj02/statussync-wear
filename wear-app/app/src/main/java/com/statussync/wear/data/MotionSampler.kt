package com.statussync.wear.data

import com.statussync.wear.domain.MotionFeature
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Convierte lecturas del contador de pasos (acumulado) en ventanas de ~1 minuto.
 * Si pasó mucho tiempo entre lecturas (app cerrada) reinicia la ventana: no se inventa quietud que nadie observó.
 */
class MotionSampler(
    private val windowMillis: Long = 60_000,
    private val keepMillis: Long = 30 * 60_000,
) {
    private var windowStart: Instant? = null
    private var startSteps = 0L
    private val done = ArrayDeque<MotionFeature>()

    fun record(now: Instant, cumulativeSteps: Long) {
        val start = windowStart
        if (start == null) {
            begin(now, cumulativeSteps)
            return
        }
        val elapsed = now.toEpochMilli() - start.toEpochMilli()
        if (elapsed < windowMillis) return
        if (elapsed > windowMillis * 3) {
            begin(now, cumulativeSteps)
            return
        }
        // El contador vuelve a 0 al reiniciar el reloj; un salto negativo cuenta como 0 pasos.
        val steps = max(0L, cumulativeSteps - startSteps).toInt()
        val minutes = max(1, (elapsed / 60_000.0).roundToInt())
        done.addLast(MotionFeature(start, minutes, steps, stillness(steps, minutes)))
        begin(now, cumulativeSteps)
        while (done.isNotEmpty() && now.toEpochMilli() - done.first().windowStart.toEpochMilli() > keepMillis) {
            done.removeFirst()
        }
    }

    fun features(): List<MotionFeature> = done.toList()

    private fun begin(now: Instant, steps: Long) {
        windowStart = now
        startSteps = steps
    }

    private fun stillness(steps: Int, minutes: Int): Float = 1f - min(1f, steps / (STEPS_PER_ACTIVE_MINUTE * minutes))

    private companion object {
        const val STEPS_PER_ACTIVE_MINUTE = 30f
    }
}
