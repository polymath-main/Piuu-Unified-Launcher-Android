package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.piuu.launcher.model.SystemApp
import com.piuu.launcher.ui.theme.*
import com.piuu.launcher.repository.LauncherPreferenceManager
import com.piuu.launcher.repository.LauncherConfigManager
import com.piuu.launcher.repository.AppIconImage

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    visible: Boolean,
    apps: List<SystemApp>,
    onDismiss: () -> Unit,
    onLaunchApp: (SystemApp) -> Unit,
    onToggleFavorite: (SystemApp) -> Unit,
    onLongPressApp: (SystemApp) -> Unit,
    onAutoCategorize: (() -> Unit) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    val configManager = remember { LauncherConfigManager.getInstance(context) }
    val showFrequentlyUsed = prefManager.drawerShowFrequentlyUsed
    val showCategories = prefManager.drawerShowCategories
    val columnCount = prefManager.drawerColumnCount
    val drawerIconSize = configManager.config.drawerIconSize
    val drawerShowLabels = prefManager.drawerShowLabels

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val dynamicColumnCount = remember(screenWidthDp, columnCount) {
        if (screenWidthDp > 600) {
            (screenWidthDp / 90).coerceIn(columnCount, 8)
        } else {
            columnCount
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf("all", "piuu_suite", "social", "productivity", "entertainment", "utilities", "system")

    val filteredApps = apps.filter { app ->
        val matchesQuery = app.name.contains(searchQuery, ignoreCase = true) || app.package_name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "all") true else app.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    val frequentApps = apps.sortedByDescending { it.usage_count }.take(4)

    // Reactive preference state triggers
    var prefVersion by remember { mutableIntStateOf(0) }

    val appDrawerTransparency = remember(prefManager.appDrawerTransparency, prefVersion) {
        prefManager.appDrawerTransparency
    }
    val customBgColor = remember(prefManager.drawerBackgroundColor, prefVersion) {
        com.piuu.launcher.ui.theme.parseRgbaOrHexColor(prefManager.drawerBackgroundColor, LauncherBackground)
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { it },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { it },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            )
        ) + androidx.compose.animation.fadeOut()
    ) {
        androidx.activity.compose.BackHandler(enabled = visible) {
            onDismiss()
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                // Background Customizable Card Container with alpha and border styling
                val safeAlpha = appDrawerTransparency.coerceIn(0.12f, 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.94f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(customBgColor.copy(alpha = safeAlpha))
                        .border(
                            width = 1.2.dp,
                            color = LauncherBorder.copy(alpha = (safeAlpha + 0.2f).coerceAtMost(1.0f)),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable(enabled = true, onClick = {})
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                // Header Search Input & Close Button Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search ${apps.size} installed apps...", color = TextMuted) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = LauncherBorder,
                            focusedContainerColor = CardGlassBg,
                            unfocusedContainerColor = CardGlassBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips Row
            if (showCategories) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        var isCategorizing by remember { mutableStateOf(false) }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (!isCategorizing) {
                                    isCategorizing = true
                                    onAutoCategorize {
                                        isCategorizing = false
                                        android.widget.Toast.makeText(context, "Apps successfully categorized by AI!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCategorizing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI Tagging...", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI Organize", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardGlassBg,
                                selectedContainerColor = CardGlassBg
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = PrimaryBlue.copy(alpha = 0.6f)
                            )
                        )
                    }

                    items(categories, key = { it }) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                containerColor = CardGlassBg
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Frequently Used Row (if no search query)
            if (showFrequentlyUsed && searchQuery.isEmpty() && selectedCategory == "all") {
                Text("Smart Usage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(frequentApps, key = { it.package_name }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.longPressPulseEffect(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(drawerIconSize.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIconImage(
                                    packageName = app.package_name,
                                    modifier = Modifier.size((drawerIconSize * 0.46f).dp),
                                    fallbackIconName = app.icon_name,
                                    tintColor = AccentPurple
                                )
                            }
                            if (drawerShowLabels) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.name,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                    lineHeight = 16.sp,
                                    letterSpacing = 0.5.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(drawerIconSize.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("All Apps (${filteredApps.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            // App Grid with scroll mode support
            val scrollMode = prefManager.drawerScrollMode
            if (scrollMode == "HORIZONTAL") {
                val itemsPerPage = (dynamicColumnCount * 4).coerceAtLeast(8)
                val pagesOfApps = filteredApps.chunked(itemsPerPage)
                if (pagesOfApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No apps found", color = TextMuted)
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { pagesOfApps.size })
                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { pageIdx ->
                            val pageApps = pagesOfApps[pageIdx]
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(dynamicColumnCount),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(pageApps, key = { it.package_name }) { app ->
                                    AppGridTileItem(
                                        app = app,
                                        iconSize = drawerIconSize,
                                        showLabels = drawerShowLabels,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onLongPressApp(app) }
                                    )
                                }
                            }
                        }

                        // Page indicator
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(pagesOfApps.size) { idx ->
                                val active = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(CircleShape)
                                        .background(if (active) PrimaryBlue else TextMuted.copy(alpha = 0.4f))
                                        .size(if (active) 8.dp else 5.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(dynamicColumnCount),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps, key = { it.package_name }) { app ->
                        AppGridTileItem(
                            app = app,
                            iconSize = drawerIconSize,
                            showLabels = drawerShowLabels,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) }
                        )
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
fun AppGridTileItem(
    app: SystemApp,
    iconSize: Int,
    showLabels: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.longPressPulseEffect(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        BadgedBox(
            badge = {
                if (app.badge_count > 0) {
                    Badge(containerColor = DangerRed) { Text("${app.badge_count}") }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardGlassBg)
                    .border(1.dp, LauncherBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppIconImage(
                    packageName = app.package_name,
                    modifier = Modifier.size((iconSize * 0.48f).dp),
                    fallbackIconName = app.icon_name,
                    tintColor = if (app.category == "piuu_suite") SuccessGreen else PrimaryBlue
                )
            }
        }
        if (showLabels) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(iconSize.dp)
            )
        }
    }
}

fun getAppIconVector(iconName: String) = when(iconName) {
    "phone" -> Icons.Default.Phone
    "message" -> Icons.Default.Message
    "globe" -> Icons.Default.Public
    "camera" -> Icons.Default.CameraAlt
    "brain" -> Icons.Default.AutoAwesome
    "folder" -> Icons.Default.Folder
    "code" -> Icons.Default.Code
    "music" -> Icons.Default.MusicNote
    "mail" -> Icons.Default.Mail
    "video" -> Icons.Default.PlayCircle
    "calendar" -> Icons.Default.CalendarToday
    "settings" -> Icons.Default.Settings
    "note" -> Icons.Default.EditNote
    "calculator" -> Icons.Default.Calculate
    "cloud" -> Icons.Default.Cloud
    "file" -> Icons.Default.InsertDriveFile
    "clock" -> Icons.Default.AccessTime
    else -> Icons.Default.Apps
}
