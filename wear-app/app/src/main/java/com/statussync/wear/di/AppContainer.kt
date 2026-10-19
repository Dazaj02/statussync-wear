package com.statussync.wear.di

import android.content.Context
import com.statussync.wear.data.InMemoryStatusRepository
import com.statussync.wear.data.SharedPrefsCustomStateRepository
import com.statussync.wear.domain.CustomStateRepository
import com.statussync.wear.domain.StatusRepository
import java.time.Clock

/** Inyección manual. En W6 se cambia el adaptador mock por el real sin tocar la UI. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val statusRepository: StatusRepository = InMemoryStatusRepository(clock)
    val customStateRepository: CustomStateRepository =
        SharedPrefsCustomStateRepository(context.getSharedPreferences("statussync", Context.MODE_PRIVATE))
}
