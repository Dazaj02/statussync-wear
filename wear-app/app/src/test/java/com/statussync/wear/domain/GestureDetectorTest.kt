package com.statussync.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureDetectorTest {
    private val detector = GestureDetector()

    /** Golpe de aceleración con la desviación pedida respecto a la gravedad. */
    private fun GestureDetector.hit(t: Long, deviation: Float = 20f) =
        onAccelerometer(t, 0f, GestureDetector.GRAVITY + deviation, 0f)

    private fun GestureDetector.rest(t: Long) = onAccelerometer(t, 0f, GestureDetector.GRAVITY, 0f)

    @Test
    fun twoQuickShakesAfterRestAreADoubleShake() {
        assertNull(detector.rest(0))
        assertNull(detector.hit(2_000))
        assertNull(detector.rest(2_100))
        assertEquals(Gesture.DoubleShake, detector.hit(2_300))
    }

    @Test
    fun aSingleShakeIsIgnored() {
        assertNull(detector.hit(2_000))
        assertNull(detector.rest(2_500))
        assertNull(detector.hit(4_000)) // pasó demasiado tiempo: empieza una cadena nueva
    }

    @Test
    fun theSamePeakSpreadOverSeveralSamplesCountsOnce() {
        assertNull(detector.hit(2_000))
        assertNull(detector.hit(2_020))
        assertNull(detector.hit(2_040))
    }

    @Test
    fun gentleArmSwingFromWalkingNeverTriggers() {
        (0..60).forEach { assertNull(detector.hit(it * 400L, deviation = 5f)) }
    }

    @Test
    fun continuousStrongMotionLikeRunningTriggersAtMostOnce() {
        val fired = (0..30).mapNotNull { detector.hit(2_000L + it * 350L) }

        assertEquals(1, fired.size) // solo el arranque, que se parece a una doble sacudida
    }

    @Test
    fun cooldownAvoidsAFollowUpGesture() {
        detector.hit(2_000)
        assertEquals(Gesture.DoubleShake, detector.hit(2_300))
        assertNull(detector.hit(2_500))
        assertNull(detector.hit(2_800))
    }

    @Test
    fun aSustainedTurnAroundTheForearmIsAWristTwist() {
        assertNull(detector.onGyroscope(1_000, 0.2f, 5f, 0.1f))
        assertNull(detector.onGyroscope(1_040, 0.2f, 5f, 0.1f))
        assertEquals(Gesture.WristTwist, detector.onGyroscope(1_100, 0.2f, 5f, 0.1f))
    }

    @Test
    fun aTurnOnAnotherAxisIsNotATwist() {
        listOf(1_000L, 1_050L, 1_100L, 1_150L).forEach { assertNull(detector.onGyroscope(it, 5f, 4f, 0.1f)) }
    }

    @Test
    fun aVeryShortTurnIsIgnored() {
        assertNull(detector.onGyroscope(1_000, 0f, 5f, 0f))
        assertNull(detector.onGyroscope(1_030, 0f, 0.2f, 0f))
        assertNull(detector.onGyroscope(1_060, 0f, 5f, 0f))
    }

    @Test
    fun aShakeDoesNotCountAsATwist() {
        detector.hit(2_000)
        assertNull(detector.onGyroscope(2_050, 0f, 6f, 0f))
        assertNull(detector.onGyroscope(2_150, 0f, 6f, 0f))
    }
}
