package com.example.phonementor.focus

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ToggleCard(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val activeColor: Color,
    val isOn: Boolean
)

@Composable
fun FocusScreen(
    durationMinutes: Int = 25,
    onExit: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current

    // ---- Colors ----
    val backgroundColor = Color(0xFF000000)
    val primaryText    = Color(0xFFFFFFFF)
    val secondaryText  = Color(0xFF9AA0A6)
    val accentColor    = Color(0xFF38BDF8)
    val cardBg         = Color(0xFF141414)

    // ---- State ----
    var currentTime      by remember { mutableStateOf(Date()) }
    var remainingMillis  by remember { mutableStateOf(durationMinutes * 60_000L) }
    var finished         by remember { mutableStateOf(false) }
    var showExitDialog   by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    var wifiOn     by remember { mutableStateOf(isWifiEnabled(context)) }
    var dataOn     by remember { mutableStateOf(true) }
    var airplaneOn by remember { mutableStateOf(isAirplaneModeOn(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            if (remainingMillis > 0) {
                remainingMillis = (remainingMillis - 1_000L).coerceAtLeast(0L)
                if (remainingMillis == 0L && !finished) {
                    finished = true
                    showFinishDialog = true
                }
            }
            delay(1_000L)
        }
    }

    BackHandler { if (!finished) showExitDialog = true }

    // ---- Time split: "10:45" + "PM" ----
    val time12 = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val amPm   = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val timeString = time12.format(currentTime)
    val amPmString = amPm.format(currentTime)

    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val dateString = dateFormatter.format(currentTime)

    val remainingMinutes = (remainingMillis / 1000 / 60).toInt()
    val remainingSeconds = ((remainingMillis / 1000) % 60).toInt()
    val countdownText = String.format(Locale.getDefault(), "%02d:%02d",
        remainingMinutes, remainingSeconds)

    val toggles = listOf(
        ToggleCard("wifi", "Wi-Fi", Icons.Filled.Wifi,
            Color(0xFF0E2A33), wifiOn),
        ToggleCard("data", "Mobile Data", Icons.Filled.SignalCellular4Bar,
            Color(0xFF10261A), dataOn),
        ToggleCard("airplane", "Airplane", Icons.Filled.AirplanemodeActive,
            Color(0xFF33260E), airplaneOn)
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
    ) {
        // Exit button — door icon
        if (!finished) {
            IconButton(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Exit focus mode",
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ---- Clock with small AM/PM next to it ----
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    timeString,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    amPmString,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryText,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                dateString,
                fontSize = 15.sp,
                color = secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ---- 3 toggles ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                toggles.forEach { card ->
                    ToggleCardItem(
                        card = card,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            when (card.id) {
                                "wifi"     -> wifiOn = !wifiOn
                                "data"     -> dataOn = !dataOn
                                "airplane" -> airplaneOn = !airplaneOn
                            }
                            openInternetPanel(context)
                        }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ---- Allowed Apps placeholder ----
            Text(
                "Allowed Apps",
                color = secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No apps selected yet",
                    color = secondaryText,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // ---- Time remaining ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Time remaining", color = secondaryText, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    countdownText,
                    color = accentColor,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (finished) "Session complete" else "Focus mode is on",
                    color = accentColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit focus mode?") },
            text = { Text("Your focus session will end. Are you sure?") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onExit() }) {
                    Text("Yes, exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("No") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(Icons.Outlined.Celebration, null,
                    tint = accentColor, modifier = Modifier.size(36.dp))
            },
            title = { Text("Focus session complete! 🎉") },
            text = { Text("Great job. You will be taken back to the setup screen.") },
            confirmButton = {
                TextButton(onClick = { showFinishDialog = false; onFinish() }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ToggleCardItem(
    card: ToggleCard,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val bg = if (card.isOn) card.activeColor else Color(0xFF141414)
    val iconTint = if (card.isOn) Color(0xFF38BDF8) else Color(0xFF9AA0A6)

    Column(
        modifier
            .height(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable { onTap() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(card.icon, card.label, tint = iconTint,
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(card.label, color = Color.White, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(if (card.isOn) "On" else "Off",
            color = if (card.isOn) Color(0xFF38BDF8) else Color(0xFF9AA0A6),
            fontSize = 10.sp)
    }
}

private fun openInternetPanel(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) { }
    }
}

@Suppress("DEPRECATION")
private fun isWifiEnabled(context: Context): Boolean = try {
    (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
} catch (_: Exception) { false }

private fun isAirplaneModeOn(context: Context): Boolean = try {
    Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.AIRPLANE_MODE_ON, 0
    ) != 0
} catch (_: Exception) { false }

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FocusScreenPreview() {
    FocusScreen(durationMinutes = 25, onExit = {}, onFinish = {})
}