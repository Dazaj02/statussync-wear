package com.statussync.wear.domain

import java.time.Instant

/** Compañero del grupo. Misma forma que `Status` de `GET /groups/:id/statuses` (los compañeros solo ven los 4 estados base). */
data class GroupMember(
    val userId: String,
    val displayName: String,
    val status: Status,
)

/** Un bloque vencido cuenta como Libre aunque el servidor aún no lo haya actualizado. */
fun GroupMember.effective(now: Instant): GroupMember =
    if (status.isExpired(now)) copy(status = Status.free(now)) else this

/**
 * Orden de la lista: primero los libres (a quienes puedes interrumpir), luego los ocupados
 * por quien termina antes, y a igualdad por nombre.
 */
fun List<GroupMember>.sortedForDisplay(now: Instant): List<GroupMember> =
    map { it.effective(now) }.sortedWith(
        compareBy<GroupMember> { it.status.state != AvailabilityState.Free }
            .thenBy { it.status.until ?: Instant.MAX }
            .thenBy { it.displayName.lowercase() },
    )
