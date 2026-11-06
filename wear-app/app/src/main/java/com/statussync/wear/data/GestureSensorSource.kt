package com.statussync.wear.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.statussync.wear.domain.Gesture
import com.statussync.wear.domain.GestureDetector
import com.statussync.wear.domain.GestureSource
import com.statussync.wear.domain.PinchDetector

/**
 * Lee acelerómetro y giroscopio solo mientras la app está visible (la UI llama a [stop] al salir).
 * No guarda nada: cada muestra va a los detectores y se descarta.
 */
class GestureSensorSource(context: Context) : GestureSource, SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val shakeDetector = GestureDetector()
    private val pinchDetector = PinchDetector()
    private var listener: ((Gesture) -> Unit)? = null
    private var shakeAndTwist = false
    private var pinch = false

    override fun start(onGesture: (Gesture) -> Unit, shakeAndTwist: Boolean, pinch: Boolean) {
        val manager = sensorManager ?: return
        manager.unregisterListener(this) // cambia la configuración: se registra de nuevo
        listener = onGesture
        this.shakeAndTwist = shakeAndTwist
        this.pinch = pinch
        // El pellizco necesita ~100 Hz; sacudida y giro bastan con la frecuencia de juegos.
        val accelerometerRate = if (pinch) SensorManager.SENSOR_DELAY_FASTEST else SensorManager.SENSOR_DELAY_GAME
        accelerometer?.let { manager.registerListener(this, it, accelerometerRate) }
        if (shakeAndTwist) gyroscope?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun stop() {
        sensorManager?.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timeMs = event.timestamp / 1_000_000L
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gesture = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER ->
                (if (shakeAndTwist) shakeDetector.onAccelerometer(timeMs, x, y, z) else null)
                    ?: (if (pinch) pinchDetector.onAccelerometer(timeMs, x, y, z) else null)
            Sensor.TYPE_GYROSCOPE -> if (shakeAndTwist) shakeDetector.onGyroscope(timeMs, x, y, z) else null
            else -> null
        }
        if (gesture != null) listener?.invoke(gesture)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
