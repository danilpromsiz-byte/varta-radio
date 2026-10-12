package com.safecomm.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.safecomm.app.data.model.*
import com.safecomm.app.data.telegram.TdAuthState
import com.safecomm.app.data.telegram.TelegramAuthState
import com.safecomm.app.data.telegram.TelegramClientManager
import com.safecomm.app.data.telegram.TelegramConfig
import com.safecomm.app.data.telegram.TelegramLiveEvent
import com.safecomm.app.service.AlertNotificationManager
import com.safecomm.app.service.AppUpdateManager
import com.safecomm.app.service.LocationHelper
import com.safecomm.app.service.UpdateStatus
import com.safecomm.app.service.WalkieTalkieManager
import com.safecomm.app.ui.screens.AuthCommunityScreen
import com.safecomm.app.service.VartaRadioService
import com.safecomm.app.ui.screens.MainHubScreen
import com.safecomm.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var telegramClient: TelegramClientManager
    private lateinit var walkieTalkieManager: WalkieTalkieManager
    private lateinit var alertNotificationManager: AlertNotificationManager
    private lateinit var locationHelper: LocationHelper

    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null

    // Permissions launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationHelper.startLocationUpdates()
        }
    }

    // Camera take picture launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            currentPhotoFile?.delete()
            currentPhotoFile = null
            currentPhotoUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telegramClient = TelegramClientManager(this)
        walkieTalkieManager = WalkieTalkieManager(this)
        alertNotificationManager = AlertNotificationManager(this)
        locationHelper = LocationHelper(this)

        checkPermissions()
        locationHelper.startLocationUpdates()

        setContent {
            SafeCommTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TelegramDarkBg
                ) {
                    VartaAppContent()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            requestPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun launchCameraCapture(onUriReady: (Uri) -> Unit) {
        val cacheDir = cacheDir
        val photoFile = File(cacheDir, "alert_photo_${System.currentTimeMillis()}.jpg")
        currentPhotoFile = photoFile
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        currentPhotoUri = uri
        onUriReady(uri)
        takePictureLauncher.launch(uri)
    }

    @Composable
    private fun VartaAppContent() {
        val coroutineScope = rememberCoroutineScope()
        val authState by telegramClient.authState.collectAsState()
        val tdAuthState by telegramClient.tdlibEngine.authState.collectAsState()
        val activeMembers by telegramClient.activeMembers.collectAsState()

        val appUpdateManager = remember { AppUpdateManager(this@MainActivity) }
        val updateStatus by appUpdateManager.updateStatus.collectAsState()

        var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
        var voiceMessages by remember { mutableStateOf<List<VoiceMessage>>(emptyList()) }

        val mainPrefs = remember { getSharedPreferences("varta_main_prefs", android.content.Context.MODE_PRIVATE) }
        var isMuted by remember { mutableStateOf(mainPrefs.getBoolean("is_muted", false)) }

        val isConnected by telegramClient.isSectorConnected.collectAsState()
        val connectionStatusText by telegramClient.connectionStatusText.collectAsState()
        var alertRadiusKm by remember { mutableDoubleStateOf(2.0) }
        var soundEnabled by remember { mutableStateOf(true) }

        var capturedUri by remember { mutableStateOf<Uri?>(null) }

        val location by locationHelper.currentLocation.collectAsState()
        val userLat = location?.latitude ?: 0.0
        val userLng = location?.longitude ?: 0.0

        val isRecordingVoice by walkieTalkieManager.isRecording.collectAsState()
        val isPlayingVoice by walkieTalkieManager.isPlaying.collectAsState()
        val currentSpeaker by walkieTalkieManager.currentSpeaker.collectAsState()
        val amplitude by walkieTalkieManager.amplitude.collectAsState()

        // Auto-check GitHub updates on launch
        LaunchedEffect(Unit) {
            appUpdateManager.checkForUpdates()
        }

        // Handle incoming live events from Telegram cloud
        LaunchedEffect(Unit) {
            telegramClient.events.collect { event ->
                when (event) {
                    is TelegramLiveEvent.ConnectionState -> {
                        // Handled directly via isSectorConnected StateFlow
                    }
                    is TelegramLiveEvent.LiveVoice -> {
                        // Play loud over loudspeaker unless muted!
                        if (!isMuted) {
                            walkieTalkieManager.playLiveVoiceMessage(event.fileUrl, event.senderNick)
                        } else {
                            Toast.makeText(this@MainActivity, "🎙 Голосовое от ${event.senderNick} (Без звука)", Toast.LENGTH_SHORT).show()
                        }
                        val newVoice = VoiceMessage(
                            id = event.messageId,
                            senderNick = event.senderNick,
                            filePath = event.fileUrl,
                            durationSec = event.durationSec,
                            createdAt = "Только что"
                        )
                        voiceMessages = listOf(newVoice) + voiceMessages.filter { it.id != event.messageId }
                    }
                    is TelegramLiveEvent.LiveAlert -> {
                        // Sound siren check & vibration
                        alertNotificationManager.checkAndNotify(
                            alert = event.alert,
                            userLat = userLat,
                            userLng = userLng,
                            radiusKm = alertRadiusKm,
                            soundEnabled = soundEnabled
                        )
                        alerts = listOf(event.alert) + alerts.filter { it.id != event.alert.id }
                    }
                    is TelegramLiveEvent.LiveAlertResolved -> {
                        alerts = alerts.filter { it.id != event.alertId }
                    }
                }
            }
        }

        when (val state = authState) {
            is TelegramAuthState.Unauthenticated, is TelegramAuthState.CodeRequested, is TelegramAuthState.Error -> {
                // Screen 1: Official Telegram MTProto Login & Closed Channel Setup
                AuthCommunityScreen(
                    tdAuthState = tdAuthState,
                    currentCallSign = telegramClient.callSign,
                    currentAvatar = telegramClient.userAvatar,
                    initialCommunity = telegramClient.communityLinkOrTag,
                    initialPassword = telegramClient.communityPassword,
                    onSendPhoneNumber = { phone -> telegramClient.tdlibEngine.sendPhoneNumber(phone) },
                    onCheckAuthCode = { code -> telegramClient.tdlibEngine.checkAuthCode(code) },
                    onCheckAuthPassword = { pass -> telegramClient.tdlibEngine.checkAuthPassword(pass) },
                    onLogOutTelegram = { telegramClient.logout() },
                    onSmartConnect = { channelName, password, answer, enableVerif, question, answerForGuests ->
                        coroutineScope.launch {
                            telegramClient.enterOrJoinSmartChannel(
                                channelName = channelName,
                                password = password,
                                verificationAnswer = answer,
                                enableVerification = enableVerif,
                                newQuestion = question,
                                newAnswer = answerForGuests
                            ).onSuccess { (chatId, inviteLink) ->
                                VartaRadioService.start(this@MainActivity, channelName, telegramClient.callSign)
                                Toast.makeText(this@MainActivity, "Подключено к эфиру «$channelName»!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(this@MainActivity, "Ошибка входа: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onCheckChannelExists = { name ->
                        telegramClient.checkChannelExists(name)
                    },
                    loadRecentChats = {
                        telegramClient.tdlibEngine.loadRecentChats()
                    }
                )
            }
            is TelegramAuthState.Ready -> {
                // Screen 2: Active Walkie-Talkie & Map Operations Hub
                val activeGroup = Group(
                    id = telegramClient.activeChatId.takeIf { it != 0L } ?: 1L,
                    name = state.activeCommunityName,
                    inviteCode = telegramClient.activeInviteLink.ifBlank { state.activeCommunityName }
                )

                val currentMembers = activeMembers.ifEmpty {
                    listOf(
                        GroupMember(
                            userId = 1L,
                            nickname = "${telegramClient.callSign} (Вы)",
                            avatarUrl = telegramClient.userAvatar,
                            role = "admin"
                        )
                    )
                }

                MainHubScreen(
                    group = activeGroup,
                    members = currentMembers,
                    userAvatar = telegramClient.userAvatar,
                    onChangeAvatar = { telegramClient.userAvatar = it },
                    isMuted = isMuted,
                    onToggleMute = {
                        isMuted = !isMuted
                        mainPrefs.edit().putBoolean("is_muted", isMuted).apply()
                        val msg = if (isMuted) "🔇 Режим «Без звука» включён" else "🔊 Громкий эфир рации включён"
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    },
                    alerts = alerts,
                    voiceMessages = voiceMessages,
                    userLat = userLat,
                    userLng = userLng,
                    isConnected = isConnected,
                    isRecordingVoice = isRecordingVoice,
                    isPlayingVoice = isPlayingVoice,
                    currentSpeaker = currentSpeaker,
                    amplitude = amplitude,
                    currentRadiusKm = alertRadiusKm,
                    soundEnabled = soundEnabled,
                    botToken = telegramClient.botToken,
                    chatId = state.activeCommunityName,
                    groupPassword = telegramClient.communityPassword,
                    currentUserId = 1L,
                    capturedPhotoUri = capturedUri,
                    githubRepo = appUpdateManager.githubRepo,
                    appVersion = BuildConfig.VERSION_NAME,
                    updateStatusText = when (val s = updateStatus) {
                        is UpdateStatus.Checking -> "🔄 Проверка релизов на GitHub..."
                        is UpdateStatus.UpdateAvailable -> "🎉 Доступна новая версия: ${s.newVersion}!"
                        is UpdateStatus.UpToDate -> "✅ Установлена актуальная версия (v${BuildConfig.VERSION_NAME})"
                        is UpdateStatus.Downloading -> "⬇️ Загрузка обновления: ${s.progressPercent}%"
                        is UpdateStatus.ReadyToInstall -> "📦 Запуск установки APK..."
                        is UpdateStatus.Error -> "⚠️ ${s.message}"
                        is UpdateStatus.Idle -> ""
                    },
                    isUpdateAvailable = updateStatus is UpdateStatus.UpdateAvailable,
                    isDownloadingUpdate = updateStatus is UpdateStatus.Downloading,
                    downloadProgress = (updateStatus as? UpdateStatus.Downloading)?.progressPercent ?: 0,
                    onCheckUpdate = { repo ->
                        appUpdateManager.githubRepo = repo
                        coroutineScope.launch {
                            appUpdateManager.checkForUpdates()
                                .onSuccess { available ->
                                    if (!available) {
                                        Toast.makeText(this@MainActivity, "Установлена актуальная версия (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Найдено обновление! Нажмите «Установить»", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, err.message ?: "Ошибка проверки", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onForceUpdate = { repo ->
                        appUpdateManager.githubRepo = repo
                        coroutineScope.launch {
                            Toast.makeText(this@MainActivity, "Принудительный запрос релиза...", Toast.LENGTH_SHORT).show()
                            appUpdateManager.checkForUpdates(force = true)
                                .onSuccess { available ->
                                    if (available) {
                                        (appUpdateManager.updateStatus.value as? UpdateStatus.UpdateAvailable)?.let { updateInfo ->
                                            Toast.makeText(this@MainActivity, "Загрузка последней сборки APK (${updateInfo.newVersion})...", Toast.LENGTH_SHORT).show()
                                            appUpdateManager.downloadAndInstallApk(updateInfo.downloadUrl)
                                        }
                                    }
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, err.message ?: "Ошибка обновления", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onInstallUpdate = {
                        (updateStatus as? UpdateStatus.UpdateAvailable)?.let { updateInfo ->
                            coroutineScope.launch {
                                Toast.makeText(this@MainActivity, "Скачивание обновления...", Toast.LENGTH_SHORT).show()
                                appUpdateManager.downloadAndInstallApk(updateInfo.downloadUrl)
                            }
                        }
                    },
                    onStartPtt = {
                        val ok = walkieTalkieManager.startRecording()
                        if (ok) alertNotificationManager.triggerHapticFeedback(isLong = false)
                    },
                    onStopPtt = {
                        walkieTalkieManager.stopRecording { file, durationSec ->
                            alertNotificationManager.triggerHapticFeedback(isLong = true)
                            coroutineScope.launch {
                                telegramClient.transmitVoice(file, durationSec)
                                    .onSuccess { voiceMsg ->
                                        voiceMessages = listOf(voiceMsg) + voiceMessages
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(this@MainActivity, "Сбой передачи: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    },
                    onLaunchCamera = {
                        launchCameraCapture { uri -> capturedUri = uri }
                    },
                    onConfirmAlert = { type, desc ->
                        alertNotificationManager.triggerHapticFeedback(isLong = true)
                        coroutineScope.launch {
                            val alertLat = if (userLat != 0.0) userLat else (locationHelper.currentLocation.value?.latitude ?: 50.4501)
                            val alertLng = if (userLng != 0.0) userLng else (locationHelper.currentLocation.value?.longitude ?: 30.5234)
                            telegramClient.broadcastAlert(type, alertLat, alertLng, desc, null)
                                .onSuccess { alert ->
                                    alerts = listOf(alert) + alerts.filter { it.id != alert.id }
                                    Toast.makeText(this@MainActivity, "Тревога объявлена в эфире!", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, "Ошибка тревоги: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onConfirmPhotoReport = { type, desc ->
                        alertNotificationManager.triggerHapticFeedback(isLong = true)
                        coroutineScope.launch {
                            val alertLat = if (userLat != 0.0) userLat else (locationHelper.currentLocation.value?.latitude ?: 50.4501)
                            val alertLng = if (userLng != 0.0) userLng else (locationHelper.currentLocation.value?.longitude ?: 30.5234)
                            val photoToSend = currentPhotoFile
                            telegramClient.broadcastAlert(type, alertLat, alertLng, desc, photoToSend)
                                .onSuccess { alert ->
                                    alerts = listOf(alert) + alerts.filter { it.id != alert.id }
                                    capturedUri = null
                                    currentPhotoFile = null
                                    Toast.makeText(this@MainActivity, "Фотофиксация передана!", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, "Ошибка передачи фото: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onResolveAlert = { alert ->
                        coroutineScope.launch {
                            telegramClient.resolveAlert(alert.id)
                                .onSuccess {
                                    alerts = alerts.filter { it.id != alert.id }
                                    Toast.makeText(this@MainActivity, "Тревога снята", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onPlayHistoryVoice = { msg ->
                        walkieTalkieManager.playAudioFile(msg.filePath)
                    },
                    onStopHistoryVoice = {
                        walkieTalkieManager.stopPlayback()
                    },
                    onKickMember = {},
                    onSaveSettings = { radius, sound, token, chat, pass ->
                        alertRadiusKm = radius
                        soundEnabled = sound
                        if (token.isNotBlank()) telegramClient.botToken = token
                        if (chat.isNotBlank()) telegramClient.communityLinkOrTag = chat
                        if (pass.isNotBlank()) telegramClient.communityPassword = pass
                        telegramClient.connectToSector(telegramClient.communityLinkOrTag, telegramClient.communityPassword)
                        Toast.makeText(this@MainActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                    },
                    onHapticTick = {
                        alertNotificationManager.triggerHapticFeedback(isLong = false)
                    },
                    onLeaveRoom = {
                        telegramClient.leaveRoom()
                        Toast.makeText(this@MainActivity, "Вы вышли из комнаты", Toast.LENGTH_SHORT).show()
                    },
                    onLogOutTelegram = {
                        telegramClient.logout()
                        Toast.makeText(this@MainActivity, "Выход из Telegram выполнен", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- IN-APP MANDATORY / PROMPT UPDATE MODAL ---
        when (val s = updateStatus) {
            is UpdateStatus.UpdateAvailable -> {
                AlertDialog(
                    onDismissRequest = { /* Mandatory update cannot be dismissed */ },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                    title = {
                        Text(
                            text = "⚡ Обязательное обновление: ${s.newVersion}",
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Вышла обязательная версия приложения Varta Radio.\nОбновитесь для стабильной связи и актуальной защиты комнат.",
                                color = TelegramTextPrimary,
                                fontSize = 14.sp
                            )
                            if (s.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Что нового:",
                                    fontWeight = FontWeight.SemiBold,
                                    color = AlertMilitaryYellow,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = s.releaseNotes,
                                    color = TelegramTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    appUpdateManager.downloadAndInstallApk(s.downloadUrl)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange)
                        ) {
                            Text("⚡ Обновить сейчас", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = TelegramDarkSurface
                )
            }
            is UpdateStatus.Downloading -> {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss while downloading */ },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                    title = {
                        Text(
                            text = "⬇️ Загрузка обновления...",
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Загрузка новой версии: ${s.progressPercent}%",
                                color = TelegramTextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (s.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = AlertFireOrange,
                                trackColor = TelegramDarkBg
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Пожалуйста, подождите завершения загрузки. Установка начнется автоматически.",
                                color = TelegramTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    confirmButton = {},
                    containerColor = TelegramDarkSurface
                )
            }
            is UpdateStatus.ReadyToInstall -> {
                AlertDialog(
                    onDismissRequest = { /* Mandatory update */ },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                    title = {
                        Text(
                            text = "📦 Обновление загружено",
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Файл обновления успешно загружен на устройство.",
                                color = TelegramTextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нажмите «Установить сейчас» для применения новой версии.",
                                color = TelegramTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                appUpdateManager.triggerApkInstall(s.apkFile)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertMedicalGreen)
                        ) {
                            Text("📦 Установить сейчас", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = TelegramDarkSurface
                )
            }
            is UpdateStatus.Error -> {
                AlertDialog(
                    onDismissRequest = { appUpdateManager.resetStatus() },
                    title = {
                        Text(
                            text = "⚠️ Ошибка обновления",
                            color = AlertShellingRed,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = s.message,
                            color = TelegramTextPrimary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { appUpdateManager.resetStatus() },
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramDarkCard)
                        ) {
                            Text("Понятно")
                        }
                    },
                    containerColor = TelegramDarkSurface
                )
            }
            else -> {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
        walkieTalkieManager.stopPlayback()
        telegramClient.stopLiveCommunityStream()
        VartaRadioService.stop(this)
    }
}
