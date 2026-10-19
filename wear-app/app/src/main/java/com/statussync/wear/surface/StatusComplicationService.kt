package com.statussync.wear.surface

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.statussync.wear.MainActivity
import com.statussync.wear.R
import com.statussync.wear.StatusSyncApp
import com.statussync.wear.domain.AvailabilityState

/** Complication para la esfera: estado actual en texto corto (≈7 letras) o largo, con ícono y minutos restantes. */
class StatusComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, StatusSummary(AvailabilityState.Focus, null, AvailabilityState.Focus.colorArgb, 25))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val container = (application as StatusSyncApp).container
        container.statusRepository.expireIfDue()
        val summary = container.statusRepository.status.value.toSummary(container.clock.instant())
        return build(request.complicationType, summary)
    }

    private fun build(type: ComplicationType, summary: StatusSummary): ComplicationData? {
        val fullName = summary.customName ?: getString(summary.state.labelRes)
        val remaining = summary.remainingMinutes?.let { getString(R.string.complication_remaining_format, it) }
        val description = listOfNotNull(fullName, remaining).joinToString(", ")
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, summary.state.iconRes)).build()
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(getString(shortLabelRes(summary.state))).build(),
                contentDescription = PlainComplicationText.Builder(description).build(),
            )
                .setTitle(remaining?.let { PlainComplicationText.Builder(it).build() })
                .setMonochromaticImage(icon)
                .setTapAction(tap)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(fullName).build(),
                contentDescription = PlainComplicationText.Builder(description).build(),
            )
                .setTitle(remaining?.let { PlainComplicationText.Builder(it).build() })
                .setMonochromaticImage(icon)
                .setTapAction(tap)
                .build()

            else -> null
        }
    }

    private fun shortLabelRes(state: AvailabilityState) = when (state) {
        AvailabilityState.Free -> R.string.short_free
        AvailabilityState.Focus -> R.string.short_focus
        AvailabilityState.Class -> R.string.short_class
        AvailabilityState.Call -> R.string.short_call
    }
}
