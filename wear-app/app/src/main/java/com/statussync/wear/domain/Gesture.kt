package com.statussync.wear.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Gestos de muñeca que reconoce el reloj mientras la app está visible. */
enum class Gesture {
    /** Dos sacudidas seguidas: Libre pasa a Enfoque. */
    DoubleShake,

    /** Giro de muñeca sobre el eje del antebrazo: abre el selector de estado. */
    WristTwist,
}

/** Origen de gestos. Se suelta al salir de la app para no gastar batería. */
interface GestureSource {
    fun start(onGesture: (Gesture) -> Unit)

    fun stop()

    object None : GestureSource {
        override fun start(onGesture: (Gesture) -> Unit) = Unit
        override fun stop() = Unit
    }
}

/**
 * Detector puro (sin Android) para poder probarlo con muestras sintéticas.
 * Aceleración en m/s² (con gravedad) y giro en rad/s; los ejes son los del reloj:
 * Y apunta hacia la mano, así que el giro de muñeca es rotación sobre Y.
 */
class GestureDetector {
    private var cooldownUntil = Long.MIN_VALUE
    private var chainCount = 0
    private var chainClean = false
    private var lastPeak = Long.MIN_VALUE / 2
    private var twistStart = -1L

    /** Una muestra de acelerómetro. Devuelve [Gesture.DoubleShake] al completarse la segunda sacudida. */
    fun onAccelerometer(timeMs: Long, x: Float, y: Float, z: Float): Gesture? {
        val deviation = abs(sqrt(x * x + y * y + z * z) - GRAVITY)
        if (deviation < SHAKE_PEAK) return null
        if (timeMs < cooldownUntil) {
            lastPeak = timeMs // el movimiento sigue: la próxima cadena no cuenta como "tras reposo"
            return null
        }
        if (chainCount > 0 && timeMs - lastPeak < PEAK_GAP_MIN_MS) return null // sigue siendo el mismo golpe

        if (chainCount > 0 && timeMs - lastPeak > PEAK_GAP_MAX_MS) chainCount = 0
        if (chainCount == 0) chainClean = timeMs - lastPeak > QUIET_BEFORE_MS
        chainCount++
        lastPeak = timeMs

        if (chainCount < 2) return null
        chainCount = 0
        // Si había movimiento justo antes (caminar, correr), no es un gesto deliberado.
        if (!chainClean) return null
        cooldownUntil = timeMs + COOLDOWN_MS
        return Gesture.DoubleShake
    }

    /** Una muestra de giroscopio. Devuelve [Gesture.WristTwist] si el giro sobre Y se sostiene lo suficiente. */
    fun onGyroscope(timeMs: Long, x: Float, y: Float, z: Float): Gesture? {
        if (timeMs < cooldownUntil) return null
        val onForearmAxis = abs(y) >= TWIST_RATE && abs(y) >= TWIST_DOMINANCE * max(abs(x), abs(z))
        // Una sacudida también gira un poco la muñeca: no se confunde con el giro.
        if (!onForearmAxis || timeMs - lastPeak < SHAKE_SETTLE_MS) {
            twistStart = -1L
            return null
        }
        if (twistStart < 0) twistStart = timeMs
        if (timeMs - twistStart < TWIST_MIN_MS) return null
        twistStart = -1L
        cooldownUntil = timeMs + COOLDOWN_MS
        return Gesture.WristTwist
    }

    companion object {
        const val GRAVITY = 9.81f
        /** Desviación de la gravedad que cuenta como golpe de sacudida. Caminar queda por debajo. */
        const val SHAKE_PEAK = 12f
        const val PEAK_GAP_MIN_MS = 120L
        const val PEAK_GAP_MAX_MS = 800L
        const val QUIET_BEFORE_MS = 1_000L
        const val COOLDOWN_MS = 1_500L
        const val TWIST_RATE = 3.5f
        const val TWIST_DOMINANCE = 1.5f
        const val TWIST_MIN_MS = 80L
        const val SHAKE_SETTLE_MS = 300L
    }
}
