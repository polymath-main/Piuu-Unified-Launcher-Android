package com.piuu.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.piuu.launcher.R
import com.piuu.launcher.marketplace.MarketplaceCore
import com.piuu.launcher.marketplace.PiuuPluginSdk
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
    onInstallItem: (MarketplaceItem) -> Unit,
    onPreviewTheme: ((MarketplaceItem) -> Unit)? = null
) {
    if (!visible) return

    val context = LocalContext.current
    val marketplaceCore = remember { MarketplaceCore.getInstance(context) }
    val installedPlugins by marketplaceCore.installedPluginsFlow.collectAsState()

    var showDeveloperTools by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var aiPrompt by remember { mutableStateOf("") }
    var isGeneratingAiTheme by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val tabs = remember(showDeveloperTools) {
        if (showDeveloperTools) {
            listOf("all", "theming", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills", "sdk_sandbox")
        } else {
            listOf("all", "theming", "fonts", "layouts", "widgets", "icons", "agents")
        }
    }

    var customManifestJson by remember {
        mutableStateOf("""{
  "id": "custom_dev_widget",
  "name": "Live Dev Telemetry",
  "version": "1.0.0",
  "category": "widgets",
  "type": "WIDGET",
  "author": "Piuu Developer",
  "description": "Realtime CPU & Storage Monitor",
  "permissions": ["system_stats", "persistent_storage"]
}""".trimIndent())
    }

    var customPayloadJson by remember {
        mutableStateOf("""{
  "metric_type": "battery_storage",
  "chart_type": "gauge"
}""".trimIndent())
    }

    var sdkTestOutput by remember { mutableStateOf("") }

    val builtinItems = remember {
        listOf(
            MarketplaceItem("builtin_piuu_notes", "Piuu Notes & Quick Memo", "widgets", "Piuu Suite", 5.0f, 1250, "Persistent notes and quick memo launcher widget", is_installed = true),
            MarketplaceItem("builtin_pip_edge", "Piuu Side Edge Assist", "widgets", "Piuu Suite", 5.0f, 980, "Floating side edge bar with quick notes & app access", is_installed = true),
            MarketplaceItem("builtin_piuu_control", "Piuu Launcher Control Hub", "widgets", "Piuu Suite", 5.0f, 1420, "Native C telemetry performance & system control hub", is_installed = true)
        )
    }

    val combinedCatalog = remember(catalog) {
        val existingIds = catalog.map { it.id }.toSet()
        builtinItems.filterNot { existingIds.contains(it.id) } + catalog
    }

    val filteredItems = combinedCatalog.filter { item ->
        val matchesCategory = if (selectedTab == "all" || selectedTab == "sdk_sandbox") true else item.category.equals(selectedTab, ignoreCase = true)
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
                var titleClickCount by remember { mutableStateOf(0) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            titleClickCount++
                            if (titleClickCount >= 5) {
                                showDeveloperTools = !showDeveloperTools
                                titleClickCount = 0
                                android.widget.Toast.makeText(
                                    context,
                                    if (showDeveloperTools) "Developer Mode Activated 🛠️" else "Standard Mode Activated 🌸",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Piuu Design Store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                if (showDeveloperTools) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = SuccessGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("DEV CORE v2.1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(
                                text = if (showDeveloperTools) "Sandbox, Telemetries & Custom SDK Plugins active" else "Beautiful Theme Packs, Custom Fonts & Widgets",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (showDeveloperTools) {
                    // Marketplace Core Engine Telemetry Bar
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LauncherBorder, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Catalog Items", fontSize = 10.sp, color = TextMuted)
                                    Text("${catalog.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                }
                                Column {
                                    Text("Active Plugins", fontSize = 10.sp, color = TextMuted)
                                    Text("${installedPlugins.count { it.isInstalled }}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                                Column {
                                    Text("SDK Sandbox", fontSize = 10.sp, color = TextMuted)
                                    Text("Isolated", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                }
                            }

                            TextButton(
                                onClick = { selectedTab = "sdk_sandbox" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SDK Dev Hub", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Friendly User Banner (Minimal visual concept!)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LauncherBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Aesthetic Home customizers", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Tap on any card to preview, or apply instantly to beautify your home screen.", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search themes, fonts, widgets, extensions...", color = TextMuted, fontSize = 13.sp) },
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
                                    text = when (tab) {
                                        "all" -> "All Schemes"
                                        "sdk_sandbox" -> "⚡ SDK Sandbox"
                                        else -> tab.replaceFirstChar { it.uppercase() }
                                    },
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

                    // SDK Sandbox Inspector Tab
                    if (selectedTab == "sdk_sandbox") {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, PrimaryBlue, RoundedCornerShape(18.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Terminal, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Marketplace Core SDK Sandbox", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                        Surface(color = PrimaryBlue.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text("v2.1.0 Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text("Test custom plugin manifests, permissions, and runtime SDK sandbox invocation.", fontSize = 11.sp, color = TextMuted)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Direct SDK Quick Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val dummyPlugin = com.piuu.launcher.marketplace.MarketplacePlugin(
                                                    manifest = com.piuu.launcher.marketplace.MarketplacePluginManifest(
                                                        id = "telemetry_demo",
                                                        name = "System Telemetry",
                                                        permissions = listOf("system_stats")
                                                    )
                                                )
                                                val res = PiuuPluginSdk.getSystemMetrics(context, dummyPlugin)
                                                sdkTestOutput = "SDK getSystemMetrics():\nSuccess: ${res.success}\nMessage: ${res.message}\nData: ${res.data}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue.copy(alpha = 0.2f)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Run Metrics API", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val (valid, manifest) = PiuuPluginSdk.validateManifest(customManifestJson)
                                                sdkTestOutput = "Manifest Validation:\nValid: $valid\nParsed Manifest: $manifest"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple.copy(alpha = 0.2f)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Validate Manifest", fontSize = 11.sp, color = AccentPurple, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Plugin Manifest JSON:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    OutlinedTextField(
                                        value = customManifestJson,
                                        onValueChange = { customManifestJson = it },
                                        modifier = Modifier.fillMaxWidth().height(110.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                            focusedTextColor = PrimaryBlue,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Plugin Payload JSON:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    OutlinedTextField(
                                        value = customPayloadJson,
                                        onValueChange = { customPayloadJson = it },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                            focusedTextColor = PrimaryBlue,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val (valid, manifest) = com.piuu.launcher.marketplace.PiuuPluginSdk.validateManifest(customManifestJson)
                                            if (valid && manifest != null) {
                                                val dummyItem = MarketplaceItem(
                                                    id = manifest.id,
                                                    name = manifest.name,
                                                    category = manifest.category,
                                                    author = manifest.author,
                                                    rating = manifest.rating,
                                                    downloads = manifest.downloads,
                                                    description = manifest.description,
                                                    preview_badge = "SDK Sandbox",
                                                    payload = customPayloadJson,
                                                    is_installed = true
                                                )
                                                onInstallItem(dummyItem)
                                                sdkTestOutput = "Registered & Installed custom SDK plugin '${manifest.name}' [ID: ${manifest.id}]"
                                            } else {
                                                sdkTestOutput = "Validation Failed: Invalid JSON or missing manifest parameters."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Register & Install Custom Plugin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("Interactive SDK Output:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black)
                                            .padding(10.dp)
                                    ) {
                                        Text(sdkTestOutput, fontSize = 11.sp, color = SuccessGreen)
                                    }
                                }
                            }
                        }
                    } else if (filteredItems.isEmpty()) {
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
                            MarketplaceCardItem(
                                item = item,
                                onInstall = { onInstallItem(item) },
                                onPreview = if (onPreviewTheme != null && (item.category.lowercase() == "theming" || item.category.lowercase() == "themes")) {
                                    { onPreviewTheme(item) }
                                } else null,
                                onSdkTestAction = { action ->
                                    val res = marketplaceCore.executePluginSdkAction(item.id, action)
                                    sdkTestOutput = "Executed '$action' on ${item.name}:\nSuccess: ${res.success}\nData: ${res.data}"
                                    selectedTab = "sdk_sandbox"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceCardItem(
    item: MarketplaceItem,
    onInstall: () -> Unit,
    onPreview: (() -> Unit)? = null,
    onSdkTestAction: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .hoverable(interactionSource)
            .testTag("marketplace_card_${item.id}")
            .border(
                width = if (item.is_installed) 1.5.dp else 1.dp,
                color = if (item.is_installed) SuccessGreen else LauncherBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable {
                onPreview?.invoke()
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Badge
                val badgeGradient = remember(item.category, activePrimaryColor, activeAccentColor) {
                    when (item.category.lowercase()) {
                        "theming", "themes" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(activePrimaryColor, activePrimaryColor.copy(alpha = 0.5f)))
                        "fonts" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(activeAccentColor, activeAccentColor.copy(alpha = 0.5f)))
                        "layouts" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.5f)))
                        "faces", "widgets" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(WarningAmber, WarningAmber.copy(alpha = 0.5f)))
                        "icons" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(activePrimaryColor, activePrimaryColor.copy(alpha = 0.5f)))
                        "agents" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(activeAccentColor, activeAccentColor.copy(alpha = 0.5f)))
                        else -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(activePrimaryColor, activePrimaryColor.copy(alpha = 0.5f)))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeGradient),
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Details",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${item.author} • ⭐ ${item.rating} ", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = if (item.is_installed) SuccessGreen.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (item.is_installed) "Installed" else "Available",
                                color = if (item.is_installed) SuccessGreen else PrimaryBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Action Buttons Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onPreview != null) {
                        OutlinedButton(
                            onClick = onPreview,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.btn_preview),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Color Swatches Preview for Themes
            if ((item.category.lowercase().contains("theme") || item.category.lowercase().contains("theming")) && !item.payload.isNullOrBlank()) {
                var primaryHex = "#3B82F6"
                var accentHex = "#8B5CF6"
                var bgHex = "#020817"
                var hasPalette = false
                try {
                    val json = JSONObject(item.payload)
                    primaryHex = json.optString("primary_blue", json.optString("primary_color", ""))
                    accentHex = json.optString("accent_purple", json.optString("accent_color", ""))
                    bgHex = json.optString("bg_color", json.optString("bg_overlay", ""))
                    if (primaryHex.isNotBlank() || accentHex.isNotBlank()) {
                        hasPalette = true
                    }
                } catch (_: Exception) {}

                if (hasPalette) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("Colors:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        val pCol = try { Color(android.graphics.Color.parseColor(primaryHex)) } catch (_: Exception) { PrimaryBlue }
                        val aCol = try { Color(android.graphics.Color.parseColor(accentHex)) } catch (_: Exception) { AccentPurple }
                        val bCol = try { Color(android.graphics.Color.parseColor(bgHex)) } catch (_: Exception) { Color(0xFF020817) }

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(pCol)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(aCol)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(bCol)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (isExpanded) {
                // Parse schema metadata payload
                var version = "1.0.0"
                var fileSize = "128 KB"
                var sdkReq = "v2.1"
                if (!item.payload.isNullOrBlank()) {
                    try {
                        val json = JSONObject(item.payload)
                        version = json.optString("version", json.optString("ver", "1.0.0"))
                        fileSize = json.optString("file_size", json.optString("size", "128 KB"))
                        sdkReq = json.optString("sdk_version", "v2.1")
                    } catch (_: Exception) {}
                }

                Text(item.description, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = LauncherGlass),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("SCHEMA METADATA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Version", fontSize = 11.sp, color = TextMuted)
                            Text(version, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = LauncherBorder.copy(alpha = 0.5f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Size", fontSize = 11.sp, color = TextMuted)
                            Text(fileSize, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = LauncherBorder.copy(alpha = 0.5f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Category", fontSize = 11.sp, color = TextMuted)
                            Text(item.category.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = LauncherBorder.copy(alpha = 0.5f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Author", fontSize = 11.sp, color = TextMuted)
                            Text(item.author, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = LauncherBorder.copy(alpha = 0.5f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Required SDK", fontSize = 11.sp, color = TextMuted)
                            Text(sdkReq, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            } else {
                Text(item.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Permission & Capability Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = LauncherGlass, shape = RoundedCornerShape(6.dp)) {
                    Text("SDK v2.1", fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Surface(color = PrimaryBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text("Sandboxed", fontSize = 9.sp, color = PrimaryBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                if (item.category.lowercase() in listOf("widgets", "faces", "agents")) {
                    Surface(color = AccentPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("SYSTEM_STATS", fontSize = 9.sp, color = AccentPurple, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onSdkTestAction != null) {
                    TextButton(
                        onClick = { onSdkTestAction("inspect_manifest") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("SDK Test", fontSize = 10.sp, color = AccentPurple)
                    }
                }
            }

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
                val packId = item.id.lowercase()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Icons.Default.Phone, Icons.Default.CameraAlt, Icons.Default.Settings).forEach { icon ->
                            when {
                                packId.contains("neon") -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F172A))
                                            .border(1.5.dp, Color(0xFF06B6D4), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(13.dp))
                                    }
                                }
                                packId.contains("pastel") -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFDE2E4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                    }
                                }
                                packId.contains("monochrome") || packId.contains("wireframe") -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF09090B))
                                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                }
                                packId.contains("pixel") -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFEF4444))
                                            .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                }
                                packId.contains("gold") -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    listOf(Color(0xFFFCD34D), Color(0xFFD97706))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF451A03), modifier = Modifier.size(13.dp))
                                    }
                                }
                                packId.contains("vintage") -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF5E6CA))
                                            .border(1.dp, Color(0xFFD4B499), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF5C3D2E), modifier = Modifier.size(13.dp))
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(activePrimaryColor.copy(alpha = 0.2f))
                                            .border(1.dp, activePrimaryColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = activePrimaryColor, modifier = Modifier.size(13.dp))
                                    }
                                }
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

