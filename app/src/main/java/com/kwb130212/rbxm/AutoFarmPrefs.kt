package com.kwb130212.rbxm

import android.content.Context

/** Persistent auto-farm profile. The service uses this as configuration, not as a bypass. */
object AutoFarmPrefs {
    enum class Mode(val label: String) {
        SAFE("안전 우선"),
        BALANCED("균형"),
        AGGRESSIVE("공격 우선")
    }

    private const val PREFS = "auto_farm"
    private const val ENABLED = "enabled"
    private const val MODE = "mode"
    private const val KEEP_AWAKE = "keep_awake"
    private const val INTERVAL = "interval"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context, value: Boolean? = null): Boolean {
        value?.let { prefs(context).edit().putBoolean(ENABLED, it).apply() }
        return prefs(context).getBoolean(ENABLED, false)
    }

    fun mode(context: Context, value: Mode? = null): Mode {
        value?.let { prefs(context).edit().putString(MODE, it.name).apply() }
        return runCatching { Mode.valueOf(prefs(context).getString(MODE, Mode.BALANCED.name)!!) }.getOrDefault(Mode.BALANCED)
    }

    fun keepAwake(context: Context, value: Boolean? = null): Boolean {
        value?.let { prefs(context).edit().putBoolean(KEEP_AWAKE, it).apply() }
        return prefs(context).getBoolean(KEEP_AWAKE, true)
    }

    fun intervalMs(context: Context, value: Long? = null): Long {
        value?.let { prefs(context).edit().putLong(INTERVAL, it).apply() }
        return prefs(context).getLong(INTERVAL, 1500L).coerceIn(500L, 10_000L)
    }
}
