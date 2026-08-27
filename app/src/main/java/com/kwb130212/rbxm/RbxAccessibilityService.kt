package com.kwb130212.rbxm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.kwb130212.rbxm.ai.AiAction
import com.kwb130212.rbxm.ai.AiBrain
import com.kwb130212.rbxm.ai.GameState
import com.kwb130212.rbxm.ai.OnlineLearner
import com.kwb130212.rbxm.ai.VisionEngine

class RbxAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val vision = VisionEngine()
    private val learner = OnlineLearner()
    private val brain = AiBrain(learner)
    private var running = false
    @Volatile private var foregroundPackage: String? = null
    private var lastState: GameState? = null
    private var lastAction: AiAction? = null

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

    fun startMacro(intervalMs: Long) {
        running = true
        handler.removeCallbacksAndMessages(null)
        scheduleCapture(intervalMs.coerceIn(350L, 2_000L))
    }

    fun stopMacro() {
        running = false
        handler.removeCallbacksAndMessages(null)
        lastAction = null
    }

    private fun scheduleCapture(intervalMs: Long) {
        if (!running) return
        if (android.os.Build.VERSION.SDK_INT >= 30 && foregroundPackage == ROBLOX_PACKAGE) {
            takeScreenshot(DISPLAY_ID, android.os.HandlerExecutor(handler), object : TakeScreenshotCallback() {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    screenshot.asBitmap()?.let { bitmap ->
                        val state = vision.analyze(bitmap)
                        lastState = state
                        val action = brain.decide(state)
                        execute(action, state)
                        bitmap.recycle()
                    }
                    handler.postDelayed({ scheduleCapture(intervalMs) }, intervalMs)
                }
                override fun onFailure(errorCode: Int) {
                    handler.postDelayed({ scheduleCapture(intervalMs) }, intervalMs)
                }
            })
        } else {
            handler.postDelayed({ scheduleCapture(intervalMs) }, intervalMs)
        }
    }

    private fun execute(action: AiAction, state: GameState) {
        val player = state.player ?: return
        when (action.type) {
            AiAction.Type.MOVE, AiAction.Type.DODGE -> swipe(player.center.x, player.center.y, action.x, action.y, action.durationMs)
            AiAction.Type.ATTACK -> tap(action.x, action.y)
            AiAction.Type.IDLE -> Unit
        }
        lastAction = action
        RbxLogger.info(this, "AI action=${action.type} confidence=${action.score} danger=${state.danger}")
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 55L)).build(), null, null)
    }

    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, duration: Long) {
        val path = Path().apply { moveTo(fromX, fromY); lineTo(toX, toY) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, duration.coerceIn(80L, 500L))).build(), null, null)
    }

    companion object {
        const val ROBLOX_PACKAGE = "com.supercell.brawlstars"
        private const val DISPLAY_ID = 0
        @Volatile var isConnected: Boolean = false
            private set
        @Volatile var instance: RbxAccessibilityService? = null
            private set
    }
}
