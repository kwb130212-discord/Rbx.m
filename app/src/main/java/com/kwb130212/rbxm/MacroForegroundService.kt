package com.kwb130212.rbxm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/** Keeps the controller alive while the phone remains powered on.
 * Android may still stop/restrict background work depending on OEM policies.
 */
class MacroForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("서비스 대기 중"))
        RbxLogger.info(this, "Foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RbxLogger.info(this, "Service command: ${intent?.action ?: "RECREATE"}")
        when (intent?.action) {
            ACTION_START -> startMacro()
            ACTION_STOP -> stopMacro()
            else -> if (MacroPrefs.isRunning(this)) startMacro()
        }
        return START_STICKY
    }

    private fun startMacro() {
        MacroPrefs.setRunning(this, true)
        acquireWakeLockIfNeeded()
        val interval = MacroPrefs.intervalMs(this)
        RbxAccessibilityService.instance?.startMacro(interval)
        updateNotification("매크로 실행 중 · ${interval / 1000}s · 서비스 유지")
        RbxLogger.info(this, "Macro started; intervalMs=$interval")
    }

    private fun stopMacro() {
        MacroPrefs.setRunning(this, false)
        RbxAccessibilityService.instance?.stopMacro()
        releaseWakeLock()
        RbxLogger.info(this, "Macro stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLockIfNeeded() {
        if (!AutoFarmPrefs.keepAwake(this)) return
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:macro").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (MacroPrefs.isRunning(this)) {
            RbxLogger.info(this, "App task removed; foreground service kept alive")
            startForeground(NOTIFICATION_ID, notification("매크로 실행 중 · 서비스 유지"))
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        RbxLogger.info(this, "Foreground service destroyed")
        if (!MacroPrefs.isRunning(this)) RbxAccessibilityService.instance?.stopMacro()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rbx.m 매크로", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 10,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingIntentFlags()
        )
        val stopIntent = PendingIntent.getService(
            this, 11,
            Intent(this, MacroForegroundService::class.java).setAction(ACTION_STOP),
            pendingIntentFlags()
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Rbx.m")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "정지", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text))
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val ACTION_START = "com.kwb130212.rbxm.action.START"
        const val ACTION_STOP = "com.kwb130212.rbxm.action.STOP"
        private const val CHANNEL_ID = "rbxm_macro"
        private const val NOTIFICATION_ID = 1001
    }
}

object MacroPrefs {
    private const val PREFS = "macro"
    private const val INTERVAL = "interval_ms"
    private const val RUNNING = "running"

    fun intervalMs(context: android.content.Context, value: Long? = null): Long {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        if (value != null) prefs.edit().putLong(INTERVAL, value).apply()
        return prefs.getLong(INTERVAL, 10_000L).coerceIn(1_000L, 40_000L)
    }

    fun setRunning(context: android.content.Context, running: Boolean) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(RUNNING, running).apply()
    }

    fun isRunning(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getBoolean(RUNNING, false)
}
