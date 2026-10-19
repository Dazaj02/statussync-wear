package com.statussync.wear.domain

/**
 * Estado creado por el usuario. Vive solo en el reloj: a los compañeros se les comparte su estado base
 * (`base`), así el contrato (docs/plan/CONTRATO.md §1) no cambia.
 */
data class CustomState(
    val id: String,
    val name: String,
    val base: AvailabilityState,
    val colorArgb: Long,
    val durationsMin: List<Int>,
) {
    val hasTimer: Boolean get() = base.hasTimer && durationsMin.isNotEmpty()

    companion object {
        const val MAX_COUNT = 8
    }
}
