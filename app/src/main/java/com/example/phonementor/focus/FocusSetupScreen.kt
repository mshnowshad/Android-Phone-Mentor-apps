package com.example.phonementor.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonementor.ui.theme.PhoneMentorTheme

/**
 * FocusSetupScreen
 *
 * UI structure:
 *  [TopBar: ← back]
 *  [Big icon]
 *  [Title: "How long do you want to focus?"]
 *  [Subtitle]
 *  [Presets: 25 min / 45 min / 60 min / Custom]
 *  [If Custom selected → input field for minutes]
 *  [Start Focus button]
 */
@Composable
fun FocusSetupScreen(
    onStartFocus: (Int) -> Unit,
    onBack: () -> Unit
) {
    // Selected duration in minutes (default 25)
    var selectedMinutes by remember { mutableStateOf(25) }
    // Whether "Custom" chip is selected
    var isCustom by remember { mutableStateOf(false) }
    // Text typed in the custom input
    var customText by remember { mutableStateOf("") }

    val presets = listOf(25, 45, 60)

    // Final duration used when starting
    // - If custom: parse text, clamp 1..600
    // - Else: preset
    val finalMinutes: Int = if (isCustom) {
        customText.toIntOrNull()?.coerceIn(1, 600) ?: 0
    } else {
        selectedMinutes
    }

    // Validation: custom must be between 1 and 600
    val isValid = finalMinutes in 1..600

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        // ---- Top bar ----
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Focus Mode",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(28.dp))

        // ---- Icon ----
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.CenterFocusStrong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "How long do you want to focus?",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "During this time you can only use your allowed apps.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(24.dp))

        // ---- Preset chips (2 per row) ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DurationChip(
                label = "25 min",
                selected = !isCustom && selectedMinutes == 25,
                onClick = {
                    isCustom = false
                    selectedMinutes = 25
                },
                modifier = Modifier.weight(1f)
            )
            DurationChip(
                label = "45 min",
                selected = !isCustom && selectedMinutes == 45,
                onClick = {
                    isCustom = false
                    selectedMinutes = 45
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DurationChip(
                label = "1 hour",
                selected = !isCustom && selectedMinutes == 60,
                onClick = {
                    isCustom = false
                    selectedMinutes = 60
                },
                modifier = Modifier.weight(1f)
            )
            DurationChip(
                label = "Custom",
                selected = isCustom,
                onClick = { isCustom = true },
                modifier = Modifier.weight(1f)
            )
        }

        // ---- Custom input field (only when Custom is selected) ----
        if (isCustom) {
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = customText,
                onValueChange = { newValue ->
                    // Allow only digits, max 3 chars (1..600)
                    val digitsOnly = newValue.filter { it.isDigit() }.take(3)
                    customText = digitsOnly
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Minutes (1 – 600)") },
                placeholder = { Text("e.g. 30") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = customText.isNotEmpty() && !isValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            if (customText.isNotEmpty() && !isValid) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Please enter a number between 1 and 600.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // ---- Start Focus button ----
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isValid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(enabled = isValid) { onStartFocus(finalMinutes) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = if (isValid) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isValid) "Start Focus" else "Enter minutes",
                    color = if (isValid) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val border = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Box(
        modifier
            .height(64.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FocusSetupPreview() {
    PhoneMentorTheme {
        FocusSetupScreen(onStartFocus = {}, onBack = {})
    }
}