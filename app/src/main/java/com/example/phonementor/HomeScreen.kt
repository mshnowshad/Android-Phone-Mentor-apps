package com.example.phonementor

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.phonementor.focus.AllowedAppsActivity
import com.example.phonementor.focus.FocusGuardService
import com.example.phonementor.focus.FocusSetupActivity
import com.example.phonementor.ui.theme.PhoneMentorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Feature(val title: String, val icon: ImageVector)

private val features = listOf(
    Feature("Focus mode", Icons.Outlined.CenterFocusStrong),
    Feature("Quick controls", Icons.Outlined.ToggleOn),
    Feature("Allowed apps", Icons.Outlined.Apps),
    Feature("Shorts blocker", Icons.Outlined.VideocamOff),
    Feature("Mentor prompts", Icons.Outlined.QuestionAnswer),
    Feature("Daily report", Icons.Outlined.BarChart),
    Feature("Goals", Icons.Outlined.Flag),
    Feature("Rewards", Icons.Outlined.CardGiftcard)
)

private val panelItems = listOf(
    Feature("Profile", Icons.Outlined.Person),
    Feature("History", Icons.Outlined.History),
    Feature("Permissions", Icons.Outlined.Security),
    Feature("Settings", Icons.Outlined.Settings),
    Feature("About", Icons.Outlined.Info)
)

private enum class PermKind { Accessibility, UsageAccess, Overlay, Notification }

private data class PermItem(
    val kind: PermKind,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val allPermissions = listOf(
    PermItem(
        kind = PermKind.Accessibility,
        title = "Accessibility (Required)",
        description = "Keeps you locked on the focus screen. Without this, the phone lock will not work.",
        icon = Icons.Outlined.Lock
    ),
    PermItem(
        kind = PermKind.UsageAccess,
        title = "Usage access",
        description = "Needed for daily screen-time reports. Your data stays on this phone.",
        icon = Icons.Outlined.BarChart
    ),
    PermItem(
        kind = PermKind.Overlay,
        title = "Display over apps",
        description = "Lets Phone Mentor show the focus lock screen above other apps.",
        icon = Icons.Outlined.Layers
    ),
    PermItem(
        kind = PermKind.Notification,
        title = "Notifications",
        description = "Shows focus timer and reminders. You can change this later.",
        icon = Icons.Outlined.Notifications
    )
)

private fun isAccessibilityGranted(context: android.content.Context): Boolean {
    val service = ComponentName(context, FocusGuardService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(service, ignoreCase = true) }
}

private fun isUsageAccessGranted(context: android.content.Context): Boolean {
    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun isOverlayGranted(context: android.content.Context): Boolean =
    Settings.canDrawOverlays(context)

private fun isNotificationGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun isPermissionGranted(context: android.content.Context, kind: PermKind): Boolean =
    when (kind) {
        PermKind.Accessibility -> isAccessibilityGranted(context)
        PermKind.UsageAccess   -> isUsageAccessGranted(context)
        PermKind.Overlay       -> isOverlayGranted(context)
        PermKind.Notification  -> isNotificationGranted(context)
    }

private fun openPermissionSettings(context: android.content.Context, kind: PermKind) {
    try {
        when (kind) {
            PermKind.Accessibility -> context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            PermKind.UsageAccess -> context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            PermKind.Overlay -> context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            PermKind.Notification -> { /* Runtime launcher handles it */ }
        }
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}

@Composable
fun HomeScreen() {
    var panelOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var grantedMap by remember {
        mutableStateOf(PermKind.values().associateWith { isPermissionGranted(context, it) })
    }

    LaunchedEffect(Unit) {
        while (true) {
            grantedMap = PermKind.values().associateWith { isPermissionGranted(context, it) }
            delay(1_500L)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        grantedMap = grantedMap.toMutableMap().apply { put(PermKind.Notification, granted) }
    }

    BackHandler(enabled = panelOpen) { panelOpen = false }

    fun comingSoon(title: String) {
        Toast.makeText(context, "$title: coming soon", Toast.LENGTH_SHORT).show()
    }

    fun onPermissionAction(kind: PermKind) {
        when (kind) {
            PermKind.Notification -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            else -> openPermissionSettings(context, kind)
        }
    }

    val missing = allPermissions.filter { grantedMap[it.kind] != true }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeContent(
            missingPermissions = missing,
            onPermissionAction = { onPermissionAction(it) },
            onMenuClick = { panelOpen = true },
            onFeatureClick = { feature ->
                when (feature.title) {
                    "Focus mode" -> context.startActivity(Intent(context, FocusSetupActivity::class.java))
                    "Allowed apps" -> context.startActivity(Intent(context, AllowedAppsActivity::class.java))
                    else -> comingSoon(feature.title)
                }
            }
        )

        AnimatedVisibility(visible = panelOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { panelOpen = false }
            )
        }

        AnimatedVisibility(
            visible = panelOpen,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.72f),
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SidePanel(onItemClick = { panelOpen = false; comingSoon(it.title) })
        }
    }
}

@Composable
private fun HomeContent(
    missingPermissions: List<PermItem>,
    onPermissionAction: (PermKind) -> Unit,
    onMenuClick: () -> Unit,
    onFeatureClick: (Feature) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TopBar(onMenuClick)
        Spacer(Modifier.height(12.dp))

        ClockCard()

        if (missingPermissions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            PermissionCarousel(items = missingPermissions, onAction = onPermissionAction)
        }

        Spacer(Modifier.height(20.dp))

        Text("Features", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            features.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { f -> FeatureTile(f, Modifier.weight(1f)) { onFeatureClick(f) } }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ClockCard() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); delay(1_000L) }
    }

    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val amPmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                timeFmt.format(now),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.width(6.dp))
            Text(
                amPmFmt.format(now),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            dateFmt.format(now),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionCarousel(items: List<PermItem>, onAction: (PermKind) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            PermissionCard(item = items[page], onAction = { onAction(items[page].kind) })
        }
        if (items.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(items.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermItem, onAction: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onAction() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (item.kind == PermKind.Notification) "Allow" else "Open settings",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TopBar(onMenuClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Phone Mentor",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun FeatureTile(feature: Feature, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .height(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(feature.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SidePanel(onItemClick: (Feature) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Your name", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("3 day streak", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
        Spacer(Modifier.height(8.dp))
        panelItems.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun HomeLightPreview() {
    PhoneMentorTheme(darkTheme = false) { HomeScreen() }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeDarkPreview() {
    PhoneMentorTheme(darkTheme = true) { HomeScreen() }
}