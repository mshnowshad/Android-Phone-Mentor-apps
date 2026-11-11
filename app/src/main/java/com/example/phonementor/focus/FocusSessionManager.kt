package com.example.phonementor.focus

import android.content.Context
import android.content.SharedPreferences

/**
 * FocusSessionManager
 *
 * Focus mode active kina, koto khon por sesh, kon app allowed — sob track kore.
 * AccessibilityService ar Activity duijon e ei class use kore.
 */
object FocusSessionManager {

    private const val PREFS = "focus_session_prefs"
    private const val KEY_ACTIVE = "active"
    private const val KEY_END_TIME = "end_time"
    private const val KEY_DURATION = "duration_minutes"
    private const val KEY_ALLOWED = "allowed_packages"

    fun start(context: Context, durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + durationMinutes * 60_000L
        val allowed = AllowedAppsStore.load(context).map { it.packageName }.toSet()
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_END_TIME, endTime)
            .putInt(KEY_DURATION, durationMinutes)
            .putStringSet(KEY_ALLOWED, allowed)
            .apply()
    }

    fun stop(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVE, false)

    fun endTime(context: Context): Long =
        prefs(context).getLong(KEY_END_TIME, 0L)

    fun durationMinutes(context: Context): Int =
        prefs(context).getInt(KEY_DURATION, 25)

    fun allowedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ALLOWED, emptySet()) ?: emptySet()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}