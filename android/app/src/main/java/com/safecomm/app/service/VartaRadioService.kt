package com.safecomm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.safecomm.app.MainActivity
import com.safecomm.app.R

/**
 * 24/7 Always-On Foreground Service for Varta Radio PTT.
 * Keeps CPU and network sockets awake during screen-off and Doze mode,
 * ensuring real-time Walkie-Talkie voice playback and sirens even when phone is locked.
 */
class VartaRadioService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "varta_ptt_channel"
        private const val CHANNEL_NAME = "Varta Radio PTT - Активный эфир"

        const val ACTION_START = "com.safecomm.app.service.START"
        const val ACTION_STOP = "com.safecomm.app.service.STOP"
        const val EXTRA_SECTOR = "extra_sector"
        const val EXTRA_CALLSIGN = "extra_callsign"

        fun start(context: Context, sector: String, callSign: String) {
            val intent = Intent(context, VartaRadioService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECTOR, sector)
                putExtra(EXTRA_CALLSIGN, callSign)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VartaRadioService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        acquireLocks()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val sector = intent?.getStringExtra(EXTRA_SECTOR) ?: "Закрытый эфир"
        val callSign = intent?.getStringExtra(EXTRA_CALLSIGN) ?: "Оператор"

        val notification = buildForegroundNotification(sector, callSign)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VartaRadio::AlwaysOnWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours timeout safety
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "VartaRadio::WifiLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d("VartaService", "WakeLock & WifiLock acquired successfully")
        } catch (e: Exception) {
            Log.e("VartaService", "Error acquiring locks", e)
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wifiLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
        wifiLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Поддержание рации и тревожных сигналов в активном состоянии при заблокированном экране"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(sector: String, callSign: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Varta Radio PTT: $sector")
            .setContentText("Позывной: $callSign • Рация на связи (Экран выкл. активен)")
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        Log.d("VartaService", "VartaRadioService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
