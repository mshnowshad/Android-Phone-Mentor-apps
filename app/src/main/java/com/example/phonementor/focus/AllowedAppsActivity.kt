package com.example.phonementor.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.phonementor.ui.theme.PhoneMentorTheme

/**
 * AllowedAppsActivity
 *
 * Activity wrapper for AllowedAppsScreen.
 * HomeScreen er "Allowed apps" tile theke eita launch hobe.
 */
class AllowedAppsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhoneMentorTheme {
                AllowedAppsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}