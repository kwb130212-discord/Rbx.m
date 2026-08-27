package com.kwb130212.rbxm

import android.content.Context

/** Stores screen-relative coordinates so profiles survive resolution changes. */
object OverlayPositionStore {
    private const val PREFS = "overlay_positions"
    private const val PROFILE = "profile"

    data class Point(val x: Float, val y: Float)

    enum class Action(val key: String, val label: String) {
        ATTACK("attack", "⚔ 공격"),
        SUPER("super", "★ 특수"),
        MOVE("move", "✚ 이동")
    }

    fun get(context: Context, action: Action): Point {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Point(
            p.getFloat("${PROFILE}_${action.key}_x", defaultX(action)),
            p.getFloat("${PROFILE}_${action.key}_y", defaultY(action))
        )
    }

    fun set(context: Context, action: Action, x: Float, y: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("${PROFILE}_${action.key}_x", x.coerceIn(0f, 1f))
            .putFloat("${PROFILE}_${action.key}_y", y.coerceIn(0f, 1f))
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun defaultX(action: Action): Float = when (action) {
        Action.ATTACK, Action.SUPER -> 0.84f
        Action.MOVE -> 0.18f
    }

    private fun defaultY(action: Action): Float = when (action) {
        Action.ATTACK -> 0.78f
        Action.SUPER -> 0.62f
        Action.MOVE -> 0.78f
    }
}
