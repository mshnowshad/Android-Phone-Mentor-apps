package com.example.phonementor.focus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AppPickerRepository
 *
 * Phone e installed launcher apps list kore (YouTube, Camera, Contacts etc).
 * Filter kore:
 * - nijer app baad
 * - launcher activity na thakle baad
 */
object AppPickerRepository {

    /**
     * Installed apps er list (label alphabetical order e).
     * Result: List<Pair<packageName, label>>
     */
    suspend fun getInstalledApps(context: Context): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

            @Suppress("DEPRECATION")
            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )

            val seen = mutableSetOf<String>()
            val result = mutableListOf<Pair<String, String>>()

            resolveInfos.forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@forEach // nijer app baad
                if (!seen.add(pkg)) return@forEach // duplicate baad

                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) {
                    pkg
                }
                result.add(pkg to label)
            }

            result.sortedBy { it.second.lowercase() }
        }
}