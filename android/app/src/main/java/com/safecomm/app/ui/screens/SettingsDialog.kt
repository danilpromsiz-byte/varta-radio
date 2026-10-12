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
    isRoomOwner: Boolean = false,
    initialVerificationEnabled: Boolean = true,
    initialVerificationQuestion: String = "",
    initialAutoDeleteEnabled: Boolean = true,
    initialTelegramBridgeEnabled: Boolean = false,
    initialRedirectionMessage: String = "",
    onSaveRoomModeration: ((enabled: Boolean, question: String) -> Unit)? = null,
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
    var selectedRadius by remember { mutableDoubleStateOf(currentRadius) }
    var isSoundOn by remember { mutableStateOf(soundEnabled) }
    var tokenInput by remember { mutableStateOf(botToken) }
    var chatIdInput by remember { mutableStateOf(chatId) }
    var passwordInput by remember { mutableStateOf(groupPassword) }
    var isVerificationEnabled by remember { mutableStateOf(initialVerificationEnabled) }
    var verificationQuestionInput by remember { mutableStateOf(initialVerificationQuestion) }
    var newRoomPasswordInput by remember { mutableStateOf("") }
    var isAutoDeleteEnabled by remember { mutableStateOf(initialAutoDeleteEnabled) }
    var isTelegramBridgeEnabled by remember { mutableStateOf(initialTelegramBridgeEnabled) }
    var showLegacyTelegramSettings by remember { mutableStateOf(false) }
    var redirectionMessageInput by remember { mutableStateOf(initialRedirectionMessage) }

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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLegacyTelegramSettings = !showLegacyTelegramSettings },
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤖 Прямой бот Telegram (опционально)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextSecondary
                            )
                            Text(
                                text = if (showLegacyTelegramSettings) "▲" else "▼",
                                fontSize = 12.sp,
                                color = TelegramTextSecondary
                            )
                        }

                        if (showLegacyTelegramSettings) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Токен бота и ID группы для прямого резервного опроса без TDLib:",
                                fontSize = 10.sp,
                                color = TelegramTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it.trim() },
                                label = { Text("Токен бота Telegram (@BotFather)", color = TelegramTextSecondary, fontSize = 11.sp) },
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

                            OutlinedTextField(
                                value = chatIdInput,
                                onValueChange = { chatIdInput = it.trim() },
                                label = { Text("ID группы/канала (-100...)", color = TelegramTextSecondary, fontSize = 11.sp) },
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

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Пароль сообщества", color = TelegramTextSecondary, fontSize = 11.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- ROOM SETTINGS & SECURITY (FOR OWNER) ---
                if (isRoomOwner || onSaveRoomModeration != null) {
                    Text(
                        text = "🛡️ НАСТРОЙКИ КОМНАТЫ (СОЗДАТЕЛЬ)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramTextPrimary
                    )
                    Text(
                        text = "Управление безопасностью, временными сообщениями и шлюзом Telegram.",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Смена пароля комнаты
                    Text(
                        text = "🔑 Смена пароля комнаты",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TelegramTextPrimary
                    )
                    OutlinedTextField(
                        value = newRoomPasswordInput,
                        onValueChange = { newRoomPasswordInput = it },
                        label = { Text("Новый секретный пароль", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Введите новый пароль", color = TelegramTextSecondary) },
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

                    if (newRoomPasswordInput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                onChangeRoomPassword?.invoke(newRoomPasswordInput)
                                newRoomPasswordInput = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600))
                        ) {
                            Text("🔑 Сохранить новый пароль", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Временные сообщения (Автоудаление 24ч) - по умолчанию ВКЛЮЧЕНО
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "⏳ Временные сообщения (24 часа)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = if (isAutoDeleteEnabled) "Включено: автоудаление через 24 часа" else "Выключено: история бессрочна",
                                fontSize = 11.sp,
                                color = if (isAutoDeleteEnabled) Color(0xFF4CAF50) else TelegramTextSecondary
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

                    // 3. Трансляция в Telegram (Закрытый канал) - по умолчанию ВЫКЛЮЧЕНО
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "✈️ Трансляция в Telegram",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = if (isTelegramBridgeEnabled) "Включено: канал виден в Telegram" else "Выключено: Dark Room — общение только в Varta GO",
                                fontSize = 11.sp,
                                color = if (isTelegramBridgeEnabled) TelegramBlue else Color(0xFFFFD600)
                            )
                        }
                        Switch(
                            checked = isTelegramBridgeEnabled,
                            onCheckedChange = {
                                isTelegramBridgeEnabled = it
                                onToggleTelegramBridge?.invoke(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Проверочный вопрос
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Проверочный вопрос для гостей",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = if (isVerificationEnabled) "Включён: ручной допуск создателем" else "Выключен: вход только по паролю",
                                fontSize = 11.sp,
                                color = if (isVerificationEnabled) Color(0xFFFFD600) else TelegramTextSecondary
                            )
                        }
                        Switch(
                            checked = isVerificationEnabled,
                            onCheckedChange = { isVerificationEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue
                            )
                        )
                    }

                    if (isVerificationEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = verificationQuestionInput,
                            onValueChange = { verificationQuestionInput = it },
                            label = { Text("Вопрос создателя для гостей *", color = TelegramTextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("Например: Ваш позывной или подразделение?", color = TelegramTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TelegramTextPrimary,
                                unfocusedTextColor = TelegramTextPrimary,
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = TelegramDivider,
                                focusedContainerColor = TelegramDarkCard,
                                unfocusedContainerColor = TelegramDarkCard
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            onSaveRoomModeration?.invoke(isVerificationEnabled, verificationQuestionInput)
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Text("💾 Применить настройки модерации", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Записка / перенаправление для гостей
                    Text(
                        text = "📢 Записка для гостей (если вышли / перешли)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TelegramTextPrimary
                    )
                    Text(
                        text = "Увидят те, кто стучится в комнату (например: «Мы перешли в комнату 24»)",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = redirectionMessageInput,
                        onValueChange = { redirectionMessageInput = it },
                        label = { Text("Сообщение для стучащихся в комнату", color = TelegramTextSecondary, fontSize = 11.sp) },
                        placeholder = { Text("Например: Мы перешли в комнату 24, пароль тот же", color = TelegramTextSecondary) },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TelegramTextPrimary,
                            unfocusedTextColor = TelegramTextPrimary,
                            focusedBorderColor = AlertFireOrange,
                            unfocusedBorderColor = TelegramDivider,
                            focusedContainerColor = TelegramDarkCard,
                            unfocusedContainerColor = TelegramDarkCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (redirectionMessageInput.trim() != initialRedirectionMessage.trim()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                onSaveRedirectionMessage?.invoke(redirectionMessageInput.trim())
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertFireOrange)
                        ) {
                            Text("📢 Сохранить записку для гостей", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // --- GITHUB AUTO-UPDATE SECTION ---
                Text(
                    text = "🚀 ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )
                Text(
                    text = "Текущая версия: v$appVersion",
                    fontSize = 12.sp,
                    color = TelegramTextSecondary
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

                if (isUpdateAvailable) {
                    Button(
                        onClick = onInstallUpdate,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📦 Скачать и обновить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onCheckUpdate(githubRepo) },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔄 Проверить обновления", fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                    Text("💾 Сохранить настройки", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        Text("🚪 Сменить комнату", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isRoomOwner && onDeleteRoom != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onDeleteRoom()
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                        ) {
                            Text("🗑️ Удалить комнату из сети", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onLogOutTelegram()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("🔴 Выйти из радиоэфира", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
