package com.piuu.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.piuu.launcher.model.*
import com.piuu.launcher.repository.LauncherConfig
import com.piuu.launcher.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenGrid(
    page: LauncherPage,
    config: LauncherConfig,
    installedApps: List<SystemApp>,
    onLaunchApp: (SystemApp) -> Unit,
    onLongPressApp: (SystemApp) -> Unit,
    onLongPressElement: (LauncherElement) -> Unit,
    onElementMoved: (elementId: String, newX: Int, newY: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingElementId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var gridCellWidth by remember { mutableStateOf(120f) }
    var gridCellHeight by remember { mutableStateOf(140f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val cols = config.homeColumns
        val rows = config.homeRows

        gridCellWidth = widthPx / cols
        gridCellHeight = heightPx / rows

        // Underlay: Drag visual grid snapping hints when dragging
        if (draggingElementId != null) {
            CanvasGridHints(cols, rows, gridCellWidth, gridCellHeight)
        }

        val density = LocalDensity.current

        // Workspace elements
        page.elements.forEach { elem ->
            val isDragging = draggingElementId == elem.element_id
            val defaultW = when (elem.type) {
                ElementType.CLOCK, ElementType.WEATHER, ElementType.NOTIFICATION_CENTER -> cols
                else -> 1
            }
            val defaultH = when (elem.type) {
                ElementType.CLOCK, ElementType.WEATHER -> 2
                else -> 1
            }

            val itemW = elem.style_props.w ?: defaultW
            val itemH = elem.style_props.h ?: defaultH
            val gridX = elem.style_props.x ?: 0
            val gridY = elem.style_props.y ?: 0

            val itemWidthDp = with(density) { (itemW * gridCellWidth).toDp() }
            val itemHeightDp = with(density) { (itemH * gridCellHeight).toDp() }

            Box(
                modifier = Modifier
                    .offset {
                        val isCurrentDragging = draggingElementId == elem.element_id
                        val currentX = if (isCurrentDragging) (gridX * gridCellWidth) + dragOffset.x else gridX * gridCellWidth
                        val currentY = if (isCurrentDragging) (gridY * gridCellHeight) + dragOffset.y else gridY * gridCellHeight
                        IntOffset(currentX.roundToInt(), currentY.roundToInt())
                    }
                    .size(itemWidthDp, itemHeightDp)
                    .pointerInput(elem.element_id) {
                        detectDragGestures(
                            onDragStart = {
                                draggingElementId = elem.element_id
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val finalX = (gridX + (dragOffset.x / gridCellWidth)).roundToInt().coerceIn(0, cols - itemW)
                                val finalY = (gridY + (dragOffset.y / gridCellHeight)).roundToInt().coerceIn(0, rows - itemH)
                                onElementMoved(elem.element_id, finalX, finalY)
                                draggingElementId = null
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                draggingElementId = null
                                dragOffset = Offset.Zero
                            }
                        )
                    }
                    .padding(6.dp)
                    .clip(RoundedCornerShape((elem.style_props.borderRadius ?: 16).dp))
                    .background(CardGlassBg.copy(alpha = elem.style_props.opacity ?: 0.85f))
                    .border(
                        width = if (isDragging) 2.dp else 1.dp,
                        color = if (isDragging) PrimaryBlue else LauncherBorder,
                        shape = RoundedCornerShape((elem.style_props.borderRadius ?: 16).dp)
                    )
                    .combinedClickable(
                        onClick = {
                            if (elem.type == ElementType.APP_GRID) {
                                // Launch standard action
                            }
                        },
                        onLongClick = {
                            onLongPressElement(elem)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Drag Indicator overlay for accessibility & styling
                if (isDragging) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Dragging",
                        tint = PrimaryBlue,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(16.dp)
                    )
                }

                // Render dynamic content types based on architecture
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((elem.style_props.padding ?: 12).dp)
                ) {
                    when (elem.type) {
                        ElementType.APP_GRID -> {
                            val targetPackage = elem.action_intent?.target ?: ""
                            val app = installedApps.find { it.package_name == targetPackage }
                            if (app != null) {
                                AppIconItem(
                                    app = app,
                                    iconSize = config.homeIconSize,
                                    onLaunch = { onLaunchApp(app) },
                                    onLongPress = { onLongPressApp(app) }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.AppSettingsAlt, contentDescription = null, tint = TextMuted)
                                }
                            }
                        }
                        ElementType.CLOCK -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                                Text("10:42", fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextPrimary)
                                Text("Saturday, July 25", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        ElementType.WEATHER -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                                Text("72°F", fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextPrimary)
                                Text("Sunny • Palo Alto", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        ElementType.NOTIFICATION_CENTER -> {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                Text("Neural Core", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = PrimaryBlue)
                                Text("No alerts active", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(elem.title ?: elem.type.name, fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.AppIconItem(
    app: SystemApp,
    iconSize: Int,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onLongPress
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AppSettingsAlt,
                contentDescription = app.name,
                tint = PrimaryBlue,
                modifier = Modifier.size((iconSize * 0.6f).dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            fontSize = 10.sp,
            color = TextPrimary,
            maxLines = 1,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Composable
fun CanvasGridHints(cols: Int, rows: Int, cellW: Float, cellH: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0..cols) {
            val x = i * cellW
            drawLine(
                color = PrimaryBlue.copy(alpha = 0.15f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2f
            )
        }
        for (j in 0..rows) {
            val y = j * cellH
            drawLine(
                color = PrimaryBlue.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
        }
    }
}
