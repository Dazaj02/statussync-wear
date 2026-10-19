package com.statussync.wear.data

import android.content.SharedPreferences
import com.statussync.wear.domain.CustomState

/** Guarda los estados propios en el reloj para que sobrevivan al cierre de la app. */
class SharedPrefsCustomStateRepository(
    private val prefs: SharedPreferences,
) : InMemoryCustomStateRepository(CustomStateCodec.decode(prefs.getString(KEY, null))) {

    override fun onChanged(states: List<CustomState>) {
        prefs.edit().putString(KEY, CustomStateCodec.encode(states)).apply()
    }

    private companion object {
        const val KEY = "custom_states"
    }
}
