package com.safecomm.app.data.telegram

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import com.safecomm.app.util.VartaChannelToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class TdAuthState {
    object Initializing : TdAuthState()
    object WaitPhoneNumber : TdAuthState()
    data class WaitCode(val phoneNumber: String) : TdAuthState()
    object WaitPassword : TdAuthState()
    data class Authenticated(val user: TdApi.User) : TdAuthState()
    data class Error(val message: String) : TdAuthState()
}

sealed class TdRadioEvent {
    data class IncomingVoice(
        val senderNick: String,
        val filePath: String,
        val durationSec: Int,
        val messageId: Long
    ) : TdRadioEvent()

    data class IncomingAlert(
        val senderNick: String,
        val alertType: String,
        val lat: Double,
        val lng: Double,
        val desc: String,
        val photoPath: String,
        val messageId: Long
    ) : TdRadioEvent()

    data class MemberJoined(val memberName: String) : TdRadioEvent()
    data class ConnectionStatus(val isConnected: Boolean, val message: String) : TdRadioEvent()
}

/**
 * Genuine Telegram MTProto Client Engine powered by official Telegram TDLib.
 * Connects directly to Telegram DC cloud infrastructure using user's phone number
 * and registered API_ID / API_HASH from my.telegram.org.
 */
class TdlibEngine(private val context: Context) : Client.ResultHandler {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: Client? = null

    private val _authState = MutableStateFlow<TdAuthState>(TdAuthState.Initializing)
    val authState: StateFlow<TdAuthState> = _authState.asStateFlow()

    private val _isConnectionReady = MutableStateFlow(false)
    val isConnectionReady: StateFlow<Boolean> = _isConnectionReady.asStateFlow()

    private val _events = MutableSharedFlow<TdRadioEvent>(replay = 1, extraBufferCapacity = 64)
    val events: SharedFlow<TdRadioEvent> = _events.asSharedFlow()

    private var currentChatId: Long = 0L
    private var currentUser: TdApi.User? = null
    private var pendingPhone: String = ""

    // Map of downloaded voice files: fileId -> message metadata
    private val pendingVoiceDownloads = ConcurrentHashMap<Int, Triple<String, Int, Long>>() // fileId -> (senderNick, duration, msgId)

    init {
        try {
            Client.setLogMessageHandler(0) { level, message ->
                if (level < 2) Log.e("TDLib", "[$level] $message")
            }
        } catch (_: Exception) {}
    }

    fun start() {
        if (client != null) return
        Log.d("TDLib", "Starting official Telegram MTProto Client...")
        client = Client.create(this, null, null)
    }

    fun setActiveChatId(chatId: Long) {
        this.currentChatId = chatId
        Log.d("TDLib", "Active radio chat set to: $chatId")
    }

    fun getActiveChatId(): Long = currentChatId

    // --- Coroutine Send Wrapper ---

    suspend fun <T : TdApi.Object> send(query: TdApi.Function<T>): T = suspendCancellableCoroutine { cont ->
        val c = client
        if (c == null) {
            cont.resumeWithException(IllegalStateException("TDLib client is not running"))
            return@suspendCancellableCoroutine
        }
        c.send(query) { result ->
            if (result is TdApi.Error) {
                Log.e("TDLib", "API error for ${query.javaClass.simpleName}: [${result.code}] ${result.message}")
                cont.resumeWithException(Exception("[${result.code}] ${result.message}"))
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resume(result as T)
            }
        }
    }

    // --- Authentication Actions ---

    fun sendPhoneNumber(phoneNumber: String) {
        val normalized = normalizePhoneNumber(phoneNumber)
        pendingPhone = normalized
        val c = client
        if (c == null) {
            start()
            scope.launch {
                delay(800)
                sendPhoneNumber(phoneNumber)
            }
            return
        }
        val settings = TdApi.PhoneNumberAuthenticationSettings().apply {
            allowFlashCall = false
            allowMissedCall = false
            isCurrentPhoneNumber = true
            allowSmsRetrieverApi = false
        }
        c.send(TdApi.SetAuthenticationPhoneNumber(normalized, settings), this)
    }

    fun restartAuth(retryPhone: String? = null) {
        scope.launch {
            Log.d("TDLib", "Restarting TDLib authentication engine cleanly...")
            _authState.value = TdAuthState.Initializing
            val oldClient = client
            client = null
            try {
                oldClient?.send(TdApi.Close(), null)
            } catch (_: Exception) {}
            delay(400)
            start()
            if (!retryPhone.isNullOrBlank()) {
                delay(800)
                sendPhoneNumber(retryPhone)
            }
        }
    }

    companion object {
        fun normalizePhoneNumber(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            if (digits.isBlank()) return raw.trim()

            if (raw.trim().startsWith("+")) {
                return "+$digits"
            }

            // Ukrainian domestic 10 digits starting with 0: 0997799880 -> +380997799880
            if (digits.length == 10 && digits.startsWith("0")) {
                return "+38$digits"
            }

            // Ukrainian 12 digits with 380: 380997799880 -> +380997799880
            if (digits.length == 12 && digits.startsWith("380")) {
                return "+$digits"
            }

            // 9 digits without leading 0: 997799880 -> +380997799880
            if (digits.length == 9) {
                return "+380$digits"
            }

            // 11 digits starting with 80 (legacy 80...): -> +380...
            if (digits.length == 11 && digits.startsWith("80")) {
                return "+3$digits"
            }

            // Any number starting with 0:
            if (digits.startsWith("0")) {
                return "+38$digits"
            }

            return "+$digits"
        }
    }

    fun checkAuthCode(code: String) {
        val c = client ?: return
        c.send(TdApi.CheckAuthenticationCode(code.trim()), this)
    }

    fun checkAuthPassword(password: String) {
        val c = client ?: return
        c.send(TdApi.CheckAuthenticationPassword(password), this)
    }

    fun logOut() {
        scope.launch {
            if (_authState.value is TdAuthState.Authenticated) {
                try {
                    send(TdApi.LogOut())
                } catch (_: Exception) {
                    restartAuth()
                }
            } else {
                restartAuth()
            }
        }
    }

    // --- Result Handler Callback ---

    override fun onResult(`object`: TdApi.Object?) {
        if (`object` == null) return

        when (`object`) {
            is TdApi.UpdateAuthorizationState -> {
                handleAuthState(`object`.authorizationState)
            }
            is TdApi.UpdateNewMessage -> {
                handleNewMessage(`object`.message)
            }
            is TdApi.UpdateFile -> {
                handleFileUpdate(`object`.file)
            }
            is TdApi.UpdateConnectionState -> {
                handleConnectionState(`object`.state)
            }
            is TdApi.User -> {
                currentUser = `object`
            }
            is TdApi.Error -> {
                Log.e("TDLib", "TDLib API error: [${`object`.code}] ${`object`.message}")
                val upper = `object`.message.uppercase()
                if (upper.contains("PHONE_NUMBER_ALREADY_SET") ||
                    upper.contains("AUTHORIZATIONSTATEWAITCODE") ||
                    upper.contains("METHOD IS NOT AVAILABLE IN STATE")) {
                    Log.w("TDLib", "TDLib stuck in state machine error, triggering clean restart...")
                    restartAuth()
                } else if (_authState.value !is TdAuthState.Authenticated) {
                    _authState.value = TdAuthState.Error(`object`.message)
                }
            }
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val dbDir = File(context.filesDir, "tdlib").apply { mkdirs() }.absolutePath
                val filesDir = File(context.cacheDir, "tdlib_files").apply { mkdirs() }.absolutePath

                val params = TdApi.SetTdlibParameters().apply {
                    useTestDc = false
                    databaseDirectory = dbDir
                    filesDirectory = filesDir
                    useFileDatabase = true
                    useChatInfoDatabase = true
                    useMessageDatabase = true
                    useSecretChats = false
                    apiId = TelegramConfig.API_ID
                    apiHash = TelegramConfig.API_HASH
                    systemLanguageCode = "ru"
                    deviceModel = "Android"
                    systemVersion = Build.VERSION.RELEASE
                    applicationVersion = "1.1.5"
                }
                client?.send(params, this)
            }

            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _authState.value = TdAuthState.WaitPhoneNumber
            }

            is TdApi.AuthorizationStateWaitCode -> {
                _authState.value = TdAuthState.WaitCode(pendingPhone)
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                _authState.value = TdAuthState.WaitPassword
            }

            is TdApi.AuthorizationStateReady -> {
                _isConnectionReady.value = true
                scope.launch {
                    try {
                        val me = send(TdApi.GetMe())
                        currentUser = me
                        _authState.value = TdAuthState.Authenticated(me)
                        Log.d("TDLib", "Telegram Authorized successfully as @${me.usernames?.activeUsernames?.firstOrNull() ?: me.firstName}")
                    } catch (e: Exception) {
                        Log.e("TDLib", "Error getting user profile", e)
                    }
                }
            }

            is TdApi.AuthorizationStateClosed -> {
                _isConnectionReady.value = false
                _authState.value = TdAuthState.WaitPhoneNumber
                client = null
            }

            is TdApi.AuthorizationStateLoggingOut -> {
                _isConnectionReady.value = false
                _authState.value = TdAuthState.Initializing
            }
        }
    }

    private fun handleConnectionState(state: TdApi.ConnectionState) {
        when (state) {
            is TdApi.ConnectionStateReady -> {
                _isConnectionReady.value = true
                scope.launch { _events.emit(TdRadioEvent.ConnectionStatus(true, "Подключено к серверам Telegram")) }
            }
            is TdApi.ConnectionStateConnecting -> {
                _isConnectionReady.value = false
                scope.launch { _events.emit(TdRadioEvent.ConnectionStatus(false, "Подключение к Telegram...")) }
            }
            is TdApi.ConnectionStateWaitingForNetwork -> {
                _isConnectionReady.value = false
                scope.launch { _events.emit(TdRadioEvent.ConnectionStatus(false, "Ожидание сети...")) }
            }
            is TdApi.ConnectionStateUpdating -> {
                _isConnectionReady.value = true
                scope.launch { _events.emit(TdRadioEvent.ConnectionStatus(true, "Синхронизация Telegram...")) }
            }
        }
    }

    private fun handleNewMessage(msg: TdApi.Message) {
        // Only process messages for the active radio channel
        if (currentChatId != 0L && msg.chatId != currentChatId) return

        // Ignore our own messages
        val myId = currentUser?.id ?: 0L
        val senderUserId = (msg.senderId as? TdApi.MessageSenderUser)?.userId ?: 0L
        if (senderUserId == myId) return

        val senderNick = (msg.senderId as? TdApi.MessageSenderUser)?.let { "Участник #${it.userId % 1000}" } ?: "Пользователь"

        when (val content = msg.content) {
            is TdApi.MessageVoiceNote -> {
                val voiceFile = content.voiceNote.voice
                if (voiceFile.local.isDownloadingCompleted) {
                    // Audio already available locally
                    scope.launch {
                        _events.emit(
                            TdRadioEvent.IncomingVoice(
                                senderNick = senderNick,
                                filePath = voiceFile.local.path,
                                durationSec = content.voiceNote.duration,
                                messageId = msg.id
                            )
                        )
                    }
                } else {
                    // Request download from Telegram cloud
                    pendingVoiceDownloads[voiceFile.id] = Triple(senderNick, content.voiceNote.duration, msg.id)
                    client?.send(TdApi.DownloadFile(voiceFile.id, 1, 0, 0, true), this)
                }
            }

            is TdApi.MessageLocation -> {
                scope.launch {
                    _events.emit(
                        TdRadioEvent.IncomingAlert(
                            senderNick = senderNick,
                            alertType = "warning",
                            lat = content.location.latitude,
                            lng = content.location.longitude,
                            desc = "Геолокация из Telegram",
                            photoPath = "",
                            messageId = msg.id
                        )
                    )
                }
            }

            is TdApi.MessageText -> {
                val text = content.text.text
                if (text.contains("ТРЕВОГА") || text.contains("ОПАСНОСТЬ")) {
                    scope.launch {
                        _events.emit(
                            TdRadioEvent.IncomingAlert(
                                senderNick = senderNick,
                                alertType = "general",
                                lat = 0.0,
                                lng = 0.0,
                                desc = text,
                                photoPath = "",
                                messageId = msg.id
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleFileUpdate(file: TdApi.File) {
        if (file.local.isDownloadingCompleted && pendingVoiceDownloads.containsKey(file.id)) {
            val meta = pendingVoiceDownloads.remove(file.id) ?: return
            scope.launch {
                _events.emit(
                    TdRadioEvent.IncomingVoice(
                        senderNick = meta.first,
                        filePath = file.local.path,
                        durationSec = meta.second,
                        messageId = meta.third
                    )
                )
            }
        }
    }

    // --- Channel / Supergroup Management ---

    suspend fun createRadioChannel(title: String, description: String): Result<TdApi.Chat> = withContext(Dispatchers.IO) {
        try {
            val query = TdApi.CreateNewSupergroupChat().apply {
                this.title = title
                this.isForum = false
                this.isChannel = false
                this.description = description
                this.location = null
                this.messageAutoDeleteTime = 0
                this.forImport = false
            }
            val newGroup = send<TdApi.Chat>(query)
            setActiveChatId(newGroup.id)
            hideChatInArchive(newGroup.id)
            Result.success(newGroup)
        } catch (e: Exception) {
            Log.e("TDLib", "Failed to create radio channel", e)
            Result.failure(e)
        }
    }

    suspend fun getInviteLink(chatId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val query = TdApi.CreateChatInviteLink().apply {
                this.chatId = chatId
                this.name = "Приглашение в рацию"
                this.expirationDate = 0
                this.memberLimit = 0
                this.createsJoinRequest = false
            }
            val link = send<TdApi.ChatInviteLink>(query)
            Result.success(link.inviteLink)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinChannelByLink(inviteLink: String): Result<TdApi.Chat> = withContext(Dispatchers.IO) {
        try {
            val query = TdApi.JoinChatByInviteLink().apply {
                this.inviteLink = inviteLink.trim()
            }
            val chat = send<TdApi.Chat>(query)
            setActiveChatId(chat.id)
            hideChatInArchive(chat.id)
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun hideChatInArchive(chatId: Long) {
        scope.launch {
            try {
                send(TdApi.AddChatToList(chatId, TdApi.ChatListArchive()))
                val notif = TdApi.ChatNotificationSettings().apply {
                    muteFor = 2147483647
                    useDefaultMuteFor = false
                }
                send(TdApi.SetChatNotificationSettings(chatId, notif))
                Log.d("TDLib", "Chat $chatId successfully moved to Telegram Archive and muted")
            } catch (e: Exception) {
                Log.w("TDLib", "Failed to archive chat $chatId: ${e.message}")
            }
        }
    }

    suspend fun openChat(chatId: Long): Result<TdApi.Chat> = withContext(Dispatchers.IO) {
        try {
            val query = TdApi.GetChat().apply {
                this.chatId = chatId
            }
            val chat = send<TdApi.Chat>(query)
            setActiveChatId(chat.id)
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChat(chatId: Long): Result<TdApi.Chat> = withContext(Dispatchers.IO) {
        try {
            val query = TdApi.GetChat().apply {
                this.chatId = chatId
            }
            val chat = send<TdApi.Chat>(query)
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPublicChat(username: String): Result<TdApi.Chat> = withContext(Dispatchers.IO) {
        try {
            val clean = username.trim()
                .removePrefix("https://t.me/")
                .removePrefix("http://t.me/")
                .removePrefix("t.me/")
                .removePrefix("@")
            val query = TdApi.SearchPublicChat().apply {
                this.username = clean
            }
            val chat = send<TdApi.Chat>(query)
            setActiveChatId(chat.id)
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinOrOpenChannel(input: String): Result<TdApi.Chat> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(IllegalArgumentException("Канал не указан"))

        // 1. Try Varta Secure Token (VR-...)
        if (VartaChannelToken.isVartaToken(trimmed)) {
            val tokenInfo = VartaChannelToken.decode(trimmed)
            if (tokenInfo != null) {
                if (tokenInfo.inviteLink.isNotBlank()) {
                    val joinRes = joinChannelByLink(tokenInfo.inviteLink)
                    if (joinRes.isSuccess) return@withContext joinRes
                }
                if (tokenInfo.chatId != 0L) {
                    val openRes = openChat(tokenInfo.chatId)
                    if (openRes.isSuccess) return@withContext openRes
                }
            }
        }

        // 2. Try numeric chat ID
        trimmed.toLongOrNull()?.let { numId ->
            val chatRes = openChat(numId)
            if (chatRes.isSuccess) return@withContext chatRes
        }

        // 3. Try invite link (https://t.me/+... or joinchat or +...)
        if (trimmed.contains("/+") || trimmed.contains("joinchat") || trimmed.startsWith("+")) {
            val linkRes = joinChannelByLink(trimmed)
            if (linkRes.isSuccess) return@withContext linkRes
        }

        // 4. Try matching user's own existing Telegram chats by title/name
        val myChats = loadRecentChats(50)
        val matchedChat = myChats.firstOrNull {
            it.title.equals(trimmed, ignoreCase = true) || it.title.contains(trimmed, ignoreCase = true)
        }
        if (matchedChat != null) {
            setActiveChatId(matchedChat.id)
            return@withContext Result.success(matchedChat)
        }

        // 5. Try public chat username
        val pubRes = searchPublicChat(trimmed)
        if (pubRes.isSuccess) return@withContext pubRes

        // 6. Fallback: try join by link directly
        val fallbackRes = joinChannelByLink(trimmed)
        if (fallbackRes.isSuccess) return@withContext fallbackRes

        Result.failure(IllegalArgumentException("Канал «$trimmed» не найден. Создайте закрытый канал или введите защищённый Код (VR-...)."))
    }

    suspend fun loadRecentChats(limit: Int = 30): List<TdApi.Chat> = withContext(Dispatchers.IO) {
        try {
            try {
                send(TdApi.LoadChats(TdApi.ChatListMain(), limit))
            } catch (_: Exception) {}
            val chats = send(TdApi.GetChats(TdApi.ChatListMain(), limit))
            val result = mutableListOf<TdApi.Chat>()
            for (id in chats.chatIds) {
                try {
                    val chat = send(TdApi.GetChat(id))
                    if (chat.type is TdApi.ChatTypeSupergroup || chat.type is TdApi.ChatTypeBasicGroup) {
                        result.add(chat)
                    }
                } catch (_: Exception) {}
            }
            result
        } catch (e: Exception) {
            Log.e("TDLib", "Failed to load recent chats", e)
            emptyList()
        }
    }

    fun getCurrentUser(): TdApi.User? = currentUser

    // --- Outgoing Voice & Alert Transmissions ---

    suspend fun transmitVoiceNote(audioFile: File, durationSec: Int, callSign: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (currentChatId == 0L) {
                return@withContext Result.failure(IllegalStateException("Канал Telegram не выбран"))
            }

            val inputFile = TdApi.InputFileLocal(audioFile.absolutePath)
            val voiceContent = TdApi.InputMessageVoiceNote().apply {
                this.voiceNote = inputFile
                this.duration = durationSec
                this.waveform = null
                this.caption = TdApi.FormattedText("🎙 Рация: $callSign", null)
            }

            val sendQuery = TdApi.SendMessage().apply {
                this.chatId = currentChatId
                this.inputMessageContent = voiceContent
            }

            val msg = send<TdApi.Message>(sendQuery)
            Result.success(msg.id)
        } catch (e: Exception) {
            Log.e("TDLib", "Failed to send voice note to Telegram", e)
            Result.failure(e)
        }
    }

    suspend fun broadcastAlert(
        type: String,
        lat: Double,
        lng: Double,
        desc: String,
        photoFile: File?,
        callSign: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (currentChatId == 0L) {
                return@withContext Result.failure(IllegalStateException("Канал Telegram не выбран"))
            }

            val text = "🚨 ТРЕВОГА: $type\nПозывной: $callSign\n$desc"
            val caption = TdApi.FormattedText(text, null)

            val content: TdApi.InputMessageContent = if (photoFile != null && photoFile.exists()) {
                TdApi.InputMessagePhoto().apply {
                    this.photo = TdApi.InputFileLocal(photoFile.absolutePath)
                    this.caption = caption
                }
            } else if (lat != 0.0 && lng != 0.0) {
                TdApi.InputMessageLocation().apply {
                    this.location = TdApi.Location(lat, lng, 0.0)
                }
            } else {
                TdApi.InputMessageText().apply {
                    this.text = caption
                }
            }

            val sendQuery = TdApi.SendMessage().apply {
                this.chatId = currentChatId
                this.inputMessageContent = content
            }

            val msg = send<TdApi.Message>(sendQuery)
            Result.success(msg.id)
        } catch (e: Exception) {
            Log.e("TDLib", "Failed to send alert to Telegram", e)
            Result.failure(e)
        }
    }
}
