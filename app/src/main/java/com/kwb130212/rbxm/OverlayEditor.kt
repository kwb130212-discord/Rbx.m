package com.kwb130212.rbxm

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** Floating, draggable action markers. X closes the editor without changing saved positions. */
object OverlayEditor {
    private var wm: WindowManager? = null
    private val views = mutableListOf<View>()
    private var toolbar: View? = null

    fun show(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "먼저 '다른 앱 위에 표시' 권한을 허용하세요.", Toast.LENGTH_LONG).show()
            return
        }
        hide()
        val app = context.applicationContext
        wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        OverlayPositionStore.Action.values().forEach { action -> addMarker(app, action) }
        addToolbar(app)
    }

    fun hide() {
        val manager = wm ?: return
        views.forEach { runCatching { manager.removeView(it) } }
        toolbar?.let { runCatching { manager.removeView(it) } }
        views.clear()
        toolbar = null
        wm = null
    }

    private fun addMarker(context: Context, action: OverlayPositionStore.Action) {
        val marker = TextView(context).apply {
            text = action.label
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xCC20252B.toInt())
                cornerRadius = 40f
                setStroke(2, Color.WHITE)
            }
            elevation = 12f
        }
        val size = dp(context, 70)
        val params = WindowManager.LayoutParams(
            size, dp(context, 44),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val display = context.resources.displayMetrics
            x = (OverlayPositionStore.get(context, action).x * display.widthPixels).roundToInt() - size / 2
            y = (OverlayPositionStore.get(context, action).y * display.heightPixels).roundToInt() - dp(context, 22)
        }
        marker.setOnTouchListener(DragListener(context, action, marker, params))
        wm?.addView(marker, params)
        views += marker
    }

    private fun addToolbar(context: Context) {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(context, 6), dp(context, 4), dp(context, 6), dp(context, 4))
            background = GradientDrawable().apply {
                setColor(0xEE17191C.toInt())
                cornerRadius = dp(context, 14).toFloat()
            }
        }
        val close = TextView(context).apply {
            text = "✕"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(context, 14), dp(context, 5), dp(context, 14), dp(context, 5))
            contentDescription = "위치 설정 닫기"
            setOnClickListener { hide() }
        }
        val done = TextView(context).apply {
            text = "완료"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7))
            setOnClickListener { hide() }
        }
        val reset = TextView(context).apply {
            text = "초기화"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7))
            setOnClickListener {
                OverlayPositionStore.reset(context)
                show(context)
            }
        }
        bar.addView(close)
        bar.addView(done)
        bar.addView(reset)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(context, 10)
            y = dp(context, 28)
        }
        wm?.addView(bar, params)
        toolbar = bar
    }

    private class DragListener(
        private val context: Context,
        private val action: OverlayPositionStore.Action,
        private val view: View,
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).roundToInt()
                    params.y = startY + (event.rawY - downY).roundToInt()
                    wm?.updateViewLayout(view, params)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dm = context.resources.displayMetrics
                    val cx = ((params.x + view.width / 2f) / dm.widthPixels).coerceIn(0f, 1f)
                    val cy = ((params.y + view.height / 2f) / dm.heightPixels).coerceIn(0f, 1f)
                    OverlayPositionStore.set(context, action, cx, cy)
                    return true
                }
            }
            return false
        }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}
