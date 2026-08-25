package com.kwb130212.rbxm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToLong
import kotlin.random.Random

class RbxAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    @Volatile private var foregroundPackage: String? = null
    private var intervalMs = 10_000L
    private var randomized = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
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

    fun startMacro(interval: Long, randomize: Boolean) {
        running = true
        intervalMs = interval.coerceIn(1_000L, 40_000L)
        randomized = randomize
        handler.removeCallbacksAndMessages(null)
        val loop = object : Runnable {
            override fun run() {
                if (!running) return
                if (foregroundPackage == ROBLOX_PACKAGE) tapCenter()
                handler.postDelayed(this, nextDelay())
            }
        }
        handler.post(loop)
    }

    fun stopMacro() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    fun testCenterTap() {
        if (foregroundPackage == ROBLOX_PACKAGE) tapCenter()
    }

    private fun nextDelay(): Long {
        if (!randomized) return intervalMs
        val min = (intervalMs * 0.75).roundToLong().coerceAtLeast(1_000L)
        val max = (intervalMs * 1.25).roundToLong().coerceAtMost(40_000L)
        return Random.nextLong(min, max + 1)
    }

    private fun tapCenter() {
        val metrics = resources.displayMetrics
        val path = Path().apply { moveTo(metrics.widthPixels / 2f, metrics.heightPixels / 2f) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 40L))
            .build()
        if (dispatchGesture(gesture, null, null)) {
            MacroPrefs.addTap(this)
        }
    }

    companion object {
        const val ROBLOX_PACKAGE = "com.roblox.client"
        @Volatile var isConnected: Boolean = false
            private set
        @Volatile var instance: RbxAccessibilityService? = null
            private set
    }
}
