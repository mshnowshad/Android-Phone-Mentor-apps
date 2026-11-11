package com.example.phonementor.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AllowedAppsScreen
 *
 * User installed apps er list dekhte pabe. Max 5 ta select korte parbe.
 * Save button e click korle SharedPreferences e save hobe, tarpor onBack() call.
 */
@Composable
fun AllowedAppsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Installed apps list
    var installedApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Already selected apps (load on start)
    val savedApps = remember { AllowedAppsStore.load(context) }
    var selectedPkgs by remember {
        mutableStateOf(savedApps.map { it.packageName }.toSet())
    }

    // Load apps
    LaunchedEffect(Unit) {
        installedApps = AppPickerRepository.getInstalledApps(context)
        loading = false
    }

    BackHandler { onBack() }

    val maxReached = selectedPkgs.size >= AllowedAppsStore.MAX_ALLOWED_APPS

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ---- Top bar ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                "Allowed Apps",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(48.dp))
        }

        // ---- Info ----
        Text(
            "Select up to ${AllowedAppsStore.MAX_ALLOWED_APPS} apps you can use during focus mode.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Text(
            "${selectedPkgs.size} / ${AllowedAppsStore.MAX_ALLOWED_APPS} selected",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(8.dp))

        // ---- App list ----
        if (loading) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(installedApps, key = { it.first }) { (pkg, label) ->
                    val selected = pkg in selectedPkgs
                    val enabled = selected || !maxReached
                    AppRow(
                        packageName = pkg,
                        label = label,
                        selected = selected,
                        enabled = enabled,
                        onToggle = {
                            selectedPkgs = if (selected) {
                                selectedPkgs - pkg
                            } else {
                                if (selectedPkgs.size < AllowedAppsStore.MAX_ALLOWED_APPS)
                                    selectedPkgs + pkg
                                else selectedPkgs
                            }
                        }
                    )
                }
            }
        }

        // ---- Bottom Save button ----
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selectedPkgs.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
                .clickable(enabled = selectedPkgs.isNotEmpty()) {
                    // Save: build AllowedApp list with icons
                    scope.launch {
                        val apps = withContext(Dispatchers.IO) {
                            selectedPkgs.mapNotNull { p ->
                                val pm = context.packageManager
                                val label = installedApps.firstOrNull { it.first == p }?.second ?: p
                                try {
                                    val drawable = pm.getApplicationIcon(p)
                                    AllowedApp(
                                        packageName = p,
                                        label = label,
                                        iconBase64 = AllowedAppsStore.drawableToBase64(drawable)
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                        AllowedAppsStore.save(context, apps)
                        onBack()
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (selectedPkgs.isEmpty()) "Select at least 1 app"
                else "Save (${selectedPkgs.size})",
                color = if (selectedPkgs.isEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppRow(
    packageName: String,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val iconBitmap = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(64, 64)
                .asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    val rowBg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surface
    val alpha = if (enabled) 1f else 0.4f

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Icon(
            if (selected) Icons.Outlined.CheckCircle
            else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
    }
}