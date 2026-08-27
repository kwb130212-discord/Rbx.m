package com.kwb130212.rbxm.ai

import android.content.Context
import org.json.JSONObject

class LearningStore(context: Context) {
    private val prefs = context.getSharedPreferences("ai_model", Context.MODE_PRIVATE)

    fun load(learner: OnlineLearner) {
        val json = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        for (key in ACTIONS) json.optDouble(key, Double.NaN).takeUnless { it.isNaN() }?.let { learner.setValue(key, it.toFloat()) }
        learner.preferLeftDodge = json.optBoolean("left", true)
    }

    fun save(learner: OnlineLearner) {
        val json = JSONObject()
        learner.snapshot().forEach { (key, value) -> json.put(key, value) }
        json.put("left", learner.preferLeftDodge)
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    companion object {
        private const val KEY = "policy_v1"
        private val ACTIONS = listOf("attack", "dodge", "move", "explore")
    }
}
