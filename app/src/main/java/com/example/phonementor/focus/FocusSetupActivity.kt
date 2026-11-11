package com.example.phonementor.focus

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.phonementor.ui.theme.PhoneMentorTheme

class FocusSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhoneMentorTheme {
                FocusSetupScreen(
                    onStartFocus = { durationMinutes -> ensureAccessibilityAndStart(durationMinutes) },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun ensureAccessibilityAndStart(durationMinutes: Int) {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(
                this,
                "Enable Phone Mentor in Accessibility settings to lock your phone.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        val intent = Intent(this, FocusModeActivity::class.java).apply {
            putExtra("EXTRA_DURATION_MINUTES", durationMinutes)
        }
        startActivity(intent)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = ComponentName(this, FocusGuardService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(service, ignoreCase = true) }
    }
}