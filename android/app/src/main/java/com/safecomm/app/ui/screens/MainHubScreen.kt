package com.safecomm.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.safecomm.app.data.model.*
import com.safecomm.app.ui.components.*
import com.safecomm.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    group: Group?,
    members: List<GroupMember>,
    alerts: List<Alert>,
    voiceMessages: List<VoiceMessage>,
    userLat: Double,
    userLng: Double,
    isConnected: Boolean,
    isRecordingVoice: Boolean,
    isPlayingVoice: Boolean,
    currentSpeaker: String?,
    amplitude: Float,
    currentRadiusKm: Double,
    soundEnabled: Boolean,
    botToken: String = "",
    chatId: String = "",
    groupPassword: String = "",
    currentUserId: Long,
    capturedPhotoUri: Uri?,
    githubRepo: String = "danilpromsiz-byte/varta-radio",
    appVersion: String = "1.0.0",
    updateStatusText: String = "",
    isUpdateAvailable: Boolean = false,
    isDownloadingUpdate: Boolean = false,
    downloadProgress: Int = 0,
    onCheckUpdate: (String) -> Unit = {},
    onForceUpdate: (String) -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onStartPtt: () -> Unit,
    onStopPtt: () -> Unit,
    onLaunchCamera: () -> Unit,
    onConfirmAlert: (type: AlertType, description: String) -> Unit,
    onConfirmPhotoReport: (type: AlertType, description: String) -> Unit,
    onResolveAlert: (Alert) -> Unit,
    onPlayHistoryVoice: (VoiceMessage) -> Unit,
    onStopHistoryVoice: () -> Unit,
    userAvatar: String = "shield",
    onChangeAvatar: (String) -> Unit = {},
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    onKickMember: (GroupMember) -> Unit,
    onSaveSettings: (radius: Double, sound: Boolean, token: String, chat: String, pass: String) -> Unit,
    onHapticTick: () -> Unit
) {
    var showDangerDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var selectedAlertForDetail by remember { mutableStateOf<Alert?>(null) }

    val isAdmin = members.any { it.userId == currentUserId && it.role == "admin" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User Avatar in Circle (tappable to change avatar)
                        com.safecomm.app.ui.components.UserAvatarView(
                            avatarString = userAvatar,
                            size = 36.dp,
                            borderWidth = 1.5.dp,
                            borderColor = if (isConnected) AlertMedicalGreen else TelegramDivider,
                            modifier = Modifier.clickable { showAvatarDialog = true }
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        // Connection Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) AlertMedicalGreen else AlertShellingRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = group?.name ?: "SafeComm",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = if (isConnected) {
                                    "${members.size} в сети" + (if (isMuted) " • 🔇 Без звука" else " • 🔊 Рация активна")
                                } else "Подключение к сети...",
                                fontSize = 11.sp,
                                color = if (isConnected) (if (isMuted) Color(0xFFFFD600) else TelegramTextSecondary) else AlertShellingRed
                            )
                        }
                    }
                },
                actions = {
                    // Mute / Live Audio Toggle Button
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Включить звук" else "Без звука",
                            tint = if (isMuted) Color(0xFFFF5252) else Color(0xFFFFD600)
                        )
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "История рации", tint = TelegramBlue)
                    }
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = "Участники", tint = TelegramTextSecondary)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Настройки", tint = TelegramTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelegramDarkBg)
            )
        },
        containerColor = TelegramDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- UPPER 40%: Leaflet/OSM Native Map ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
            ) {
                OsmMapView(
                    userLat = userLat,
                    userLng = userLng,
                    alerts = alerts,
                    onAlertClick = { selectedAlertForDetail = it }
                )
            }

            // --- ACTIVE ALERTS BANNER ---
            ActiveAlertsBanner(
                alerts = alerts,
                userLat = userLat,
                userLng = userLng,
                onAlertClick = { selectedAlertForDetail = it }
            )

            // --- LOWER AREA: CONTROLS & ERGONOMICS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.58f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // --- DEDICATED ACTIVE SPEAKER / TRANSMISSION HUD BANNER ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isPlayingVoice -> Color(0xFF263238)
                            isRecordingVoice -> Color(0xFF3E1C1C)
                            else -> TelegramDarkSurface.copy(alpha = 0.8f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeSpeakerAvatar = when {
                                isRecordingVoice -> userAvatar
                                isPlayingVoice -> members.firstOrNull { it.nickname == currentSpeaker || it.nickname.startsWith(currentSpeaker ?: "") }?.avatarUrl ?: "radio"
                                else -> null
                            }

                            if (activeSpeakerAvatar != null) {
                                com.safecomm.app.ui.components.UserAvatarView(
                                    avatarString = activeSpeakerAvatar,
                                    size = 24.dp,
                                    borderWidth = 1.dp,
                                    borderColor = if (isPlayingVoice) AlertFireOrange else AlertShellingRed
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                            Text(
                                text = when {
                                    isPlayingVoice -> "ГОЛОС В ЭФИРЕ: ${currentSpeaker ?: "Собеседник"}"
                                    isRecordingVoice -> "ВЫ В ЭФИРЕ (Передача)"
                                    else -> "Радиоэфир свободен"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = when {
                                    isPlayingVoice -> AlertFireOrange
                                    isRecordingVoice -> AlertShellingRed
                                    else -> TelegramTextSecondary
                                }
                            )
                        }

                        Text(
                            text = if (isPlayingVoice || isRecordingVoice) "🔊 АКТИВНО" else "${members.size} в сети",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPlayingVoice || isRecordingVoice) Color.White else TelegramTextSecondary
                        )
                    }
                }

                // 3D Convex PTT Walkie-Talkie Button
                PttButton(
                    isRecording = isRecordingVoice,
                    amplitude = amplitude,
                    isReceivingVoice = isPlayingVoice,
                    onStartTalk = onStartPtt,
                    onStopTalk = onStopPtt
                )

                // Secondary Emergency Actions (Alarm & Photo)
                AlertActionButtons(
                    onTriggerAlarm = { showDangerDialog = true },
                    onTriggerPhoto = {
                        onLaunchCamera()
                        showPhotoDialog = true
                    }
                )
            }
        }
    }

    // Dialogs & Modals
    if (showDangerDialog) {
        DangerReportDialog(
            onDismiss = { showDangerDialog = false },
            onConfirmAlert = { type, desc ->
                showDangerDialog = false
                onConfirmAlert(type, desc)
            },
            onHapticTick = onHapticTick
        )
    }

    if (showPhotoDialog) {
        PhotoReportDialog(
            photoUri = capturedPhotoUri,
            onLaunchCamera = onLaunchCamera,
            onDismiss = { showPhotoDialog = false },
            onConfirmReport = { type, desc ->
                showPhotoDialog = false
                onConfirmPhotoReport(type, desc)
            },
            onHapticTick = onHapticTick
        )
    }

    if (showHistoryDialog) {
        VoiceHistoryDialog(
            voiceMessages = voiceMessages,
            isPlaying = isPlayingVoice,
            onPlayVoice = onPlayHistoryVoice,
            onStopVoice = onStopHistoryVoice,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showMembersDialog) {
        GroupMembersDialog(
            group = group,
            members = members,
            currentUserId = currentUserId,
            isAdmin = isAdmin,
            onKickMember = onKickMember,
            onDismiss = { showMembersDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentRadius = currentRadiusKm,
            soundEnabled = soundEnabled,
            botToken = botToken,
            chatId = chatId,
            groupPassword = groupPassword,
            githubRepo = githubRepo,
            appVersion = appVersion,
            updateStatusText = updateStatusText,
            isUpdateAvailable = isUpdateAvailable,
            isDownloadingUpdate = isDownloadingUpdate,
            downloadProgress = downloadProgress,
            onCheckUpdate = onCheckUpdate,
            onForceUpdate = onForceUpdate,
            onInstallUpdate = onInstallUpdate,
            onSaveTelegramConfig = { r, s, t, c, p ->
                showSettingsDialog = false
                onSaveSettings(r, s, t, c, p)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    selectedAlertForDetail?.let { alert ->
        AlertDetailDialog(
            alert = alert,
            userLat = userLat,
            userLng = userLng,
            onResolve = {
                selectedAlertForDetail = null
                onResolveAlert(it)
            },
            onDismiss = { selectedAlertForDetail = null }
        )
    }

    if (showAvatarDialog) {
        com.safecomm.app.ui.components.AvatarSelectionDialog(
            currentAvatar = userAvatar,
            onSelectAvatar = {
                showAvatarDialog = false
                onChangeAvatar(it)
            },
            onDismiss = { showAvatarDialog = false }
        )
    }
}
