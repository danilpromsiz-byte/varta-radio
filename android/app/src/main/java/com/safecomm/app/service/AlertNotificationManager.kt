package com.safecomm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.safecomm.app.MainActivity
import com.safecomm.app.data.model.Alert
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.i18n.LanguageManager
import com.safecomm.app.util.GeoUtils
import kotlinx.coroutines.*

class AlertNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val CHANNEL_HAZARD_ID = "varta_hazard_alerts_v2"
        const val CHANNEL_VOICE_ID = "varta_unplayed_voice_v2"

        const val EXTRA_TARGET_ROOM = "EXTRA_TARGET_ROOM"
        const val EXTRA_PLAY_VOICE_ID = "EXTRA_PLAY_VOICE_ID"
        const val EXTRA_TARGET_ALERT_ID = "EXTRA_TARGET_ALERT_ID"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val lang = LanguageManager.getLanguage(context)

            val hazardChannel = NotificationChannel(
                CHANNEL_HAZARD_ID,
                I18n.hazardNotificationChannel(lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Экстренные метки об опасностях поверх всех окон"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 800)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE_ID,
                I18n.voiceNotificationChannel(lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Непрослушанные голосовые сообщения рации поверх всех окон"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(hazardChannel)
            notificationManager.createNotificationChannel(voiceChannel)
        }
    }

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
        val isWithinRadius = radiusKm >= 999.0 || (distanceKm in 0.0..radiusKm)

        Log.d("AlertNotify", "Alert ${alert.id} (${alert.alertType}) dist=${distanceKm}km, limit=${radiusKm}km, inside=$isWithinRadius")

        if (isWithinRadius) {
            triggerHapticAlert(alert.typedCategory)
            if (soundEnabled) {
                triggerSoundAlert(alert.typedCategory)
            }
        }

        return distanceKm
    }

    fun triggerRoomSiren(type: AlertType = AlertType.SHELLING, soundEnabled: Boolean = true, vibrateEnabled: Boolean = true) {
        if (vibrateEnabled) {
            triggerHapticAlert(type)
        }
        if (soundEnabled) {
            triggerSoundAlert(type)
        }
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

    fun triggerHapticAlert(type: AlertType = AlertType.SHELLING) {
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

    fun triggerSoundAlert(type: AlertType = AlertType.SHELLING) {
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

    // --- System Notifications (Heads-Up & Notification Drawer) ---

    fun showHazardNotification(alert: Alert, roomName: String, isFromOtherRoom: Boolean = false) {
        if (notificationManager == null) return
        val lang = LanguageManager.getLanguage(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_ROOM, roomName)
            putExtra(EXTRA_TARGET_ALERT_ID, alert.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (alert.id % 100000).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val typeName = I18n.alertTypeName(lang, alert.alertType)
        val title = if (isFromOtherRoom) {
            "🚨 $typeName • Комната «$roomName»"
        } else {
            "🚨 $typeName"
        }

        val desc = if (alert.description.isNotBlank()) alert.description else "Метка опасности"
        val body = "$desc • Сообщил: ${alert.senderNick} (${alert.createdAt})"

        val notif = NotificationCompat.Builder(context, CHANNEL_HAZARD_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nКомната: $roomName"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notifId = (alert.id % 100000).toInt() + 10000
        notificationManager.notify(notifId, notif)
    }

    fun showUnplayedVoiceNotification(
        senderNick: String,
        roomName: String,
        durationSec: Int,
        filePath: String,
        messageId: Long,
        isFromOtherRoom: Boolean = false
    ) {
        if (notificationManager == null) return
        val lang = LanguageManager.getLanguage(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_ROOM, roomName)
            putExtra(EXTRA_PLAY_VOICE_ID, messageId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (messageId % 100000).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isFromOtherRoom) {
            "🎙 Рация: $senderNick (Комната «$roomName»)"
        } else {
            "🎙 Рация: $senderNick"
        }
        val body = "Длительность: ${durationSec}с • Нажмите для прослушивания"

        val notif = NotificationCompat.Builder(context, CHANNEL_VOICE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nКомната: $roomName"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                I18n.playVoiceAction(lang),
                pendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notifId = (messageId % 100000).toInt() + 20000
        notificationManager.notify(notifId, notif)
    }

    fun dismissVoiceNotification(messageId: Long) {
        val notifId = (messageId % 100000).toInt() + 20000
        notificationManager?.cancel(notifId)
    }

    fun dismissHazardNotification(alertId: Long) {
        val notifId = (alertId % 100000).toInt() + 10000
        notificationManager?.cancel(notifId)
    }
}
