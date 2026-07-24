package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.MarketplaceItem
import com.piuu.launcher.model.LauncherTheme
import com.piuu.launcher.repository.AiEngine
import com.piuu.launcher.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceModal(
    visible: Boolean,
    catalog: List<MarketplaceItem>,
    aiEngine: AiEngine? = null,
    onDismiss: () -> Unit,
    onApplyAiTheme: ((LauncherTheme) -> Unit)? = null,
    onInstallItem: (MarketplaceItem) -> Unit
) {
    if (!visible) return

    var selectedTab by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var aiPrompt by remember { mutableStateOf("") }
    var isGeneratingAiTheme by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val tabs = listOf("all", "theming", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")

    val filteredItems = catalog.filter { item ->
        val matchesCategory = if (selectedTab == "all") true else item.category.equals(selectedTab, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.author.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LauncherBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Piuu Marketplace", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Schemes, Themes, Fonts & AI Agents", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search themes, fonts, layouts...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted) } }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardGlassBg,
                        unfocusedContainerColor = CardGlassBg,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = LauncherBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.replaceFirstChar { it.uppercase() },
                                    color = if (selectedTab == tab) PrimaryBlue else TextSecondary,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // AI Theme Generator Prompt Card at top
                    if (aiEngine != null && onApplyAiTheme != null && (selectedTab == "all" || selectedTab == "theming")) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gemini AI Theme Generator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Text("Type any visual vibe to synthesize a custom scheme (e.g. 'Nordic Forest Dark' or 'Cyber Sunset')", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = aiPrompt,
                                            onValueChange = { aiPrompt = it },
                                            placeholder = { Text("Describe theme style...", color = TextMuted, fontSize = 12.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = CardGlassBg,
                                                unfocusedContainerColor = CardGlassBg,
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = LauncherBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (aiPrompt.isNotBlank() && !isGeneratingAiTheme) {
                                                    isGeneratingAiTheme = true
                                                    scope.launch {
                                                        val generated = aiEngine.generateThemeFromAiPrompt(aiPrompt)
                                                        isGeneratingAiTheme = false
                                                        onApplyAiTheme(generated)
                                                    }
                                                }
                                            },
                                            enabled = !isGeneratingAiTheme && aiPrompt.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            if (isGeneratingAiTheme) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Text("Synthesize", fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No marketplace schemes found.", color = TextMuted, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            MarketplaceCardItem(item = item, onInstall = { onInstallItem(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceCardItem(item: MarketplaceItem, onInstall: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.is_installed) 1.5.dp else 1.dp,
                color = if (item.is_installed) SuccessGreen else LauncherBorder,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (item.category.lowercase()) {
                                "theming", "themes" -> PrimaryBlue
                                "fonts" -> AccentPurple
                                "layouts" -> SuccessGreen
                                "faces", "widgets" -> WarningAmber
                                "icons" -> PrimaryBlue
                                "agents" -> AccentPurple
                                else -> SuccessGreen
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when(item.category.lowercase()) {
                            "theming", "themes" -> Icons.Default.Palette
                            "fonts" -> Icons.Default.FormatSize
                            "layouts" -> Icons.Default.Dashboard
                            "faces" -> Icons.Default.AccessTime
                            "widgets" -> Icons.Default.Widgets
                            "icons" -> Icons.Default.Apps
                            "agents" -> Icons.Default.SmartToy
                            "skills" -> Icons.Default.Bolt
                            else -> Icons.Default.Extension
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (item.preview_badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (item.is_installed) SuccessGreen.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (item.is_installed) "ACTIVE" else item.preview_badge,
                                    color = if (item.is_installed) SuccessGreen else PrimaryBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text("${item.author} • ⭐ ${item.rating} (${item.downloads} downloads)", fontSize = 11.sp, color = TextMuted)
                }

                // Action Button
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.is_installed) SuccessGreen.copy(alpha = 0.18f) else PrimaryBlue
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.is_installed) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (item.is_installed) "Applied" else "Apply",
                            color = if (item.is_installed) SuccessGreen else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(item.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(10.dp))

            // ── Small Visual Content Preview ──────────────────────────────────────
            ItemPreviewWidget(item = item)
        }
    }
}

@Composable
fun ItemPreviewWidget(item: MarketplaceItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(0.5.dp, LauncherBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        when (item.category.lowercase()) {
            "theming", "themes" -> {
                // Color Swatches & Mini Theme Card Preview
                var primaryColor = "#3B82F6"
                var accentColor = "#8B5CF6"
                var bgColor = "#0A0A10"
                if (!item.payload.isNullOrBlank()) {
                    try {
                        val json = JSONObject(item.payload)
                        primaryColor = json.optString("primary_blue", json.optString("primary_color", primaryColor))
                        accentColor = json.optString("accent_purple", json.optString("accent_color", accentColor))
                        bgColor = json.optString("bg_color", json.optString("card_bg", bgColor))
                    } catch (_: Exception) {}
                }

                val pCol = parseHexColor(primaryColor, PrimaryBlue)
                val aCol = parseHexColor(accentColor, AccentPurple)
                val bCol = parseHexColor(bgColor, Color(0xFF0F172A))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Theme Swatches:", fontSize = 10.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(pCol))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(aCol))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(bCol))
                    }

                    // Mini preview mockup
                    Surface(
                        color = pCol.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(0.5.dp, aCol, RoundedCornerShape(6.dp))
                    ) {
                        Text("Live Preview", color = pCol, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            "fonts" -> {
                var fontFamilyName = item.name
                if (!item.payload.isNullOrBlank()) {
                    try {
                        val json = JSONObject(item.payload)
                        fontFamilyName = json.optString("font_family", fontFamilyName)
                    } catch (_: Exception) {}
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Aa Bb 123 • $fontFamilyName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("The quick brown fox jumps over the lazy dog", fontSize = 10.sp, color = TextMuted)
                    }
                    Surface(color = AccentPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text("Typography", color = AccentPurple, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            "layouts" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(4) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (idx % 2 == 0) PrimaryBlue.copy(alpha = 0.4f) else AccentPurple.copy(alpha = 0.4f))
                            )
                        }
                    }
                    Text("Grid & Folder Layout Diagram", fontSize = 10.sp, color = TextMuted)
                }
            }
            "icons" -> {
                var glowColor = PrimaryBlue
                if (!item.payload.isNullOrBlank()) {
                    try {
                        val json = JSONObject(item.payload)
                        glowColor = parseHexColor(json.optString("primary_glow", "#3B82F6"), PrimaryBlue)
                    } catch (_: Exception) {}
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Icons.Default.Phone, Icons.Default.CameraAlt, Icons.Default.Settings).forEach { icon ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(glowColor.copy(alpha = 0.2f))
                                    .border(1.dp, glowColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = glowColor, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Text("Sample Iconpack Preview", fontSize = 10.sp, color = TextMuted)
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verified System Component", fontSize = 10.sp, color = TextMuted)
                    }
                    Text("Piuu Extension", fontSize = 9.sp, color = PrimaryBlue)
                }
            }
        }
    }
}

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else if (clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

