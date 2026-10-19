package com.statussync.wear.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.statussync.wear.domain.MotionFeature
import com.statussync.wear.domain.MotionSource
import java.time.Instant

/**
 * Movimiento a partir del contador de pasos del reloj. Solo observa mientras la app está visible
 * (el asistente llama a [release] al salir) y guarda agregados por minuto, nunca lecturas crudas.
 */
class StepCounterMotionSource(
    private val context: Context,
    private val sampler: MotionSampler = MotionSampler(),
) : MotionSource, SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    @Volatile private var latest: Long? = null
    private var listening = false

    override suspend fun recent(now: Instant): List<MotionFeature> {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted || stepSensor == null) {
            release()
            return emptyList()
        }
        if (!listening) {
            listening = sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL) == true
        }
        latest?.let { sampler.record(now, it) }
        return sampler.features()
    }

    override fun release() {
        if (listening) sensorManager?.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        latest = event.values.firstOrNull()?.toLong()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
