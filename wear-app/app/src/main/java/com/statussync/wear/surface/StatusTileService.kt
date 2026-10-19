package com.statussync.wear.surface

import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.statussync.wear.MainActivity
import com.statussync.wear.R
import com.statussync.wear.StatusSyncApp
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.ui.theme.onColor
import java.time.Duration

/**
 * Tile de cambio rápido: muestra el estado actual y 4 botones (ícono + color) que lo cambian con un toque.
 * Los botones usan la duración rápida de cada estado; para elegir otra, se abre la app.
 */
class StatusTileService : TileService() {
    private val repository get() = (application as StatusSyncApp).container.statusRepository
    private val clock get() = (application as StatusSyncApp).container.clock

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        applyClick(requestParams.currentState.lastClickableId)
        repository.expireIfDue()

        val summary = repository.status.value.toSummary(clock.instant())
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(summary)))
            // Con temporizador, refresca cada minuto para que los minutos restantes no se queden viejos.
            .setFreshnessIntervalMillis(if (summary.remainingMinutes != null) 60_000L else 0L)
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION)
        AvailabilityState.entries.forEach { state ->
            resources.addIdToImageMapping(
                iconId(state),
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(state.iconRes).build(),
                    )
                    .build(),
            )
        }
        return Futures.immediateFuture(resources.build())
    }

    /**
     * El Tile recuerda el último botón tocado y lo reenvía en cada refresco. Cada render pone un sello distinto
     * en los ids, así un toque nuevo (id nuevo) se aplica y un reenvío del mismo id se ignora.
     */
    private fun applyClick(clickableId: String) {
        if (!clickableId.startsWith(CLICK_PREFIX) || clickableId == lastHandledClick) return
        lastHandledClick = clickableId
        val wire = clickableId.removePrefix(CLICK_PREFIX).substringBefore('@')
        val state = AvailabilityState.fromWire(wire) ?: return
        repository.setState(state, state.quickDurationMin?.let { Duration.ofMinutes(it.toLong()) })
    }

    private fun layout(summary: StatusSummary): LayoutElement {
        val stamp = System.currentTimeMillis()
        val name = summary.customName ?: getString(summary.state.labelRes)
        val detail = summary.remainingMinutes?.let { getString(R.string.tile_remaining_format, it) }
            ?: getString(R.string.free_hint)

        val buttons = Row.Builder().setHeight(wrap())
        AvailabilityState.entries.forEachIndexed { index, state ->
            if (index > 0) buttons.addContent(Spacer.Builder().setWidth(dp(8f)).build())
            buttons.addContent(stateButton(state, selected = state == summary.state && summary.customName == null, stamp))
        }

        return Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                Text.Builder()
                    .setText(name)
                    .setFontStyle(
                        FontStyle.Builder().setSize(sp(22f)).setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                            .setColor(argb(summary.colorArgb.toInt())).build(),
                    )
                    .setModifiers(openAppModifiers())
                    .build(),
            )
            .addContent(
                Text.Builder()
                    .setText(detail)
                    .setFontStyle(FontStyle.Builder().setSize(sp(13f)).setColor(argb(COLOR_MUTED)).build())
                    .build(),
            )
            .addContent(Spacer.Builder().setHeight(dp(14f)).build())
            .addContent(buttons.build())
            .build()
    }

    private fun stateButton(state: AvailabilityState, selected: Boolean, stamp: Long): LayoutElement {
        val modifiers = ModifiersBuilders.Modifiers.Builder()
            .setBackground(
                ModifiersBuilders.Background.Builder()
                    .setColor(argb(state.colorArgb.toInt()))
                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(24f)).build())
                    .build(),
            )
            .setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setId("$CLICK_PREFIX${state.wireName}@$stamp")
                    .setOnClick(ActionBuilders.LoadAction.Builder().build())
                    .build(),
            )
            .setSemantics(
                ModifiersBuilders.Semantics.Builder().setContentDescription(getString(state.labelRes)).build(),
            )
        if (selected) {
            modifiers.setBorder(ModifiersBuilders.Border.Builder().setWidth(dp(3f)).setColor(argb(COLOR_ON_DARK)).build())
        }
        return Box.Builder()
            .setWidth(dp(48f))
            .setHeight(dp(48f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(modifiers.build())
            .addContent(
                Image.Builder()
                    .setResourceId(iconId(state))
                    .setWidth(dp(26f))
                    .setHeight(dp(26f))
                    .setColorFilter(
                        LayoutElementBuilders.ColorFilter.Builder().setTint(argb(state.onColor().toArgb())).build(),
                    )
                    .build(),
            )
            .build()
    }

    private fun openAppModifiers(): ModifiersBuilders.Modifiers =
        ModifiersBuilders.Modifiers.Builder()
            .setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setId("open_app")
                    .setOnClick(
                        ActionBuilders.LaunchAction.Builder()
                            .setAndroidActivity(
                                ActionBuilders.AndroidActivity.Builder()
                                    .setPackageName(packageName)
                                    .setClassName(MainActivity::class.java.name)
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()

    private fun iconId(state: AvailabilityState) = "icon_${state.wireName}"

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val CLICK_PREFIX = "set_"
        const val COLOR_MUTED = 0xFFB4BAC2.toInt()
        const val COLOR_ON_DARK = 0xFFF2F4F7.toInt()

        @Volatile var lastHandledClick: String = ""
    }
}
