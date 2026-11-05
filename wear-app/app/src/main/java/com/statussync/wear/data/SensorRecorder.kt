package com.statussync.wear.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File

/**
 * Herramienta de laboratorio: graba acelerómetro y giroscopio a la máxima frecuencia a un CSV
 * (`t_ns,sensor,x,y,z`) para estudiar si un pellizco índice-pulgar se distingue del ruido.
 * Los archivos quedan en el almacenamiento de la app y se leen con `adb pull`.
 */
class SensorRecorder(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rows = ArrayList<String>(60_000)
    private var recording = false

    fun start(): Boolean {
        val manager = sensorManager ?: return false
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        rows.clear()
        recording = true
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        return true
    }

    /** Detiene la grabación y guarda el CSV. Devuelve cuántas muestras se guardaron. */
    fun stop(label: String): Int {
        cancel()
        val count = rows.size
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        File(dir, "lab_${label}_${System.currentTimeMillis()}.csv")
            .writeText("t_ns,sensor,x,y,z\n" + rows.joinToString("\n"))
        rows.clear()
        return count
    }

    /** Suelta los sensores sin guardar. */
    fun cancel() {
        if (recording) sensorManager?.unregisterListener(this)
        recording = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!recording) return
        val sensor = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) "a" else "g"
        rows.add("${event.timestamp},$sensor,${event.values[0]},${event.values[1]},${event.values[2]}")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
