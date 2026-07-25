package com.piuu.launcher.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.piuu.launcher.model.LauncherSchema
import com.piuu.launcher.model.LauncherTheme
import com.piuu.launcher.repository.SchemeManager
import com.piuu.launcher.repository.SchemeConfig
import com.piuu.launcher.ui.theme.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemaEditorModal(
    visible: Boolean,
    schema: LauncherSchema,
    onDismiss: () -> Unit,
    onSaveSchema: (LauncherSchema) -> Unit,
    onResetSchema: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val prettyGson = remember { GsonBuilder().setPrettyPrinting().create() }
    var jsonText by remember(schema) { mutableStateOf(prettyGson.toJson(schema)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Tab Selection: 0 = Raw IDE, 1 = SchemeStore Customizer
    var activeTab by remember { mutableStateOf(0) }

    // Scheme Customizer States
    var selectedCategory by remember { mutableStateOf("themes") }
    val categories = listOf("themes", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")
    
    val schemeStore = remember { SchemeManager.getInstance(context) }
    var schemesList by remember(selectedCategory) { 
        mutableStateOf(schemeStore.getSchemesForCategory(selectedCategory)) 
    }
    
    var selectedScheme by remember { mutableStateOf<SchemeConfig?>(null) }
    
    // Customization editing fields
    var editId by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var editAuthor by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    
    // Theme-specific fields
    var themePrimary by remember { mutableStateOf("#3B82F6") }
    var themeAccent by remember { mutableStateOf("#8B5CF6") }
    var themeBgOverlay by remember { mutableStateOf("rgba(2, 8, 23, 0.85)") }
    
    // Category specific generic param field (font family, layout name, etc.)
    var genericParam by remember { mutableStateOf("") }

    // Load fields when a scheme is selected to customize
    LaunchedEffect(selectedScheme) {
        selectedScheme?.let { scheme ->
            editId = scheme.id + "_custom"
            editName = scheme.name + " (Custom)"
            editAuthor = "Piuu Customizer"
            editDesc = "User customized version of ${scheme.name}"
            
            try {
                val json = JSONObject(scheme.payload)
                if (selectedCategory == "themes") {
                    themePrimary = json.optString("primary_color", "#3B82F6")
                    themeAccent = json.optString("accent_color", "#8B5CF6")
                    themeBgOverlay = json.optString("bg_overlay", "rgba(2, 8, 23, 0.85)")
                } else {
                    genericParam = when (selectedCategory) {
                        "fonts" -> json.optString("font_family", "Inter")
                        "layouts" -> json.optString("layout_name", "categorized")
                        "faces" -> json.optString("face_name", "dashboard")
                        "widgets" -> json.optString("widget_name", "battery")
                        "icons" -> json.optString("icon_pack_name", "default")
                        "agents" -> json.optString("agent_name", "briefing")
                        "skills" -> json.optString("skill_name", "coder")
                        else -> ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = WarningAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Piuu Schema IDE & Store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Declarative IDE", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("SchemeStore Customizer", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == 0) {
                // Declarative JSON IDE View
                Text("Live Declarative JSON Launcher Schema", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val parsed = prettyGson.fromJson(jsonText, LauncherSchema::class.java)
                                onSaveSchema(parsed)
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "JSON Error: ${e.localizedMessage}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Schema")
                    }

                    OutlinedButton(
                        onClick = onResetSchema,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default")
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = DangerRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text editor
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = WarningAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF090D16), RoundedCornerShape(14.dp))
                        .border(1.dp, LauncherBorder, RoundedCornerShape(14.dp))
                )
            } else {
                // SchemeStore Customizer View
                Text("Select marketplace category subdirectories and customize pre-built schemes", fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))

                // Subdirectory Category selector
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp
                ) {
                    categories.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { 
                                selectedCategory = cat 
                                selectedScheme = null
                            },
                            text = { Text(cat.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxSize()) {
                    // Schemes Selector Pane (Left)
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                    ) {
                        Text("Subdirectory: SchemeStore/$selectedCategory", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(schemesList, key = { it.id }) { scheme ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedScheme?.id == scheme.id) CardGlassBg else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp, 
                                            if (selectedScheme?.id == scheme.id) PrimaryBlue else LauncherBorder.copy(alpha = 0.5f), 
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedScheme = scheme }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(scheme.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("By ${scheme.author}", fontSize = 9.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }

                    // Customize panel (Right)
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .background(Color(0xFF090D16), RoundedCornerShape(14.dp))
                            .border(1.dp, LauncherBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        if (selectedScheme == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Select a scheme\nto customize", fontSize = 12.sp, color = TextMuted, modifier = Modifier.align(Alignment.Center))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text("Customizer parameters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                item {
                                    OutlinedTextField(
                                        value = editId,
                                        onValueChange = { editId = it },
                                        label = { Text("Scheme ID", fontSize = 11.sp) },
                                        textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Display Name", fontSize = 11.sp) },
                                        textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = editAuthor,
                                        onValueChange = { editAuthor = it },
                                        label = { Text("Creator/Author", fontSize = 11.sp) },
                                        textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = editDesc,
                                        onValueChange = { editDesc = it },
                                        label = { Text("Description", fontSize = 11.sp) },
                                        textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (selectedCategory == "themes") {
                                    item {
                                        OutlinedTextField(
                                            value = themePrimary,
                                            onValueChange = { themePrimary = it },
                                            label = { Text("Primary Color", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    item {
                                        OutlinedTextField(
                                            value = themeAccent,
                                            onValueChange = { themeAccent = it },
                                            label = { Text("Accent Color", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    item {
                                        OutlinedTextField(
                                            value = themeBgOverlay,
                                            onValueChange = { themeBgOverlay = it },
                                            label = { Text("Background Overlay", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    item {
                                        val paramLabel = when (selectedCategory) {
                                            "fonts" -> "Font Family"
                                            "layouts" -> "Layout Name"
                                            "faces" -> "Face Name"
                                            "widgets" -> "Widget Name"
                                            "icons" -> "Icon Pack Name"
                                            "agents" -> "Agent Name"
                                            "skills" -> "Skill Name"
                                            else -> "Defined Parameter Value"
                                        }
                                        OutlinedTextField(
                                            value = genericParam,
                                            onValueChange = { genericParam = it },
                                            label = { Text(paramLabel, fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val payloadJson = JSONObject()
                                            payloadJson.put("id", editId)
                                            payloadJson.put("name", editName)
                                            payloadJson.put("author", editAuthor)
                                            payloadJson.put("description", editDesc)
                                            
                                            if (selectedCategory == "themes") {
                                                payloadJson.put("primary_color", themePrimary)
                                                payloadJson.put("accent_color", themeAccent)
                                                payloadJson.put("bg_overlay", themeBgOverlay)
                                                payloadJson.put("font_family", "Inter")
                                            } else {
                                                val key = when (selectedCategory) {
                                                    "fonts" -> "font_family"
                                                    "layouts" -> "layout_name"
                                                    "faces" -> "face_name"
                                                    "widgets" -> "widget_name"
                                                    "icons" -> "icon_pack_name"
                                                    "agents" -> "agent_name"
                                                    "skills" -> "skill_name"
                                                    else -> "value"
                                                }
                                                payloadJson.put(key, genericParam)
                                            }

                                            val success = schemeStore.saveCustomizedScheme(
                                                selectedCategory,
                                                editId,
                                                payloadJson.toString()
                                            )
                                            
                                            if (success) {
                                                Toast.makeText(context, "Saved custom scheme SchemeStore/$selectedCategory/$editId.json", Toast.LENGTH_LONG).show()
                                                schemesList = schemeStore.getSchemesForCategory(selectedCategory)
                                            } else {
                                                Toast.makeText(context, "Failed to save scheme", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text("Save Scheme", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val currentTheme = schema.theme
                                            var updatedTheme = currentTheme
                                            var updatedSchema = schema

                                            when (selectedCategory) {
                                                "themes" -> {
                                                    updatedTheme = currentTheme.copy(
                                                        id = editId,
                                                        name = editName,
                                                        primary_color = themePrimary,
                                                        accent_color = themeAccent,
                                                        bg_overlay = themeBgOverlay
                                                    )
                                                    updatedSchema = schema.copy(theme = updatedTheme)
                                                }
                                                "fonts" -> {
                                                    updatedTheme = currentTheme.copy(font_family = genericParam)
                                                    updatedSchema = schema.copy(theme = updatedTheme, active_font = genericParam)
                                                }
                                                "layouts" -> {
                                                    updatedSchema = schema.copy(drawer_layout = genericParam)
                                                }
                                                "icons" -> {
                                                    updatedSchema = schema.copy(active_icon_pack = genericParam)
                                                }
                                                else -> {
                                                    val plugins = schema.installed_plugins.toMutableList()
                                                    if (!plugins.contains(editId)) {
                                                        plugins.add(editId)
                                                    }
                                                    updatedSchema = schema.copy(installed_plugins = plugins)
                                                }
                                            }

                                            onSaveSchema(updatedSchema)
                                            Toast.makeText(context, "Applied Customized Scheme: $editName", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Apply Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text("Apply Scheme", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
