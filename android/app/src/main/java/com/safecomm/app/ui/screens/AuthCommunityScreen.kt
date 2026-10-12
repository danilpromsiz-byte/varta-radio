package com.safecomm.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.ui.components.AvatarSelectionDialog
import com.safecomm.app.ui.components.UserAvatarView
import com.safecomm.app.ui.theme.*

/**
 * Authentication and Community Access Screen.
 *
 * Direct Telegram-based authentication and secure Telegram Group channels:
 * - Direct Telegram account identifier (@username)
 * - Tactical callsign & avatar selection
 * - Telegram Group channel targeting (-100... / @group)
 * - MANDATORY channel password with HMAC-SHA256 signature verification
 */
@Composable
fun AuthCommunityScreen(
    currentCallSign: String,
    currentAvatar: String = "shield",
    initialCommunity: String,
    initialPassword: String,
    initialTelegramUsername: String = "",
    initialBotToken: String = "",
    onEnterCommunity: (callSign: String, communityTarget: String, groupPassword: String, avatar: String, telegramUsername: String, botToken: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var callSign by remember { mutableStateOf(currentCallSign.ifBlank { "Позывной-" + (100..999).random() }) }
    var userAvatar by remember { mutableStateOf(currentAvatar) }
    var telegramUsername by remember { mutableStateOf(initialTelegramUsername) }
    var botToken by remember { mutableStateOf(initialBotToken) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var communityTarget by remember { mutableStateOf(initialCommunity) }
    var groupPassword by remember { mutableStateOf(initialPassword) }
    var isCreateChannelMode by remember { mutableStateOf(false) }
    var showBotTokenField by remember { mutableStateOf(botToken.isNotBlank()) }

    val scrollState = rememberScrollState()

    val canEnter = callSign.isNotBlank() &&
            communityTarget.isNotBlank() &&
            groupPassword.isNotBlank() &&
            telegramUsername.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TelegramDarkBg)
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "🛡️ Varta Radio PTT",
            style = MaterialTheme.typography.headlineLarge,
            color = TelegramTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Оперативная связь на базе групп Telegram\n(Защищённый радиоэфир прямого действия)",
            style = MaterialTheme.typography.bodyMedium,
            color = TelegramTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --- BLOCK 1: TELEGRAM AUTHORIZATION & PROFILE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "✈️", fontSize = 18.sp)
                    Text(
                        text = "1. Авторизация в Telegram",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                }
                Text(
                    text = "Идентификация в защищённой радиосети через ваш Telegram аккаунт",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Telegram username input
                OutlinedTextField(
                    value = telegramUsername,
                    onValueChange = { input ->
                        val clean = input.trim()
                        telegramUsername = if (clean.startsWith("@") || clean.isEmpty()) clean else "@$clean"
                    },
                    label = { Text("Telegram никнейм (@username)", color = TelegramTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("@oper_commander", color = TelegramTextSecondary) },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Callsign and Avatar picker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clickable Avatar Circle
                    Box(
                        modifier = Modifier
                            .clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        UserAvatarView(
                            avatarString = userAvatar,
                            size = 56.dp,
                            borderWidth = 2.dp,
                            borderColor = TelegramBlue
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color(0xFFFFD600), RoundedCornerShape(10.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✏️", fontSize = 10.sp)
                        }
                    }

                    // Callsign input field
                    OutlinedTextField(
                        value = callSign,
                        onValueChange = { callSign = it },
                        label = { Text("Позывной в эфире", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Сокол, Дозор-1...", color = TelegramTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
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
                }

                if (telegramUsername.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ Telegram профиль привязан: $telegramUsername",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- BLOCK 2: TELEGRAM GROUP CHANNEL ACCESS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📢", fontSize = 18.sp)
                    Text(
                        text = "2. Закрытый канал связи (Telegram)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                }
                Text(
                    text = "Комната = закрытый канал связи. Участники с одинаковым каналом и паролем входят в единый защищённый радиоэфир.",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector: Вход в канал vs Создать канал
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TelegramDarkCard, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { isCreateChannelMode = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCreateChannelMode) TelegramBlue else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Войти в канал",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isCreateChannelMode) Color.White else TelegramTextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            isCreateChannelMode = true
                            if (communityTarget.isBlank()) {
                                communityTarget = (10000..99999).random().toString()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCreateChannelMode) Color(0xFFFFD600) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Создать канал",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCreateChannelMode) Color.Black else TelegramTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isCreateChannelMode) {
                    // Create Channel Assistant
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Новый номер канала:",
                            fontSize = 12.sp,
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { communityTarget = (10000..99999).random().toString() }) {
                            Text("🎲 Случайный ID", fontSize = 11.sp, color = TelegramBlue)
                        }
                    }

                    OutlinedTextField(
                        value = communityTarget,
                        onValueChange = { communityTarget = it.trim() },
                        label = { Text("Номер или название канала", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Например: 74821 или Сектор-1", color = TelegramTextSecondary) },
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

                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TelegramBlue)
                    ) {
                        Text("✈️ Создать закрытую группу в Telegram", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 Вы можете создать группу в Telegram и привязать рацию, либо использовать номер канала выше.",
                        fontSize = 10.sp,
                        color = TelegramTextSecondary
                    )
                } else {
                    // Enter existing channel
                    OutlinedTextField(
                        value = communityTarget,
                        onValueChange = { communityTarget = it.trim() },
                        label = { Text("Номер канала или ID группы Telegram", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Например: 12345 или -10019283746", color = TelegramTextSecondary) },
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
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle / Input for Bot Token
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Токен бота Telegram (опционально)",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )
                    TextButton(onClick = { showBotTokenField = !showBotTokenField }) {
                        Text(
                            text = if (showBotTokenField) "Скрыть" else "Настроить бота",
                            fontSize = 11.sp,
                            color = TelegramBlue
                        )
                    }
                }

                if (showBotTokenField) {
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it.trim() },
                        label = { Text("HTTP API токен бота от @BotFather", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("123456789:ABCDefGhIJKlmNoPQR...", color = TelegramTextSecondary) },
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 Бот дублирует аудио рации и тревоги напрямую в группу Telegram.",
                        fontSize = 10.sp,
                        color = TelegramTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- BLOCK 3: MANDATORY CHANNEL PASSWORD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔒", fontSize = 18.sp)
                    Text(
                        text = "3. Пароль канала (ОБЯЗАТЕЛЬНО)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (groupPassword.isBlank()) Color(0xFFFFD600) else TelegramBlue
                    )
                }
                Text(
                    text = "Без пароля вход в эфир закрыт. Пароль используется для сквозной защиты и HMAC-SHA256 подписи каждого сообщения.",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = groupPassword,
                    onValueChange = { groupPassword = it },
                    label = { Text("Секретный пароль канала *", color = TelegramTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("Введите пароль доступа", color = TelegramTextSecondary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = if (groupPassword.isBlank()) Color(0xFFFFD600) else TelegramBlue,
                        unfocusedBorderColor = if (groupPassword.isBlank()) Color(0x66FFD600) else TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (groupPassword.isBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠️ Введите секретный пароль для доступа к каналу",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD600)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        Button(
            onClick = {
                if (canEnter) {
                    onEnterCommunity(
                        callSign.trim(),
                        communityTarget.trim(),
                        groupPassword.trim(),
                        userAvatar,
                        telegramUsername.trim(),
                        botToken.trim()
                    )
                }
            },
            enabled = canEnter,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TelegramBlue,
                disabledContainerColor = TelegramDarkCard
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Войти в защищённый эфир Telegram",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (canEnter) Color.White else TelegramTextSecondary
            )
        }

        if (!canEnter) {
            Spacer(modifier = Modifier.height(8.dp))
            val hint = when {
                telegramUsername.isBlank() -> "Укажите ваш Telegram никнейм (@username)"
                callSign.isBlank() -> "Укажите ваш позывной"
                communityTarget.isBlank() -> "Укажите ID или название группы Telegram"
                groupPassword.isBlank() -> "Задайте секретный пароль канала (обязательно)"
                else -> ""
            }
            if (hint.isNotBlank()) {
                Text(
                    text = "⚠️ Для входа: $hint",
                    fontSize = 11.sp,
                    color = Color(0xFFFFD600),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showAvatarPicker) {
            AvatarSelectionDialog(
                currentAvatar = userAvatar,
                onSelectAvatar = { userAvatar = it },
                onDismiss = { showAvatarPicker = false }
            )
        }
    }
}
