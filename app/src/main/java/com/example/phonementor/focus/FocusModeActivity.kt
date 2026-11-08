package com.example.phonementor.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.phonementor.ui.theme.PhoneMentorTheme

/**
 * FocusModeActivity
 *
 * - FocusSetupActivity থেকে duration নিয়ে আসে।
 * - FocusScreen-এ কাউন্টডাউন দেখায়।
 * - Exit চাপলে কনফার্মেশন dialog দেখায়।
 * - Confirm করলে finish() → ফিরে যাবে FocusSetupActivity-তে।
 */
class FocusModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup থেকে পাঠানো ডিউরেশন (ডিফল্ট ২৫ মিনিট)
        val durationMinutes = intent.getIntExtra("EXTRA_DURATION_MINUTES", 25)

        setContent {
            PhoneMentorTheme {
                FocusScreen(
                    durationMinutes = durationMinutes,
                    onExit = { finish() }   // Exit confirm হলে activity বন্ধ
                )
            }
        }
    }
}