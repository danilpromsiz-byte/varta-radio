package com.safecomm.app.data.model

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RoomHistoryItem(
    val roomName: String,
    val savedPassword: String = "",
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val lastJoinedAt: Long = System.currentTimeMillis()
)

object RoomHistoryManager {
    private const val PREFS_NAME = "varta_room_history_prefs"
    private const val KEY_HISTORY = "history_rooms_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun getHistory(context: Context): List<RoomHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<RoomHistoryItem>>(raw)
                .sortedByDescending { it.lastJoinedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addOrUpdateRoom(
        context: Context,
        roomName: String,
        password: String,
        isOwner: Boolean,
        isAdmin: Boolean
    ) {
        val clean = roomName.trim()
        if (clean.isBlank()) return

        val current = getHistory(context).toMutableList()
        val index = current.indexOfFirst { it.roomName.equals(clean, ignoreCase = true) }
        val existing = if (index >= 0) current[index] else null

        val updated = RoomHistoryItem(
            roomName = clean,
            savedPassword = if (password.isNotBlank()) password.trim() else (existing?.savedPassword ?: ""),
            isOwner = isOwner || (existing?.isOwner == true),
            isAdmin = isAdmin || isOwner || (existing?.isAdmin == true),
            lastJoinedAt = System.currentTimeMillis()
        )

        if (index >= 0) {
            current.removeAt(index)
        }
        current.add(0, updated)
        saveHistory(context, current)
    }

    fun updateRole(
        context: Context,
        roomName: String,
        isOwner: Boolean,
        isAdmin: Boolean
    ) {
        val clean = roomName.trim()
        if (clean.isBlank()) return

        val current = getHistory(context).toMutableList()
        val index = current.indexOfFirst { it.roomName.equals(clean, ignoreCase = true) }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(
                isOwner = isOwner || old.isOwner,
                isAdmin = isAdmin || isOwner || old.isAdmin
            )
            saveHistory(context, current)
        }
    }

    fun removeRoom(context: Context, roomName: String) {
        val clean = roomName.trim()
        val current = getHistory(context).filterNot { it.roomName.equals(clean, ignoreCase = true) }
        saveHistory(context, current)
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(context: Context, items: List<RoomHistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = json.encodeToString(items.take(40))
        prefs.edit().putString(KEY_HISTORY, raw).apply()
    }
}
