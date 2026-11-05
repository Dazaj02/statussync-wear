package com.statussync.wear.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.statussync.wear.domain.Gesture
import com.statussync.wear.domain.GestureDetector
import com.statussync.wear.domain.GestureSource

/**
 * Lee acelerómetro y giroscopio solo mientras la app está visible (la UI llama a [stop] al salir).
 * No guarda nada: cada muestra va al detector y se descarta.
 */
class GestureSensorSource(context: Context) : GestureSource, SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val detector = GestureDetector()
    private var listener: ((Gesture) -> Unit)? = null

    override fun start(onGesture: (Gesture) -> Unit) {
        listener = onGesture
        sensorManager ?: return
        // Registrar dos veces el mismo listener solo reemplaza el registro anterior.
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun stop() {
        sensorManager?.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timeMs = event.timestamp / 1_000_000L
        val (x, y, z) = event.values.let { Triple(it[0], it[1], it[2]) }
        val gesture = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> detector.onAccelerometer(timeMs, x, y, z)
            Sensor.TYPE_GYROSCOPE -> detector.onGyroscope(timeMs, x, y, z)
            else -> null
        }
        if (gesture != null) listener?.invoke(gesture)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
