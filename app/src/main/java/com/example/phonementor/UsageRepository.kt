package com.example.phonementor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.util.Calendar
import android.content.pm.PackageManager
import android.util.Log


data class AppUsage(val packageName: String, val label: String, val millis: Long)

fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Suppress("DEPRECATION")
fun todayUsage(context: Context): List<AppUsage> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager

    val dayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = System.currentTimeMillis()

    val events = usm.queryEvents(dayStart - 12 * 60 * 60 * 1000L, end)
    val event = UsageEvents.Event()

    val totals = mutableMapOf<String, Long>()
    var currentPkg: String? = null
    var currentSince = 0L

    fun addTime(pkg: String, from: Long, to: Long) {
        val a = maxOf(from, dayStart)
        val b = minOf(to, end)
        if (b > a) totals[pkg] = (totals[pkg] ?: 0L) + (b - a)
    }

    fun closeCurrent(at: Long) {
        currentPkg?.let { addTime(it, currentSince, at) }
        currentPkg = null
    }

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val pkg = event.packageName ?: continue
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                if (currentPkg != pkg) {
                    closeCurrent(event.timeStamp)
                    currentPkg = pkg
                    currentSince = event.timeStamp
                }
            }
            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                if (currentPkg == pkg) closeCurrent(event.timeStamp)
            }
            16, 17 -> closeCurrent(event.timeStamp) // screen off, keyguard shown
        }
    }


    // Ekhono khola thaka app
    closeCurrent(end)

    val homePkgs = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_ALL
    ).map { it.activityInfo.packageName }.toSet()

    val result = totals
        .filter { (pkg, ms) ->
            ms > 0 &&
                    pkg !in homePkgs &&
                    (pkg == context.packageName || pm.getLaunchIntentForPackage(pkg) != null)
        }
        .map { (pkg, ms) ->
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg
            }
            AppUsage(pkg, label, ms)
        }
        .sortedByDescending { it.millis }

    Log.d("UsageDebug", "TOTAL = ${formatDuration(result.sumOf { it.millis })}")
    result.take(8).forEach { Log.d("UsageDebug", "${it.label}: ${formatDuration(it.millis)}") }

    return result
}