package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStateCodecTest {
    private val studying = CustomState("a1", "Estudiando", AvailabilityState.Focus, 0xFF3B82F6, listOf(25, 50, 90))
    private val rest = CustomState("b2", "Descanso", AvailabilityState.Free, 0xFF14B8A6, emptyList())

    @Test
    fun roundTripKeepsEveryField() {
        val decoded = CustomStateCodec.decode(CustomStateCodec.encode(listOf(studying, rest)))
        assertEquals(listOf(studying, rest), decoded)
    }

    @Test
    fun emptyOrMissingDataGivesAnEmptyList() {
        assertTrue(CustomStateCodec.decode(null).isEmpty())
        assertTrue(CustomStateCodec.decode("").isEmpty())
        assertTrue(CustomStateCodec.decode(CustomStateCodec.encode(emptyList())).isEmpty())
    }

    @Test
    fun damagedLinesAreSkippedInsteadOfCrashing() {
        val raw = listOf(
            "solo\tdos",
            "x\tNombre\tnope\t1\t15",
            "y\tNombre\tcall\tNaN\t15",
            "z\tNombre\tcall\t5\tabc",
            CustomStateCodec.encode(listOf(studying)),
        ).joinToString("\n")
        assertEquals(listOf(studying), CustomStateCodec.decode(raw))
    }

    @Test
    fun separatorsInsideANameCannotBreakTheFormat() {
        val tricky = studying.copy(name = "Con\ttab\ny salto")
        val decoded = CustomStateCodec.decode(CustomStateCodec.encode(listOf(tricky, rest)))
        assertEquals(listOf("Con tab y salto", "Descanso"), decoded.map { it.name })
    }
}
