package com.statussync.wear.domain

import java.time.Duration
import kotlinx.coroutines.flow.StateFlow

/** Puerto del estado propio. W1-W2: en memoria; W6: Supabase + API. */
interface StatusRepository {
    val status: StateFlow<Status>

    /**
     * Cambia el estado. Con `duration` el estado vence solo y vuelve a Libre.
     * `custom` es el estado propio elegido (su `base` debe ser `state`); solo afecta a lo que se muestra.
     */
    fun setState(state: AvailabilityState, duration: Duration? = null, custom: CustomState? = null)

    /** Vuelve a Libre si el bloque actual ya venció. */
    fun expireIfDue()
}
