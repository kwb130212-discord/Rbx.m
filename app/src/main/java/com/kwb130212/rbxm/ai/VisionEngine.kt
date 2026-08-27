package com.kwb130212.rbxm.ai

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.abs

/** Lightweight on-device visual heuristic. Replace thresholds with a trained model later. */
class VisionEngine {
    fun analyze(bitmap: Bitmap): GameState {
        val w = bitmap.width
        val h = bitmap.height
        val player = findPlayer(bitmap)
        val enemies = findEnemyCandidates(bitmap, player)
        val projectiles = findProjectileCandidates(bitmap, player)
        return GameState(w, h, player, enemies, projectiles, System.currentTimeMillis())
    }

    private fun findPlayer(bitmap: Bitmap): TrackedObject? {
        // Conservative center-biased scan. It intentionally avoids OCR or network inspection.
        val cx = bitmap.width / 2f
        val cy = bitmap.height * 0.55f
        val p = nearestColorCluster(bitmap, cx, cy, 24, 0.35f)
        return p?.let { TrackedObject(it, 0.35f, TrackedObject.Kind.PLAYER) }
    }

    private fun findEnemyCandidates(bitmap: Bitmap, player: TrackedObject?): List<TrackedObject> {
        if (player == null) return emptyList()
        val result = ArrayList<TrackedObject>()
        val step = 12
        for (y in (bitmap.height * 0.18f).toInt() until (bitmap.height * 0.82f).toInt() step step) {
            for (x in (bitmap.width * 0.08f).toInt() until (bitmap.width * 0.92f).toInt() step step) {
                val c = bitmap.getPixel(x, y)
                val r = (c shr 16) and 255
                val g = (c shr 8) and 255
                val b = c and 255
                if (r > 150 && r > g * 1.35f && r > b * 1.25f) {
                    if (distance(x.toFloat(), y.toFloat(), player.center.x, player.center.y) > bitmap.width * 0.10f) {
                        result += TrackedObject(PointF(x.toFloat(), y.toFloat()), 0.25f, TrackedObject.Kind.ENEMY)
                    }
                }
            }
        }
        return dedupe(result, 40f).take(8)
    }

    private fun findProjectileCandidates(bitmap: Bitmap, player: TrackedObject?): List<TrackedObject> {
        if (player == null) return emptyList()
        val result = ArrayList<TrackedObject>()
        val step = 8
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val c = bitmap.getPixel(x, y)
                val r = (c shr 16) and 255
                val g = (c shr 8) and 255
                val b = c and 255
                val bright = r + g + b > 690
                val smallAccent = abs(r - g) > 45 || abs(g - b) > 45
                if (bright && smallAccent && distance(x.toFloat(), y.toFloat(), player.center.x, player.center.y) < bitmap.width * 0.30f) {
                    result += TrackedObject(PointF(x.toFloat(), y.toFloat()), 0.18f, TrackedObject.Kind.PROJECTILE)
                }
            }
        }
        return dedupe(result, 24f).take(12)
    }

    private fun nearestColorCluster(bitmap: Bitmap, tx: Float, ty: Float, radius: Int, threshold: Float): PointF? {
        var best: PointF? = null
        var bestScore = Float.MAX_VALUE
        for (y in (ty - radius).toInt().coerceAtLeast(0)..(ty + radius).toInt().coerceAtMost(bitmap.height - 1) step 4) {
            for (x in (tx - radius).toInt().coerceAtLeast(0)..(tx + radius).toInt().coerceAtMost(bitmap.width - 1) step 4) {
                val d = distance(x.toFloat(), y.toFloat(), tx, ty)
                if (d < bestScore) { bestScore = d; best = PointF(x.toFloat(), y.toFloat()) }
            }
        }
        return if (best != null && threshold > 0f) best else null
    }

    private fun dedupe(items: List<TrackedObject>, minDistance: Float): List<TrackedObject> {
        val out = ArrayList<TrackedObject>()
        for (item in items) {
            if (out.none { distance(it.center.x, it.center.y, item.center.x, item.center.y) < minDistance }) out += item
        }
        return out
    }

    private fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float =
        kotlin.math.hypot(ax - bx, ay - by)
}
