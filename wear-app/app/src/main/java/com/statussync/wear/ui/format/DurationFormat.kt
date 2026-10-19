package com.statussync.wear.ui.format

import java.time.Duration

/** "24:05" o "1:30:00" para la cuenta regresiva. Redondea hacia arriba al segundo. */
fun Duration.formatClock(): String {
    val totalSeconds = (toMillis() + 999) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** "25 min", "1 h", "1 h 30 min". */
fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}
