package com.statussync.wear.data

import android.content.SharedPreferences
import com.statussync.wear.domain.CustomState
import com.statussync.wear.domain.Status
import java.time.Clock

/** Guarda el estado actual en el reloj: sobrevive si Android cierra la app y lo ven el Tile y la esfera. */
class SharedPrefsStatusRepository(
    private val prefs: SharedPreferences,
    clock: Clock,
    customStates: List<CustomState>,
) : InMemoryStatusRepository(
    clock,
    StatusCodec.decode(prefs.getString(KEY, null), customStates, clock.instant()),
) {
    override fun onChanged(status: Status) {
        prefs.edit().putString(KEY, StatusCodec.encode(status)).apply()
    }

    private companion object {
        const val KEY = "status"
    }
}
