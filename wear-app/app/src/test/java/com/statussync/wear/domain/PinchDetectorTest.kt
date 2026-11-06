package com.statussync.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reproduce grabaciones reales del Galaxy Watch4 (acelerómetro a 100 Hz) a través del detector. */
class PinchDetectorTest {
    private fun detections(recording: String): Int {
        val detector = PinchDetector()
        val lines = javaClass.getResourceAsStream("/pinch/$recording.csv")!!.bufferedReader().readLines().drop(1)
        return lines.count { line ->
            val f = line.split(",")
            detector.onAccelerometer(f[0].toLong() / 1_000_000L, f[1].toFloat(), f[2].toFloat(), f[3].toFloat()) ==
                Gesture.DoublePinch
        }
    }

    @Test
    fun staysSilentWhileTheHandRests() = assertEquals(0, detections("reposo"))

    @Test
    fun walkingDoesNotTrigger() = assertEquals(0, detections("caminar"))

    @Test
    fun detectsMostDoublePinchesOfACleanRecording() {
        val found = detections("limpia") // la toma tiene unas 12 series de pellizcos
        assertTrue("detectó $found", found in 7..14)
    }

    @Test
    fun noisyRecordingsNeverProduceFalseAlarmsBeyondTheirSeries() {
        assertTrue(detections("ruidosa") <= 2)
    }
}
