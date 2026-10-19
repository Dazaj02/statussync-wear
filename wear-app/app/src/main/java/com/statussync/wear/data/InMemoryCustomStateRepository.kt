package com.statussync.wear.data

import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.CustomStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

open class InMemoryCustomStateRepository(initial: List<CustomState> = emptyList()) : CustomStateRepository {
    private val _states = MutableStateFlow(initial)
    override val states: StateFlow<List<CustomState>> = _states.asStateFlow()

    override fun add(state: CustomState): Boolean {
        var added = false
        _states.update { current ->
            if (current.size >= CustomState.MAX_COUNT) current else (current + state).also { added = true }
        }
        if (added) onChanged(_states.value)
        return added
    }

    override fun remove(id: String) {
        _states.update { current -> current.filterNot { it.id == id } }
        onChanged(_states.value)
    }

    /** Gancho para persistir; la versión en memoria no hace nada. */
    protected open fun onChanged(states: List<CustomState>) = Unit
}
