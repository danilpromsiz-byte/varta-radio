package com.safecomm.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.data.telegram.ChannelDescriptor
import com.safecomm.app.data.telegram.TdAuthState
import com.safecomm.app.data.telegram.TdlibEngine
import com.safecomm.app.util.VartaChannelToken
import com.safecomm.app.ui.components.AvatarSelectionDialog
import com.safecomm.app.ui.components.UserAvatarView
import com.safecomm.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/**
 * Authentication & Closed Community Screen powered by official Telegram MTProto (TDLib).
 *
 * Flow:
 * 1. Genuine Telegram Login: Phone number -> Telegram SMS/in-app code -> optional 2FA password.
 * 2. Closed Channel (Supergroup): Create a new closed group or enter an existing one by invite link / ID.
 * 3. MANDATORY Channel Password: Encrypts and authenticates every voice transmission via HMAC-SHA256.
 */
private fun mapTelegramError(raw: String): String {
    val upper = raw.uppercase()
    return when {
        upper.contains("PHONE_NUMBER_INVALID") ->
            "Некорректный номер телефона. Введите номер в формате 099 779 98 80 или +380997799880."
        upper.contains("PHONE_CODE_INVALID") ->
            "Неверный 5-значный код. Проверьте сообщение в приложении Telegram."
        upper.contains("PHONE_CODE_EXPIRED") ->
            "Срок действия кода истёк. Нажмите «Назад» и запросите новый код."
        upper.contains("PASSWORD_HASH_INVALID") ->
            "Неверный облачный пароль 2FA Telegram. Попробуйте снова."
        upper.contains("PHONE_NUMBER_BANNED") ->
            "Этот номер телефона заблокирован в Telegram."
        upper.contains("FLOOD_WAIT") ->
            "Слишком много запросов к Telegram. Пожалуйста, подождите несколько минут."
        upper.contains("PHONE_NUMBER_UNREGISTERED") ->
            "Этот номер не зарегистрирован в Telegram. Сначала создайте аккаунт в Telegram."
        else -> "Ошибка Telegram: $raw"
    }
}

@Composable
fun AuthCommunityScreen(
    tdAuthState: TdAuthState,
    currentCallSign: String,
    currentAvatar: String = "shield",
    initialCommunity: String,
    initialPassword: String,
    onSendPhoneNumber: (String) -> Unit,
    onCheckAuthCode: (String) -> Unit,
    onCheckAuthPassword: (String) -> Unit,
    onLogOutTelegram: () -> Unit,
    onSmartConnect: (
        channelName: String,
        password: String,
        verificationAnswer: String,
        enableVerification: Boolean,
        newQuestion: String,
        newAnswer: String
    ) -> Unit,
    onCheckChannelExists: suspend (channelName: String) -> ChannelDescriptor?,
    loadRecentChats: suspend () -> List<TdApi.Chat>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var callSign by remember { mutableStateOf(currentCallSign.ifBlank { "Позывной-" + (100..999).random() }) }
    var userAvatar by remember { mutableStateOf(currentAvatar) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    // Telegram MTProto Login Inputs
    var inputPhone by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var input2faPassword by remember { mutableStateOf("") }
    var showChangePhoneConfirmDialog by remember { mutableStateOf(false) }

    // Channel Inputs & State
    var channelNameInput by remember { mutableStateOf(initialCommunity) }
    var groupPassword by remember { mutableStateOf(initialPassword) }
    var guestVerificationAnswer by remember { mutableStateOf("") }

    // Owner verification settings (by default: TRUE as requested!)
    var enableVerification by remember { mutableStateOf(true) }
    var newVerificationQuestion by remember { mutableStateOf("") }
    var newVerificationAnswer by remember { mutableStateOf("") }

    // Channel discovery state
    var existingChannelDescriptor by remember { mutableStateOf<ChannelDescriptor?>(null) }
    var isCheckingChannel by remember { mutableStateOf(false) }

    var recentChats by remember { mutableStateOf<List<TdApi.Chat>>(emptyList()) }
    var isLoadingRecentChats by remember { mutableStateOf(false) }
    var showSelectTgGroupDialog by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }

    val isAuthenticated = tdAuthState is TdAuthState.Authenticated
    val scrollState = rememberScrollState()

    // Auto-check channel uniqueness and existence when channel name changes
    LaunchedEffect(channelNameInput) {
        val trimmed = channelNameInput.trim()
        if (trimmed.isNotBlank()) {
            isCheckingChannel = true
            delay(400)
            existingChannelDescriptor = onCheckChannelExists(trimmed)
            isCheckingChannel = false
        } else {
            existingChannelDescriptor = null
            isCheckingChannel = false
        }
    }

    // Load recent chats once authenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            isLoadingRecentChats = true
            recentChats = loadRecentChats()
            isLoadingRecentChats = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TelegramDarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "🛡️ Varta Radio PTT",
            style = MaterialTheme.typography.headlineLarge,
            color = TelegramTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Радиосеть на базе Telegram (MTProto)\nКаналы = закрытые группы Telegram с паролем",
            style = MaterialTheme.typography.bodyMedium,
            color = TelegramTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // =========================================================================
        // BLOCK 1: TELEGRAM MTPROTO AUTHORIZATION
        // =========================================================================
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
                    Text(text = "✈️", fontSize = 20.sp)
                    Text(
                        text = "1. Авторизация в Telegram",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (val state = tdAuthState) {
                    is TdAuthState.Initializing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TelegramBlue, strokeWidth = 2.dp)
                            Text("Подключение к серверам Telegram...", fontSize = 12.sp, color = TelegramTextSecondary)
                        }
                    }

                    is TdAuthState.WaitPhoneNumber -> {
                        Text(
                            text = "Введите ваш номер телефона Telegram. Вам придёт официальный код подтверждения в приложение Telegram.",
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Country Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val countryOptions = listOf(
                                Pair("🇺🇦 +380", "+380"),
                                Pair("🇵🇱 +48", "+48"),
                                Pair("🇩🇪 +49", "+49"),
                                Pair("🇺🇸 +1", "+1")
                            )
                            countryOptions.forEach { (label, prefix) ->
                                val isSelected = inputPhone.startsWith(prefix) || (prefix == "+380" && (inputPhone.startsWith("0") || !inputPhone.startsWith("+")))
                                Surface(
                                    modifier = Modifier.clickable {
                                        inputPhone = if (prefix == "+380") {
                                            if (inputPhone.startsWith("+")) inputPhone.replaceFirst(Regex("^\\+\\d+"), "0") else inputPhone
                                        } else {
                                            prefix + inputPhone.filter { it.isDigit() }.trimStart('0')
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0x3326A5E4) else TelegramDarkCard,
                                    border = if (isSelected) BorderStroke(1.dp, TelegramBlue) else null
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else TelegramTextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it.trim() },
                            label = { Text("Номер телефона (🇺🇦 Украина: 0... или +380...)", color = TelegramTextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("099 779 98 80 или +380991234567", color = TelegramTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                        val normalizedPreview = TdlibEngine.normalizePhoneNumber(inputPhone)
                        if (inputPhone.isNotBlank() && normalizedPreview.length >= 6) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Формат для Telegram: $normalizedPreview",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD600)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (inputPhone.isNotBlank()) {
                                    onSendPhoneNumber(normalizedPreview)
                                }
                            },
                            enabled = inputPhone.filter { it.isDigit() }.length >= 7,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                        ) {
                            Text("📲 Получить код в Telegram", fontWeight = FontWeight.Bold)
                        }
                    }

                    is TdAuthState.WaitCode -> {
                        Text(
                            text = "Код подтверждения отправлен в ваш Telegram на номер:\n${state.phoneNumber}",
                            fontSize = 12.sp,
                            color = Color(0xFFFFD600),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it.trim() },
                            label = { Text("5-значный код из Telegram", color = TelegramTextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("12345", color = TelegramTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showChangePhoneConfirmDialog = true },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Изменить номер", color = TelegramTextSecondary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (inputCode.isNotBlank()) {
                                        onCheckAuthCode(inputCode)
                                    }
                                },
                                enabled = inputCode.isNotBlank(),
                                modifier = Modifier.weight(2f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                            ) {
                                Text("Войти", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showChangePhoneConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showChangePhoneConfirmDialog = false },
                                title = { Text("Изменить номер телефона?", fontWeight = FontWeight.Bold) },
                                text = {
                                    Text(
                                        "Вы уверены, что хотите вернуться к вводу номера?\nЗапрошенный код подтверждения Telegram для ${state.phoneNumber} будет аннулирован.",
                                        fontSize = 13.sp,
                                        color = TelegramTextSecondary
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showChangePhoneConfirmDialog = false
                                            onLogOutTelegram()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                    ) {
                                        Text("Да, изменить", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showChangePhoneConfirmDialog = false }) {
                                        Text("Отмена", color = TelegramTextPrimary)
                                    }
                                },
                                containerColor = TelegramDarkSurface,
                                titleContentColor = TelegramTextPrimary,
                                textContentColor = TelegramTextSecondary,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    is TdAuthState.WaitPassword -> {
                        Text(
                            text = "На вашем Telegram аккаунте включен облачный пароль (2FA). Введите его для завершения входа:",
                            fontSize = 12.sp,
                            color = Color(0xFFFFD600)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = input2faPassword,
                            onValueChange = { input2faPassword = it },
                            label = { Text("Облачный пароль 2FA Telegram", color = TelegramTextSecondary, fontSize = 12.sp) },
                            visualTransformation = PasswordVisualTransformation(),
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

                        Button(
                            onClick = {
                                if (input2faPassword.isNotBlank()) {
                                    onCheckAuthPassword(input2faPassword)
                                }
                            },
                            enabled = input2faPassword.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                        ) {
                            Text("🔑 Подтвердить пароль", fontWeight = FontWeight.Bold)
                        }
                    }

                    is TdAuthState.Authenticated -> {
                        val user = state.user
                        val displayName = listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank {
                            user.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" } ?: user.phoneNumber
                        }
                        val username = user.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" } ?: user.phoneNumber

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1A4CAF50), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "✅ Вы авторизованы в Telegram",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$displayName ($username)",
                                    fontSize = 13.sp,
                                    color = TelegramTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = { onLogOutTelegram() }) {
                                Text("Выйти", color = Color(0xFFFF5252), fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Callsign and Avatar Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.clickable { showAvatarPicker = true },
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                UserAvatarView(
                                    avatarString = userAvatar,
                                    size = 54.dp,
                                    borderWidth = 2.dp,
                                    borderColor = TelegramBlue
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color(0xFFFFD600), CircleShape)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✏️", fontSize = 9.sp)
                                }
                            }

                            OutlinedTextField(
                                value = callSign,
                                onValueChange = { callSign = it },
                                label = { Text("Позывной в радиоэфире", color = TelegramTextSecondary, fontSize = 12.sp) },
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
                    }

                    is TdAuthState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x26FF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Text(
                                        text = "Ошибка Telegram",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = mapTelegramError(state.message),
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Country Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val countryOptions = listOf(
                                Pair("🇺🇦 +380", "+380"),
                                Pair("🇵🇱 +48", "+48"),
                                Pair("🇩🇪 +49", "+49"),
                                Pair("🇺🇸 +1", "+1")
                            )
                            countryOptions.forEach { (label, prefix) ->
                                val isSelected = inputPhone.startsWith(prefix) || (prefix == "+380" && (inputPhone.startsWith("0") || !inputPhone.startsWith("+")))
                                Surface(
                                    modifier = Modifier.clickable {
                                        inputPhone = if (prefix == "+380") {
                                            if (inputPhone.startsWith("+")) inputPhone.replaceFirst(Regex("^\\+\\d+"), "0") else inputPhone
                                        } else {
                                            prefix + inputPhone.filter { it.isDigit() }.trimStart('0')
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0x3326A5E4) else TelegramDarkCard,
                                    border = if (isSelected) BorderStroke(1.dp, TelegramBlue) else null
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else TelegramTextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it.trim() },
                            label = { Text("Номер телефона (🇺🇦 Украина: 0... или +380...)", color = TelegramTextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("099 779 98 80 или +380991234567", color = TelegramTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                        val normalizedPreview = TdlibEngine.normalizePhoneNumber(inputPhone)
                        if (inputPhone.isNotBlank() && normalizedPreview.length >= 6) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Формат для Telegram: $normalizedPreview",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD600)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onLogOutTelegram() },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Сброс", color = TelegramTextSecondary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (inputPhone.isNotBlank()) {
                                        onSendPhoneNumber(normalizedPreview)
                                    }
                                },
                                enabled = inputPhone.filter { it.isDigit() }.length >= 7,
                                modifier = Modifier.weight(2f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                            ) {
                                Text("📲 Повторить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (!isAuthenticated) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔒", fontSize = 28.sp)
                    Text(
                        text = "Вход в закрытые каналы заблокирован",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD600)
                    )
                    Text(
                        text = "Сначала выполните вход в свой аккаунт Telegram на Шаге 1 выше.\nПосле авторизации здесь станет доступен выбор, создание и подключение к закрытым радиоканалам с паролем.",
                        fontSize = 12.sp,
                        color = TelegramTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // BLOCK 2: CLOSED COMMUNITY / ROOM (UNIFIED SMART INPUT)
            // =========================================================================
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
                        Text(text = "📢", fontSize = 20.sp)
                        Text(
                            text = "2. Комната связи (Канал)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TelegramBlue
                        )
                    }
                    Text(
                        text = "Укажите номер или название комнаты. Все участники с одинаковой комнатой и паролем объединяются в общий радиоэфир.",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Channel Name/Number input + Random ID button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = channelNameInput,
                            onValueChange = { channelNameInput = it },
                            label = { Text("Номер или название комнаты", color = TelegramTextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("35435 или Дозор-1", color = TelegramTextSecondary) },
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

                        OutlinedButton(
                            onClick = {
                                channelNameInput = (10000..99999).random().toString()
                            },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600))
                        ) {
                            Text("🎲 Номер", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Optional picker from user's Telegram groups
                    TextButton(
                        onClick = {
                            showSelectTgGroupDialog = true
                            if (recentChats.isEmpty()) {
                                coroutineScope.launch {
                                    isLoadingRecentChats = true
                                    recentChats = loadRecentChats()
                                    isLoadingRecentChats = false
                                }
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("📁 Выбрать из существующих групп Telegram", fontSize = 11.sp, color = TelegramBlue)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic channel discovery status card
                    if (isCheckingChannel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TelegramDarkCard, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TelegramBlue, strokeWidth = 2.dp)
                            Text("Поиск комнаты в радиосети...", fontSize = 12.sp, color = TelegramTextSecondary)
                        }
                    } else if (existingChannelDescriptor != null) {
                        val descriptor = existingChannelDescriptor!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1A4CAF50)),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🟢", fontSize = 14.sp)
                                    Text(
                                        text = "Комната найдена в сети!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Создатель: ${descriptor.ownerCallSign}",
                                        fontSize = 11.sp,
                                        color = TelegramTextSecondary
                                    )
                                }

                                if (descriptor.authMode == "QUESTION" && descriptor.verificationQuestion.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "❓ Проверочный вопрос создателя:\n«${descriptor.verificationQuestion}»",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFD600),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = guestVerificationAnswer,
                                        onValueChange = { guestVerificationAnswer = it },
                                        label = { Text("Ваш ответ на проверочный вопрос *", color = TelegramTextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("Введите ответ для входа", color = TelegramTextSecondary) },
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
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🔑 Для входа требуется только правильный пароль комнаты.",
                                        fontSize = 11.sp,
                                        color = TelegramTextSecondary
                                    )
                                }
                            }
                        }
                    } else if (channelNameInput.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1A26A5E4)),
                            border = BorderStroke(1.dp, TelegramBlue)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(
                                    text = "✨ Новая комната. Вы будете её владельцем.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TelegramBlue
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = enableVerification,
                                        onCheckedChange = { enableVerification = it }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Проверочный вопрос (По умолчанию)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TelegramTextPrimary
                                        )
                                        Text(
                                            text = "Гости смогут войти в комнату, только ответив на ваш вопрос",
                                            fontSize = 10.sp,
                                            color = TelegramTextSecondary
                                        )
                                    }
                                }

                                if (enableVerification) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = newVerificationQuestion,
                                        onValueChange = { newVerificationQuestion = it },
                                        label = { Text("Секретный вопрос для гостей *", color = TelegramTextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("Например: Номер нашего батальона?", color = TelegramTextSecondary) },
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
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = newVerificationAnswer,
                                        onValueChange = { newVerificationAnswer = it },
                                        label = { Text("Правильный ответ *", color = TelegramTextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("Секретный ответ для сверки", color = TelegramTextSecondary) },
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
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // BLOCK 3: MANDATORY CHANNEL PASSWORD
            // =========================================================================
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
                        Text(text = "🔒", fontSize = 20.sp)
                        Text(
                            text = "3. Пароль комнаты (ОБЯЗАТЕЛЬНО)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (groupPassword.isBlank()) Color(0xFFFFD600) else TelegramBlue
                        )
                    }
                    Text(
                        text = "Пароль комнаты обязателен для всех участников. Он шифрует доступ к Telegram и подписывает каждое голосовое сообщение.",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = groupPassword,
                        onValueChange = { groupPassword = it },
                        label = { Text("Секретный пароль комнаты *", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Введите секретный пароль", color = TelegramTextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TelegramTextPrimary,
                            unfocusedTextColor = TelegramTextPrimary,
                            focusedBorderColor = if (groupPassword.isBlank()) Color(0xFFFFD600) else TelegramBlue,
                            unfocusedBorderColor = if (groupPassword.isBlank()) Color(0x88FFD600) else TelegramDivider,
                            focusedContainerColor = TelegramDarkCard,
                            unfocusedContainerColor = TelegramDarkCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (groupPassword.isBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ Задайте секретный пароль доступа к комнате",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD600)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // SUBMIT BUTTON: ENTER TELEGRAM SECURE RADIO
            // =========================================================================
            val hasValidQuestionOrAnswer = if (existingChannelDescriptor != null) {
                existingChannelDescriptor?.authMode != "QUESTION" || guestVerificationAnswer.isNotBlank()
            } else {
                !enableVerification || (newVerificationQuestion.isNotBlank() && newVerificationAnswer.isNotBlank())
            }

            val canSubmit = isAuthenticated &&
                    groupPassword.isNotBlank() &&
                    callSign.isNotBlank() &&
                    channelNameInput.isNotBlank() &&
                    hasValidQuestionOrAnswer

            Button(
                onClick = {
                    if (canSubmit && !isOperating) {
                        isOperating = true
                        onSmartConnect(
                            channelNameInput.trim(),
                            groupPassword.trim(),
                            guestVerificationAnswer.trim(),
                            enableVerification,
                            newVerificationQuestion.trim(),
                            newVerificationAnswer.trim()
                        )
                    }
                },
                enabled = canSubmit && !isOperating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue,
                    disabledContainerColor = TelegramDarkCard
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isOperating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Подключение к эфиру...", fontSize = 14.sp, color = Color.White)
                } else {
                    val buttonText = if (existingChannelDescriptor != null) {
                        "⚡ Войти в комнату «${channelNameInput.trim()}»"
                    } else {
                        "⚡ Создать комнату «${channelNameInput.trim().ifBlank { "Новая" }}» и войти"
                    }
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (canSubmit) Color.White else TelegramTextSecondary
                    )
                }
            }

            if (!canSubmit) {
                Spacer(modifier = Modifier.height(8.dp))
                val missingReason = when {
                    !isAuthenticated -> "Требуется авторизация в Telegram на Шаге 1"
                    callSign.isBlank() -> "Укажите ваш позывной"
                    channelNameInput.isBlank() -> "Укажите номер или название комнаты"
                    groupPassword.isBlank() -> "Задайте обязательный секретный пароль комнаты"
                    existingChannelDescriptor != null && existingChannelDescriptor?.authMode == "QUESTION" && guestVerificationAnswer.isBlank() ->
                        "Ответьте на проверочный вопрос создателя комнаты"
                    existingChannelDescriptor == null && enableVerification && (newVerificationQuestion.isBlank() || newVerificationAnswer.isBlank()) ->
                        "Заполните проверочный вопрос и ответ для гостей"
                    else -> ""
                }
                if (missingReason.isNotBlank()) {
                    Text(
                        text = "⚠️ $missingReason",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD600),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar Picker Modal Dialog
        if (showAvatarPicker) {
            AvatarSelectionDialog(
                currentAvatar = userAvatar,
                onSelectAvatar = { userAvatar = it },
                onDismiss = { showAvatarPicker = false }
            )
        }

        // Telegram Group Picker Dialog
        if (showSelectTgGroupDialog) {
            AlertDialog(
                onDismissRequest = { showSelectTgGroupDialog = false },
                title = {
                    Text(
                        text = "📁 Ваши группы Telegram",
                        fontWeight = FontWeight.Bold,
                        color = TelegramTextPrimary,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Выберите существующую группу для привязки к рации:",
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingRecentChats) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TelegramBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Загрузка групп...", fontSize = 12.sp, color = TelegramTextSecondary)
                            }
                        } else if (recentChats.isEmpty()) {
                            Text(
                                text = "Группы не найдены в вашем Telegram аккаунте.",
                                fontSize = 12.sp,
                                color = TelegramTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(recentChats) { chat ->
                                    val isSelected = channelNameInput == chat.title
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                channelNameInput = chat.title
                                                showSelectTgGroupDialog = false
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0x3326A5E4) else TelegramDarkCard
                                        ),
                                        border = if (isSelected) BorderStroke(1.dp, TelegramBlue) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = chat.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else TelegramTextPrimary,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Text("Выбрано ✅", fontSize = 11.sp, color = Color(0xFF4CAF50))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSelectTgGroupDialog = false }) {
                        Text("Закрыть", color = TelegramBlue)
                    }
                },
                containerColor = TelegramDarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
