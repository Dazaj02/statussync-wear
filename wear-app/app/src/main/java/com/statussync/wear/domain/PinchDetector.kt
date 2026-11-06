package com.statussync.wear.domain

import kotlin.math.sqrt

/**
 * Detector experimental de "dos pellizcos índice-pulgar", calibrado con grabaciones reales del Galaxy Watch4
 * (acelerómetro a 100 Hz). Un pellizco es un golpe corto de 0,5–1,3 m/s² sobre la parte rápida de la señal.
 *
 * Una serie de 2 a 5 golpes seguidos, con el primer par separado entre 0,15 y 0,5 s y precedida por la mano quieta,
 * cuenta como doble pellizco. El resultado se entrega 0,6 s después del último golpe, cuando se sabe que la serie terminó.
 * Limitación conocida: con el antebrazo apoyado o moviéndose la señal se ensucia y no detecta.
 */
class PinchDetector {
    private class Tap(val timeMs: Long, val magnitude: Float)
    private class Sample(val timeMs: Long, val magnitude: Float)

    private val window = ArrayDeque<FloatArray>()
    private val history = ArrayDeque<Sample>()
    private var prev1: Sample? = null
    private var prev2: Sample? = null
    private var series = mutableListOf<Tap>()
    private var cooldownUntil = Long.MIN_VALUE

    fun onAccelerometer(timeMs: Long, x: Float, y: Float, z: Float): Gesture? {
        window.addLast(floatArrayOf(x, y, z))
        if (window.size > WINDOW) window.removeFirst()
        val magnitude = if (window.size == WINDOW) fastComponent(x, y, z) else 0f
        val sample = Sample(timeMs, magnitude)
        history.addLast(sample)
        while (history.isNotEmpty() && history.first().timeMs < timeMs - HISTORY_MS) history.removeFirst()

        val p1 = prev1
        val p2 = prev2
        if (p1 != null && p2 != null && p1.magnitude >= TAP_THRESHOLD && p1.magnitude >= p2.magnitude && p1.magnitude > magnitude) {
            registerTap(p1)
        }
        prev2 = prev1
        prev1 = sample

        val last = series.lastOrNull() ?: return null
        if (timeMs - last.timeMs < SETTLE_MS) return null
        val valid = series.size in 2..MAX_TAPS && (series[1].timeMs - series[0].timeMs) in GAP_MIN_MS..GAP_MAX_MS
        series = mutableListOf()
        if (!valid) return null
        cooldownUntil = timeMs + COOLDOWN_MS
        return Gesture.DoublePinch
    }

    /** Parte rápida de la señal: la muestra menos la media de las últimas [WINDOW] (quita gravedad y movimiento lento). */
    private fun fastComponent(x: Float, y: Float, z: Float): Float {
        var mx = 0f
        var my = 0f
        var mz = 0f
        for (v in window) {
            mx += v[0]
            my += v[1]
            mz += v[2]
        }
        val n = window.size
        val dx = x - mx / n
        val dy = y - my / n
        val dz = z - mz / n
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun registerTap(sample: Sample) {
        if (sample.timeMs < cooldownUntil) return
        val last = series.lastOrNull()
        when {
            last != null && sample.timeMs - last.timeMs < REFRACTORY_MS -> {
                // Sigue siendo el mismo golpe: se queda con el pico más alto.
                if (sample.magnitude > last.magnitude) series[series.lastIndex] = Tap(sample.timeMs, sample.magnitude)
            }
            last != null && sample.timeMs - last.timeMs < SETTLE_MS -> series.add(Tap(sample.timeMs, sample.magnitude))
            else -> {
                series = if (wasStill(sample.timeMs)) mutableListOf(Tap(sample.timeMs, sample.magnitude)) else mutableListOf()
            }
        }
    }

    /** ¿La mano estaba quieta en los 0,5 s previos al primer golpe? Caminar o mover el brazo no lo cumple. */
    private fun wasStill(tapMs: Long): Boolean {
        var sum = 0f
        var count = 0
        for (s in history) {
            if (s.timeMs > tapMs - QUIET_GUARD_MS || s.timeMs <= tapMs - QUIET_LOOKBACK_MS) continue
            sum += s.magnitude * s.magnitude
            count++
        }
        return count == 0 || sqrt(sum / count) < QUIET_RMS
    }

    private companion object {
        const val WINDOW = 10
        const val HISTORY_MS = 1_500L
        const val TAP_THRESHOLD = 0.45f
        const val REFRACTORY_MS = 80L
        const val SETTLE_MS = 600L
        const val GAP_MIN_MS = 150L
        const val GAP_MAX_MS = 500L
        const val MAX_TAPS = 5
        const val QUIET_RMS = 0.25f
        const val QUIET_GUARD_MS = 100L
        const val QUIET_LOOKBACK_MS = 600L
        const val COOLDOWN_MS = 200L
    }
}
