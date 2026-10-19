package com.statussync.wear

import android.app.Application
import com.statussync.wear.di.AppContainer

class StatusSyncApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
