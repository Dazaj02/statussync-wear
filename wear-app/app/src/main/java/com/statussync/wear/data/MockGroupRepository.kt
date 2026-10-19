package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.GroupMember
import com.statussync.wear.domain.GroupRepository
import com.statussync.wear.domain.Status
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Grupo de mentira para la demo: 5 compañeros inventados cuyo estado cambia solo cada cierto tiempo.
 * Los bloques con temporizador vencen y vuelven a Libre. Los datos son ficticios.
 */
class MockGroupRepository(
    private val clock: Clock,
    private val random: Random = Random.Default,
    private val tickMillis: Long = 5_000,
    private val changeEveryTicks: Int = 2,
    override val groupName: String = "Equipo Diseño",
) : GroupRepository {
    private var members: List<GroupMember> = seed(clock.instant())

    override fun members(): Flow<List<GroupMember>> = flow {
        var tick = 0
        while (true) {
            val now = clock.instant()
            if (tick > 0 && tick % changeEveryTicks == 0) members = changeOne(members, now)
            emit(members)
            tick++
            delay(tickMillis)
        }
    }

    private fun changeOne(current: List<GroupMember>, now: Instant): List<GroupMember> {
        val index = random.nextInt(current.size)
        val member = current[index]
        val busy = member.status.state != AvailabilityState.Free && !member.status.isExpired(now)
        val next = if (busy) Status.free(now) else randomBusy(now)
        return current.toMutableList().also { it[index] = member.copy(status = next) }
    }

    private fun randomBusy(now: Instant): Status {
        val state = AvailabilityState.entries.filter { it.hasTimer }.random(random)
        val minutes = state.durationOptionsMin.random(random)
        return Status(state, now, now.plus(Duration.ofMinutes(minutes.toLong())))
    }

    private companion object {
        fun seed(now: Instant): List<GroupMember> = listOf(
            member("ana", "Ana", AvailabilityState.Focus, 22, now),
            member("luis", "Luis", null, 0, now),
            member("marta", "Marta", AvailabilityState.Class, 40, now),
            member("diego", "Diego", AvailabilityState.Call, 12, now),
            member("sofia", "Sofía", null, 0, now),
        )

        fun member(id: String, name: String, state: AvailabilityState?, minutes: Long, now: Instant) = GroupMember(
            userId = id,
            displayName = name,
            status = if (state == null) Status.free(now) else Status(state, now, now.plus(Duration.ofMinutes(minutes))),
        )
    }
}
