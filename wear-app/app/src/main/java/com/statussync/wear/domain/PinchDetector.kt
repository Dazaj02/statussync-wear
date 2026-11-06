package com.statussync.wear.domain

import kotlin.math.sqrt

/**
 * Detector experimental de "dos pellizcos índice-pulgar", calibrado con grabaciones reales del Galaxy Watch4
 * (acelerómetro a 100 Hz). Un pellizco es un golpe corto de 0,5–1,3 m/s² sobre la parte rápida de la señal.
 *
 * Dos golpes separados entre 0,15 y 0,5 s y precedidos por la mano quieta cuentan como doble pellizco.
 * El resultado se entrega 0,3 s después del segundo golpe, si no siguen más de uno extra (una serie larga es caminar o ruido).
 * Limitación conocida: con el antebrazo apoyado o moviéndose la señal se ensucia y no detecta.
 */
class PinchDetector(
    /** Se llama al detectar el primer golpe de una posible serie (para dar aviso táctil). */
    private val onFirstTap: () -> Unit = {},
    /** Se llama al descartar una serie, con el motivo: `move`, `gap` o `single`. */
    private val onReject: (timeMs: Long, reason: String) -> Unit = { _, _ -> },
) {
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

        if (series.isNotEmpty() && magnitude > MOTION_ABORT) {
            // Un pellizco nunca mueve tanto: es la muñeca o el brazo.
            series = mutableListOf()
            cooldownUntil = timeMs + MOTION_COOLDOWN_MS
            onReject(timeMs, "move")
        }

        val p1 = prev1
        val p2 = prev2
        if (p1 != null && p2 != null && p1.magnitude >= TAP_THRESHOLD && p1.magnitude >= p2.magnitude && p1.magnitude > magnitude) {
            registerTap(p1)
        }
        prev2 = prev1
        prev1 = sample

        val last = series.lastOrNull() ?: return null
        if (series.size >= 2) {
            if (timeMs - last.timeMs < CONFIRM_MS) return null
            val similar = series[0].magnitude <= SIMILAR_RATIO * series[1].magnitude &&
                series[1].magnitude <= SIMILAR_RATIO * series[0].magnitude
            val valid = similar && series.size <= MAX_TAPS && (series[1].timeMs - series[0].timeMs) in GAP_MIN_MS..GAP_MAX_MS
            series = mutableListOf()
            if (!valid) {
                onReject(timeMs, "long") // serie larga: caminar o ruido
                return null
            }
            cooldownUntil = timeMs + COOLDOWN_MS
            return Gesture.DoublePinch
        }
        if (timeMs - last.timeMs > GAP_MAX_MS) {
            series = mutableListOf()
            onReject(timeMs, "single") // un golpe aislado, sin segundo
        }
        return null
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
        if (sample.magnitude > MAX_TAP) {
            series = mutableListOf()
            onReject(sample.timeMs, "move")
            return
        }
        val last = series.lastOrNull()
        when {
            last != null && sample.timeMs - last.timeMs < REFRACTORY_MS -> {
                // Sigue siendo el mismo golpe: se queda con el pico más alto.
                if (sample.magnitude > last.magnitude) series[series.lastIndex] = Tap(sample.timeMs, sample.magnitude)
            }
            last != null && sample.timeMs - last.timeMs <= GAP_MAX_MS -> {
                if (series.size >= 2 || sample.timeMs - last.timeMs >= GAP_MIN_MS) {
                    series.add(Tap(sample.timeMs, sample.magnitude))
                } else {
                    onReject(sample.timeMs, "gap")
                    series = mutableListOf(Tap(sample.timeMs, sample.magnitude))
                }
            }
            else -> {
                if (wasStill(sample.timeMs)) {
                    series = mutableListOf(Tap(sample.timeMs, sample.magnitude))
                    onFirstTap()
                } else {
                    series = mutableListOf()
                    onReject(sample.timeMs, "move")
                }
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
        const val HISTORY_MS = 2_000L
        const val TAP_THRESHOLD = 0.45f
        const val REFRACTORY_MS = 80L
        const val MAX_TAP = 2.0f
        const val MOTION_ABORT = 2.5f
        const val MOTION_COOLDOWN_MS = 800L
        const val SIMILAR_RATIO = 3f
        const val CONFIRM_MS = 300L
        const val MAX_TAPS = 3
        const val GAP_MIN_MS = 150L
        const val GAP_MAX_MS = 500L
        const val QUIET_RMS = 0.2f
        const val QUIET_GUARD_MS = 100L
        const val QUIET_LOOKBACK_MS = 800L
        const val COOLDOWN_MS = 1_200L
    }
}
