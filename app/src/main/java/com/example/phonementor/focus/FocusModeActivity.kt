package com.example.phonementor.focus

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.phonementor.ui.theme.PhoneMentorTheme

class FocusModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val durationMinutes = intent.getIntExtra("EXTRA_DURATION_MINUTES", 25)

        // Session na thakle notun session start koro
        if (!FocusSessionManager.isActive(this)) {
            FocusSessionManager.start(this, durationMinutes)
        }

        val endTime = FocusSessionManager.endTime(this)

        // ---- Immersive mode: status bar + navigation bar hide ----
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // ---- Screen Pinning ----
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                startLockTask()
            } catch (_: Exception) { }
        }

        setContent {
            PhoneMentorTheme {
                FocusScreen(
                    durationMinutes = durationMinutes,
                    endTimeMillis = endTime,
                    onExit = { finishAndStop() },
                    onFinish = { finishAndStop() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Jodi user screen pinning theke ber hoye jay, abar pin koro
        if (FocusSessionManager.isActive(this) && !isInLockTaskMode()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    startLockTask()
                } catch (_: Exception) { }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Activity already alive → timer reset hobe na
    }

    private fun isInLockTaskMode(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    private fun finishAndStop() {
        FocusSessionManager.stop(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask()
            } catch (_: Exception) { }
        }
        finish()
    }

    override fun onDestroy() {
        if (isFinishing) {
            FocusSessionManager.stop(this)
        }
        super.onDestroy()
    }
}