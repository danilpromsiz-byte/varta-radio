package com.safecomm.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    appVersion: String = "1.0.0",
    updateStatusText: String = "",
    isUpdateAvailable: Boolean = false,
    isDownloadingUpdate: Boolean = false,
    downloadProgress: Int = 0,
    onCheckUpdate: (String) -> Unit = {},
    onForceUpdate: (String) -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onSaveTelegramConfig: (radiusKm: Double, soundEnabled: Boolean, token: String, chat: String, pass: String) -> Unit,
    onLeaveRoom: () -> Unit = {},
    onLogOutTelegram: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedRadius by remember { mutableDoubleStateOf(currentRadius) }
    var isSoundOn by remember { mutableStateOf(soundEnabled) }
    var tokenInput by remember { mutableStateOf(botToken) }
    var chatIdInput by remember { mutableStateOf(chatId) }
    var passwordInput by remember { mutableStateOf(groupPassword) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
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
                        text = "⚙️ НАСТРОЙКИ СВЯЗИ",
                        style = MaterialTheme.typography.titleMedium,
                        color = TelegramTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Радиус звукового оповещения опасности:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelegramTextPrimary
                )
                Text(
                    text = "Звуковая сирена сработает, если опасность находится в пределах:",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets row
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
                                .height(56.dp)
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
                                    fontSize = 12.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

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
                        Column {
                            Text("Звуковая сирена", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TelegramTextPrimary)
                            Text("Громкий сигнал тревоги при опасности рядом", fontSize = 11.sp, color = TelegramTextSecondary)
                        }
                        Switch(
                            checked = isSoundOn,
                            onCheckedChange = { isSoundOn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue,
                                uncheckedThumbColor = TelegramTextSecondary,
                                uncheckedTrackColor = TelegramDarkSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ИНТЕГРАЦИЯ С ТЕЛЕГРАМ (БЕЗ СЕРВЕРА):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Telegram Bot Token
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it.trim() },
                    label = { Text("Токен бота Telegram (от @BotFather)", color = TelegramTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("123456789:ABCDefGhIJKlmNoPQR", color = TelegramTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chat ID
                OutlinedTextField(
                    value = chatIdInput,
                    onValueChange = { chatIdInput = it.trim() },
                    label = { Text("ID группы/канала (напр. -1001234567890)", color = TelegramTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Пароль закрытого сообщества", color = TelegramTextSecondary, fontSize = 12.sp) },
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
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // --- GITHUB AUTO-UPDATE SECTION ---
                Text(
                    text = "🚀 АВТООБНОВЛЕНИЕ С GITHUB",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )
                Text(
                    text = "Текущая версия приложения: v$appVersion\nПроверка релизов на GitHub напрямую без Google Play",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                var repoInput by remember { mutableStateOf(githubRepo) }
                OutlinedTextField(
                    value = repoInput,
                    onValueChange = { repoInput = it.trim() },
                    label = { Text("GitHub репозиторий (owner/repo)", color = TelegramTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("owner/varta-radio", color = TelegramTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (updateStatusText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateStatusText,
                        fontSize = 12.sp,
                        color = when {
                            isUpdateAvailable -> AlertFireOrange
                            updateStatusText.contains("⚠️") -> AlertShellingRed
                            updateStatusText.contains("✅") -> Color(0xFF4CAF50)
                            else -> TelegramTextSecondary
                        },
                        fontWeight = if (isUpdateAvailable || updateStatusText.contains("⚠️") || updateStatusText.contains("✅")) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (isDownloadingUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = TelegramBlue
                    )
                    Text(
                        text = "Загрузка обновления: $downloadProgress%",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onCheckUpdate(repoInput) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔍 Проверить обновления", fontSize = 13.sp)
                }

                if (isUpdateAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onInstallUpdate,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📦 Скачать и установить обновление", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSaveTelegramConfig(selectedRadius, isSoundOn, tokenInput, chatIdInput, passwordInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить и подключиться", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- SESSION & ROOM MANAGEMENT ---
                Text(
                    text = "🚪 УПРАВЛЕНИЕ СЕССИЕЙ И КОМНАТОЙ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onLeaveRoom()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600))
                    ) {
                        Text("🔄 Сменить комнату", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onLogOutTelegram()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("Выйти из Telegram", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
