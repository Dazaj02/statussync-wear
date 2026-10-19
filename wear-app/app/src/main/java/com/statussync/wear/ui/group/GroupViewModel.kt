package com.statussync.wear.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.GroupMember
import com.statussync.wear.domain.GroupRepository
import com.statussync.wear.domain.sortedForDisplay
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MemberRow(
    val userId: String,
    val name: String,
    val state: AvailabilityState,
    /** Minutos que le faltan, redondeados hacia arriba. Null si está libre. */
    val remainingMinutes: Long?,
)

data class GroupUiState(
    val groupName: String,
    val rows: List<MemberRow> = emptyList(),
) {
    val total: Int get() = rows.size
    val freeCount: Int get() = rows.count { it.state == AvailabilityState.Free }
}

class GroupViewModel(
    repository: GroupRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    /** Se suscribe al grupo solo mientras la pantalla está visible. */
    val uiState: StateFlow<GroupUiState> = repository.members()
        .map { members -> GroupUiState(repository.groupName, members.toRows()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupUiState(repository.groupName))

    private fun List<GroupMember>.toRows(): List<MemberRow> {
        val now = clock.instant()
        return sortedForDisplay(now).map { member ->
            MemberRow(
                userId = member.userId,
                name = member.displayName,
                state = member.status.state,
                remainingMinutes = member.status.remaining(now)?.let(::ceilMinutes),
            )
        }
    }

    private fun ceilMinutes(duration: Duration): Long = (duration.seconds + 59) / 60
}
