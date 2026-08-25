package com.kwb130212.rbxm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MacroForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("서비스 대기 중"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMacro()
            ACTION_STOP -> stopMacro()
            ACTION_TEST_TAP -> RbxAccessibilityService.instance?.testCenterTap()
            else -> if (MacroPrefs.isRunning(this)) startMacro()
        }
        return START_STICKY
    }

    private fun startMacro() {
        MacroPrefs.setRunning(this, true)
        MacroPrefs.resetSession(this)
        RbxAccessibilityService.instance?.startMacro(
            MacroPrefs.intervalMs(this), MacroPrefs.randomInterval(this)
        )
        updateNotification("매크로 실행 중 · ${MacroPrefs.intervalMs(this) / 1000}s")
    }

    private fun stopMacro() {
        MacroPrefs.setRunning(this, false)
        RbxAccessibilityService.instance?.stopMacro()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (MacroPrefs.isRunning(this)) startForeground(NOTIFICATION_ID, notification("매크로 실행 중 · 서비스 유지"))
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (!MacroPrefs.isRunning(this)) RbxAccessibilityService.instance?.stopMacro()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rbx.m 매크로", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(this, 10,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), pendingIntentFlags())
        val stopIntent = PendingIntent.getService(this, 11,
            Intent(this, MacroForegroundService::class.java).setAction(ACTION_STOP), pendingIntentFlags())
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Rbx.m")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "정지", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) = getSystemService(NotificationManager::class.java)
        .notify(NOTIFICATION_ID, notification(text))

    private fun pendingIntentFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val ACTION_START = "com.kwb130212.rbxm.action.START"
        const val ACTION_STOP = "com.kwb130212.rbxm.action.STOP"
        const val ACTION_TEST_TAP = "com.kwb130212.rbxm.action.TEST_TAP"
        private const val CHANNEL_ID = "rbxm_macro"
        private const val NOTIFICATION_ID = 1001
    }
}

object MacroPrefs {
    private const val PREFS = "macro"
    private const val INTERVAL = "interval_ms"
    private const val RANDOM = "random_interval"
    private const val RUNNING = "running"
    private const val SESSION_TAPS = "session_taps"
    private const val TOTAL_TAPS = "total_taps"

    fun intervalMs(context: android.content.Context, value: Long? = null): Long {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        if (value != null) p.edit().putLong(INTERVAL, value).apply()
        return p.getLong(INTERVAL, 10_000L).coerceIn(1_000L, 40_000L)
    }

    fun randomInterval(context: android.content.Context, value: Boolean? = null): Boolean {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        if (value != null) p.edit().putBoolean(RANDOM, value).apply()
        return p.getBoolean(RANDOM, false)
    }

    fun setRunning(context: android.content.Context, running: Boolean) =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().putBoolean(RUNNING, running).apply()

    fun isRunning(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getBoolean(RUNNING, false)

    fun resetSession(context: android.content.Context) =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().putInt(SESSION_TAPS, 0).apply()

    fun addTap(context: android.content.Context) {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        p.edit().putInt(SESSION_TAPS, p.getInt(SESSION_TAPS, 0) + 1)
            .putInt(TOTAL_TAPS, p.getInt(TOTAL_TAPS, 0) + 1).apply()
    }

    fun sessionTaps(context: android.content.Context) =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getInt(SESSION_TAPS, 0)

    fun totalTaps(context: android.content.Context) =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getInt(TOTAL_TAPS, 0)
}
