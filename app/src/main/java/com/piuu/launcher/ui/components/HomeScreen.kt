package com.piuu.launcher.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.*
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    schema: LauncherSchema,
    metrics: SystemMetrics,
    installedApps: List<SystemApp>,
    notes: List<NoteItem>,
    onLaunchApp: (SystemApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenBrainChat: () -> Unit,
    onOpenFolder: () -> Unit,
    onAddNote: (String) -> Unit
) {
    val context = LocalContext.current
    val pages = schema.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })

    val dockApps = remember(schema.dock.app_packages, installedApps) {
        schema.dock.app_packages.mapNotNull { pkg ->
            installedApps.find { it.package_name == pkg }
        }.ifEmpty { installedApps.take(5) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Horizontal Pager across launcher schema pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex)
            if (page != null) {
                // Flex & Grid container with proper padding & margin scopes
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Page Title Header Span
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = page.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }

                    // Render grid widget items with flexible span management
                    items(
                        items = page.elements,
                        span = { elem ->
                            val spanWidth = when (elem.type) {
                                ElementType.CLOCK,
                                ElementType.WEATHER,
                                ElementType.AGENT_WIDGET,
                                ElementType.MEDIA_PLAYER,
                                ElementType.PIUU_SUITE_FOLDER,
                                ElementType.CUSTOM_CARD -> 2
                                else -> if ((elem.style_props.w ?: 2) >= 4) 2 else 1
                            }
                            GridItemSpan(spanWidth)
                        }
                    ) { elem ->
                        when (elem.type) {
                            ElementType.CLOCK -> ClockWidgetComposable()
                            ElementType.WEATHER -> WeatherWidgetComposable()
                            ElementType.BATTERY_STATUS -> BatteryWidgetComposable(metrics)
                            ElementType.SYSTEM_STATS -> SystemStatsWidgetComposable(metrics)
                            ElementType.MEDIA_PLAYER -> MediaPlayerWidgetComposable()
                            ElementType.AGENT_WIDGET -> AiInsightWidgetComposable(onOpenBrainChat)
                            ElementType.QUICK_NOTES -> QuickNotesWidgetComposable(notes, onAddNote)
                            ElementType.PIUU_SUITE_FOLDER -> PiuuSuiteFolderWidgetComposable(onOpenFolder)
                            ElementType.PREDICTIVE_SUGGESTIONS -> PredictiveSuggestionsWidgetComposable(installedApps, onLaunchApp)
                            ElementType.APP_GRID -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, LauncherBorder, RoundedCornerShape(24.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Frequent Apps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            installedApps.take(4).forEach { app ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.clickable { onLaunchApp(app) }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                            .background(PrimaryBlue.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = getAppIconVector(app.icon_name),
                                                            contentDescription = app.name,
                                                            tint = PrimaryBlue
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(app.name, fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = elem.title ?: "Launcher Widget",
                                        modifier = Modifier.padding(16.dp),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Page Indicator Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) PrimaryBlue else TextMuted.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                )
            }
        }

        // Dock Bar
        DockBar(
            dockApps = dockApps,
            onLaunchApp = onLaunchApp,
            onOpenDrawer = onOpenDrawer
        )
    }
}
