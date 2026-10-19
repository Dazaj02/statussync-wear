package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.Status
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusCodecTest {
    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val gym = CustomState("g1", "Gimnasio", AvailabilityState.Focus, 0xFF3B82F6, listOf(25, 50))

    @Test
    fun roundTripsARunningStatus() {
        val status = Status(AvailabilityState.Call, now.minusSeconds(60), now.plus(Duration.ofMinutes(10)))

        assertEquals(status, StatusCodec.decode(StatusCodec.encode(status), emptyList(), now))
    }

    @Test
    fun restoresTheCustomStateByIdAndBase() {
        val status = Status(AvailabilityState.Focus, now, now.plus(Duration.ofMinutes(20)), gym)

        assertEquals(gym, StatusCodec.decode(StatusCodec.encode(status), listOf(gym), now).custom)
        assertNull(StatusCodec.decode(StatusCodec.encode(status), emptyList(), now).custom)
    }

    @Test
    fun anExpiredBlockComesBackFree() {
        val old = Status(AvailabilityState.Focus, now.minusSeconds(900), now.minusSeconds(5))

        assertEquals(AvailabilityState.Free, StatusCodec.decode(StatusCodec.encode(old), emptyList(), now).state)
    }

    @Test
    fun missingOrDamagedDataIsFree() {
        listOf(null, "", "basura", "focus|x|y|z", "nope|1|2|").forEach {
            assertEquals(AvailabilityState.Free, StatusCodec.decode(it, emptyList(), now).state)
        }
    }

    @Test
    fun freeHasNoTimer() {
        assertNull(StatusCodec.decode(StatusCodec.encode(Status.free(now)), emptyList(), now).until)
    }
}
