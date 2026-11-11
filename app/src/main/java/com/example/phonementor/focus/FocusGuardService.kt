package com.example.phonementor.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * FocusGuardService
 *
 * Focus mode active thakle, onno kono app foreground e ashle sathe sathe
 * FocusModeActivity ke abar front e niye ashe. Eta e "phone lock" er main guard.
 */
class FocusGuardService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!FocusSessionManager.isActive(this)) return

        val pkg = event.packageName?.toString() ?: return

        // Nijer app → allow
        if (pkg == packageName) return

        // User-selected allowed apps → allow
        if (pkg in FocusSessionManager.allowedPackages(this)) return

        // SystemUI (lock screen, status bar) ar keyboard → allow
        // (na hole screen lock/unlock korte parbe na)
        if (!isUserApp(pkg)) return

        // Eta blocked app → Focus screen e ferot pathao
        bringFocusToFront()
    }

    /**
     * Launcher icon ache emon app = user app (Settings, YouTube, Chrome, Home launcher etc).
     * Keyboard, SystemUI er launcher icon nei → allow.
     */
    private fun isUserApp(pkg: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(pkg)
            packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun bringFocusToFront() {
        try {
            val intent = Intent(this, FocusModeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("EXTRA_DURATION_MINUTES", FocusSessionManager.durationMinutes(this@FocusGuardService))
                putExtra("EXTRA_END_TIME", FocusSessionManager.endTime(this@FocusGuardService))
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Fallback — jodi activity start na hoy, back press koro
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}