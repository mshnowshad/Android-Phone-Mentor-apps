package com.example.phonementor

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonementor.ui.theme.PhoneMentorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// PHASE -2
import android.content.Intent
import com.example.phonementor.focus.FocusSetupActivity


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

// Ekhon sample data. Pore UsageStatsManager diye asol data boshabo.


@Composable
fun HomeScreen() {
    var panelOpen by remember { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    val context = LocalContext.current

    // Permission na thakle 1.5s por por check, thakle 15s por por data refresh
    LaunchedEffect(Unit) {
        while (true) {
            val granted = hasUsageAccess(context)
            hasAccess = granted
            if (granted) {
                usage = withContext(Dispatchers.Default) { todayUsage(context) }
            }
            delay(if (granted) 5_000 else 1_500)
        }
    }

    BackHandler(enabled = panelOpen) { panelOpen = false }

    fun comingSoon(title: String) {
        Toast.makeText(context, "$title: coming soon", Toast.LENGTH_SHORT).show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeContent(
            hasAccess = hasAccess,
            usage = usage,
            onGrantClick = { openUsageAccessSettings(context) },
            onMenuClick = { panelOpen = true },
            onFeatureClick = { feature ->
                if (feature.title == "Focus mode") {
                    context.startActivity(Intent(context, FocusSetupActivity::class.java))
                } else {
                    comingSoon(feature.title)
                }

            }
        )

        AnimatedVisibility(
            visible = panelOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
            SidePanel(
                onItemClick = {
                    panelOpen = false
                    comingSoon(it.title)
                }
            )
        }
    }
}






@Composable
private fun HomeContent(
    hasAccess: Boolean,
    usage: List<AppUsage>,
    onGrantClick: () -> Unit,
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
        if (hasAccess) ScreenTimeCard(usage) else PermissionCard(onGrantClick)
        Spacer(Modifier.height(20.dp))
        Text(
            "Features",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            features.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { f ->
                        FeatureTile(f, Modifier.weight(1f)) { onFeatureClick(f) }
                    }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


@Composable
private fun TopBar(onMenuClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Phone Mentor",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ScreenTimeCard(usage: List<AppUsage>) {
    val shape = RoundedCornerShape(20.dp)
    val total = usage.sumOf { it.millis }
    val limit = 5L * 60 * 60 * 1000
    val percent = ((total * 100) / limit).toInt().coerceAtMost(100)
    val top = usage.take(3)
    val maxMillis = top.maxOfOrNull { it.millis } ?: 1L

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(20.dp)
    ) {
        Text(
            "Screen time today",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            formatDuration(total),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            "Limit 5h, $percent% used",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))
        if (top.isEmpty()) {
            Text(
                "No app usage yet today.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        top.forEach { app ->
            Row(
                Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    app.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(80.dp)
                )
                UsageBar(app.millis.toFloat() / maxMillis, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDuration(app.millis),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrantClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(20.dp)
    ) {
        Icon(
            Icons.Outlined.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Allow usage access",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Phone Mentor needs this to show your screen time. Your data stays on this phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onGrantClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Open settings",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}





@Composable
private fun UsageBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MaterialTheme.colorScheme.primary)
        )
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
        Icon(
            feature.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            feature.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "A",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Your name",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "3 day streak",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
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
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun HomeLightPreview() {
    PhoneMentorTheme(darkTheme = false) { HomeScreen() }
}

@Preview(
    showBackground = true,
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeDarkPreview() {
    PhoneMentorTheme(darkTheme = true) { HomeScreen() }
}