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

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            foregroundPackage = event.packageName?.toString()
        }
    }

    override fun onInterrupt() = Unit

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
                if (foregroundPackage == ROBLOX_PACKAGE) tapCenter()
                handler.postDelayed(this, intervalMs.coerceIn(1_000L, 40_000L))
            }
        }
        handler.post(loop)
    }

    fun stopMacro() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun tapCenter() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val y = metrics.heightPixels / 2f
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 40L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    companion object {
        const val ROBLOX_PACKAGE = "com.roblox.client"
        @Volatile var isConnected: Boolean = false
            private set
        @Volatile var instance: RbxAccessibilityService? = null
            private set
    }
}
