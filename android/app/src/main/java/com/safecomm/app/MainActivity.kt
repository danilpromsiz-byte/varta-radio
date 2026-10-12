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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.safecomm.app.data.model.*
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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

    private var googleAccountEmail by mutableStateOf<String?>(null)

    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            googleAccountEmail = account?.email
            account?.displayName?.let { name ->
                if (telegramClient.callSign.startsWith("Позывной-")) {
                    telegramClient.callSign = name.split(" ").firstOrNull() ?: name
                }
            }
            Toast.makeText(this, "Вход Google: ${account?.email}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Google sign-in error", e)
            Toast.makeText(this, "Google вход: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun signOutGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            googleAccountEmail = null
            Toast.makeText(this, "Вы вышли из Google", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Check if user was previously signed in with Google
        val lastAccount = GoogleSignIn.getLastSignedInAccount(this)
        googleAccountEmail = lastAccount?.email

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
        val activeMembers by telegramClient.activeMembers.collectAsState()

        val appUpdateManager = remember { AppUpdateManager(this@MainActivity) }
        val updateStatus by appUpdateManager.updateStatus.collectAsState()

        var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
        var voiceMessages by remember { mutableStateOf<List<VoiceMessage>>(emptyList()) }

        var isConnected by remember { mutableStateOf(false) }
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
                        isConnected = event.isConnected
                    }
                    is TelegramLiveEvent.LiveVoice -> {
                        // Immediately play loud over loudspeaker!
                        walkieTalkieManager.playLiveVoiceMessage(event.fileUrl, event.senderNick)
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
                // Screen 1: Enter custom call-sign and exact community sector (no chat listing)
                AuthCommunityScreen(
                    currentCallSign = telegramClient.callSign,
                    initialCommunity = telegramClient.communityLinkOrTag,
                    initialPassword = telegramClient.communityPassword,
                    googleAccountEmail = googleAccountEmail,
                    onGoogleSignInClick = { signInWithGoogle() },
                    onGoogleSignOutClick = { signOutGoogle() },
                    onEnterCommunity = { callSign, communityTarget, groupPassword ->
                        coroutineScope.launch {
                            telegramClient.callSign = callSign
                            telegramClient.verifyCodeAndSignIn("OK", communityTarget, groupPassword)
                                .onSuccess {
                                    VartaRadioService.start(this@MainActivity, communityTarget, callSign)
                                    Toast.makeText(this@MainActivity, "Сектор «$communityTarget» подключён", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )
            }
            is TelegramAuthState.Ready -> {
                // Screen 2: Active Walkie-Talkie & Map Operations Hub
                val activeGroup = Group(
                    id = 1L,
                    name = state.activeCommunityName,
                    inviteCode = state.activeCommunityName
                )

                val currentMembers = activeMembers.ifEmpty {
                    listOf(
                        GroupMember(
                            userId = 1L,
                            nickname = "${telegramClient.callSign} (Вы)",
                            role = "admin"
                        )
                    )
                }

                MainHubScreen(
                    group = activeGroup,
                    members = currentMembers,
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
                    botToken = TelegramConfig.APP_TITLE,
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
                    onSaveSettings = { radius, sound, _, chat, pass ->
                        alertRadiusKm = radius
                        soundEnabled = sound
                        if (chat.isNotBlank()) telegramClient.communityLinkOrTag = chat
                        if (pass.isNotBlank()) telegramClient.communityPassword = pass
                        Toast.makeText(this@MainActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                    },
                    onHapticTick = {
                        alertNotificationManager.triggerHapticFeedback(isLong = false)
                    }
                )
            }
        }

        // --- GITHUB UPDATE AVAILABLE DIALOG ---
        if (updateStatus is UpdateStatus.UpdateAvailable) {
            val updateInfo = updateStatus as UpdateStatus.UpdateAvailable
            AlertDialog(
                onDismissRequest = { appUpdateManager.resetStatus() },
                title = {
                    Text(
                        text = "🚀 Доступно обновление: ${updateInfo.newVersion}",
                        color = TelegramTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "В репозитории GitHub найдена новая версия приложения.\n\n${updateInfo.releaseNotes.ifBlank { "Рекомендуется обновить приложение для стабильной связи." }}",
                        color = TelegramTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                appUpdateManager.downloadAndInstallApk(updateInfo.downloadUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertFireOrange)
                    ) {
                        Text("Обновить сейчас")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appUpdateManager.resetStatus() }) {
                        Text("Позже", color = TelegramTextSecondary)
                    }
                },
                containerColor = TelegramDarkSurface
            )
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
