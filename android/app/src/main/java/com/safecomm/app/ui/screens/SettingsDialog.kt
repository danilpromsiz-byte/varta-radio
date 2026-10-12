package com.safecomm.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.theme.*

data class RadiusPreset(val label: String, val radiusKm: Double, val subtitle: String)

@Composable
fun SettingsDialog(
    currentRadius: Double,
    soundEnabled: Boolean,
    botToken: String,
    chatId: String,
    groupPassword: String,
    githubRepo: String = "danilpromsiz-byte/varta-radio",
    appVersion: String = "1.2.3",
    updateStatusText: String = "",
    isUpdateAvailable: Boolean = false,
    isDownloadingUpdate: Boolean = false,
    downloadProgress: Int = 0,
    onCheckUpdate: (String) -> Unit = {},
    onForceUpdate: (String) -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    isRoomOwner: Boolean = false,
    isSuperAdmin: Boolean = false,
    onUnlockSuperAdmin: ((String) -> Boolean)? = null,
    onWipeAllRooms: (() -> Unit)? = null,
    currentLanguage: AppLanguage = AppLanguage.EN,
    onLanguageChange: (AppLanguage) -> Unit = {},
    bannedUsers: Map<String, Long> = emptyMap(),
    initialAutoDeleteEnabled: Boolean = true,
    initialTelegramBridgeEnabled: Boolean = false,
    initialRedirectionMessage: String = "",
    onUnbanUser: ((String) -> Unit)? = null,
    onSaveRedirectionMessage: ((String) -> Unit)? = null,
    onChangeRoomPassword: ((String) -> Unit)? = null,
    onToggleAutoDelete: ((Boolean) -> Unit)? = null,
    onToggleTelegramBridge: ((Boolean) -> Unit)? = null,
    onSaveTelegramConfig: (radiusKm: Double, soundEnabled: Boolean, token: String, chat: String, pass: String) -> Unit,
    onLeaveRoom: () -> Unit = {},
    onDeleteRoom: (() -> Unit)? = null,
    onLogOutTelegram: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedRadius by remember { mutableDoubleStateOf(currentRadius) }
    var isSoundOn by remember { mutableStateOf(soundEnabled) }
    var tokenInput by remember { mutableStateOf(botToken) }
    var chatIdInput by remember { mutableStateOf(chatId) }
    var passwordInput by remember { mutableStateOf(groupPassword) }
    var newRoomPasswordInput by remember { mutableStateOf("") }
    var isAutoDeleteEnabled by remember { mutableStateOf(initialAutoDeleteEnabled) }
    var isTelegramBridgeEnabled by remember { mutableStateOf(initialTelegramBridgeEnabled) }
    var redirectionMessageInput by remember { mutableStateOf(initialRedirectionMessage) }

    var isSuperAdminActive by remember { mutableStateOf(isSuperAdmin) }
    var showSuperAdminSecretDialog by remember { mutableStateOf(false) }
    var superAdminKeyInput by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    var easterEggTaps by remember { mutableIntStateOf(0) }
    var lastEasterEggTime by remember { mutableLongStateOf(0L) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ ${I18n.settingsTitle(context)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TelegramTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = I18n.close(context), tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =====================================================================
                // 1. LANGUAGE SELECTOR (Order: EN, UK, RU)
                // =====================================================================
                Text(
                    text = "🌍 ${I18n.languageSection(context)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelegramTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = currentLanguage == lang
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TelegramBlue else TelegramDivider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onLanguageChange(lang) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TelegramBlue.copy(alpha = 0.2f) else TelegramDarkCard
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(lang.flag, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lang.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TelegramTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================================================================
                // 2. APP UPDATES SECTION (Restored & Enhanced)
                // =====================================================================
                Text(
                    text = I18n.updatesSection(context),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelegramTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = I18n.currentVersionLabel(context, appVersion),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelegramTextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TelegramBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GitHub Releases",
                                    fontSize = 10.sp,
                                    color = TelegramBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (updateStatusText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateStatusText,
                                fontSize = 12.sp,
                                color = if (isUpdateAvailable) AlertFireOrange else TelegramTextSecondary
                            )
                        }

                        if (isDownloadingUpdate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = AlertFireOrange,
                                trackColor = TelegramDarkBg
                            )
                            Text(
                                text = I18n.downloadingUpdateStatus(context, downloadProgress),
                                fontSize = 11.sp,
                                color = TelegramTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onCheckUpdate(githubRepo) },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(I18n.checkForUpdatesBtn(context), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (isUpdateAvailable) {
                                Button(
                                    onClick = onInstallUpdate,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(I18n.installUpdateBtn(context), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================================================================
                // 3. AUDIO & LOUDSPEAKER
                // =====================================================================
                Text(
                    text = "🔊 ${I18n.audioSection(context)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelegramTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = I18n.muteSpeakerTitle(context),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = I18n.muteSpeakerDesc(context),
                                    fontSize = 11.sp,
                                    color = TelegramTextSecondary
                                )
                            }
                            Switch(
                                checked = !isSoundOn,
                                onCheckedChange = { isSoundOn = !it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF5252),
                                    checkedTrackColor = Color(0xFFFF5252).copy(alpha = 0.4f),
                                    uncheckedThumbColor = TelegramBlue,
                                    uncheckedTrackColor = TelegramDarkSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================================================================
                // 4. SECURITY & STORAGE (Auto-Delete 24h & Redirection)
                // =====================================================================
                Text(
                    text = "🛡️ ${I18n.storageSection(context)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelegramTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Ephemeral 24h toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = I18n.autoDeleteToggle(context),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = I18n.ephemeralDesc(context),
                                    fontSize = 11.sp,
                                    color = TelegramTextSecondary
                                )
                            }
                            Switch(
                                checked = isAutoDeleteEnabled,
                                onCheckedChange = {
                                    isAutoDeleteEnabled = it
                                    onToggleAutoDelete?.invoke(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TelegramBlue,
                                    checkedTrackColor = TelegramBlue.copy(alpha = 0.4f),
                                    uncheckedThumbColor = TelegramTextSecondary,
                                    uncheckedTrackColor = TelegramDarkSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Redirection Announcement note for empty room visitors
                        Text(
                            text = I18n.redirectionSection(context),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TelegramTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = redirectionMessageInput,
                            onValueChange = { redirectionMessageInput = it },
                            placeholder = { Text(I18n.redirectionHint(context), fontSize = 11.sp, color = TelegramTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TelegramTextPrimary,
                                unfocusedTextColor = TelegramTextPrimary,
                                focusedBorderColor = TelegramBlue,
                                unfocusedBorderColor = TelegramDivider,
                                focusedContainerColor = TelegramDarkSurface,
                                unfocusedContainerColor = TelegramDarkSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                onSaveRedirectionMessage?.invoke(redirectionMessageInput)
                            },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(I18n.saveRedirectionBtn(context), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // =====================================================================
                // 5. BANNED USERS MODERATION (If any exist)
                // =====================================================================
                if (bannedUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = I18n.bannedMembersSection(context),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlertShellingRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            bannedUsers.forEach { (userKey, until) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = userKey, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TelegramTextPrimary)
                                        val untilText = if (until <= 0L) "навсегда" else "до ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(until))}"
                                        Text(text = "Бан $untilText", fontSize = 11.sp, color = TelegramTextSecondary)
                                    }
                                    Button(
                                        onClick = { onUnbanUser?.invoke(userKey) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertMedicalGreen),
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(I18n.unbanBtn(context), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save General Config button
                Button(
                    onClick = {
                        onSaveTelegramConfig(selectedRadius, isSoundOn, tokenInput, chatIdInput, passwordInput)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(I18n.save(context), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =====================================================================
                // 6. ROOM ACTIONS (LEAVE / DELETE)
                // =====================================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onLeaveRoom()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600))
                    ) {
                        Text(I18n.leaveRoomBtn(context), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Only Chief Admin / Creator (or unlocked Superadmin) can delete room
                    if ((isRoomOwner || isSuperAdminActive) && onDeleteRoom != null) {
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed)
                        ) {
                            Text(I18n.deleteRoomBtn(context), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // =====================================================================
                // 7. VERSION FOOTER WITH SECRET 7-TAP EASTER EGG FOR SUPER ADMIN
                // =====================================================================
                Text(
                    text = "Varta GO • v$appVersion",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val now = System.currentTimeMillis()
                            if (now - lastEasterEggTime > 1500L) {
                                easterEggTaps = 1
                            } else {
                                easterEggTaps++
                                if (easterEggTaps >= 7) {
                                    easterEggTaps = 0
                                    showSuperAdminSecretDialog = true
                                }
                            }
                            lastEasterEggTime = now
                        }
                )
            }
        }
    }

    // Modal Confirmation: Delete Room
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(I18n.deleteRoomConfirmTitle(context), fontWeight = FontWeight.Bold, color = AlertShellingRed)
            },
            text = {
                Text(I18n.deleteRoomConfirmText(context), color = TelegramTextPrimary, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDeleteRoom?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed)
                ) {
                    Text(I18n.deleteRoomConfirmAction(context), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(I18n.cancel(context), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }

    // Modal Secret Easter Egg: Super Admin Master Terminal
    if (showSuperAdminSecretDialog) {
        AlertDialog(
            onDismissRequest = { showSuperAdminSecretDialog = false },
            title = {
                Text(I18n.superAdminSection(context), fontWeight = FontWeight.Bold, color = Color(0xFFFFD600))
            },
            text = {
                Column {
                    if (isSuperAdminActive) {
                        Text(
                            text = I18n.superAdminStatusActive(context),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD600)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = I18n.superAdminDesc(context),
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                showSuperAdminSecretDialog = false
                                showWipeConfirmDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(I18n.wipeAllRoomsBtn(context), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = "Введите мастер-пароль суперадмина для активации:",
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = superAdminKeyInput,
                            onValueChange = { superAdminKeyInput = it },
                            placeholder = { Text(I18n.superAdminKeyPlaceholder(context), fontSize = 12.sp, color = TelegramTextSecondary) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TelegramTextPrimary,
                                unfocusedTextColor = TelegramTextPrimary,
                                focusedBorderColor = TelegramBlue,
                                unfocusedBorderColor = TelegramDivider,
                                focusedContainerColor = TelegramDarkCard,
                                unfocusedContainerColor = TelegramDarkCard
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSuperAdminActive) {
                    Button(
                        onClick = {
                            val ok = onUnlockSuperAdmin?.invoke(superAdminKeyInput) ?: false
                            if (ok) {
                                isSuperAdminActive = true
                                Toast.makeText(context, "⚡ Super Admin Activated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid key", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                    ) {
                        Text(I18n.superAdminActivateBtn(context), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuperAdminSecretDialog = false }) {
                    Text(I18n.close(context), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }

    // Modal Confirmation: Wipe ALL rooms
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = {
                Text(I18n.wipeAllRoomsConfirmTitle(context), fontWeight = FontWeight.Bold, color = AlertShellingRed)
            },
            text = {
                Text(I18n.wipeAllRoomsConfirmText(context), color = TelegramTextPrimary, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirmDialog = false
                        onWipeAllRooms?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed)
                ) {
                    Text(I18n.wipeAllRoomsConfirmAction(context), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text(I18n.cancel(context), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }
}
