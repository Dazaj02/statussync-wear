package com.statussync.wear.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.ScheduleBlock
import com.statussync.wear.domain.ScheduleClassifier
import com.statussync.wear.domain.ScheduleSource
import com.statussync.wear.domain.SuggestionEngine
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Agenda del calendario del reloj (si el reloj la tiene). Sin permiso o sin datos devuelve una lista vacía. */
class CalendarScheduleSource(private val context: Context) : ScheduleSource {
    override suspend fun upcoming(now: Instant): List<ScheduleBlock> = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext emptyList()

        val from = now.toEpochMilli()
        val to = now.plus(Duration.ofMinutes(SuggestionEngine.SOON_MINUTES)).toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from)
            ContentUris.appendId(it, to)
        }.build()

        try {
            context.contentResolver.query(uri, PROJECTION, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            if (cursor.getInt(3) == 1) continue // eventos de todo el día no dicen nada del momento
                            val state = ScheduleClassifier.classify(cursor.getString(2))
                            if (state == AvailabilityState.Free) continue
                            add(ScheduleBlock(state, Instant.ofEpochMilli(cursor.getLong(0)), Instant.ofEpochMilli(cursor.getLong(1))))
                        }
                    }
                }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.ALL_DAY,
        )
    }
}
