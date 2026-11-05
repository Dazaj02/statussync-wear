package com.statussync.wear.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.statussync.wear.R
import com.statussync.wear.data.SensorRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Session(val label: String, val seconds: Int, val buttonRes: Int, val hintRes: Int)

private val SESSIONS = listOf(
    Session("reposo", 15, R.string.lab_rest, R.string.lab_rest_hint),
    Session("pellizco2", 20, R.string.lab_pinch2, R.string.lab_pinch_hint),
    Session("pellizco3", 20, R.string.lab_pinch3, R.string.lab_pinch_hint),
    Session("caminar", 15, R.string.lab_walk, R.string.lab_walk_hint),
)

/** Pantalla de pruebas: graba sensores 15–20 s mientras haces cada movimiento. No forma parte del uso normal. */
@Composable
fun SensorLabScreen(recorder: SensorRecorder) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var message by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    // Con la pantalla apagada Wear OS detiene los sensores: se mantiene encendida mientras estás aquí.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            recorder.cancel()
        }
    }

    val getReady = stringResource(R.string.lab_get_ready)
    val recordingFormat = stringResource(R.string.lab_recording_format)
    val doneFormat = stringResource(R.string.lab_done_format)
    val noSensor = stringResource(R.string.lab_no_sensor)

    fun run(session: Session) {
        scope.launch {
            busy = true
            hint = session.hintRes
            for (i in 3 downTo 1) {
                message = getReady.format(i)
                delay(1_000)
            }
            if (!recorder.start()) {
                message = noSensor
                busy = false
                return@launch
            }
            for (s in session.seconds downTo 1) {
                message = recordingFormat.format(s)
                delay(1_000)
            }
            message = doneFormat.format(recorder.stop(session.label))
            busy = false
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.lab_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = message.ifEmpty { stringResource(R.string.lab_intro) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hint != 0) {
            item {
                Text(
                    text = stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SESSIONS.forEach { session ->
            item {
                Button(
                    onClick = { run(session) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    label = { Text(stringResource(session.buttonRes)) },
                )
            }
        }
    }
}
