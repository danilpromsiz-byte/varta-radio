package com.safecomm.app.data.websocket

import android.util.Log
import com.safecomm.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit

sealed class WSEvent {
    data class VoiceReceived(val payload: WSVoicePayload, val groupId: Long) : WSEvent()
    data class AlertReceived(val alert: Alert, val groupId: Long) : WSEvent()
    data class AlertResolved(val alertId: Long, val groupId: Long) : WSEvent()
    data class ConnectionState(val isConnected: Boolean) : WSEvent()
}

class SafeCommWebSocket {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _events = MutableSharedFlow<WSEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<WSEvent> = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isManualDisconnect = false
    private var currentUrl: String = ""

    fun connect(baseUrl: String, token: String, groupId: Long) {
        if (token.isBlank() || groupId <= 0) return

        isManualDisconnect = false
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws?token=$token&group_id=$groupId"

        currentUrl = wsUrl
        connectInternal(wsUrl)
    }

    private fun connectInternal(url: String) {
        webSocket?.cancel()

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SafeCommWS", "WebSocket connected!")
                scope.launch { _events.emit(WSEvent.ConnectionState(true)) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SafeCommWS", "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SafeCommWS", "WebSocket closed")
                scope.launch { _events.emit(WSEvent.ConnectionState(false)) }
                reconnectIfNeeded()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SafeCommWS", "WebSocket error: ${t.message}")
                scope.launch { _events.emit(WSEvent.ConnectionState(false)) }
                reconnectIfNeeded()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val root = json.parseToJsonElement(text).jsonObject
            val type = root["type"]?.jsonPrimitive?.content ?: return
            val groupId = root["group_id"]?.jsonPrimitive?.longOrNull ?: 0L
            val payload = root["payload"] ?: return

            when (type) {
                "voice" -> {
                    val voicePayload = json.decodeFromJsonElement<WSVoicePayload>(payload)
                    scope.launch { _events.emit(WSEvent.VoiceReceived(voicePayload, groupId)) }
                }
                "alert" -> {
                    val alertPayload = json.decodeFromJsonElement<WSAlertPayload>(payload)
                    scope.launch { _events.emit(WSEvent.AlertReceived(alertPayload.alert, groupId)) }
                }
                "alert_resolved" -> {
                    val resPayload = json.decodeFromJsonElement<WSResolvedPayload>(payload)
                    scope.launch { _events.emit(WSEvent.AlertResolved(resPayload.alertId, groupId)) }
                }
            }
        } catch (e: Exception) {
            Log.e("SafeCommWS", "Failed to parse message: $text", e)
        }
    }

    private fun reconnectIfNeeded() {
        if (!isManualDisconnect && currentUrl.isNotBlank()) {
            scope.launch {
                delay(3000)
                Log.d("SafeCommWS", "Reconnecting...")
                connectInternal(currentUrl)
            }
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
