package com.piuu.launcher.sdk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.AtomicElement
import com.piuu.launcher.ui.components.AppWidgetHostEngine
import com.piuu.launcher.ui.theme.PrimaryBlue
import com.piuu.launcher.ui.theme.TextPrimary
import com.piuu.launcher.ui.theme.TextSecondary

object WidgetCompiler {
    @Composable
    fun CompileWidget(
        element: AtomicElement,
        systemData: Map<String, Any>,
        onLaunchApp: (String) -> Unit = {}
    ) {
        val bgColor = try {
            Color(android.graphics.Color.parseColor(element.style.backgroundColor)).copy(alpha = element.style.alpha)
        } catch (e: Exception) {
            Color(0xFF1E1E24).copy(alpha = element.style.alpha)
        }

        val borderColor = try {
            Color(android.graphics.Color.parseColor(element.style.borderColor))
        } catch (e: Exception) {
            Color(0xFFA239CA)
        }

        Surface(
            modifier = Modifier
                .padding(element.style.padding.dp)
                .fillMaxSize()
                .clickable {
                    if (element.action.type == "LAUNCH_APP") {
                        onLaunchApp(element.action.target)
                    }
                },
            shape = RoundedCornerShape(element.style.borderRadius.dp),
            color = bgColor,
            border = BorderStroke(element.style.borderWidth.dp, borderColor)
        ) {
            when (element.type) {
                "NATIVE_CUSTOM_WIDGET" -> RenderTemplate(element.dataBinding.templateJson, systemData)
                "APP_ICON" -> RenderIconElement(element)
                "ANDROID_APP_WIDGET" -> {
                    val widgetId = element.elementId.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    NativeWidgetHostWrapper(widgetId)
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = element.elementId,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderTemplate(templateJson: String, systemData: Map<String, Any>) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                val batteryLevel = systemData["battery_pct"] ?: 86
                val isCharging = systemData["is_charging"] as? Boolean ?: false
                
                Text(
                    text = "Battery: $batteryLevel%",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isCharging) "Charging" else "Discharging",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                if (templateJson.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Template Bound",
                        color = PrimaryBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    @Composable
    private fun RenderIconElement(element: AtomicElement) {
        val shape = when (element.iconProperties.maskShape.uppercase()) {
            "CIRCLE" -> CircleShape
            "ROUNDED" -> RoundedCornerShape(12.dp)
            "SQUIRCLE" -> RoundedCornerShape(18.dp)
            else -> RoundedCornerShape(16.dp)
        }

        val labelColor = try {
            Color(android.graphics.Color.parseColor(element.iconProperties.labelColor))
        } catch (e: Exception) {
            Color.White
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(element.iconProperties.size.dp)
                    .clip(shape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size((element.iconProperties.size * 0.6f).dp)
                )
            }

            if (element.iconProperties.showLabel) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = element.elementId.substringAfterLast("."),
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun NativeWidgetHostWrapper(widgetId: Int) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppWidgetHostEngine(appWidgetId = widgetId)
        }
    }
}
