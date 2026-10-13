package com.safecomm.app.data.telegram

import android.content.Context
import android.util.Base64
import android.util.Log
import com.safecomm.app.data.model.Alert
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.data.model.VoiceMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class SafeCommMetadata(
    val v: Int = 1,
    val nick: String,
    val avatar: String = "shield",
    val action: String, // "VOICE", "ALERT", "RESOLVE"
    val threat: String = "other",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val desc: String = "",
    val sig: String = "" // HMAC signature verifying group password
)

sealed class TelegramLiveEvent {
    data class LiveVoice(
        val senderNick: String,
        val fileUrl: String,
        val durationSec: Int,
        val messageId: Long,
        val roomName: String = "",
        val isFromOtherRoom: Boolean = false
    ) : TelegramLiveEvent()

    data class LiveAlert(
        val alert: Alert,
        val roomName: String = "",
        val isFromOtherRoom: Boolean = false
    ) : TelegramLiveEvent()

    data class LiveAlertResolved(
        val alertId: Long,
        val roomName: String = ""
    ) : TelegramLiveEvent()

    data class ConnectionState(
        val isConnected: Boolean,
        val message: String = ""
    ) : TelegramLiveEvent()
}

class TelegramDirectClient(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val prefs = context.getSharedPreferences("safecomm_tg_prefs", Context.MODE_PRIVATE)

    var botToken: String
        get() = prefs.getString("bot_token", "") ?: ""
        set(value) = prefs.edit().putString("bot_token", value.trim()).apply()

    var chatId: String
        get() = prefs.getString("chat_id", "") ?: ""
        set(value) = prefs.edit().putString("chat_id", value.trim()).apply()

    var groupPassword: String
        get() = prefs.getString("group_password", "") ?: ""
        set(value) = prefs.edit().putString("group_password", value).apply()

    var callSign: String
        get() {
            val saved = prefs.getString("call_sign", null)
            if (!saved.isNullOrBlank()) return saved
            val generated = "Позывной-" + (100..999).random()
            prefs.edit().putString("call_sign", generated).apply()
            return generated
        }
        set(value) {
            val trimmed = value.trim()
            if (trimmed.isNotBlank()) {
                prefs.edit().putString("call_sign", trimmed).apply()
            }
        }

    var userAvatar: String
        get() = prefs.getString("user_avatar", "shield") ?: "shield"
        set(value) = prefs.edit().putString("user_avatar", value.trim()).apply()

    var groupName: String
        get() = prefs.getString("group_name", "Telegram Сектор") ?: "Telegram Сектор"
        set(value) = prefs.edit().putString("group_name", value.trim()).apply()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // Long polling needs generous timeout
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<TelegramLiveEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<TelegramLiveEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastUpdateId: Long = 0

    private val tgBaseUrl: String
        get() = "https://api.telegram.org/bot$botToken"

    private val tgFileBaseUrl: String
        get() = "https://api.telegram.org/file/bot$botToken"

    // --- Authentication & Signature ---

    private fun generateSignature(content: String, password: String): String {
        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(key)
            val hash = hmac.doFinal(content.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hash, Base64.NO_WRAP).take(16)
        } catch (_: Exception) {
            "demo"
        }
    }

    private fun verifySignature(content: String, signature: String, password: String): Boolean {
        if (password.isBlank()) return true
        val expected = generateSignature(content, password)
        return expected == signature
    }

    // --- Real-Time Polling Directly from Telegram Servers ---

    fun startPolling() {
        if (botToken.isBlank() || chatId.isBlank()) {
            scope.launch {
                _events.emit(TelegramLiveEvent.ConnectionState(false, "Требуется настройка Telegram токена и Chat ID"))
            }
            return
        }

        pollingJob?.cancel()
        pollingJob = scope.launch {
            _events.emit(TelegramLiveEvent.ConnectionState(true, "Подключено к серверам Telegram"))
            Log.d("TelegramDirect", "Starting long-polling to Telegram servers directly...")

            while (isActive) {
                try {
                    val url = "$tgBaseUrl/getUpdates?offset=$lastUpdateId&timeout=25&allowed_updates=[\"message\"]"
                    val request = Request.Builder().url(url).get().build()

                    val response = client.newCall(request).execute()
                    val bodyString = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        parseTelegramUpdates(bodyString)
                    } else {
                        Log.e("TelegramDirect", "getUpdates HTTP ${response.code}: $bodyString")
                        _events.emit(TelegramLiveEvent.ConnectionState(false, "Ошибка Telegram API: ${response.code}"))
                        delay(4000)
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e("TelegramDirect", "Polling network error: ${e.message}")
                        _events.emit(TelegramLiveEvent.ConnectionState(false, "Переподключение к Telegram..."))
                        delay(3000)
                    }
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun parseTelegramUpdates(jsonStr: String) {
        try {
            val root = json.parseToJsonElement(jsonStr).jsonObject
            val ok = root["ok"]?.jsonPrimitive?.content == "true"
            if (!ok) return

            val resultArr = root["result"]?.let { json.decodeFromJsonElement<List<TelegramUpdate>>(it) } ?: emptyList()

            for (update in resultArr) {
                if (update.updateId >= lastUpdateId) {
                    lastUpdateId = update.updateId + 1
                }

                val msg = update.message ?: continue
                if (msg.chat.id.toString() != chatId && msg.chat.id.toString() != chatId.replace("-100", "-")) {
                    continue // Ignore messages from other chats
                }

                handleIncomingMessage(msg)
            }
        } catch (e: Exception) {
            Log.e("TelegramDirect", "Error parsing updates", e)
        }
    }

    private suspend fun handleIncomingMessage(msg: TelegramMessage) {
        val captionOrText = msg.caption ?: msg.text ?: ""

        // Check if message has SafeComm metadata tag: #SafeComm:{...}
        val tagPrefix = "#SafeComm:"
        if (!captionOrText.contains(tagPrefix)) return

        val metaJson = captionOrText.substringAfter(tagPrefix).trim().lines().firstOrNull() ?: return
        val metadata = try {
            json.decodeFromString<SafeCommMetadata>(metaJson)
        } catch (_: Exception) {
            return
        }

        // Verify password signature if group has password
        if (groupPassword.isNotBlank()) {
            val contentToVerify = "${metadata.nick}:${metadata.action}:${metadata.threat}"
            if (!verifySignature(contentToVerify, metadata.sig, groupPassword)) {
                Log.w("TelegramDirect", "Rejected message: invalid password signature!")
                return
            }
        }

        when (metadata.action) {
            "VOICE" -> {
                val voice = msg.voice ?: msg.audio ?: return
                // Resolve Telegram direct CDN file URL
                val fileUrl = getTelegramFileUrl(voice.fileId)
                if (fileUrl != null) {
                    _events.emit(
                        TelegramLiveEvent.LiveVoice(
                            senderNick = metadata.nick,
                            fileUrl = fileUrl,
                            durationSec = voice.duration ?: 0,
                            messageId = msg.messageId
                        )
                    )
                }
            }
            "ALERT" -> {
                val photoUrl = msg.photo?.lastOrNull()?.let { getTelegramFileUrl(it.fileId) } ?: ""
                val alert = Alert(
                    id = msg.messageId,
                    groupId = msg.chat.id,
                    senderId = msg.from?.id ?: 0,
                    senderNick = metadata.nick,
                    alertType = metadata.threat,
                    latitude = if (msg.location != null) msg.location.latitude else metadata.lat,
                    longitude = if (msg.location != null) msg.location.longitude else metadata.lng,
                    description = metadata.desc,
                    photoPath = photoUrl,
                    isActive = true,
                    createdAt = msg.date.toString()
                )
                _events.emit(TelegramLiveEvent.LiveAlert(alert))
            }
            "RESOLVE" -> {
                _events.emit(TelegramLiveEvent.LiveAlertResolved(msg.messageId))
            }
        }
    }

    // --- Sending Directly to Telegram Cloud ---

    suspend fun sendVoice(audioFile: File, durationSec: Int): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val contentToSign = "$callSign:VOICE:other"
            val sig = generateSignature(contentToSign, groupPassword)
            val metadata = SafeCommMetadata(
                nick = callSign,
                avatar = userAvatar,
                action = "VOICE",
                sig = sig
            )
            val caption = "🎙 Голосовое от $callSign\n#SafeComm:${json.encodeToString(metadata)}"

            val fileBody = audioFile.asRequestBody("audio/ogg".toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("voice", audioFile.name, fileBody)
                .addFormDataPart("duration", durationSec.toString())
                .addFormDataPart("caption", caption)
                .build()

            val request = Request.Builder()
                .url("$tgBaseUrl/sendVoice")
                .post(multipart)
                .build()

            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Telegram sendVoice failed: ${response.code} $respStr"))
            }

            val msgId = parseMessageId(respStr)
            Result.success(msgId)
        } catch (e: Exception) {
            Log.e("TelegramDirect", "sendVoice error", e)
            Result.failure(e)
        }
    }

    suspend fun sendAlert(
        type: AlertType,
        lat: Double,
        lng: Double,
        desc: String,
        photoFile: File? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val contentToSign = "$callSign:ALERT:${type.rawValue}"
            val sig = generateSignature(contentToSign, groupPassword)
            val metadata = SafeCommMetadata(
                nick = callSign,
                avatar = userAvatar,
                action = "ALERT",
                threat = type.rawValue,
                lat = lat,
                lng = lng,
                desc = desc,
                sig = sig
            )
            val caption = "${type.icon} ТРЕВОГА: ${type.label}\nПозывной: $callSign\n${desc.ifBlank { "Координаты зафиксированы" }}\n#SafeComm:${json.encodeToString(metadata)}"

            val request = if (photoFile != null && photoFile.exists()) {
                // Send Photo
                val photoBody = photoFile.asRequestBody("image/jpeg".toMediaType())
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("photo", photoFile.name, photoBody)
                    .addFormDataPart("caption", caption)
                    .build()

                Request.Builder().url("$tgBaseUrl/sendPhoto").post(multipart).build()
            } else {
                // Send Location + text message
                val jsonPayload = """
                    {
                        "chat_id": "$chatId",
                        "latitude": $lat,
                        "longitude": $lng
                    }
                """.trimIndent().toRequestBody("application/json".toMediaType())

                // Also send structured text for caption
                val locReq = Request.Builder().url("$tgBaseUrl/sendLocation").post(jsonPayload).build()
                client.newCall(locReq).execute() // Fire location pin

                val textBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("text", caption)
                    .build()

                Request.Builder().url("$tgBaseUrl/sendMessage").post(textBody).build()
            }

            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Telegram sendAlert failed: ${response.code} $respStr"))
            }

            val msgId = parseMessageId(respStr)
            Result.success(msgId)
        } catch (e: Exception) {
            Log.e("TelegramDirect", "sendAlert error", e)
            Result.failure(e)
        }
    }

    suspend fun resolveAlert(alertId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentToSign = "$callSign:RESOLVE:other"
            val sig = generateSignature(contentToSign, groupPassword)
            val metadata = SafeCommMetadata(
                nick = callSign,
                action = "RESOLVE",
                sig = sig
            )
            val text = "✅ ТРЕВОГА СНЯТА (Опасность миновала)\n#SafeComm:${json.encodeToString(metadata)}"

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("reply_to_message_id", alertId.toString())
                .addFormDataPart("text", text)
                .build()

            val request = Request.Builder().url("$tgBaseUrl/sendMessage").post(body).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Resolve failed: ${response.code}"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Helper to resolve direct audio/photo download link from Telegram CDN ---

    suspend fun getTelegramFileUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$tgBaseUrl/getFile?file_id=$fileId"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null

            val root = json.parseToJsonElement(body).jsonObject
            val filePath = root["result"]?.jsonObject?.get("file_path")?.jsonPrimitive?.content
            if (filePath != null) {
                return@withContext "$tgFileBaseUrl/$filePath"
            }
            null
        } catch (e: Exception) {
            Log.e("TelegramDirect", "getFile error for $fileId", e)
            null
        }
    }

    private fun parseMessageId(respStr: String): Long {
        return try {
            val root = json.parseToJsonElement(respStr).jsonObject
            root["result"]?.jsonObject?.get("message_id")?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

// --- Minimal DTOs for Telegram Updates ---

@Serializable
data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,
    val message: TelegramMessage? = null
)

@Serializable
data class TelegramMessage(
    @SerialName("message_id") val messageId: Long,
    val from: TelegramFrom? = null,
    val chat: TelegramChat,
    val date: Long = 0,
    val text: String? = null,
    val caption: String? = null,
    val voice: TelegramVoice? = null,
    val audio: TelegramVoice? = null,
    val photo: List<TelegramPhotoSize>? = null,
    val location: TelegramLocation? = null
)

@Serializable
data class TelegramFrom(
    val id: Long,
    val first_name: String = "",
    val username: String? = null
)

@Serializable
data class TelegramChat(
    val id: Long,
    val title: String? = null,
    val type: String = "supergroup"
)

@Serializable
data class TelegramVoice(
    @SerialName("file_id") val fileId: String,
    val duration: Int? = 0
)

@Serializable
data class TelegramPhotoSize(
    @SerialName("file_id") val fileId: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class TelegramLocation(
    val latitude: Double,
    val longitude: Double
)
