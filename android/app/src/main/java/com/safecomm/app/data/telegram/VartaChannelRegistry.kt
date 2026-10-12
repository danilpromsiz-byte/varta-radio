package com.safecomm.app.data.telegram

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume

@Serializable
data class ChannelDescriptor(
    val channelName: String,
    val ownerCallSign: String,
    val ownerId: String,
    val passwordHash: String,
    val authMode: String = "QUESTION", // "QUESTION" (manual approval) or "PASSWORD_ONLY"
    val verificationQuestion: String = "",
    val verificationAnswerHash: String = "",
    val telegramBridgeEnabled: Boolean = false, // false = Dark room (invisible in Telegram), true = bridged
    val telegramChatId: Long = 0L,
    val encryptedInviteLink: String = "",
    val autoDeleteTtl: Int = 86400, // 86400 = 24 hours (default ephemeral mode), 0 = disabled
    val redirectionMessage: String = "", // Announcement / redirect message for visitors (e.g. "We are in room 24")
    val isAbandoned: Boolean = false,    // Marked true when owner leaves/closes room
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class KnockRequest(
    val requestId: String = java.util.UUID.randomUUID().toString(),
    val channelName: String,
    val guestCallSign: String,
    val guestSenderId: String,
    val question: String = "",
    val answer: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class KnockResponse(
    val requestId: String,
    val status: String, // "APPROVED" or "REJECTED"
    val reason: String = "",
    val encryptedInviteLink: String = "",
    val ownerCallSign: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object ChannelSecurity {
    fun hashString(input: String): String {
        val sha = MessageDigest.getInstance("SHA-256")
        val bytes = sha.digest(input.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashAnswer(answer: String): String {
        return hashString(answer.trim().lowercase())
    }

    fun encryptInviteLink(inviteLink: String, password: String): String {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            val key = SecretKeySpec(keyBytes, "AES")
            val iv = ByteArray(16) { ((it * 23 + 41) and 0xFF).toByte() }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val encrypted = cipher.doFinal(inviteLink.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (_: Exception) {
            inviteLink
        }
    }

    fun decryptInviteLink(encryptedBase64: String, password: String): String? {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            val key = SecretKeySpec(keyBytes, "AES")
            val iv = ByteArray(16) { ((it * 23 + 41) and 0xFF).toByte() }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun normalizeChannelName(raw: String): String {
        val trimmed = raw.trim().lowercase()
        val cleaned = trimmed.replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁіІїЇєЄ_-]"), "_")
        return cleaned.ifBlank { "default" }
    }
}

/**
 * Distributed Channel Registry for Varta Radio.
 *
 * Stores and resolves channel metadata across participants via retained MQTT topics:
 * varta/registry/<normalized_channel_name>
 */
class VartaChannelRegistry {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val brokerUrls = arrayOf(
        "tcp://broker.emqx.io:1883",
        "tcp://broker.hivemq.com:1883",
        "tcp://test.mosquitto.org:1883"
    )

    private fun getTopicForChannel(channelName: String): String {
        val clean = ChannelSecurity.normalizeChannelName(channelName)
        return "varta/registry/$clean"
    }

    /**
     * Publishes a channel descriptor with retained=true so any peer joining with the same name
     * immediately discovers the existing room and its Telegram group.
     */
    suspend fun registerChannel(descriptor: ChannelDescriptor): Result<Unit> = withContext(Dispatchers.IO) {
        val topic = getTopicForChannel(descriptor.channelName)
        val payload = json.encodeToString(descriptor)
        var client: MqttAsyncClient? = null

        try {
            val clientId = "RegPub_" + java.util.UUID.randomUUID().toString().take(8)
            client = MqttAsyncClient(brokerUrls[0], clientId, MemoryPersistence())
            val connOpts = MqttConnectOptions().apply {
                serverURIs = brokerUrls
                connectionTimeout = 5
                keepAliveInterval = 10
                isCleanSession = true
            }

            suspendCancellableCoroutine<Unit> { cont ->
                client.connect(connOpts, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        try {
                            val msg = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                                qos = 1
                                isRetained = true
                            }
                            client.publish(topic, msg, null, object : IMqttActionListener {
                                override fun onSuccess(t: IMqttToken?) {
                                    Log.d("VartaRegistry", "Channel registered with retain on $topic")
                                    if (cont.isActive) cont.resume(Unit)
                                }
                                override fun onFailure(t: IMqttToken?, e: Throwable?) {
                                    Log.w("VartaRegistry", "Publish failed on $topic", e)
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            })
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.w("VartaRegistry", "Connect for register failed: ${exception?.message}")
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VartaRegistry", "Error registering channel", e)
            Result.failure(e)
        } finally {
            try {
                if (client?.isConnected == true) client?.disconnect()
                client?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Queries the registry for an existing channel with the given name.
     * Returns ChannelDescriptor if it exists, or null if no channel is registered.
     */
    suspend fun queryChannel(channelName: String): ChannelDescriptor? = withContext(Dispatchers.IO) {
        val topic = getTopicForChannel(channelName)
        var client: MqttAsyncClient? = null
        var foundDescriptor: ChannelDescriptor? = null

        try {
            val clientId = "RegQuery_" + java.util.UUID.randomUUID().toString().take(8)
            client = MqttAsyncClient(brokerUrls[0], clientId, MemoryPersistence())
            val connOpts = MqttConnectOptions().apply {
                serverURIs = brokerUrls
                connectionTimeout = 4
                keepAliveInterval = 10
                isCleanSession = true
            }

            withTimeoutOrNull(2500) {
                suspendCancellableCoroutine<Unit> { cont ->
                    client.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {}
                        override fun messageArrived(incomingTopic: String?, message: MqttMessage?) {
                            if (message != null && incomingTopic == topic) {
                                val str = String(message.payload, Charsets.UTF_8).trim()
                                if (str.isNotBlank() && str.startsWith("{")) {
                                    try {
                                        foundDescriptor = json.decodeFromString<ChannelDescriptor>(str)
                                        Log.d("VartaRegistry", "Found retained channel descriptor: ${foundDescriptor?.channelName}")
                                    } catch (e: Exception) {
                                        Log.w("VartaRegistry", "Error parsing descriptor: ${e.message}")
                                    }
                                }
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }
                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })

                    client.connect(connOpts, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            try {
                                client.subscribe(topic, 1, null, object : IMqttActionListener {
                                    override fun onSuccess(t: IMqttToken?) {
                                        Log.d("VartaRegistry", "Subscribed to $topic for query")
                                    }
                                    override fun onFailure(t: IMqttToken?, e: Throwable?) {
                                        if (cont.isActive) cont.resume(Unit)
                                    }
                                })
                            } catch (_: Exception) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            if (cont.isActive) cont.resume(Unit)
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.w("VartaRegistry", "Query error: ${e.message}")
        } finally {
            try {
                if (client?.isConnected == true) client?.disconnect()
                client?.close()
            } catch (_: Exception) {}
        }

        foundDescriptor
    }

    fun getKnockTopic(channelName: String): String {
        val clean = ChannelSecurity.normalizeChannelName(channelName)
        return "varta/knock/$clean"
    }

    fun getKnockReplyTopic(channelName: String, guestSenderId: String): String {
        val clean = ChannelSecurity.normalizeChannelName(channelName)
        return "varta/knock_reply/$clean/$guestSenderId"
    }

    /**
     * Guest sends a knock request to the room owner and waits for an APPROVED or REJECTED response.
     */
    suspend fun requestKnockApproval(request: KnockRequest, timeoutMs: Long = 15000L): Result<KnockResponse> = withContext(Dispatchers.IO) {
        val knockTopic = getKnockTopic(request.channelName)
        val replyTopic = getKnockReplyTopic(request.channelName, request.guestSenderId)
        val payload = json.encodeToString(request)
        var client: MqttAsyncClient? = null
        var response: KnockResponse? = null

        try {
            val clientId = "Knock_" + java.util.UUID.randomUUID().toString().take(8)
            client = MqttAsyncClient(brokerUrls[0], clientId, MemoryPersistence())
            val connOpts = MqttConnectOptions().apply {
                serverURIs = brokerUrls
                connectionTimeout = 5
                keepAliveInterval = 10
                isCleanSession = true
            }

            withTimeout(timeoutMs) {
                suspendCancellableCoroutine<Unit> { cont ->
                    client.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {}
                        override fun messageArrived(incomingTopic: String?, message: MqttMessage?) {
                            if (message != null && incomingTopic == replyTopic) {
                                val str = String(message.payload, Charsets.UTF_8).trim()
                                try {
                                    val resp = json.decodeFromString<KnockResponse>(str)
                                    if (resp.requestId == request.requestId || resp.requestId.isNotBlank()) {
                                        response = resp
                                        if (cont.isActive) cont.resume(Unit)
                                    }
                                } catch (e: Exception) {
                                    Log.w("VartaRegistry", "Error parsing knock response: ${e.message}")
                                }
                            }
                        }
                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })

                    client.connect(connOpts, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            try {
                                client.subscribe(replyTopic, 1, null, object : IMqttActionListener {
                                    override fun onSuccess(t: IMqttToken?) {
                                        // Once subscribed to reply, publish the knock request
                                        val msg = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                                            qos = 1
                                            isRetained = false
                                        }
                                        client.publish(knockTopic, msg, null, object : IMqttActionListener {
                                            override fun onSuccess(pubToken: IMqttToken?) {
                                                Log.d("VartaRegistry", "Knock request published to $knockTopic")
                                            }
                                            override fun onFailure(pubToken: IMqttToken?, exception: Throwable?) {
                                                Log.w("VartaRegistry", "Failed to publish knock request: ${exception?.message}")
                                                if (cont.isActive) cont.resume(Unit)
                                            }
                                        })
                                    }
                                    override fun onFailure(t: IMqttToken?, e: Throwable?) {
                                        if (cont.isActive) cont.resume(Unit)
                                    }
                                })
                            } catch (_: Exception) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            if (cont.isActive) cont.resume(Unit)
                        }
                    })
                }
            }

            if (response != null) {
                Result.success(response!!)
            } else {
                Result.failure(Exception("Создатель комнаты не ответил на запрос"))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Создатель комнаты не ответил на запрос (оффлайн или занят)"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                if (client?.isConnected == true) client?.disconnect()
                client?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Room owner sends an approval or rejection response to the guest.
     */
    suspend fun sendKnockResponse(channelName: String, guestSenderId: String, response: KnockResponse): Result<Unit> = withContext(Dispatchers.IO) {
        val replyTopic = getKnockReplyTopic(channelName, guestSenderId)
        val payload = json.encodeToString(response)
        var client: MqttAsyncClient? = null

        try {
            val clientId = "KnockResp_" + java.util.UUID.randomUUID().toString().take(8)
            client = MqttAsyncClient(brokerUrls[0], clientId, MemoryPersistence())
            val connOpts = MqttConnectOptions().apply {
                serverURIs = brokerUrls
                connectionTimeout = 5
                keepAliveInterval = 10
                isCleanSession = true
            }

            suspendCancellableCoroutine<Unit> { cont ->
                client.connect(connOpts, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        try {
                            val msg = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                                qos = 1
                                isRetained = false
                            }
                            client.publish(replyTopic, msg, null, object : IMqttActionListener {
                                override fun onSuccess(t: IMqttToken?) {
                                    Log.d("VartaRegistry", "Knock response published to $replyTopic")
                                    if (cont.isActive) cont.resume(Unit)
                                }
                                override fun onFailure(t: IMqttToken?, e: Throwable?) {
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            })
                        } catch (_: Exception) {
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                if (client?.isConnected == true) client?.disconnect()
                client?.close()
            } catch (_: Exception) {}
        }
    }
}
