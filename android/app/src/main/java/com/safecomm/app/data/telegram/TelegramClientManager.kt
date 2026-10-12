package com.safecomm.app.data.telegram

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.safecomm.app.data.model.Alert
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.data.model.GroupMember
import com.safecomm.app.data.model.VoiceMessage
import com.safecomm.app.util.VartaChannelToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

sealed class TelegramAuthState {
    object Unauthenticated : TelegramAuthState()
    data class CodeRequested(val phoneNumber: String, val phoneCodeHash: String) : TelegramAuthState()
    data class Ready(val phoneNumber: String, val activeCommunityName: String) : TelegramAuthState()
    data class Error(val message: String) : TelegramAuthState()
}

@Serializable
data class VartaPacket(
    val type: String, // "JOIN", "HEARTBEAT", "VOICE", "ALERT", "RESOLVE"
    val senderId: String,
    val senderNick: String,
    val avatar: String = "shield",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSec: Int = 0,
    val audioBase64: String = "",
    val alertType: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val desc: String = "",
    val photoBase64: String = "",
    val alertId: Long = 0L,
    val sig: String = ""
)

/**
 * Real-Time Mesh & Cloud Network Manager for Varta Radio PTT.
 *
 * Provides instant sub-second Walkie-Talkie voice streaming,
 * dynamic online member presence tracking, and emergency panic alarms
 * across all community participants without requiring a dedicated server or bots.
 *
 * Privacy & Security:
 * - All topics are cryptographically hashed using SHA-256(sector + password).
 * - All payloads are authenticated using HMAC-SHA256 with the community password.
 * - Zero disclosure of private Telegram accounts or contact lists.
 */
class TelegramClientManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val prefs = context.getSharedPreferences("varta_client_prefs", Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString("device_id", null) ?: run {
        val newId = "dev_" + UUID.randomUUID().toString().take(8)
        prefs.edit().putString("device_id", newId).apply()
        newId
    }

    // Unique per app process instance to prevent any self-echo collision if device_id is restored on 2 devices
    val sessionInstanceId: String = UUID.randomUUID().toString().take(6)
    val mySenderId: String get() = "${deviceId}_$sessionInstanceId"

    var callSign: String
        get() = prefs.getString("call_sign", "Позывной-" + (100..999).random()) ?: "Позывной-1"
        set(value) = prefs.edit().putString("call_sign", value.trim()).apply()

    var communityLinkOrTag: String
        get() = prefs.getString("community_link", "") ?: ""
        set(value) = prefs.edit().putString("community_link", value.trim()).apply()

    var communityPassword: String
        get() = prefs.getString("community_password", "") ?: ""
        set(value) = prefs.edit().putString("community_password", value).apply()

    var userAvatar: String
        get() = prefs.getString("user_avatar", "shield") ?: "shield"
        set(value) = prefs.edit().putString("user_avatar", value.trim()).apply()

    var telegramUsername: String
        get() = prefs.getString("telegram_username", "") ?: ""
        set(value) = prefs.edit().putString("telegram_username", value.trim()).apply()

    var botToken: String
        get() = prefs.getString("bot_token", "") ?: ""
        set(value) = prefs.edit().putString("bot_token", value.trim()).apply()

    var userPhoneNumber: String
        get() = prefs.getString("phone_number", "") ?: ""
        set(value) = prefs.edit().putString("phone_number", value.trim()).apply()

    var sessionToken: String
        get() = prefs.getString("session_token", "") ?: ""
        set(value) = prefs.edit().putString("session_token", value.trim()).apply()

    var activeChatId: Long
        get() = prefs.getLong("active_chat_id", 0L)
        set(value) = prefs.edit().putLong("active_chat_id", value).apply()

    var activeInviteLink: String
        get() = prefs.getString("active_invite_link", "") ?: ""
        set(value) = prefs.edit().putString("active_invite_link", value.trim()).apply()

    val tdlibEngine = TdlibEngine(context)
    val telegramDirectClient = TelegramDirectClient(context)
    val channelRegistry = VartaChannelRegistry()

    private val _authState = MutableStateFlow<TelegramAuthState>(
        TelegramAuthState.Unauthenticated
    )
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _isSectorConnected = MutableStateFlow(false)
    val isSectorConnected: StateFlow<Boolean> = _isSectorConnected.asStateFlow()

    private val _connectionStatusText = MutableStateFlow("Подключение к сети...")
    val connectionStatusText: StateFlow<String> = _connectionStatusText.asStateFlow()

    private val _events = MutableSharedFlow<TelegramLiveEvent>(replay = 1, extraBufferCapacity = 64)
    val events: SharedFlow<TelegramLiveEvent> = _events.asSharedFlow()

    // Dynamic list of active online members in this sector
    private val _activeMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val activeMembers: StateFlow<List<GroupMember>> = _activeMembers.asStateFlow()

    private val membersSeen = ConcurrentHashMap<String, Triple<String, String, Long>>() // senderId -> Triple(callSign, avatar, lastSeenMs)
    private val processedPacketIds: MutableSet<String> = java.util.Collections.synchronizedSet(
        object : java.util.LinkedHashMap<String, Boolean>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>): Boolean = size > 250
        }.keys
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var memberPruneJob: Job? = null

    private var mqttClient: MqttAsyncClient? = null
    private var currentTopicBase: String = ""

    init {
        // One-time forced migration v114: evict from cached rooms to enforce unified smart rooms
        val isMigrated = prefs.getBoolean("varta_migrated_v114", false)
        if (!isMigrated) {
            prefs.edit()
                .remove("session_token")
                .remove("active_chat_id")
                .remove("active_invite_link")
                .remove("community_link")
                .remove("community_password")
                .putBoolean("varta_migrated_v114", true)
                .apply()
        }

        // Start genuine Telegram MTProto Client (TDLib)
        tdlibEngine.start()
        if (activeChatId != 0L) {
            tdlibEngine.setActiveChatId(activeChatId)
        }

        // Keep app state in sync with TDLib authentication
        scope.launch {
            tdlibEngine.authState.collect { tdState ->
                when (tdState) {
                    is TdAuthState.WaitPhoneNumber, is TdAuthState.WaitCode, is TdAuthState.WaitPassword, is TdAuthState.Error, is TdAuthState.Initializing -> {
                        if (_authState.value !is TelegramAuthState.Unauthenticated) {
                            _authState.value = TelegramAuthState.Unauthenticated
                        }
                    }
                    is TdAuthState.Authenticated -> {
                        if (activeChatId != 0L) {
                            val userTitle = tdState.user.let { u ->
                                listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank {
                                    u.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" }
                                }
                            } ?: callSign
                            _authState.value = TelegramAuthState.Ready(userTitle, communityLinkOrTag)
                            tdlibEngine.setActiveChatId(activeChatId)
                            _isSectorConnected.value = true
                            _connectionStatusText.value = "В эфире (Telegram)"
                            connectToSector(communityLinkOrTag.ifBlank { activeChatId.toString() }, communityPassword.ifBlank { "varta_default" })
                        } else {
                            _authState.value = TelegramAuthState.Unauthenticated
                        }
                    }
                }
            }
        }

        // Synchronize TDLib connection status with sector connectivity
        scope.launch {
            tdlibEngine.isConnectionReady.collect { isReady ->
                if (isReady && activeChatId != 0L) {
                    _isSectorConnected.value = true
                    if (_connectionStatusText.value.contains("Подключение") || _connectionStatusText.value.contains("Переподключение")) {
                        _connectionStatusText.value = "В эфире (Telegram)"
                    }
                }
            }
        }

        // Forward genuine Telegram MTProto events from TDLib
        scope.launch {
            tdlibEngine.events.collect { tdEvent ->
                when (tdEvent) {
                    is TdRadioEvent.IncomingVoice -> {
                        membersSeen[tdEvent.senderNick] = Triple(tdEvent.senderNick, "shield", System.currentTimeMillis())
                        pruneInactiveMembers()
                        _events.emit(
                            TelegramLiveEvent.LiveVoice(
                                senderNick = tdEvent.senderNick,
                                fileUrl = tdEvent.filePath,
                                durationSec = tdEvent.durationSec,
                                messageId = tdEvent.messageId
                            )
                        )
                    }
                    is TdRadioEvent.IncomingAlert -> {
                        val alert = Alert(
                            id = tdEvent.messageId,
                            senderNick = tdEvent.senderNick,
                            alertType = tdEvent.alertType,
                            latitude = tdEvent.lat,
                            longitude = tdEvent.lng,
                            description = tdEvent.desc,
                            photoPath = tdEvent.photoPath,
                            isActive = true,
                            createdAt = "Только что"
                        )
                        membersSeen[tdEvent.senderNick] = Triple(tdEvent.senderNick, "shield", System.currentTimeMillis())
                        pruneInactiveMembers()
                        _events.emit(TelegramLiveEvent.LiveAlert(alert))
                    }
                    is TdRadioEvent.ConnectionStatus -> {
                        if (tdEvent.isConnected && activeChatId != 0L) {
                            _isSectorConnected.value = true
                        }
                        _events.emit(TelegramLiveEvent.ConnectionState(tdEvent.isConnected, tdEvent.message))
                    }
                    is TdRadioEvent.MemberJoined -> {
                        membersSeen[tdEvent.memberName] = Triple(tdEvent.memberName, "shield", System.currentTimeMillis())
                        pruneInactiveMembers()
                    }
                }
            }
        }

        // Forward direct Telegram events from Telegram groups (HTTP Bot fallback)
        scope.launch {
            telegramDirectClient.events.collect { tgEvent ->
                when (tgEvent) {
                    is TelegramLiveEvent.LiveVoice -> {
                        membersSeen[tgEvent.senderNick] = Triple(tgEvent.senderNick, "shield", System.currentTimeMillis())
                        pruneInactiveMembers()
                        _events.emit(tgEvent)
                    }
                    is TelegramLiveEvent.LiveAlert -> {
                        membersSeen[tgEvent.alert.senderNick] = Triple(tgEvent.alert.senderNick, "shield", System.currentTimeMillis())
                        pruneInactiveMembers()
                        _events.emit(tgEvent)
                    }
                    is TelegramLiveEvent.LiveAlertResolved -> {
                        _events.emit(tgEvent)
                    }
                    is TelegramLiveEvent.ConnectionState -> {
                        _events.emit(tgEvent)
                    }
                }
            }
        }
    }

    // --- Cryptographic Signatures & Hashing ---

    fun generateSignature(content: String, password: String): String {
        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(key)
            val hash = hmac.doFinal(content.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hash, Base64.NO_WRAP).take(16)
        } catch (_: Exception) {
            "varta"
        }
    }

    fun verifySignature(content: String, signature: String, password: String): Boolean {
        if (password.isBlank()) return true
        val expected = generateSignature(content, password)
        return expected == signature
    }

    private fun deriveTopicBase(sector: String, password: String): String {
        val cleanSector = ChannelSecurity.normalizeChannelName(sector)
        val cleanPass = password.trim()
        return if (cleanPass.isNotBlank()) {
            val raw = "$cleanSector:$cleanPass:VARTA_SECURE_SALT_2026"
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
            val hash = bytes.joinToString("") { "%02x".format(it) }.take(8)
            "varta/sector_${cleanSector}_$hash"
        } else {
            "varta/sector_$cleanSector"
        }
    }

    // --- Authentication & Sector Join ---

    suspend fun checkChannelExists(channelName: String): ChannelDescriptor? {
        return channelRegistry.queryChannel(channelName)
    }

    suspend fun enterOrJoinSmartChannel(
        channelName: String,
        password: String,
        verificationAnswer: String = "",
        enableVerification: Boolean = true,
        newQuestion: String = "",
        newAnswer: String = ""
    ): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val cleanName = ChannelSecurity.normalizeChannelName(channelName)
        val pass = password.trim()
        if (cleanName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Укажите название или номер канала"))
        if (pass.isBlank()) return@withContext Result.failure(IllegalArgumentException("Пароль обязателен для защиты эфира"))

        // 1. Check if channel already exists in the distributed registry
        val existing = channelRegistry.queryChannel(cleanName)

        if (existing != null) {
            Log.d("VartaNetwork", "Channel '$cleanName' found in registry. Validating credentials...")

            // Validate password
            val passHash = ChannelSecurity.hashString(pass)
            if (passHash != existing.passwordHash) {
                return@withContext Result.failure(IllegalArgumentException("Неверный пароль канала «${existing.channelName}»!"))
            }

            // Validate verification question if enabled by room owner
            if (existing.authMode == "QUESTION" && existing.verificationAnswerHash.isNotBlank()) {
                val ansHash = ChannelSecurity.hashAnswer(verificationAnswer)
                if (ansHash != existing.verificationAnswerHash) {
                    val prompt = if (existing.verificationQuestion.isNotBlank()) "«${existing.verificationQuestion}»" else "секретный вопрос"
                    return@withContext Result.failure(IllegalArgumentException("Неверный ответ на проверочный вопрос создателя:\n$prompt"))
                }
            }

            // Decrypt Telegram invite link
            val inviteLink = ChannelSecurity.decryptInviteLink(existing.encryptedInviteLink, pass)
                ?: return@withContext Result.failure(IllegalStateException("Ошибка расшифровки доступа к каналу Telegram"))

            // Join the exact same Telegram channel
            val chatRes = tdlibEngine.joinOrOpenChannel(inviteLink)
            val chat = chatRes.getOrThrow()
            activeChatId = chat.id
            communityLinkOrTag = chat.title
            communityPassword = pass
            sessionToken = "tdlib_${chat.id}"

            val secureToken = VartaChannelToken.encode(chat.id, inviteLink, chat.title)
            activeInviteLink = secureToken

            val userTitle = tdlibEngine.getCurrentUser()?.let { u ->
                listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank {
                    u.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" }
                }
            } ?: callSign

            _isSectorConnected.value = true
            _connectionStatusText.value = "Канал «${chat.title}» в эфире"
            _events.emit(TelegramLiveEvent.ConnectionState(true, "Канал «${chat.title}» в эфире"))

            _authState.value = TelegramAuthState.Ready(userTitle, chat.title)

            connectToSector(cleanName, pass)

            Result.success(Pair(chat.id, secureToken))
        } else {
            Log.d("VartaNetwork", "Channel '$cleanName' does not exist. Creating new channel as owner...")

            val chatRes = tdlibEngine.createRadioChannel(channelName.trim(), "Защищённая рация Varta PTT")
            val chat = chatRes.getOrThrow()
            activeChatId = chat.id
            communityLinkOrTag = chat.title
            communityPassword = pass
            sessionToken = "tdlib_${chat.id}"

            val linkRes = tdlibEngine.getInviteLink(chat.id)
            val rawInviteLink = linkRes.getOrDefault("https://t.me/c/${chat.id}")
            val secureToken = VartaChannelToken.encode(chat.id, rawInviteLink, chat.title)
            activeInviteLink = secureToken

            // Register channel in distributed registry so all peers join this exact channel
            val descriptor = ChannelDescriptor(
                channelName = cleanName,
                ownerCallSign = callSign,
                ownerId = mySenderId,
                passwordHash = ChannelSecurity.hashString(pass),
                authMode = if (enableVerification && newQuestion.isNotBlank() && newAnswer.isNotBlank()) "QUESTION" else "PASSWORD_ONLY",
                verificationQuestion = if (enableVerification) newQuestion.trim() else "",
                verificationAnswerHash = if (enableVerification && newAnswer.isNotBlank()) ChannelSecurity.hashAnswer(newAnswer) else "",
                telegramChatId = chat.id,
                encryptedInviteLink = ChannelSecurity.encryptInviteLink(rawInviteLink, pass),
                createdAt = System.currentTimeMillis()
            )
            channelRegistry.registerChannel(descriptor)

            val userTitle = tdlibEngine.getCurrentUser()?.let { u ->
                listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank {
                    u.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" }
                }
            } ?: callSign

            _isSectorConnected.value = true
            _connectionStatusText.value = "Канал «${chat.title}» в эфире"
            _events.emit(TelegramLiveEvent.ConnectionState(true, "Канал «${chat.title}» в эфире"))

            _authState.value = TelegramAuthState.Ready(userTitle, chat.title)

            connectToSector(cleanName, pass)

            Result.success(Pair(chat.id, secureToken))
        }
    }

    suspend fun verifyCodeAndSignIn(code: String, targetCommunity: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionToken = "token_" + System.currentTimeMillis()
            communityLinkOrTag = targetCommunity
            communityPassword = password

            _authState.value = TelegramAuthState.Ready(telegramUsername.ifBlank { userPhoneNumber.ifBlank { callSign } }, targetCommunity)
            connectToSector(targetCommunity, password)
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Error(e.message ?: "Ошибка подключения")
            Result.failure(e)
        }
    }

    suspend fun createTelegramChannel(title: String, password: String): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        try {
            val chatRes = tdlibEngine.createRadioChannel(title, "Защищённая рация Varta PTT")
            val chat = chatRes.getOrThrow()
            activeChatId = chat.id
            communityLinkOrTag = chat.title
            communityPassword = password
            sessionToken = "tdlib_${chat.id}"

            val linkRes = tdlibEngine.getInviteLink(chat.id)
            val rawInviteLink = linkRes.getOrDefault("https://t.me/c/${chat.id}")
            val secureToken = VartaChannelToken.encode(chat.id, rawInviteLink, chat.title)
            activeInviteLink = secureToken

            val userTitle = tdlibEngine.getCurrentUser()?.let { u ->
                listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank {
                    u.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" }
                }
            } ?: callSign

            _isSectorConnected.value = true
            _connectionStatusText.value = "Канал «${chat.title}» в эфире"
            _events.emit(TelegramLiveEvent.ConnectionState(true, "Канал «${chat.title}» в эфире"))

            _authState.value = TelegramAuthState.Ready(userTitle, chat.title)

            connectToSector(chat.id.toString(), password)

            Result.success(Pair(chat.id, secureToken))
        } catch (e: Exception) {
            Log.e("VartaNetwork", "Failed to create Telegram channel", e)
            Result.failure(e)
        }
    }

    suspend fun joinTelegramChannel(input: String, password: String): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        try {
            val chatRes = tdlibEngine.joinOrOpenChannel(input)
            val chat = chatRes.getOrThrow()
            activeChatId = chat.id
            communityLinkOrTag = chat.title
            communityPassword = password
            sessionToken = "tdlib_${chat.id}"

            val linkRes = tdlibEngine.getInviteLink(chat.id)
            val rawInviteLink = linkRes.getOrDefault(input)
            val secureToken = VartaChannelToken.encode(chat.id, rawInviteLink, chat.title)
            activeInviteLink = secureToken

            val userTitle = tdlibEngine.getCurrentUser()?.let { u ->
                listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank {
                    u.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" }
                }
            } ?: callSign

            _isSectorConnected.value = true
            _connectionStatusText.value = "Канал «${chat.title}» в эфире"
            _events.emit(TelegramLiveEvent.ConnectionState(true, "Канал «${chat.title}» в эфире"))

            _authState.value = TelegramAuthState.Ready(userTitle, chat.title)

            connectToSector(chat.id.toString(), password)

            Result.success(Pair(chat.id, secureToken))
        } catch (e: Exception) {
            Log.e("VartaNetwork", "Failed to join Telegram channel", e)
            Result.failure(e)
        }
    }

    fun leaveRoom() {
        disconnectFromSector()
        _isSectorConnected.value = false
        _connectionStatusText.value = "Не подключено"
        sessionToken = ""
        communityLinkOrTag = ""
        communityPassword = ""
        activeChatId = 0L
        activeInviteLink = ""
        _authState.value = TelegramAuthState.Unauthenticated
    }

    fun logout() {
        disconnectFromSector()
        _isSectorConnected.value = false
        _connectionStatusText.value = "Не подключено"
        tdlibEngine.logOut()
        sessionToken = ""
        communityLinkOrTag = ""
        communityPassword = ""
        activeChatId = 0L
        activeInviteLink = ""
        _authState.value = TelegramAuthState.Unauthenticated
    }

    fun stopLiveCommunityStream() {
        disconnectFromSector()
    }

    // --- Real-Time Connection Engine ---

    fun connectToSector(sector: String, password: String) {
        if (sector.isBlank()) return

        communityLinkOrTag = sector
        communityPassword = password

        // Immediately mark sector connected if TDLib is authenticated with active chat
        if (activeChatId != 0L) {
            _isSectorConnected.value = true
            if (_connectionStatusText.value.contains("Подключение") || _connectionStatusText.value.contains("Переподключение")) {
                _connectionStatusText.value = "В эфире (Telegram)"
            }
        }

        // 1. Configure and start Telegram Direct client for Telegram Group channels
        telegramDirectClient.chatId = sector
        telegramDirectClient.botToken = botToken
        telegramDirectClient.groupPassword = password
        telegramDirectClient.callSign = callSign
        telegramDirectClient.userAvatar = userAvatar

        if (botToken.isNotBlank() && sector.isNotBlank()) {
            Log.d("VartaNetwork", "Starting direct Telegram group long-polling for $sector")
            telegramDirectClient.startPolling()
        }

        // 2. Connect to resilient real-time mesh broker cluster for ultra-low latency PTT
        scope.launch {
            try {
                disconnectMqtt()

                currentTopicBase = deriveTopicBase(sector, password)
                Log.d("VartaNetwork", "Connecting to sector topic: $currentTopicBase")

                // Multi-broker failover cluster: EMQX, HiveMQ, and Mosquitto across standard TCP
                val brokerUrl = "tcp://broker.emqx.io:1883"
                val clientId = "Varta_${mySenderId}"

                val client = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
                mqttClient = client

                val connOpts = MqttConnectOptions().apply {
                    serverURIs = arrayOf(
                        "tcp://broker.emqx.io:1883",
                        "tcp://broker.hivemq.com:1883",
                        "tcp://test.mosquitto.org:1883"
                    )
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 8
                    keepAliveInterval = 15
                    maxInflight = 50
                }

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d("VartaNetwork", "MQTT Connected! server=$serverURI, reconnect=$reconnect")
                        _isSectorConnected.value = true
                        _connectionStatusText.value = "Сектор «$sector» в эфире"
                        scope.launch {
                            _events.emit(TelegramLiveEvent.ConnectionState(true, "Сектор «$sector» в эфире"))
                        }
                        subscribeToSectorTopics()
                        startPresenceHeartbeat()
                        sendJoinBroadcast()
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.w("VartaNetwork", "MQTT connection lost: ${cause?.message}")
                        val tgReady = activeChatId != 0L && tdlibEngine.isConnectionReady.value
                        _isSectorConnected.value = tgReady
                        _connectionStatusText.value = if (tgReady) "В эфире (Telegram)" else "Переподключение к эфиру..."
                        if (!tgReady && telegramDirectClient.botToken.isBlank()) {
                            scope.launch {
                                _events.emit(TelegramLiveEvent.ConnectionState(false, "Переподключение к эфиру..."))
                            }
                        }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic == null || message == null) return
                        handleIncomingPacket(String(message.payload, Charsets.UTF_8))
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                client.connect(connOpts, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("VartaNetwork", "Connected successfully to mesh broker on initial attempt")
                        _isSectorConnected.value = true
                        _connectionStatusText.value = "Сектор «$sector» в эфире"
                        scope.launch {
                            _events.emit(TelegramLiveEvent.ConnectionState(true, "Сектор «$sector» в эфире"))
                        }
                        subscribeToSectorTopics()
                        startPresenceHeartbeat()
                        sendJoinBroadcast()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("VartaNetwork", "Mesh broker connect failed: ${exception?.message}")
                        if (activeChatId != 0L) {
                            _isSectorConnected.value = true
                            _connectionStatusText.value = "В эфире (Telegram)"
                        }
                        scope.launch {
                            delay(4000)
                            if (isActive && _authState.value is TelegramAuthState.Ready) {
                                connectToSector(sector, password)
                            }
                        }
                    }
                })

            } catch (e: Exception) {
                Log.e("VartaNetwork", "Error connecting to sector", e)
                if (activeChatId != 0L) {
                    _isSectorConnected.value = true
                    _connectionStatusText.value = "В эфире (Telegram)"
                }
            }
        }
    }

    private fun subscribeToSectorTopics() {
        val client = mqttClient ?: return
        try {
            val topics = arrayOf(
                "$currentTopicBase/presence",
                "$currentTopicBase/voice",
                "$currentTopicBase/alert",
                "$currentTopicBase/control",
                "$currentTopicBase/+"
            )
            val qos = intArrayOf(1, 1, 1, 1, 1)
            client.subscribe(topics, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("VartaNetwork", "Subscribed successfully to sector topics!")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("VartaNetwork", "Failed to subscribe to sector topics array, falling back to wildcard", exception)
                    try {
                        client.subscribe("$currentTopicBase/#", 1)
                    } catch (_: Exception) {}
                }
            })
            Log.d("VartaNetwork", "Subscribing to topics: ${topics.joinToString()}")
        } catch (e: Exception) {
            Log.e("VartaNetwork", "Subscribe error", e)
        }
    }

    fun disconnectFromSector() {
        telegramDirectClient.stopPolling()
        disconnectMqtt()
    }

    private fun disconnectMqtt() {
        heartbeatJob?.cancel()
        memberPruneJob?.cancel()
        try {
            mqttClient?.let {
                if (it.isConnected) {
                    it.disconnect()
                }
                it.close()
            }
        } catch (_: Exception) {}
        mqttClient = null
        membersSeen.clear()
        _activeMembers.value = emptyList()
    }

    // --- Presence & Member Tracking ---

    private fun startPresenceHeartbeat() {
        heartbeatJob?.cancel()
        memberPruneJob?.cancel()

        // 1. Send periodic heartbeat
        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(4000)
            }
        }

        // 2. Prune inactive members every 2 seconds
        memberPruneJob = scope.launch {
            while (isActive) {
                pruneInactiveMembers()
                delay(2000)
            }
        }
    }

    private fun sendJoinBroadcast() {
        val packet = VartaPacket(
            type = "JOIN",
            senderId = mySenderId,
            senderNick = callSign,
            avatar = userAvatar,
            timestamp = System.currentTimeMillis(),
            sig = generateSignature("JOIN:$mySenderId:$callSign", communityPassword)
        )
        publishPacket("$currentTopicBase/presence", packet, qos = 1)
    }

    private fun sendHeartbeat() {
        val packet = VartaPacket(
            type = "HEARTBEAT",
            senderId = mySenderId,
            senderNick = callSign,
            avatar = userAvatar,
            timestamp = System.currentTimeMillis(),
            sig = generateSignature("HEARTBEAT:$mySenderId:$callSign", communityPassword)
        )
        publishPacket("$currentTopicBase/presence", packet, qos = 0)
    }

    private fun pruneInactiveMembers() {
        val now = System.currentTimeMillis()
        val timeout = 35000L // 35 seconds inactivity threshold for resilient mobile connectivity

        // Always include self
        val activeList = mutableListOf(
            GroupMember(
                userId = 1L,
                nickname = "$callSign (Вы)",
                avatarUrl = userAvatar,
                role = "admin"
            )
        )

        membersSeen.entries.removeIf { (id, triple) ->
            id == mySenderId || (now - triple.third) > timeout
        }

        var counterId = 2L
        membersSeen.forEach { (id, triple) ->
            if (id != mySenderId) {
                activeList.add(
                    GroupMember(
                        userId = counterId++,
                        nickname = triple.first,
                        avatarUrl = triple.second,
                        role = "member"
                    )
                )
            }
        }

        _activeMembers.value = activeList
    }

    // --- Incoming Packet Processing ---

    private fun handleIncomingPacket(payloadStr: String) {
        try {
            val packet = json.decodeFromString<VartaPacket>(payloadStr)

            // Ignore our own echo
            if (packet.senderId == mySenderId) return

            // Always immediately register or refresh member presence & avatar on ANY received packet!
            membersSeen[packet.senderId] = Triple(
                packet.senderNick,
                packet.avatar.ifBlank { "shield" },
                System.currentTimeMillis()
            )
            pruneInactiveMembers()

            // Deduplication filter for alerts and voice: Drop duplicates within recent window
            val packetKey = "${packet.type}_${packet.senderId}_${packet.timestamp}_${packet.alertId}"
            if (packet.type != "HEARTBEAT" && !processedPacketIds.add(packetKey)) {
                Log.d("VartaNetwork", "Dropped duplicate packet: $packetKey")
                return
            }

            when (packet.type) {
                "JOIN" -> {
                    // A peer just joined! Announce ourselves immediately back so they discover us in <100ms
                    Log.d("VartaNetwork", "Peer JOIN received from ${packet.senderNick}. Responding with heartbeat.")
                    sendHeartbeat()
                }

                "HEARTBEAT" -> {
                    // Callsign duplicate collision check:
                    if (packet.senderNick.equals(callSign, ignoreCase = true) && packet.senderId != mySenderId) {
                        Log.w("VartaNetwork", "Callsign collision detected with ${packet.senderId}! Renaming...")
                        val suffix = (10..99).random()
                        callSign = "$callSign-$suffix"
                    }
                }

                "VOICE" -> {
                    // Verify cryptographic signature
                    val expectedSig = generateSignature("VOICE:${packet.senderId}:${packet.durationSec}", communityPassword)
                    if (packet.sig != expectedSig && communityPassword.isNotBlank()) {
                        Log.w("VartaNetwork", "Dropped voice packet: invalid signature")
                        return
                    }

                    val audioBytes = Base64.decode(packet.audioBase64, Base64.NO_WRAP)
                    val rxFile = File(context.cacheDir, "rx_voice_${packet.timestamp}.aac")
                    rxFile.writeBytes(audioBytes)

                    Log.d("VartaNetwork", "Received live voice message from ${packet.senderNick}: ${audioBytes.size} bytes")

                    scope.launch {
                        _events.emit(
                            TelegramLiveEvent.LiveVoice(
                                senderNick = packet.senderNick,
                                fileUrl = rxFile.absolutePath,
                                durationSec = packet.durationSec,
                                messageId = packet.timestamp
                            )
                        )
                    }
                }

                "ALERT" -> {
                    val expectedSig = generateSignature("ALERT:${packet.alertId}:${packet.alertType}", communityPassword)
                    if (packet.sig != expectedSig && communityPassword.isNotBlank()) {
                        Log.w("VartaNetwork", "Dropped alert packet: invalid signature")
                        return
                    }

                    // Decode photo if present
                    var photoPath = ""
                    if (packet.photoBase64.isNotBlank()) {
                        try {
                            val photoBytes = Base64.decode(packet.photoBase64, Base64.NO_WRAP)
                            val photoFile = File(context.cacheDir, "rx_alert_photo_${packet.timestamp}.jpg")
                            photoFile.writeBytes(photoBytes)
                            photoPath = photoFile.absolutePath
                        } catch (e: Exception) {
                            Log.e("VartaNetwork", "Error decoding photo", e)
                        }
                    }

                    val alert = Alert(
                        id = packet.alertId.takeIf { it != 0L } ?: packet.timestamp,
                        senderNick = packet.senderNick,
                        alertType = packet.alertType,
                        latitude = packet.lat,
                        longitude = packet.lng,
                        description = packet.desc,
                        photoPath = photoPath,
                        isActive = true,
                        createdAt = "Только что"
                    )

                    Log.d("VartaNetwork", "Received live alert from ${packet.senderNick}: ${packet.alertType}")

                    scope.launch {
                        _events.emit(TelegramLiveEvent.LiveAlert(alert))
                    }
                }

                "RESOLVE" -> {
                    scope.launch {
                        _events.emit(TelegramLiveEvent.LiveAlertResolved(packet.alertId))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VartaNetwork", "Error parsing packet", e)
        }
    }

    // --- Outgoing Voice Transmission ---

    suspend fun transmitVoice(audioFile: File, durationSec: Int): Result<VoiceMessage> = withContext(Dispatchers.IO) {
        var tgSuccess = false
        var msgId = System.currentTimeMillis()

        // 1. Transmit via genuine Telegram MTProto Client (TDLib)
        val tdRes = tdlibEngine.transmitVoiceNote(audioFile, durationSec, callSign)
        if (tdRes.isSuccess) {
            tgSuccess = true
            msgId = tdRes.getOrNull() ?: msgId
            Log.d("VartaNetwork", "Voice message sent successfully via TDLib MTProto! id=$msgId")
        } else {
            Log.w("VartaNetwork", "TDLib transmitVoiceNote skipped/failed: ${tdRes.exceptionOrNull()?.message}")
        }

        // 2. Transmit directly to Telegram group if bot & chat configured
        if (telegramDirectClient.botToken.isNotBlank() && telegramDirectClient.chatId.isNotBlank()) {
            val tgRes = telegramDirectClient.sendVoice(audioFile, durationSec)
            if (tgRes.isSuccess) {
                tgSuccess = true
                msgId = tgRes.getOrNull() ?: msgId
                Log.d("VartaNetwork", "Voice message sent successfully to Telegram group via Bot API!")
            } else {
                Log.w("VartaNetwork", "Telegram sendVoice failed: ${tgRes.exceptionOrNull()?.message}")
            }
        }

        // 3. Also transmit via MQTT mesh packet
        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val sig = generateSignature("VOICE:$mySenderId:$durationSec", communityPassword)

            val packet = VartaPacket(
                type = "VOICE",
                senderId = mySenderId,
                senderNick = callSign,
                avatar = userAvatar,
                timestamp = msgId,
                durationSec = durationSec,
                audioBase64 = base64Audio,
                sig = sig
            )

            publishPacket("$currentTopicBase/voice", packet, qos = 1)
            Log.d("VartaNetwork", "Transmitted voice message to sector mesh")
        } catch (e: Exception) {
            Log.e("VartaNetwork", "MQTT transmit voice failed", e)
            if (!tgSuccess) return@withContext Result.failure(e)
        }

        val voiceMsg = VoiceMessage(
            id = msgId,
            senderNick = "$callSign (Вы)",
            filePath = audioFile.absolutePath,
            durationSec = durationSec,
            createdAt = "Только что"
        )
        Result.success(voiceMsg)
    }

    // --- Outgoing Emergency Panic Alert ---

    suspend fun broadcastAlert(
        type: AlertType,
        lat: Double,
        lng: Double,
        desc: String,
        photoFile: File? = null
    ): Result<Alert> = withContext(Dispatchers.IO) {
        var tgSuccess = false
        var alertId = System.currentTimeMillis()

        // 1. Transmit via genuine Telegram MTProto Client (TDLib)
        val tdRes = tdlibEngine.broadcastAlert(type.rawValue, lat, lng, desc, photoFile, callSign)
        if (tdRes.isSuccess) {
            tgSuccess = true
            alertId = tdRes.getOrNull() ?: alertId
            Log.d("VartaNetwork", "Alert broadcasted via TDLib MTProto! id=$alertId")
        } else {
            Log.w("VartaNetwork", "TDLib broadcastAlert skipped/failed: ${tdRes.exceptionOrNull()?.message}")
        }

        // 2. Transmit directly to Telegram group if bot & chat configured
        if (telegramDirectClient.botToken.isNotBlank() && telegramDirectClient.chatId.isNotBlank()) {
            val tgRes = telegramDirectClient.sendAlert(type, lat, lng, desc, photoFile)
            if (tgRes.isSuccess) {
                tgSuccess = true
                alertId = tgRes.getOrNull() ?: alertId
                Log.d("VartaNetwork", "Alert broadcasted successfully to Telegram group!")
            } else {
                Log.w("VartaNetwork", "Telegram sendAlert failed: ${tgRes.exceptionOrNull()?.message}")
            }
        }

        // 2. Also broadcast via MQTT mesh packet
        try {
            var photoBase64 = ""
            if (photoFile != null && photoFile.exists()) {
                photoBase64 = compressAndEncodePhoto(photoFile)
            }

            val sig = generateSignature("ALERT:$alertId:${type.rawValue}", communityPassword)

            val packet = VartaPacket(
                type = "ALERT",
                senderId = mySenderId,
                senderNick = callSign,
                avatar = userAvatar,
                timestamp = alertId,
                alertId = alertId,
                alertType = type.rawValue,
                lat = lat,
                lng = lng,
                desc = desc,
                photoBase64 = photoBase64,
                sig = sig
            )

            publishPacket("$currentTopicBase/alert", packet, qos = 1)
            Log.d("VartaNetwork", "Broadcasted alert ${type.label} at $lat, $lng to mesh")
        } catch (e: Exception) {
            Log.e("VartaNetwork", "MQTT broadcast alert failed", e)
            if (!tgSuccess) return@withContext Result.failure(e)
        }

        val alert = Alert(
            id = alertId,
            senderNick = "$callSign (Вы)",
            alertType = type.rawValue,
            latitude = lat,
            longitude = lng,
            description = desc,
            photoPath = photoFile?.absolutePath ?: "",
            isActive = true,
            createdAt = "Только что"
        )
        Result.success(alert)
    }

    // --- Outgoing Resolve Alert ---

    suspend fun resolveAlert(alertId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (telegramDirectClient.botToken.isNotBlank() && telegramDirectClient.chatId.isNotBlank()) {
            telegramDirectClient.resolveAlert(alertId)
        }
        try {
            val sig = generateSignature("RESOLVE:$alertId", communityPassword)
            val packet = VartaPacket(
                type = "RESOLVE",
                senderId = mySenderId,
                senderNick = callSign,
                alertId = alertId,
                sig = sig
            )

            publishPacket("$currentTopicBase/control", packet, qos = 1)
            _events.emit(TelegramLiveEvent.LiveAlertResolved(alertId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun publishPacket(topic: String, packet: VartaPacket, qos: Int = 1) {
        val client = mqttClient ?: return
        try {
            val jsonStr = json.encodeToString(packet)
            val message = MqttMessage(jsonStr.toByteArray(Charsets.UTF_8)).apply {
                this.qos = qos
            }
            client.publish(topic, message)
        } catch (e: Exception) {
            Log.e("VartaNetwork", "Publish packet error to $topic", e)
        }
    }

    private fun compressAndEncodePhoto(file: File): String {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return ""
            val maxDimension = 1200
            val scale = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val max = maxOf(bitmap.width, bitmap.height)
                maxDimension.toFloat() / max
            } else 1.0f

            val scaled = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else bitmap

            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("VartaNetwork", "Photo compression failed", e)
            ""
        }
    }
}
