package com.statussync.wear.surface

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.statussync.wear.StatusSyncApp
import java.time.Instant

/** Vibración de "se acabó el bloque". La usan la app abierta y la alarma con la app cerrada. */
fun vibrateTimerFinished(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    val effect = VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250, 120, 250), -1)
    // Con uso ALARMA Android deja vibrar aunque la app esté en segundo plano; sin él lo descarta ("ignored_background").
    val alarmUsage = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    @Suppress("DEPRECATION")
    vibrator.vibrate(effect, alarmUsage)
}

/** Programa (o cancela) la alarma del sistema que avisa cuando vence el temporizador aunque la app esté cerrada. */
class TimerAlarm(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent() = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TimerEndReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(until: Instant?) {
        val manager = alarms ?: return
        if (until == null) {
            manager.cancel(pendingIntent())
            return
        }
        val at = until.toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent())
        } else {
            // Sin permiso de alarmas exactas, Android puede retrasarla unos minutos en reposo.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent())
        }
    }
}

class TimerEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as StatusSyncApp).container.statusRepository
        // Si la app abierta ya devolvió el estado a Libre y vibró, no se vibra dos veces.
        val due = repository.status.value.isExpired(Instant.now())
        repository.expireIfDue()
        if (due) vibrateTimerFinished(context)
    }
}

/** Vibración corta: "te entendí" tras un gesto de muñeca. */
/** Vibración mínima al detectar el primer pellizco, para saber que el reloj te está leyendo. */
fun vibrateTapSeen(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    vibrator.vibrate(VibrationEffect.createOneShot(25, 120))
}

fun vibrateGestureRecognized(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
}
