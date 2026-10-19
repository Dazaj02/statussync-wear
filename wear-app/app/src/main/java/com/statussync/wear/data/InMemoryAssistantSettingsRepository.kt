package com.statussync.wear.data

import com.statussync.wear.domain.AssistantSettings
import com.statussync.wear.domain.AssistantSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

open class InMemoryAssistantSettingsRepository(
    initial: AssistantSettings = AssistantSettings(),
) : AssistantSettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    override fun update(transform: (AssistantSettings) -> AssistantSettings) {
        _settings.update(transform)
        onChanged(_settings.value)
    }

    /** Gancho para persistir; la versión en memoria no hace nada. */
    protected open fun onChanged(settings: AssistantSettings) = Unit
}
