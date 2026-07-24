package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.ChatMessage
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrainChatSheet(
    visible: Boolean,
    chatMessages: List<ChatMessage>,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    if (!visible) return

    var inputText by remember { mutableStateOf("") }

    val presetPrompts = listOf(
        "Suggest top apps for productivity",
        "How is my battery and system health?",
        "Help me customize launcher theme",
        "What can Piuu Brain do?"
    )

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
            // Header
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
                            .background(AccentPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Piuu Brain AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("GenAI Assistant • Online", fontSize = 11.sp, color = SuccessGreen)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetPrompts) { prompt ->
                    SuggestionChip(
                        onClick = { onSendMessage(prompt) },
                        label = { Text(prompt, fontSize = 11.sp, color = TextPrimary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CardGlassBg)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = false
            ) {
                items(chatMessages) { message ->
                    val isUser = message.sender == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (isUser) 18.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 18.dp
                                    )
                                )
                                .background(if (isUser) PrimaryBlue else CardGlassBg)
                                .border(1.dp, if (isUser) PrimaryBlue else LauncherBorder, RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = message.text,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.timestamp,
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Piuu Brain...", color = TextMuted) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = LauncherBorder,
                        focusedContainerColor = CardGlassBg,
                        unfocusedContainerColor = CardGlassBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    containerColor = AccentPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
}
