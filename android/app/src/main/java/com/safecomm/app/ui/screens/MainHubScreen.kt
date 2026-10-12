package com.safecomm.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.data.model.*
import com.safecomm.app.ui.components.*
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.theme.*

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
    appVersion: String = "1.2.3",
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
    onConfirmAlert: (type: AlertType, description: String, durationMinutes: Int) -> Unit,
    onConfirmPhotoReport: (type: AlertType, description: String) -> Unit,
    onResolveAlert: (Alert) -> Unit,
    onPlayHistoryVoice: (VoiceMessage) -> Unit,
    onStopHistoryVoice: () -> Unit,
    userAvatar: String = "shield",
    onChangeAvatar: (String) -> Unit = {},
    audioMode: AudioMode = AudioMode.SOUND,
    onToggleAudioMode: () -> Unit = {},
    onKickMember: (GroupMember) -> Unit,
    onBanMember: (GroupMember, Long) -> Unit = { _, _ -> },
    onDelegateAdmin: (GroupMember, Boolean) -> Unit = { _, _ -> },
    isSuperAdmin: Boolean = false,
    onUnlockSuperAdmin: ((String) -> Boolean)? = null,
    onWipeAllRooms: (() -> Unit)? = null,
    currentLanguage: AppLanguage = AppLanguage.EN,
    onLanguageChange: (AppLanguage) -> Unit = {},
    bannedUsers: Map<String, Long> = emptyMap(),
    onUnbanUser: ((String) -> Unit)? = null,
    onSaveSettings: (radius: Double, sound: Boolean, token: String, chat: String, pass: String) -> Unit,
    isRoomOwner: Boolean = false,
    initialAutoDeleteEnabled: Boolean = true,
    initialTelegramBridgeEnabled: Boolean = false,
    initialRedirectionMessage: String = "",
    onSaveRedirectionMessage: ((String) -> Unit)? = null,
    onChangeRoomPassword: ((String) -> Unit)? = null,
    onToggleAutoDelete: ((Boolean) -> Unit)? = null,
    onToggleTelegramBridge: ((Boolean) -> Unit)? = null,
    onLeaveRoom: () -> Unit = {},
    onDeleteRoom: (() -> Unit)? = null,
    onLogOutTelegram: () -> Unit = {},
    onHapticTick: () -> Unit
) {
    val context = LocalContext.current

    var showDangerDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showLeaveRoomConfirm by remember { mutableStateOf(false) }
    var selectedAlertForDetail by remember { mutableStateOf<Alert?>(null) }

    val isChiefAdmin = isRoomOwner || isSuperAdmin
    val isCurrentAdmin = isChiefAdmin || members.any {
        (it.userId == currentUserId || it.nickname.contains("(Вы)") || it.nickname.contains("(Ви)") || it.nickname.contains("(You)")) &&
        (it.role == "admin" || it.role == "owner")
    }

    // =========================================================================
    // ROOT FULLSCREEN CONTAINER (WAZE STYLE)
    // Map is the base layer underneath all tactical controls.
    // =========================================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TelegramDarkBg)
    ) {
        // --- 1. BASE FULLSCREEN NATIVE OSM MAP ---
        OsmMapView(
            userLat = userLat,
            userLng = userLng,
            alerts = alerts,
            onAlertClick = { selectedAlertForDetail = it },
            currentLanguage = currentLanguage,
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. ACTIVE ALERTS FLOATING BANNER (Under Top Bar) ---
        if (alerts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp, start = 14.dp, end = 14.dp)
            ) {
                ActiveAlertsBanner(
                    alerts = alerts,
                    userLat = userLat,
                    userLng = userLng,
                    onAlertClick = { selectedAlertForDetail = it }
                )
            }
        }

        // --- 3. TOP FLOATING WAZE TACTICAL CAPSULE ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 32.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = TelegramDarkSurface.copy(alpha = 0.68f),
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, TelegramDivider.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Avatar + Room Info & Quick Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLeaveRoomConfirm = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        com.safecomm.app.ui.components.UserAvatarView(
                            avatarString = userAvatar,
                            size = 38.dp,
                            borderWidth = 1.5.dp,
                            borderColor = if (isConnected) AlertMedicalGreen else AlertShellingRed,
                            modifier = Modifier.clickable { showAvatarDialog = true }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group?.name ?: I18n.appTitle(currentLanguage),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = I18n.switchRoomTooltip(currentLanguage),
                                    tint = Color(0xFFFFD600),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            val audioStatusText = when (audioMode) {
                                AudioMode.SOUND -> I18n.audioModeSound(currentLanguage)
                                AudioMode.VIBRATE -> I18n.audioModeVibrate(currentLanguage)
                                AudioMode.SILENT -> I18n.audioModeSilent(currentLanguage)
                            }
                            Text(
                                text = if (isConnected) {
                                    "${members.size} " + I18n.online(currentLanguage) + " • " + audioStatusText
                                } else I18n.connecting(currentLanguage),
                                fontSize = 11.sp,
                                color = if (isConnected) {
                                    when (audioMode) {
                                        AudioMode.SOUND -> Color(0xFFFFD600)
                                        AudioMode.VIBRATE -> Color(0xFF64B5F6)
                                        AudioMode.SILENT -> Color(0xFFFF5252)
                                    }
                                } else AlertShellingRed
                            )
                        }
                    }

                    // Right: Quick Tactical Actions (Audio Mode, Radio History, Members, Settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 3-State Audio Mode Toggle Button (Sound 🔊 / Vibrate 📳 / Silent 🔇)
                        val audioStatusDesc = when (audioMode) {
                            AudioMode.SOUND -> I18n.audioModeSound(currentLanguage)
                            AudioMode.VIBRATE -> I18n.audioModeVibrate(currentLanguage)
                            AudioMode.SILENT -> I18n.audioModeSilent(currentLanguage)
                        }
                        IconButton(
                            onClick = onToggleAudioMode,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = when (audioMode) {
                                    AudioMode.SOUND -> Icons.Default.VolumeUp
                                    AudioMode.VIBRATE -> Icons.Default.Vibration
                                    AudioMode.SILENT -> Icons.Default.VolumeOff
                                },
                                contentDescription = audioStatusDesc,
                                tint = when (audioMode) {
                                    AudioMode.SOUND -> Color(0xFFFFD600)
                                    AudioMode.VIBRATE -> Color(0xFF64B5F6)
                                    AudioMode.SILENT -> Color(0xFFFF5252)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Voice History Button
                        IconButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = I18n.voiceHistoryTooltip(currentLanguage),
                                tint = TelegramBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Group Members Button
                        IconButton(
                            onClick = { showMembersDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = I18n.membersTooltip(currentLanguage),
                                tint = TelegramTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = I18n.settingsTooltip(currentLanguage),
                                tint = TelegramTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 4. BOTTOM FLOATING WAZE OPERATIONAL DOCK ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Speaker HUD Floating Capsule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isPlayingVoice -> Color(0xFF263238).copy(alpha = 0.95f)
                    isRecordingVoice -> Color(0xFF3E1C1C).copy(alpha = 0.95f)
                    else -> TelegramDarkSurface.copy(alpha = 0.92f)
                },
                border = BorderStroke(1.dp, TelegramDivider.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
                            size = 20.dp,
                            borderWidth = 1.dp,
                            borderColor = if (isPlayingVoice) AlertFireOrange else AlertShellingRed
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }

                    Text(
                        text = when {
                            isPlayingVoice -> I18n.activeSpeakerVoice(currentLanguage, currentSpeaker ?: "User")
                            isRecordingVoice -> I18n.youAreTransmitting(currentLanguage)
                            else -> I18n.channelClear(currentLanguage)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = when {
                            isPlayingVoice -> AlertFireOrange
                            isRecordingVoice -> AlertShellingRed
                            else -> TelegramTextSecondary
                        }
                    )

                    if (isPlayingVoice || isRecordingVoice) {
                        Text(
                            text = I18n.activeSpeakerBadge(currentLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
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

            // Waze-Style Tactical Action Buttons (Never truncating)
            AlertActionButtons(
                onTriggerAlarm = { showDangerDialog = true },
                onTriggerPhoto = {
                    onLaunchCamera()
                    showPhotoDialog = true
                },
                currentLanguage = currentLanguage
            )
        }
    }

    // =========================================================================
    // MODAL DIALOGS
    // =========================================================================
    if (showDangerDialog) {
        DangerReportDialog(
            onDismiss = { showDangerDialog = false },
            onConfirmAlert = { type, desc, dur ->
                showDangerDialog = false
                onConfirmAlert(type, desc, dur)
            },
            onHapticTick = onHapticTick,
            currentLanguage = currentLanguage
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
            onHapticTick = onHapticTick,
            currentLanguage = currentLanguage
        )
    }

    if (showHistoryDialog) {
        VoiceHistoryDialog(
            voiceMessages = voiceMessages,
            isPlaying = isPlayingVoice,
            onPlayVoice = onPlayHistoryVoice,
            onStopVoice = onStopHistoryVoice,
            onDismiss = { showHistoryDialog = false },
            currentLanguage = currentLanguage
        )
    }

    if (showMembersDialog) {
        GroupMembersDialog(
            group = group,
            members = members,
            currentUserId = currentUserId,
            isChiefAdmin = isChiefAdmin,
            isAdmin = isCurrentAdmin,
            currentLanguage = currentLanguage,
            onDelegateAdmin = onDelegateAdmin,
            onKickMember = onKickMember,
            onBanMember = onBanMember,
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
            isRoomOwner = isChiefAdmin,
            isSuperAdmin = isSuperAdmin,
            onUnlockSuperAdmin = onUnlockSuperAdmin,
            onWipeAllRooms = onWipeAllRooms,
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            bannedUsers = bannedUsers,
            initialAutoDeleteEnabled = initialAutoDeleteEnabled,
            initialTelegramBridgeEnabled = initialTelegramBridgeEnabled,
            initialRedirectionMessage = initialRedirectionMessage,
            onUnbanUser = onUnbanUser,
            onSaveRedirectionMessage = onSaveRedirectionMessage,
            onChangeRoomPassword = onChangeRoomPassword,
            onToggleAutoDelete = onToggleAutoDelete,
            onToggleTelegramBridge = onToggleTelegramBridge,
            onSaveTelegramConfig = { r, s, t, c, p ->
                showSettingsDialog = false
                onSaveSettings(r, s, t, c, p)
            },
            onLeaveRoom = onLeaveRoom,
            onDeleteRoom = onDeleteRoom,
            onLogOutTelegram = onLogOutTelegram,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showLeaveRoomConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveRoomConfirm = false },
            title = {
                Text(
                    text = I18n.switchRoomConfirmTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )
            },
            text = {
                Text(
                    text = I18n.switchRoomConfirmDesc(currentLanguage, group?.name ?: I18n.appTitle(currentLanguage)),
                    color = TelegramTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveRoomConfirm = false
                        onLeaveRoom()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange)
                ) {
                    Text(I18n.switchRoomConfirmAction(currentLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveRoomConfirm = false }) {
                    Text(I18n.cancel(currentLanguage), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
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
            onDismiss = { selectedAlertForDetail = null },
            currentLanguage = currentLanguage
        )
    }

    if (showAvatarDialog) {
        com.safecomm.app.ui.components.AvatarSelectionDialog(
            currentAvatar = userAvatar,
            onSelectAvatar = {
                showAvatarDialog = false
                onChangeAvatar(it)
            },
            onDismiss = { showAvatarDialog = false },
            currentLanguage = currentLanguage
        )
    }
}
