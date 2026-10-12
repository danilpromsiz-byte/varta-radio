package com.safecomm.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.safecomm.app.ui.components.AvatarSelectionDialog
import com.safecomm.app.ui.components.UserAvatarView
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import com.safecomm.app.data.model.RoomHistoryItem
import com.safecomm.app.data.model.RoomHistoryManager
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private fun mapTelegramError(raw: String): String {
    val upper = raw.uppercase()
    return when {
        upper.contains("PHONE_NUMBER_INVALID") ->
            "Некорректный номер телефона. Введите номер в формате 099 779 98 80 или +380997799880."
        upper.contains("PHONE_CODE_INVALID") ->
            "Неверный 5-значный код. Проверьте сообщение в приложении Telegram."
        upper.contains("PHONE_CODE_EXPIRED") ->
            "Срок действия кода истёк. Запросите новый код."
        upper.contains("PASSWORD_HASH_INVALID") ->
            "Неверный облачный пароль 2FA Telegram. Попробуйте снова."
        upper.contains("PHONE_NUMBER_BANNED") ->
            "Этот номер телефона заблокирован в Telegram."
        upper.contains("FLOOD_WAIT") ->
            "Слишком много запросов к Telegram. Пожалуйста, подождите несколько минут."
        upper.contains("PHONE_NUMBER_UNREGISTERED") ->
            "Этот номер не зарегистрирован в Telegram."
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
    currentDeviceId: String = "",
    currentLanguage: AppLanguage = AppLanguage.RU,
    onLanguageChange: (AppLanguage) -> Unit = {},
    isSuperAdmin: Boolean = false,
    onUnlockSuperAdmin: ((String) -> Boolean)? = null,
    onWipeAllRooms: (() -> Unit)? = null,
    onSendPhoneNumber: (String) -> Unit,
    onCheckAuthCode: (String) -> Unit,
    onCheckAuthPassword: (String) -> Unit,
    onLogOutTelegram: () -> Unit,
    onSmartConnect: (
        channelName: String,
        password: String,
        telegramBridgeEnabled: Boolean,
        autoDeleteEnabled: Boolean,
        isCreatingNewRoom: Boolean
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

    // Mode: 0 = Войти в комнату, 1 = Создать комнату
    var selectedTab by remember { mutableIntStateOf(0) }

    // Inputs for Joining / Creating
    var channelNameInput by remember { mutableStateOf(initialCommunity) }
    var groupPassword by remember { mutableStateOf(initialPassword) }

    // Room creation options (Privacy-first defaults!)
    var autoDeleteEnabled by remember { mutableStateOf(true) } // По умолчанию 24ч временные сообщения
    var telegramBridgeEnabled by remember { mutableStateOf(false) } // По умолчанию ВЫКЛЮЧЕНО (Dark Room!)

    // Distributed registry discovery
    var existingChannelDescriptor by remember { mutableStateOf<ChannelDescriptor?>(null) }
    var isCheckingChannel by remember { mutableStateOf(false) }

    // Optional Telegram bridge accordion
    var isTelegramSectionExpanded by remember { mutableStateOf(false) }
    var inputPhone by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var input2faPassword by remember { mutableStateOf("") }

    var showSuperAdminModal by remember { mutableStateOf(false) }
    var superAdminKeyInput by remember { mutableStateOf("") }
    var isSuperAdminActive by remember { mutableStateOf(isSuperAdmin) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    var easterEggClicks by remember { mutableIntStateOf(0) }
    var lastEasterEggClickTime by remember { mutableLongStateOf(0L) }

    // Room History
    var roomHistory by remember { mutableStateOf(RoomHistoryManager.getHistory(context)) }
    var historyFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = My/Admin only

    LaunchedEffect(Unit) {
        roomHistory = RoomHistoryManager.getHistory(context)
    }

    val isAuthenticated = tdAuthState is TdAuthState.Authenticated
    val scrollState = rememberScrollState()

    // Auto-check channel uniqueness and existence when name changes
    LaunchedEffect(channelNameInput) {
        val trimmed = channelNameInput.trim()
        if (trimmed.isNotBlank()) {
            isCheckingChannel = true
            delay(350)
            existingChannelDescriptor = onCheckChannelExists(trimmed)
            isCheckingChannel = false
        } else {
            existingChannelDescriptor = null
            isCheckingChannel = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TelegramDarkBg)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top control bar: Language selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language selector chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.forEach { lang ->
                    val isSel = currentLanguage == lang
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) TelegramBlue.copy(alpha = 0.25f) else TelegramDarkCard)
                            .border(1.dp, if (isSel) TelegramBlue else TelegramDivider, RoundedCornerShape(8.dp))
                            .clickable { onLanguageChange(lang) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(lang.flag, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // App Header (Secret Easter Egg: 7 taps opens Super Admin mode)
        Text(
            text = "🛡️ ${I18n.appTitle(context)}",
            style = MaterialTheme.typography.headlineLarge,
            color = TelegramTextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val now = System.currentTimeMillis()
                if (now - lastEasterEggClickTime > 1500L) {
                    easterEggClicks = 1
                } else {
                    easterEggClicks++
                    if (easterEggClicks >= 7) {
                        easterEggClicks = 0
                        showSuperAdminModal = true
                    }
                }
                lastEasterEggClickTime = now
            }
        )
        Text(
            text = I18n.appSubtitle(context) + "\nAES-256 Retained P2P",
            style = MaterialTheme.typography.bodyMedium,
            color = TelegramTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // =========================================================================
        // BLOCK 1: USER PROFILE (CALLSIGN & AVATAR)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Clickable Avatar with edit badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clickable { showAvatarPicker = true },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    UserAvatarView(avatarString = userAvatar, size = 54.dp)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFFFFD600), CircleShape)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", fontSize = 10.sp)
                    }
                }

                OutlinedTextField(
                    value = callSign,
                    onValueChange = { callSign = it },
                    label = { Text("Ваш позывной в эфире *", color = TelegramTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("Комбат, Дозор-1...", color = TelegramTextSecondary) },
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

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // BLOCK 2: ROOM SELECTION (TABS: ВОЙТИ В КОМНАТУ / СОЗДАТЬ КОМНАТУ)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = TelegramDarkCard,
                    contentColor = TelegramBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Войти в комнату",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (selectedTab == 0) TelegramBlue else TelegramTextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Создать комнату",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (selectedTab == 1) TelegramBlue else TelegramTextSecondary
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // --- TAB 0: JOIN EXISTING ROOM ---
                    Text(
                        text = "Введите название или номер комнаты, созданной командиром:",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = channelNameInput,
                        onValueChange = { channelNameInput = it },
                        label = { Text("Номер или название комнаты *", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("102 или Сектор-3", color = TelegramTextSecondary) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = groupPassword,
                        onValueChange = { groupPassword = it },
                        label = { Text("Секретный пароль комнаты *", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Пароль для доступа в эфир", color = TelegramTextSecondary) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Channel Discovery Status Card
                    if (isCheckingChannel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TelegramDarkCard, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TelegramBlue, strokeWidth = 2.dp)
                            Text("Поиск комнаты в эфире...", fontSize = 11.sp, color = TelegramTextSecondary)
                        }
                    } else if (existingChannelDescriptor != null) {
                        val descriptor = existingChannelDescriptor!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1A4CAF50)),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🟢", fontSize = 14.sp)
                                    Text(
                                        text = "Комната «${descriptor.channelName}» найдена!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Создатель: ${descriptor.ownerCallSign}",
                                        fontSize = 10.sp,
                                        color = TelegramTextSecondary
                                    )
                                }

                                if (descriptor.redirectionMessage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0x33FF9800)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, AlertFireOrange)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📢", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Записка от создателя комнаты:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = AlertFireOrange
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "«${descriptor.redirectionMessage}»",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TelegramTextPrimary
                                            )
                                        }
                                    }
                                }

                                if (descriptor.isAbandoned) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "⚠️ В комнате сейчас никого нет (создатель вышел). Доступ открыт по общему паролю.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD600)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (callSign.isBlank()) {
                                Toast.makeText(context, "Укажите ваш позывной", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (channelNameInput.isBlank()) {
                                Toast.makeText(context, "Укажите номер или название комнаты", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (groupPassword.isBlank()) {
                                Toast.makeText(context, "Введите пароль комнаты", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            onSmartConnect(
                                channelNameInput.trim(),
                                groupPassword.trim(),
                                false,
                                true,
                                false
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                    ) {
                        Text("📻 ВОЙТИ В ЭФИР", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                } else {
                    // --- TAB 1: CREATE NEW ROOM ---
                    Text(
                        text = "Создайте новую закрытую комнату и станьте её владельцем:",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = channelNameInput,
                            onValueChange = { channelNameInput = it },
                            label = { Text("Название или номер комнаты *", color = TelegramTextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("102, Дозор-1...", color = TelegramTextSecondary) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = groupPassword,
                        onValueChange = { groupPassword = it },
                        label = { Text("Секретный пароль для комнаты *", color = TelegramTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("Придумайте пароль", color = TelegramTextSecondary) },
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

                    if (existingChannelDescriptor != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val desc = existingChannelDescriptor!!
                        val isOwner = currentDeviceId.isNotBlank() && (desc.ownerDeviceId == currentDeviceId || desc.ownerId.startsWith(currentDeviceId))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isOwner) Color(0x224CAF50) else Color(0x22FF9800)),
                            border = BorderStroke(1.dp, if (isOwner) Color(0xFF4CAF50) else AlertFireOrange)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(if (isOwner) "👑" else "ℹ️", fontSize = 16.sp)
                                Text(
                                    text = if (isOwner) {
                                        "Это ваша созданная ранее комната. При вводе правильного пароля её настройки будут обновлены, и вы войдете как владелец."
                                    } else {
                                        "Комната «${desc.channelName}» уже зарегистрирована в сети. Если это ваша комната, введите её пароль для восстановления управления."
                                    },
                                    fontSize = 11.sp,
                                    color = TelegramTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Временные сообщения (По умолчанию включено!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = autoDeleteEnabled,
                            onCheckedChange = { autoDeleteEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⏳ Временные сообщения (Автоудаление 24ч)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = "Все сообщения и метки удаляются через 24 часа для безопасности.",
                                fontSize = 10.sp,
                                color = TelegramTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Трансляция в Telegram (По умолчанию ВЫКЛЮЧЕНО!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = telegramBridgeEnabled,
                            onCheckedChange = { telegramBridgeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✈️ Трансляция в Telegram (Закрытый канал)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = if (telegramBridgeEnabled) "Включено: транслировать в Telegram" else "Dark Room: общение абсолютно невидимо снаружи (только в Varta GO)",
                                fontSize = 10.sp,
                                color = if (telegramBridgeEnabled) TelegramBlue else Color(0xFFFFD600)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (callSign.isBlank()) {
                                Toast.makeText(context, "Укажите ваш позывной", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (channelNameInput.isBlank()) {
                                Toast.makeText(context, "Укажите название комнаты", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (groupPassword.isBlank()) {
                                Toast.makeText(context, "Укажите секретный пароль комнаты", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            onSmartConnect(
                                channelNameInput.trim(),
                                groupPassword.trim(),
                                telegramBridgeEnabled,
                                autoDeleteEnabled,
                                true
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("🛡️ СОЗДАТЬ И ВОЙТИ В ЭФИР", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // BLOCK 2.5: ROOM ENTRY HISTORY & MY ADMIN ROOMS
        // =========================================================================
        if (roomHistory.isNotEmpty()) {
            val adminCount = roomHistory.count { it.isOwner || it.isAdmin }
            val displayedRooms = if (historyFilter == 1) {
                roomHistory.filter { it.isOwner || it.isAdmin }
            } else {
                roomHistory
            }
            val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header row: Title + Clear button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = I18n.historyRoomsTitle(context),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = I18n.historyRoomsSubtitle(context),
                                fontSize = 10.sp,
                                color = TelegramTextSecondary
                            )
                        }
                        TextButton(
                            onClick = {
                                RoomHistoryManager.clearHistory(context)
                                roomHistory = emptyList()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = I18n.historyClearBtn(context),
                                fontSize = 10.sp,
                                color = TelegramTextSecondary
                            )
                        }
                    }

                    // Filter tabs (if user has rooms with admin or owner privileges)
                    if (adminCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter 0: All
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (historyFilter == 0) TelegramBlue.copy(alpha = 0.2f) else TelegramDarkCard)
                                    .border(1.dp, if (historyFilter == 0) TelegramBlue else TelegramDivider, RoundedCornerShape(8.dp))
                                    .clickable { historyFilter = 0 }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = I18n.historyFilterAll(context, roomHistory.size),
                                    fontSize = 11.sp,
                                    fontWeight = if (historyFilter == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (historyFilter == 0) TelegramBlue else TelegramTextSecondary
                                )
                            }

                            // Filter 1: Admin / Owner only
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (historyFilter == 1) Color(0x33FFD600) else TelegramDarkCard)
                                    .border(1.dp, if (historyFilter == 1) Color(0xFFFFD600) else TelegramDivider, RoundedCornerShape(8.dp))
                                    .clickable { historyFilter = 1 }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = I18n.historyFilterAdmin(context, adminCount),
                                    fontSize = 11.sp,
                                    fontWeight = if (historyFilter == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (historyFilter == 1) Color(0xFFFFD600) else TelegramTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (displayedRooms.isEmpty()) {
                        Text(
                            text = I18n.historyEmpty(context),
                            fontSize = 11.sp,
                            color = TelegramTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedRooms.forEach { item ->
                                val isAdm = item.isOwner || item.isAdmin
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            channelNameInput = item.roomName
                                            groupPassword = item.savedPassword
                                            selectedTab = 0
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                                    border = BorderStroke(
                                        width = if (isAdm) 1.5.dp else 1.dp,
                                        color = when {
                                            item.isOwner -> Color(0xFFFFD600)
                                            item.isAdmin -> Color(0xFF4CAF50)
                                            else -> TelegramDivider
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = item.roomName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TelegramTextPrimary
                                                )

                                                // Role Badge
                                                val badgeText = when {
                                                    item.isOwner -> I18n.historyRoleOwner(context)
                                                    item.isAdmin -> I18n.historyRoleAdmin(context)
                                                    else -> I18n.historyRoleMember(context)
                                                }
                                                val badgeColor = when {
                                                    item.isOwner -> Color(0xFFFFD600)
                                                    item.isAdmin -> Color(0xFF4CAF50)
                                                    else -> TelegramTextSecondary
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = badgeColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = dateFormat.format(Date(item.lastJoinedAt)),
                                                    fontSize = 10.sp,
                                                    color = TelegramTextSecondary
                                                )
                                                if (item.savedPassword.isNotBlank()) {
                                                    Text(
                                                        text = "• 🔑 Пароль сохранён",
                                                        fontSize = 10.sp,
                                                        color = TelegramBlue
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // 1-Click Enter Button
                                            Button(
                                                onClick = {
                                                    if (callSign.isBlank()) {
                                                        Toast.makeText(context, "Укажите ваш позывной", Toast.LENGTH_SHORT).show()
                                                    } else if (item.savedPassword.isBlank()) {
                                                        channelNameInput = item.roomName
                                                        groupPassword = ""
                                                        selectedTab = 0
                                                        Toast.makeText(context, "Введите пароль для комнаты «${item.roomName}»", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        channelNameInput = item.roomName
                                                        groupPassword = item.savedPassword
                                                        selectedTab = 0
                                                        onSmartConnect(item.roomName, item.savedPassword, false, true, false)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                                            ) {
                                                Text(
                                                    text = I18n.historyQuickEnter(context),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Delete room from history
                                            IconButton(
                                                onClick = {
                                                    RoomHistoryManager.removeRoom(context, item.roomName)
                                                    roomHistory = RoomHistoryManager.getHistory(context)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("✕", fontSize = 13.sp, color = TelegramTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // =========================================================================
        // BLOCK 3: OPTIONAL TELEGRAM ACCOUNT BRIDGE (COLLAPSIBLE / ACCORDION)
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isTelegramSectionExpanded = !isTelegramSectionExpanded },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "✈️", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Подключение аккаунта Telegram (Опционально)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramTextPrimary
                            )
                            Text(
                                text = if (isAuthenticated) "✅ Авторизован в Telegram" else "Не требуется для автономного радиоэфира",
                                fontSize = 10.sp,
                                color = if (isAuthenticated) Color(0xFF4CAF50) else TelegramTextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isTelegramSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TelegramTextSecondary
                    )
                }

                AnimatedVisibility(visible = isTelegramSectionExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        when (val state = tdAuthState) {
                            is TdAuthState.Authenticated -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Сессия активна: @${state.user.usernames?.activeUsernames?.firstOrNull() ?: state.user.firstName}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = onLogOutTelegram) {
                                            Text("Выйти", color = AlertShellingRed, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            is TdAuthState.WaitPhoneNumber, is TdAuthState.Initializing -> {
                                Text(
                                    text = "Если вы хотите управлять каналами через Telegram с ПК, укажите номер телефона:",
                                    fontSize = 11.sp,
                                    color = TelegramTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = inputPhone,
                                    onValueChange = { inputPhone = it.trim() },
                                    label = { Text("Номер телефона (0... или +380...)", color = TelegramTextSecondary, fontSize = 11.sp) },
                                    placeholder = { Text("099 779 98 80", color = TelegramTextSecondary) },
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
                                    shape = RoundedCornerShape(10.dp)
                                )

                                val normalized = TdlibEngine.normalizePhoneNumber(inputPhone)
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onSendPhoneNumber(normalized) },
                                    enabled = inputPhone.filter { it.isDigit() }.length >= 7,
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                                ) {
                                    Text("Получить код подтверждения", fontSize = 12.sp)
                                }
                            }

                            is TdAuthState.WaitCode -> {
                                Text(
                                    text = "Введите 5-значный код из Telegram для номера ${state.phoneNumber}:",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFD600)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = {
                                        inputCode = it.filter { ch -> ch.isDigit() }.take(5)
                                        if (inputCode.length == 5) onCheckAuthCode(inputCode)
                                    },
                                    label = { Text("Код из Telegram", color = TelegramTextSecondary, fontSize = 11.sp) },
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

                            is TdAuthState.WaitPassword -> {
                                Text(
                                    text = "Введите облачный пароль двухфакторной защиты 2FA Telegram:",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFD600)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = input2faPassword,
                                    onValueChange = { input2faPassword = it },
                                    label = { Text("Облачный пароль 2FA", color = TelegramTextSecondary, fontSize = 11.sp) },
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onCheckAuthPassword(input2faPassword) },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                                ) {
                                    Text("Подтвердить пароль 2FA", fontSize = 12.sp)
                                }
                            }

                            is TdAuthState.Error -> {
                                Text(
                                    text = mapTelegramError(state.message),
                                    fontSize = 11.sp,
                                    color = AlertShellingRed
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = onLogOutTelegram,
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Сбросить сессию Telegram", fontSize = 11.sp, color = TelegramTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showAvatarPicker) {
        AvatarSelectionDialog(
            currentAvatar = userAvatar,
            onSelectAvatar = {
                userAvatar = it
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    if (showSuperAdminModal) {
        AlertDialog(
            onDismissRequest = { showSuperAdminModal = false },
            title = {
                Text(I18n.superAdminSection(context), fontWeight = FontWeight.Bold, color = Color(0xFFFFD600))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isSuperAdminActive) {
                        Text(I18n.superAdminStatusActive(context), fontWeight = FontWeight.Bold, color = Color(0xFFFFD600))
                        Text(I18n.superAdminDesc(context), fontSize = 12.sp, color = TelegramTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                showSuperAdminModal = false
                                showWipeConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(I18n.wipeAllRoomsBtn(context), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text("Введите мастер-ключ суперадмина:", fontSize = 12.sp, color = TelegramTextSecondary)
                        OutlinedTextField(
                            value = superAdminKeyInput,
                            onValueChange = { superAdminKeyInput = it },
                            placeholder = { Text(I18n.superAdminKeyPlaceholder(context), color = TelegramTextSecondary) },
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
                                Toast.makeText(context, "Invalid Key", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                    ) {
                        Text(I18n.superAdminActivateBtn(context), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuperAdminModal = false }) {
                    Text(I18n.close(context), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }

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
