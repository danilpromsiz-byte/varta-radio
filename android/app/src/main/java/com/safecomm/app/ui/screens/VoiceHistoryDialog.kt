package com.safecomm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.data.model.VoiceMessage
import com.safecomm.app.ui.theme.*

@Composable
fun VoiceHistoryDialog(
    voiceMessages: List<VoiceMessage>,
    baseUrl: String = "",
    isPlaying: Boolean,
    onPlayVoice: (VoiceMessage) -> Unit,
    onStopVoice: () -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    var currentlyPlayingId by remember { mutableStateOf<Long?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = com.safecomm.app.ui.i18n.I18n.voiceHistoryTitle(currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        color = TelegramBlue,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = com.safecomm.app.ui.i18n.I18n.close(currentLanguage),
                            tint = TelegramTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (voiceMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = com.safecomm.app.ui.i18n.I18n.noVoiceMessages(currentLanguage),
                            color = TelegramTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(voiceMessages, key = { it.id }) { msg ->
                            val isThisPlaying = isPlaying && currentlyPlayingId == msg.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = TelegramDarkCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play/Stop Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isThisPlaying) AlertShellingRed else TelegramBlue)
                                            .clickable {
                                                if (isThisPlaying) {
                                                    onStopVoice()
                                                    currentlyPlayingId = null
                                                } else {
                                                    currentlyPlayingId = msg.id
                                                    onPlayVoice(msg)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = msg.senderNick.ifBlank { "User #${msg.senderId}" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TelegramTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${msg.durationSec}s",
                                            fontSize = 12.sp,
                                            color = TelegramTextSecondary
                                        )
                                    }

                                    val displayTime = remember(msg.createdAt) {
                                        val raw = msg.createdAt.trim()
                                        when {
                                            raw.isEmpty() || raw == "Только что" -> {
                                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                            }
                                            raw.length <= 5 && raw.contains(":") -> raw
                                            raw.contains("T") -> {
                                                try {
                                                    raw.substringAfter("T").take(5)
                                                } catch (_: Exception) {
                                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                }
                                            }
                                            raw.contains(":") -> {
                                                try {
                                                    val parts = raw.split(":")
                                                    if (parts.size >= 2) {
                                                        val h = parts[0].takeLast(2)
                                                        val m = parts[1].take(2)
                                                        "$h:$m"
                                                    } else raw
                                                } catch (_: Exception) {
                                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                }
                                            }
                                            else -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                        }
                                    }

                                    Text(
                                        text = displayTime,
                                        fontSize = 12.sp,
                                        color = TelegramTextSecondary
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
