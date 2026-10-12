package com.safecomm.app.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.safecomm.app.data.model.Alert
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.util.GeoUtils
import kotlinx.coroutines.*

class AlertNotificationManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun checkAndNotify(
        alert: Alert,
        userLat: Double,
        userLng: Double,
        radiusKm: Double,
        soundEnabled: Boolean
    ): Double {
        val distanceKm = GeoUtils.distanceKm(userLat, userLng, alert.latitude, alert.longitude)
        val isWithinRadius = radiusKm >= 999.0 || distanceKm <= radiusKm

        Log.d("AlertNotify", "Alert ${alert.id} (${alert.alertType}) dist=${distanceKm}km, limit=${radiusKm}km, inside=$isWithinRadius")

        if (isWithinRadius) {
            triggerHapticAlert(alert.typedCategory)
            if (soundEnabled) {
                triggerSoundAlert(alert.typedCategory)
            }
        }

        return distanceKm
    }

    fun triggerHapticFeedback(isLong: Boolean = false) {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (isLong) {
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isLong) 150L else 50L)
            }
        } catch (_: Exception) {}
    }

    private fun triggerHapticAlert(type: AlertType) {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            val timings = when (type) {
                AlertType.SHELLING -> longArrayOf(0, 400, 200, 400, 200, 800)
                AlertType.FIRE -> longArrayOf(0, 300, 150, 300, 150, 300)
                AlertType.MILITARY -> longArrayOf(0, 500, 300, 500, 300, 500)
                AlertType.POLICE -> longArrayOf(0, 200, 100, 200, 100, 200)
                AlertType.MEDICAL -> longArrayOf(0, 250, 250, 250)
                AlertType.OTHER -> longArrayOf(0, 300, 200, 300)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = IntArray(timings.size) { if (it % 2 == 0) 0 else 255 }
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.e("AlertNotify", "Vibration failed", e)
        }
    }

    private fun triggerSoundAlert(type: AlertType) {
        scope.launch {
            try {
                val toneType = when (type) {
                    AlertType.SHELLING -> ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
                    AlertType.FIRE -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                    AlertType.MILITARY -> ToneGenerator.TONE_CDMA_ABBR_ALERT
                    AlertType.POLICE -> ToneGenerator.TONE_SUP_RINGTONE
                    AlertType.MEDICAL -> ToneGenerator.TONE_PROP_BEEP
                    AlertType.OTHER -> ToneGenerator.TONE_PROP_PROMPT
                }

                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                repeat(3) {
                    toneGen.startTone(toneType, 400)
                    delay(500)
                }
                toneGen.release()
            } catch (e: Exception) {
                Log.e("AlertNotify", "Tone alert failed", e)
            }
        }
    }
}
