package com.example.phonementor.focus

/**
 * AllowedApp
 *
 * Focus Mode e je app gulo allowed thakbe, tader represent kore.
 * - packageName: unique identifier (e.g. "com.whatsapp")
 * - label: user-facing name (e.g. "WhatsApp")
 * - iconBase64: app icon er base64 encoded PNG (SharedPreferences e save korar jonno)
 *
 * Kano base64? Karon SharedPreferences e direct Drawable save kora jay na.
 * Icon ta Base64 string hisebe save kore, pore decode kore Bitmap/Drawable banabo.
 */
data class AllowedApp(
    val packageName: String,
    val label: String,
    val iconBase64: String
)