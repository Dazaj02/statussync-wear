package com.statussync.wear.domain

import kotlinx.coroutines.flow.StateFlow

interface CustomStateRepository {
    val states: StateFlow<List<CustomState>>

    /** Devuelve false si ya se alcanzó [CustomState.MAX_COUNT]. */
    fun add(state: CustomState): Boolean

    fun remove(id: String)
}
