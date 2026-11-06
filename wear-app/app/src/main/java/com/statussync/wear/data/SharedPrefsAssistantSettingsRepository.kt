package com.statussync.wear.data

import android.content.SharedPreferences
import com.statussync.wear.domain.AssistantSettings

/** Guarda los interruptores del asistente en el reloj. Todo arranca apagado. */
class SharedPrefsAssistantSettingsRepository(
    private val prefs: SharedPreferences,
) : InMemoryAssistantSettingsRepository(
    AssistantSettings(
        calendar = prefs.getBoolean(CALENDAR, false),
        motion = prefs.getBoolean(MOTION, false),
        autoApply = prefs.getBoolean(AUTO, false),
        gestures = prefs.getBoolean(GESTURES, false),
        pinch = prefs.getBoolean(PINCH, false),
    ),
) {
    override fun onChanged(settings: AssistantSettings) {
        prefs.edit()
            .putBoolean(CALENDAR, settings.calendar)
            .putBoolean(MOTION, settings.motion)
            .putBoolean(AUTO, settings.autoApply)
            .putBoolean(GESTURES, settings.gestures)
            .putBoolean(PINCH, settings.pinch)
            .apply()
    }

    private companion object {
        const val CALENDAR = "assistant_calendar"
        const val MOTION = "assistant_motion"
        const val AUTO = "assistant_auto"
        const val GESTURES = "assistant_gestures"
        const val PINCH = "assistant_pinch"
    }
}
