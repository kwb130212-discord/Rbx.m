package com.kwb130212.rbxm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToInt

class RbxAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    @Volatile private var foregroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) foregroundPackage = event.packageName?.toString()
    }

    override fun onInterrupt() = stopMacro()

    override fun onDestroy() {
        stopMacro()
        if (instance === this) instance = null
        isConnected = false
        foregroundPackage = null
        super.onDestroy()
    }

    fun startMacro(intervalMs: Long) {
        running = true
        handler.removeCallbacksAndMessages(null)
        val loop = object : Runnable {
            override fun run() {
                if (!running) return
                // Safety gate: never dispatch game input unless the configured target is foreground.
                if (foregroundPackage == ROBLOX_PACKAGE) {
                    tap(OverlayPositionStore.Action.ATTACK)
                }
                handler.postDelayed(this, intervalMs.coerceIn(1_000L, 40_000L))
            }
        }
        handler.post(loop)
    }

    fun stopMacro() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    fun tap(action: OverlayPositionStore.Action): Boolean {
        if (foregroundPackage != ROBLOX_PACKAGE) return false
        val point = OverlayPositionStore.get(this, action)
        val dm = resources.displayMetrics
        val x = (point.x * dm.widthPixels).coerceIn(0f, dm.widthPixels - 1f)
        val y = (point.y * dm.heightPixels).coerceIn(0f, dm.heightPixels - 1f)
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 45L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipe(action: OverlayPositionStore.Action, dx: Float, dy: Float, durationMs: Long = 160L): Boolean {
        if (foregroundPackage != ROBLOX_PACKAGE) return false
        val point = OverlayPositionStore.get(this, action)
        val dm = resources.displayMetrics
        val sx = (point.x * dm.widthPixels).coerceIn(0f, dm.widthPixels - 1f)
        val sy = (point.y * dm.heightPixels).coerceIn(0f, dm.heightPixels - 1f)
        val ex = (sx + dx).coerceIn(0f, dm.widthPixels - 1f)
        val ey = (sy + dy).coerceIn(0f, dm.heightPixels - 1f)
        val path = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(40L, 800L)))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        const val ROBLOX_PACKAGE = "com.roblox.client"
        @Volatile var isConnected: Boolean = false
            private set
        @Volatile var instance: RbxAccessibilityService? = null
            private set
    }
}
