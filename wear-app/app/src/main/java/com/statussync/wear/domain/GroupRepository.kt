package com.statussync.wear.domain

import kotlinx.coroutines.flow.Flow

/** Puerto del grupo. En W6 lo implementa Supabase Realtime; hoy lo simula [com.statussync.wear.data.MockGroupRepository]. */
interface GroupRepository {
    val groupName: String

    /** Compañeros con su estado. Es un flujo frío: solo trabaja mientras alguien lo observa. */
    fun members(): Flow<List<GroupMember>>
}
