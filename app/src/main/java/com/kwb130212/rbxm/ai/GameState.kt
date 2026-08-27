package com.kwb130212.rbxm.ai

import android.graphics.PointF

data class TrackedObject(
    val center: PointF,
    val confidence: Float,
    val kind: Kind
) {
    enum class Kind { PLAYER, ENEMY, PROJECTILE, UNKNOWN }
}

data class GameState(
    val width: Int,
    val height: Int,
    val player: TrackedObject?,
    val enemies: List<TrackedObject>,
    val projectiles: List<TrackedObject>,
    val timestampMs: Long
) {
    val danger: Float
        get() = projectiles.minOfOrNull { p ->
            player?.let { distance(p.center, it.center) } ?: Float.MAX_VALUE
        }?.let { d -> (1f - d / (width.coerceAtLeast(height) * 0.45f)).coerceIn(0f, 1f) } ?: 0f

    private fun distance(a: PointF, b: PointF): Float =
        kotlin.math.hypot(a.x - b.x, a.y - b.y)
}

data class AiAction(
    val type: Type,
    val x: Float = 0f,
    val y: Float = 0f,
    val durationMs: Long = 120L,
    val score: Float = 0f
) {
    enum class Type { IDLE, MOVE, ATTACK, DODGE }
}
