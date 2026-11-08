package com.example.phonementor.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FocusScreen
 *
 * @param durationMinutes total focus duration in minutes
 * @param onExit called when user manually exits (after confirmation)
 * @param onFinish called when the timer naturally reaches 00:00
 *
 * Layout:
 *  [Box black]
 *      ├── Top-right ✕ (Exit → dialog)
 *      ├── Center: big clock + date + countdown card
 *      └── When timer hits 0 → "Focus complete" dialog → auto onFinish()
 */
@Composable
fun FocusScreen(
    durationMinutes: Int = 25,
    onExit: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    // ---- Colors ----
    val backgroundColor = Color(0xFF000000)
    val primaryText    = Color(0xFFFFFFFF)
    val secondaryText  = Color(0xFF9AA0A6)
    val accentColor    = Color(0xFF38BDF8)

    // ---- State ----
    var showExitDialog     by remember { mutableStateOf(false) }
    var showFinishDialog   by remember { mutableStateOf(false) }
    var currentTime        by remember { mutableStateOf(Date()) }
    var remainingMillis    by remember {
        mutableStateOf(durationMinutes * 60_000L)
    }
    // True once the countdown has finished (prevents repeated triggers)
    var finished           by remember { mutableStateOf(false) }

    // Tick every second: update clock and countdown
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            if (remainingMillis > 0) {
                remainingMillis = (remainingMillis - 1_000L).coerceAtLeast(0L)

                // When it just reached zero → show finish dialog once
                if (remainingMillis == 0L && !finished) {
                    finished = true
                    showFinishDialog = true
                }
            }
            delay(1_000L)
        }
    }

    // Back button = try to exit
    BackHandler {
        // Once finished, back shouldn't open exit dialog (celebration is showing)
        if (!finished) showExitDialog = true
    }

    // ---- Formatters ----
    val timeFormatter  = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter  = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }
    val timeString = timeFormatter.format(currentTime)
    val dateString = dateFormatter.format(currentTime)

    // ---- Countdown (MM:SS) ----
    val remainingMinutes = (remainingMillis / 1000 / 60).toInt()
    val remainingSeconds = ((remainingMillis / 1000) % 60).toInt()
    val countdownText = String.format(
        Locale.getDefault(), "%02d:%02d",
        remainingMinutes, remainingSeconds
    )

    // ---- UI ----
    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
    ) {
        // Exit button (hidden once finished)
        if (!finished) {
            IconButton(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Exit Focus Mode",
                    tint = accentColor
                )
            }
        }

        // Center content
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                timeString,
                fontSize = 96.sp,
                fontWeight = FontWeight.Light,
                color = primaryText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                dateString,
                fontSize = 18.sp,
                color = secondaryText
            )
            Spacer(Modifier.height(32.dp))

            // Countdown card
            Column(
                Modifier
                    .background(
                        Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Time remaining",
                    color = secondaryText,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    countdownText,
                    color = accentColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (finished) "Session complete" else "Focus mode is on",
                fontSize = 14.sp,
                color = accentColor.copy(alpha = 0.7f)
            )
        }
    }

    // ---- Manual exit dialog ----
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit focus mode?") },
            text = {
                Text("If you leave now, your focus session will end. Are you sure you want to exit?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text("Yes, exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // ---- Auto-finish celebration dialog ----
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { /* not dismissible by tapping outside */ },
            icon = {
                Icon(
                    Icons.Outlined.Celebration,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Focus session complete! 🎉") },
            text = {
                Text("Great job staying focused. You will be taken back to the setup screen.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    onFinish()
                }) {
                    Text("Done")
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FocusScreenPreview() {
    FocusScreen(durationMinutes = 25, onExit = {}, onFinish = {})
}