package com.example.phonementor.focus

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.phonementor.ui.theme.PhoneMentorTheme

class FocusModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val durationMinutes = intent.getIntExtra("EXTRA_DURATION_MINUTES", 25)

        setContent {
            PhoneMentorTheme {
                FocusScreen(
                    durationMinutes = durationMinutes,
                    onExit = { finish() },
                    onFinish = { finish() }
                )
            }
        }

        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        FocusAccessibilityService.isFocusActive = true
    }

    override fun onStop() {
        FocusAccessibilityService.isFocusActive = false
        super.onStop()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = "$packageName/${FocusAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, true) }
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Focus Protection")
            .setMessage(
                "To block the notification and quick settings panels " +
                        "while you focus, enable the Phone Mentor accessibility service."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) { }
            }
            .setNegativeButton("Skip", null)
            .show()
    }
}