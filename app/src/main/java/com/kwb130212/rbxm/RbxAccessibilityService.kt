package com.kwb130212.rbxm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.Display

class RbxAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        isConnected = false
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun startMacro(intervalMs: Long) {
        running = true
        handler.removeCallbacksAndMessages(null)
        val loop = object : Runnable {
            override fun run() {
                if (!running) return
                tapCenter()
                handler.postDelayed(this, intervalMs.coerceAtLeast(250L))
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
        @Volatile var isConnected: Boolean = false
            private set
        @Volatile var instance: RbxAccessibilityService? = null
    }

    init {
        instance = this
    }
}
