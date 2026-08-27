package com.kwb130212.rbxm.ai

import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class AiBrain(private val learner: OnlineLearner) {
    fun decide(state: GameState): AiAction {
        val player = state.player ?: return AiAction(AiAction.Type.IDLE)
        val nearestEnemy = state.enemies.minByOrNull { distance(it.center, player.center) }

        if (state.danger >= 0.72f) {
            val threat = state.projectiles.minByOrNull { distance(it.center, player.center) }
            val angle = threat?.let { atan2(player.center.y - it.center.y, player.center.x - it.center.x) } ?: 0f
            val side = if (learner.preferLeftDodge) -1f else 1f
            val x = (player.center.x + cos(angle + side * 1.57f) * state.width * 0.16f).coerceIn(20f, state.width - 20f)
            val y = (player.center.y + sin(angle + side * 1.57f) * state.height * 0.16f).coerceIn(20f, state.height - 20f)
            return AiAction(AiAction.Type.DODGE, x, y, 180L, learner.score("dodge"))
        }

        if (nearestEnemy != null) {
            val d = distance(nearestEnemy.center, player.center)
            if (d < state.width * 0.34f) {
                return AiAction(AiAction.Type.ATTACK, nearestEnemy.center.x, nearestEnemy.center.y, 80L, learner.score("attack"))
            }
            val dx = nearestEnemy.center.x - player.center.x
            val dy = nearestEnemy.center.y - player.center.y
            val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
            val x = (player.center.x + dx / len * state.width * 0.10f).coerceIn(20f, state.width - 20f)
            val y = (player.center.y + dy / len * state.height * 0.10f).coerceIn(20f, state.height - 20f)
            return AiAction(AiAction.Type.MOVE, x, y, 220L, learner.score("move"))
        }

        return AiAction(AiAction.Type.MOVE, player.center.x + 40f, player.center.y, 250L, learner.score("explore"))
    }

    private fun distance(a: PointF, b: PointF): Float = kotlin.math.hypot(a.x - b.x, a.y - b.y)
}

class OnlineLearner {
    private val values = mutableMapOf("attack" to 0.5f, "dodge" to 0.5f, "move" to 0.4f, "explore" to 0.2f)
    var preferLeftDodge: Boolean = true
        private set

    fun score(action: String): Float = values[action] ?: 0f

    @Synchronized fun reward(action: String, reward: Float) {
        val old = values[action] ?: 0f
        values[action] = (old + 0.08f * (reward - old)).coerceIn(-10f, 10f)
        if (action == "dodge" && reward > 0f) preferLeftDodge = !preferLeftDodge
    }

    fun snapshot(): Map<String, Float> = values.toMap()
}
