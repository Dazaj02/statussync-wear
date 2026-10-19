package com.statussync.wear

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.statussync.wear.di.AppContainer
import com.statussync.wear.surface.StatusComplicationService
import com.statussync.wear.surface.StatusTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StatusSyncApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Cada vez que cambia el estado (en la app o en el Tile), la esfera y el Tile se redibujan.
        appScope.launch {
            container.statusRepository.status
                .map { it.state to it.custom?.id to it.until }
                .distinctUntilChanged()
                .collect { refreshSurfaces() }
        }
    }

    private fun refreshSurfaces() {
        TileService.getUpdater(this).requestUpdate(StatusTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(this, ComponentName(this, StatusComplicationService::class.java))
            .requestUpdateAll()
    }
}
