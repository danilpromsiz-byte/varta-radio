package com.safecomm.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.i18n.LanguageManager
import com.safecomm.app.ui.theme.*

data class RadiusPreset(val label: String, val radiusKm: Double, val subtitle: String)

val Presets = listOf(
    RadiusPreset("500 м", 0.5, "Рядом"),
    RadiusPreset("2 км", 2.0, "Район"),
    RadiusPreset("5 км", 5.0, "Город"),
    RadiusPreset("10 км", 10.0, "Округ"),
    RadiusPreset("Все", 999.0, "Любое")
)

@Composable
fun SettingsDialog(
    currentRadius: Double,
    soundEnabled: Boolean,
    botToken: String,
    chatId: String,
    groupPassword: String,
    githubRepo: String = "danilpromsiz-byte/varta-radio",
    appVersion: String = "1.2.1",
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
    currentLanguage: AppLanguage = AppLanguage.RU,
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
    var showLegacyTelegramSettings by remember { mutableStateOf(false) }
    var redirectionMessageInput by remember { mutableStateOf(initialRedirectionMessage) }
    var superAdminKeyInput by remember { mutableStateOf("") }
    var isSuperAdminActive by remember { mutableStateOf(isSuperAdmin) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
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
                // 1. LANGUAGE SELECTOR
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
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.flag, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lang.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TelegramBlue else TelegramTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================================================================
                // 2. SOUND & ALERTS RADIUS
                // =====================================================================
                Text(
                    text = "Радиус оповещения опасности:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelegramTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (preset in Presets) {
                        val isSelected = (selectedRadius == preset.radiusKm) ||
                                (preset.radiusKm >= 999.0 && selectedRadius >= 999.0)

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) TelegramBlue else TelegramDivider,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedRadius = preset.radiusKm },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TelegramBlue.copy(alpha = 0.2f) else TelegramDarkCard
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = preset.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TelegramBlue else TelegramTextPrimary
                                )
                                Text(
                                    text = preset.subtitle,
                                    fontSize = 9.sp,
                                    color = TelegramTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sound alerts toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Звуковая сирена", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TelegramTextPrimary)
                            Text("Сигнал тревоги при опасности", fontSize = 11.sp, color = TelegramTextSecondary)
                        }
                        Switch(
                            checked = isSoundOn,
                            onCheckedChange = { isSoundOn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================================================================
                // 3. SECURITY & ROOM SETTINGS
                // =====================================================================
                Text(
                    text = "🔒 ${I18n.storageSection(context)}",
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
                        // Ephemeral messages
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = I18n.autoDeleteToggle(context),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TelegramTextPrimary
                                )
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
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TelegramBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Redirection message for visitors
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
                            placeholder = { Text(I18n.redirectionHint(context), color = TelegramTextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TelegramTextPrimary,
                                unfocusedTextColor = TelegramTextPrimary,
                                focusedBorderColor = AlertFireOrange,
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
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(I18n.saveRedirectionBtn(context), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Banned members section
                        Text(
                            text = "${I18n.bannedMembersSection(context)} (${bannedUsers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TelegramTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (bannedUsers.isEmpty()) {
                            Text(I18n.noBannedUsers(context), fontSize = 11.sp, color = TelegramTextSecondary)
                        } else {
                            bannedUsers.forEach { (userKey, banUntil) ->
                                val isPermanent = (banUntil == -1L || banUntil == Long.MAX_VALUE)
                                val remainingMins = if (!isPermanent) {
                                    ((banUntil - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
                                } else 0L

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(userKey, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TelegramTextPrimary)
                                        Text(
                                            text = if (isPermanent) "Навсегда" else "$remainingMins мин",
                                            fontSize = 10.sp,
                                            color = if (isPermanent) AlertShellingRed else Color(0xFFFFB74D)
                                        )
                                    }
                                    TextButton(onClick = { onUnbanUser?.invoke(userKey) }) {
                                        Text(I18n.unbanBtn(context), fontSize = 11.sp, color = TelegramBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================================================================
                // 4. SUPER ADMIN SECTION
                // =====================================================================
                Text(
                    text = I18n.superAdminSection(context),
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
                        if (isSuperAdminActive) {
                            Text(
                                text = I18n.superAdminStatusActive(context),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = I18n.superAdminDesc(context),
                                fontSize = 11.sp,
                                color = TelegramTextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showWipeConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(I18n.wipeAllRoomsBtn(context), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = "Введите мастер-ключ для активации суперадмина:",
                                fontSize = 11.sp,
                                color = TelegramTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = superAdminKeyInput,
                                    onValueChange = { superAdminKeyInput = it },
                                    placeholder = { Text(I18n.superAdminKeyPlaceholder(context), fontSize = 11.sp, color = TelegramTextSecondary) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.weight(1f),
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
                                    modifier = Modifier.height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(I18n.superAdminActivateBtn(context), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(I18n.save(context), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // =====================================================================
                // 5. SESSION & ROOM ACTIONS
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

                    // Only Chief Admin / Creator (or Superadmin) can delete the room!
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
