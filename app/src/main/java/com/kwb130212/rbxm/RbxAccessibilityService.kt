package com.kwb130212.rbxm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class RbxAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    @Volatile private var foregroundPackage: String? = null

    override fun onServiceConnected() { super.onServiceConnected(); isConnected = true; instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) foregroundPackage = event.packageName?.toString()
    }
    override fun onInterrupt() = stopMacro()
    override fun onDestroy() { stopMacro(); if (instance === this) instance = null; isConnected = false; foregroundPackage = null; super.onDestroy() }

    fun startMacro(intervalMs: Long) {
        running = true
        handler.removeCallbacksAndMessages(null)
        val loop = object : Runnable {
            override fun run() {
                if (!running) return
                if (foregroundPackage == TARGET_PACKAGE) {
                    if (AutoFarmPrefs.enabled(this@RbxAccessibilityService)) autoFarmTick()
                    else tap(OverlayPositionStore.Action.ATTACK)
                }
                val next = if (AutoFarmPrefs.enabled(this@RbxAccessibilityService)) AutoFarmPrefs.intervalMs(this@RbxAccessibilityService) else intervalMs.coerceIn(1_000L, 40_000L)
                handler.postDelayed(this, next)
            }
        }
        handler.post(loop)
    }

    private fun autoFarmTick() {
        when (AutoFarmPrefs.mode(this)) {
            AutoFarmPrefs.Mode.SAFE -> tap(OverlayPositionStore.Action.MOVE)
            AutoFarmPrefs.Mode.BALANCED -> tap(OverlayPositionStore.Action.ATTACK)
            AutoFarmPrefs.Mode.AGGRESSIVE -> { tap(OverlayPositionStore.Action.ATTACK); handler.postDelayed({ if (running && foregroundPackage == TARGET_PACKAGE) tap(OverlayPositionStore.Action.SUPER) }, 80L) }
        }
    }

    fun stopMacro() { running = false; handler.removeCallbacksAndMessages(null) }

    fun tap(action: OverlayPositionStore.Action): Boolean {
        if (foregroundPackage != TARGET_PACKAGE) return false
        val point = OverlayPositionStore.get(this, action)
        val dm = resources.displayMetrics
        val x = (point.x * dm.widthPixels).coerceIn(0f, dm.widthPixels - 1f)
        val y = (point.y * dm.heightPixels).coerceIn(0f, dm.heightPixels - 1f)
        val path = Path().apply { moveTo(x, y) }
        return dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 45L)).build(), null, null)
    }

    fun swipe(action: OverlayPositionStore.Action, dx: Float, dy: Float, durationMs: Long = 160L): Boolean {
        if (foregroundPackage != TARGET_PACKAGE) return false
        val point = OverlayPositionStore.get(this, action)
        val dm = resources.displayMetrics
        val sx = (point.x * dm.widthPixels).coerceIn(0f, dm.widthPixels - 1f)
        val sy = (point.y * dm.heightPixels).coerceIn(0f, dm.heightPixels - 1f)
        val ex = (sx + dx).coerceIn(0f, dm.widthPixels - 1f)
        val ey = (sy + dy).coerceIn(0f, dm.heightPixels - 1f)
        val path = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
        return dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(40L, 800L))).build(), null, null)
    }

    companion object {
        const val TARGET_PACKAGE = "com.supercell.brawlstars"
        @Volatile var isConnected: Boolean = false; private set
        @Volatile var instance: RbxAccessibilityService? = null; private set
    }
}
