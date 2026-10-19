package com.statussync.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.statussync.wear.ui.StatusSyncRoot
import com.statussync.wear.ui.home.StatusViewModel
import com.statussync.wear.ui.theme.StatusSyncTheme

class MainActivity : ComponentActivity() {
    private val viewModel: StatusViewModel by viewModels {
        viewModelFactory {
            val container = (application as StatusSyncApp).container
            initializer {
                StatusViewModel(container.statusRepository, container.customStateRepository, container.clock)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StatusSyncTheme {
                StatusSyncRoot(viewModel = viewModel, onTimerFinished = ::vibrateTimerFinished)
            }
        }
    }

    private fun vibrateTimerFinished() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250, 120, 250), -1))
    }
}
