package com.example.phonementor.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.phonementor.ui.theme.PhoneMentorTheme

/**
 * FocusSetupActivity
 *
 * কাজ:
 * - ইউজার এখানে ফোকাস মোডের ডিউরেশন সিলেক্ট করবে (২৫ মিনিট / ৪৫ মিনিট / ১ ঘণ্টা)
 * - "Start Focus" চাপলে FocusModeActivity লঞ্চ হবে, সাথে ডিউরেশন পাঠাবে
 *
 * Flow:
 * HomeScreen (Focus mode tile) → FocusSetupActivity → FocusModeActivity
 * FocusModeActivity exit হলে → ফিরে আসবে FocusSetupActivity-তে (finish না করলে)
 */
class FocusSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhoneMentorTheme {
                FocusSetupScreen(
                    onStartFocus = { durationMinutes ->
                        // নির্বাচিত ডিউরেশন নিয়ে FocusModeActivity চালু
                        val intent = Intent(this, FocusModeActivity::class.java).apply {
                            putExtra("EXTRA_DURATION_MINUTES", durationMinutes)
                        }
                        startActivity(intent)
                        // এই activity finish করছি না — যাতে Exit চাপলে এখানে ফিরে আসে
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}