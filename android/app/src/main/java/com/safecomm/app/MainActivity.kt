package com.safecomm.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.i18n.LanguageManager
import com.safecomm.app.ui.i18n.LanguageSelectionDialog
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
        handleNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val targetRoom = intent.getStringExtra(AlertNotificationManager.EXTRA_TARGET_ROOM)
        val playVoiceId = intent.getLongExtra(AlertNotificationManager.EXTRA_PLAY_VOICE_ID, 0L)
        val targetAlertId = intent.getLongExtra(AlertNotificationManager.EXTRA_TARGET_ALERT_ID, 0L)

        if (playVoiceId != 0L) {
            alertNotificationManager.dismissVoiceNotification(playVoiceId)
        }
        if (targetAlertId != 0L) {
            alertNotificationManager.dismissHazardNotification(targetAlertId)
        }

        if (!targetRoom.isNullOrBlank() && !targetRoom.equals(telegramClient.communityLinkOrTag, ignoreCase = true)) {
            val history = com.safecomm.app.data.model.RoomHistoryManager.getHistory(this)
            val item = history.find { it.roomName.equals(targetRoom, ignoreCase = true) }
            val password = item?.savedPassword ?: ""
            telegramClient.connectToSector(targetRoom, password)
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
        var appLanguage by remember { mutableStateOf(LanguageManager.getLanguage(this@MainActivity)) }
        var showLanguagePrompt by remember { mutableStateOf(LanguageManager.shouldPromptLanguage(this@MainActivity)) }

        val authState by telegramClient.authState.collectAsState()
        val tdAuthState by telegramClient.tdlibEngine.authState.collectAsState()
        val activeMembers by telegramClient.activeMembers.collectAsState()

        val appUpdateManager = remember { AppUpdateManager(this@MainActivity) }
        val updateStatus by appUpdateManager.updateStatus.collectAsState()

        if (showLanguagePrompt) {
            LanguageSelectionDialog(
                currentLanguage = appLanguage,
                onLanguageSelected = { chosen ->
                    LanguageManager.setLanguage(this@MainActivity, chosen)
                    appLanguage = chosen
                    showLanguagePrompt = false
                }
            )
        }

        var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
        var voiceMessages by remember { mutableStateOf<List<VoiceMessage>>(emptyList()) }

        val mainPrefs = remember { getSharedPreferences("varta_main_prefs", android.content.Context.MODE_PRIVATE) }
        val audioModeString = mainPrefs.getString("audio_mode", AudioMode.SOUND.name) ?: AudioMode.SOUND.name
        var audioMode by remember { mutableStateOf(try { AudioMode.valueOf(audioModeString) } catch (e: Exception) { AudioMode.SOUND }) }

        val isConnected by telegramClient.isSectorConnected.collectAsState()
        val connectionStatusText by telegramClient.connectionStatusText.collectAsState()
        val currentDescriptor by telegramClient.currentChannelDescriptor.collectAsState()
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

        // Auto-prune expired alerts periodically
        LaunchedEffect(Unit) {
            while (true) {
                val now = System.currentTimeMillis()
                alerts = alerts.filter { it.expiresAt <= 0L || it.expiresAt > now }
                kotlinx.coroutines.delay(30 * 1000L)
            }
        }

        // Auto-check GitHub updates on launch and periodically every 10 minutes when NOT in a room
        LaunchedEffect(authState) {
            if (authState !is TelegramAuthState.Ready) {
                while (true) {
                    appUpdateManager.checkForUpdates(force = false)
                    kotlinx.coroutines.delay(10 * 60 * 1000L)
                }
            }
        }

        // Handle incoming live events from Telegram cloud
        LaunchedEffect(Unit) {
            telegramClient.events.collect { event ->
                when (event) {
                    is TelegramLiveEvent.ConnectionState -> {
                        // Handled directly via isSectorConnected StateFlow
                    }
                    is TelegramLiveEvent.LiveVoice -> {
                        val targetRoom = event.roomName.ifBlank { telegramClient.communityLinkOrTag }

                        if (event.isFromOtherRoom) {
                            // Unplayed voice message from ANOTHER group:
                            // 1. Post notification in шторка & Heads-Up over all windows
                            alertNotificationManager.showUnplayedVoiceNotification(
                                senderNick = event.senderNick,
                                roomName = targetRoom,
                                durationSec = event.durationSec,
                                filePath = event.fileUrl,
                                messageId = event.messageId,
                                isFromOtherRoom = true
                            )
                            // 2. Play subtle incoming chirp / vibrate so user knows a message came in
                            when (audioMode) {
                                AudioMode.SOUND -> walkieTalkieManager.playIncomingRadioChirp()
                                AudioMode.VIBRATE -> alertNotificationManager.triggerHapticAlert()
                                AudioMode.SILENT -> {}
                            }
                        } else {
                            // Voice message from CURRENT group:
                            // 1. Post notification in шторка & Heads-Up over all windows
                            alertNotificationManager.showUnplayedVoiceNotification(
                                senderNick = event.senderNick,
                                roomName = targetRoom,
                                durationSec = event.durationSec,
                                filePath = event.fileUrl,
                                messageId = event.messageId,
                                isFromOtherRoom = false
                            )

                            when (audioMode) {
                                AudioMode.SOUND -> {
                                    walkieTalkieManager.playLiveVoiceMessage(event.fileUrl, event.senderNick) {
                                        alertNotificationManager.dismissVoiceNotification(event.messageId)
                                    }
                                }
                                AudioMode.VIBRATE -> {
                                    alertNotificationManager.triggerHapticAlert()
                                    Toast.makeText(this@MainActivity, "🎙 ${event.senderNick} (" + I18n.audioModeVibrate(appLanguage) + ")", Toast.LENGTH_SHORT).show()
                                }
                                AudioMode.SILENT -> {
                                    Toast.makeText(this@MainActivity, "🎙 ${event.senderNick} (" + I18n.audioModeSilent(appLanguage) + ")", Toast.LENGTH_SHORT).show()
                                }
                            }
                            val newVoice = VoiceMessage(
                                id = event.messageId,
                                senderNick = event.senderNick,
                                filePath = event.fileUrl,
                                durationSec = event.durationSec,
                                createdAt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            )
                            voiceMessages = listOf(newVoice) + voiceMessages.filter { it.id != event.messageId }
                        }
                    }
                    is TelegramLiveEvent.LiveAlert -> {
                        val targetRoom = event.roomName.ifBlank { telegramClient.communityLinkOrTag }

                        if (event.isFromOtherRoom) {
                            // Hazard alert from ANOTHER group:
                            alertNotificationManager.showHazardNotification(
                                alert = event.alert,
                                roomName = targetRoom,
                                isFromOtherRoom = true
                            )
                            when (audioMode) {
                                AudioMode.SOUND -> {
                                    alertNotificationManager.triggerRoomSiren(event.alert.typedCategory)
                                    alertNotificationManager.triggerHapticAlert(event.alert.typedCategory)
                                }
                                AudioMode.VIBRATE -> {
                                    alertNotificationManager.triggerHapticAlert(event.alert.typedCategory)
                                }
                                AudioMode.SILENT -> {}
                            }
                        } else {
                            // Hazard alert from CURRENT group:
                            alertNotificationManager.showHazardNotification(
                                alert = event.alert,
                                roomName = targetRoom,
                                isFromOtherRoom = false
                            )
                            when (audioMode) {
                                AudioMode.SOUND -> {
                                    alertNotificationManager.triggerRoomSiren(event.alert.typedCategory)
                                    alertNotificationManager.triggerHapticAlert(event.alert.typedCategory)
                                }
                                AudioMode.VIBRATE -> {
                                    alertNotificationManager.triggerHapticAlert(event.alert.typedCategory)
                                }
                                AudioMode.SILENT -> {
                                    // Visual alert only
                                }
                            }
                            alerts = listOf(event.alert) + alerts.filter { it.id != event.alert.id }
                        }
                    }
                    is TelegramLiveEvent.LiveAlertResolved -> {
                        alerts = alerts.filter { it.id != event.alertId }
                        alertNotificationManager.dismissHazardNotification(event.alertId)
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
                    currentDeviceId = telegramClient.deviceId,
                    currentLanguage = appLanguage,
                    onLanguageChange = { lang ->
                        LanguageManager.setLanguage(this@MainActivity, lang)
                        appLanguage = lang
                    },
                    appVersion = BuildConfig.VERSION_NAME,
                    isUpdateAvailable = updateStatus is UpdateStatus.UpdateAvailable,
                    onCheckUpdate = {
                        coroutineScope.launch {
                            Toast.makeText(this@MainActivity, I18n.checkingUpdatesStatus(this@MainActivity), Toast.LENGTH_SHORT).show()
                            appUpdateManager.checkForUpdates(force = true)
                                .onSuccess { available ->
                                    if (!available) {
                                        Toast.makeText(this@MainActivity, I18n.upToDateStatus(this@MainActivity, BuildConfig.VERSION_NAME), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, I18n.updateAvailableToast(this@MainActivity), Toast.LENGTH_LONG).show()
                                    }
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, err.message ?: "Update check error", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onInstallUpdate = {
                        (updateStatus as? UpdateStatus.UpdateAvailable)?.let { updateInfo ->
                            coroutineScope.launch {
                                Toast.makeText(this@MainActivity, I18n.downloadingApk(this@MainActivity), Toast.LENGTH_SHORT).show()
                                appUpdateManager.downloadAndInstallApk(updateInfo.downloadUrl)
                            }
                        }
                    },
                    isSuperAdmin = telegramClient.isSuperAdmin(),
                    onUnlockSuperAdmin = { key ->
                        telegramClient.unlockSuperAdmin(key)
                    },
                    onWipeAllRooms = {
                        coroutineScope.launch {
                            telegramClient.wipeAllRooms()
                                .onSuccess { count ->
                                    Toast.makeText(this@MainActivity, "Все комнаты сети удалены ($count)", Toast.LENGTH_LONG).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onSendPhoneNumber = { phone -> telegramClient.tdlibEngine.sendPhoneNumber(phone) },
                    onCheckAuthCode = { code -> telegramClient.tdlibEngine.checkAuthCode(code) },
                    onCheckAuthPassword = { pass -> telegramClient.tdlibEngine.checkAuthPassword(pass) },
                    onLogOutTelegram = { telegramClient.logout() },
                    onSmartConnect = { channelName, password, telegramBridge, autoDelete, isCreatingNewRoom ->
                        coroutineScope.launch {
                            telegramClient.enterOrJoinSmartChannel(
                                channelName = channelName,
                                password = password,
                                telegramBridgeEnabled = telegramBridge,
                                autoDeleteEnabled = autoDelete,
                                isCreatingNewRoom = isCreatingNewRoom
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
                    audioMode = audioMode,
                    onToggleAudioMode = {
                        audioMode = when (audioMode) {
                            AudioMode.SOUND -> AudioMode.VIBRATE
                            AudioMode.VIBRATE -> AudioMode.SILENT
                            AudioMode.SILENT -> AudioMode.SOUND
                        }
                        mainPrefs.edit().putString("audio_mode", audioMode.name).apply()
                        val msg = when (audioMode) {
                            AudioMode.SOUND -> "🔊 " + I18n.audioModeSound(appLanguage)
                            AudioMode.VIBRATE -> "📳 " + I18n.audioModeVibrate(appLanguage)
                            AudioMode.SILENT -> "🔇 " + I18n.audioModeSilent(appLanguage)
                        }
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
                        is UpdateStatus.Checking -> I18n.checkingUpdatesStatus(this@MainActivity)
                        is UpdateStatus.UpdateAvailable -> I18n.updateAvailableStatus(this@MainActivity, s.newVersion)
                        is UpdateStatus.UpToDate -> I18n.upToDateStatus(this@MainActivity, BuildConfig.VERSION_NAME)
                        is UpdateStatus.Downloading -> I18n.downloadingUpdateStatus(this@MainActivity, s.progressPercent)
                        is UpdateStatus.ReadyToInstall -> I18n.readyToInstallStatus(this@MainActivity)
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
                                        Toast.makeText(this@MainActivity, I18n.upToDateStatus(this@MainActivity, BuildConfig.VERSION_NAME), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, I18n.updateAvailableToast(this@MainActivity), Toast.LENGTH_LONG).show()
                                    }
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, err.message ?: "Update check error", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onForceUpdate = { repo ->
                        appUpdateManager.githubRepo = repo
                        coroutineScope.launch {
                            Toast.makeText(this@MainActivity, I18n.checkingUpdatesStatus(this@MainActivity), Toast.LENGTH_SHORT).show()
                            appUpdateManager.checkForUpdates(force = true)
                                .onSuccess { available ->
                                    if (available) {
                                        (appUpdateManager.updateStatus.value as? UpdateStatus.UpdateAvailable)?.let { updateInfo ->
                                            Toast.makeText(this@MainActivity, I18n.downloadingApk(this@MainActivity), Toast.LENGTH_SHORT).show()
                                            appUpdateManager.downloadAndInstallApk(updateInfo.downloadUrl)
                                        }
                                    }
                                }
                                .onFailure { err ->
                                    Toast.makeText(this@MainActivity, err.message ?: "Update error", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onInstallUpdate = {
                        (updateStatus as? UpdateStatus.UpdateAvailable)?.let { updateInfo ->
                            coroutineScope.launch {
                                Toast.makeText(this@MainActivity, I18n.downloadingApk(this@MainActivity), Toast.LENGTH_SHORT).show()
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
                    onConfirmAlert = { type, desc, durationMinutes ->
                        alertNotificationManager.triggerHapticFeedback(isLong = true)
                        coroutineScope.launch {
                            val alertLat = if (userLat != 0.0) userLat else (locationHelper.currentLocation.value?.latitude ?: 50.4501)
                            val alertLng = if (userLng != 0.0) userLng else (locationHelper.currentLocation.value?.longitude ?: 30.5234)
                            telegramClient.broadcastAlert(type, alertLat, alertLng, desc, null, durationMinutes)
                                .onSuccess { alert ->
                                    alerts = listOf(alert) + alerts.filter { it.id != alert.id }
                                    Toast.makeText(this@MainActivity, "Метка объявлена в эфире!", Toast.LENGTH_SHORT).show()
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
                            telegramClient.broadcastAlert(type, alertLat, alertLng, desc, photoToSend, 30)
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
                                    Toast.makeText(this@MainActivity, I18n.threatResolvedToast(appLanguage), Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onPlayHistoryVoice = { msg ->
                        walkieTalkieManager.playAudioFile(msg.filePath)
                    },
                    onStopHistoryVoice = {
                        walkieTalkieManager.stopPlayback()
                    },
                    onKickMember = { member ->
                        coroutineScope.launch {
                            telegramClient.kickMember(member)
                                .onSuccess {
                                    Toast.makeText(this@MainActivity, "Участник ${member.nickname} исключён из эфира", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка исключения: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onBanMember = { member, durationMs ->
                        coroutineScope.launch {
                            telegramClient.banMember(member, durationMs)
                                .onSuccess {
                                    val banText = if (durationMs <= 0L) "навсегда" else "на ${durationMs / 60000} мин."
                                    Toast.makeText(this@MainActivity, "Участник ${member.nickname} заблокирован $banText", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка блокировки: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    bannedUsers = currentDescriptor?.bannedUsers ?: emptyMap(),
                    onUnbanUser = { key ->
                        coroutineScope.launch {
                            telegramClient.unbanUser(key)
                                .onSuccess {
                                    Toast.makeText(this@MainActivity, "Участник $key разблокирован", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    isRoomOwner = currentDescriptor == null || telegramClient.canDeleteRoom(currentDescriptor),
                    onDelegateAdmin = { member, grant ->
                        coroutineScope.launch {
                            telegramClient.delegateAdmin(member, grant)
                                .onSuccess {
                                    val action = if (grant) "назначен администратором" else "лишён прав администратора"
                                    Toast.makeText(this@MainActivity, "${member.nickname} $action", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    isSuperAdmin = telegramClient.isSuperAdmin(),
                    onUnlockSuperAdmin = { key ->
                        telegramClient.unlockSuperAdmin(key)
                    },
                    onWipeAllRooms = {
                        coroutineScope.launch {
                            telegramClient.wipeAllRooms()
                                .onSuccess { count ->
                                    Toast.makeText(this@MainActivity, "Все комнаты сети удалены ($count)", Toast.LENGTH_LONG).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    currentLanguage = appLanguage,
                    onLanguageChange = { lang ->
                        LanguageManager.setLanguage(this@MainActivity, lang)
                        appLanguage = lang
                    },
                    initialAutoDeleteEnabled = (currentDescriptor?.autoDeleteTtl ?: 86400) > 0,
                    initialTelegramBridgeEnabled = currentDescriptor?.telegramBridgeEnabled ?: false,
                    initialRedirectionMessage = currentDescriptor?.redirectionMessage ?: "",
                    onSaveRedirectionMessage = { msg ->
                        coroutineScope.launch {
                            telegramClient.updateRedirectionMessage(msg)
                                .onSuccess {
                                    Toast.makeText(this@MainActivity, "Записка для гостей сохранена", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onChangeRoomPassword = { newPass ->
                        coroutineScope.launch {
                            telegramClient.changeRoomPassword(newPass)
                                .onSuccess {
                                    Toast.makeText(this@MainActivity, "Пароль комнаты успешно изменён", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка смены пароля: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onToggleAutoDelete = { enabled ->
                        coroutineScope.launch {
                            telegramClient.updateAutoDeleteTtl(enabled)
                                .onSuccess {
                                    val msg = if (enabled) "Временные сообщения (24ч) включены" else "Временные сообщения отключены"
                                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onToggleTelegramBridge = { enabled ->
                        coroutineScope.launch {
                            telegramClient.updateTelegramBridge(enabled)
                                .onSuccess {
                                    val msg = if (enabled) "Трансляция в Telegram включена" else "Трансляция в Telegram отключена (Dark Room)"
                                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
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
                    onDeleteRoom = {
                        coroutineScope.launch {
                            telegramClient.deleteRoom()
                                .onSuccess {
                                    Toast.makeText(this@MainActivity, "Комната удалена из сети", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, "Ошибка удаления: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
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
                            text = I18n.updateModalTitle(this@MainActivity, s.newVersion),
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = I18n.updateModalDesc(this@MainActivity),
                                color = TelegramTextPrimary,
                                fontSize = 14.sp
                            )
                            if (s.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = I18n.whatsNew(this@MainActivity),
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
                            Text(I18n.installUpdateBtn(this@MainActivity), fontWeight = FontWeight.Bold)
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
                            text = I18n.downloadingUpdateTitle(this@MainActivity),
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = I18n.downloadProgressText(this@MainActivity, s.progressPercent),
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
                                text = I18n.downloadWaitText(this@MainActivity),
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
                            text = I18n.updateDownloadedTitle(this@MainActivity),
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = I18n.updateDownloadedDesc(this@MainActivity),
                                color = TelegramTextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = I18n.installNowPrompt(this@MainActivity),
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
                            Text(I18n.installNowBtn(this@MainActivity), fontWeight = FontWeight.Bold)
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
                            text = I18n.updateErrorTitle(this@MainActivity),
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
                            Text(I18n.gotItBtn(this@MainActivity))
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
