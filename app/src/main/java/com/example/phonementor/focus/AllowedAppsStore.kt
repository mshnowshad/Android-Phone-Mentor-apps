package com.example.phonementor.focus

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * AllowedAppsStore
 *
 * SharedPreferences e allowed apps list save/load kore.
 * Max 5 ta app.
 *
 * Storage format (JSON string):
 * [
 *   { "packageName": "...", "label": "...", "iconBase64": "..." },
 *   ...
 * ]
 */
object AllowedAppsStore {

    private const val PREFS_NAME = "phone_mentor_prefs"
    private const val KEY_ALLOWED_APPS = "allowed_apps_json"
    const val MAX_ALLOWED_APPS = 4

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save full list of allowed apps (overwrites previous).
     */
    fun save(context: Context, apps: List<AllowedApp>) {
        val arr = JSONArray()
        apps.forEach { app ->
            val obj = JSONObject()
            obj.put("packageName", app.packageName)
            obj.put("label", app.label)
            obj.put("iconBase64", app.iconBase64)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_ALLOWED_APPS, arr.toString()).apply()
    }

    /**
     * Load full list of allowed apps.
     */
    fun load(context: Context): List<AllowedApp> {
        val json = prefs(context).getString(KEY_ALLOWED_APPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<AllowedApp>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    AllowedApp(
                        packageName = obj.getString("packageName"),
                        label = obj.getString("label"),
                        iconBase64 = obj.getString("iconBase64")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ALLOWED_APPS).apply()
    }

    /**
     * Drawable → Base64 encoded PNG string (icon save korar jonno).
     */
    fun drawableToBase64(drawable: Drawable): String {
        val bitmap: Bitmap = drawable.toBitmap(
            width = 96,
            height = 96,
            config = Bitmap.Config.ARGB_8888
        )
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Base64 string → Drawable (icon render korar jonno).
     */
    fun base64ToDrawable(base64: String): Drawable? {
        if (base64.isEmpty()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) BitmapDrawable(null, bitmap) else null
        } catch (_: Exception) {
            null
        }
    }
}