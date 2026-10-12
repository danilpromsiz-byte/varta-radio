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
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                        text = com.safecomm.app.ui.i18n.I18n.voiceHistoryTitle(context),
                        style = MaterialTheme.typography.titleMedium,
                        color = TelegramBlue,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = com.safecomm.app.ui.i18n.I18n.close(context), tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (voiceMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = com.safecomm.app.ui.i18n.I18n.noVoiceMessages(context),
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

                                    Text(
                                        text = msg.createdAt.takeLast(8).take(5), // HH:mm
                                        fontSize = 11.sp,
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
