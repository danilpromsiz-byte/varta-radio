package com.safecomm.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AlertType(val rawValue: String, val label: String, val icon: String, val colorHex: String) {
    @SerialName("shelling")
    SHELLING("shelling", "Обстрел", "💥", "#E53935"),

    @SerialName("fire")
    FIRE("fire", "Пожар", "🔥", "#FF9800"),

    @SerialName("medical")
    MEDICAL("medical", "Медпомощь", "🚑", "#4CAF50"),

    @SerialName("police")
    POLICE("police", "Полиция", "🚔", "#2196F3"),

    @SerialName("military")
    MILITARY("military", "Военная", "⚠️", "#FFC107"),

    @SerialName("other")
    OTHER("other", "Другое", "❓", "#9E9E9E");

    companion object {
        fun fromRaw(raw: String): AlertType {
            return entries.firstOrNull { it.rawValue == raw } ?: OTHER
        }
    }
}

@Serializable
data class User(
    val id: Long = 0,
    @SerialName("telegram_id") val telegramId: Long = 0,
    val nickname: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Group(
    val id: Long = 0,
    val name: String = "",
    @SerialName("invite_code") val inviteCode: String = "",
    @SerialName("creator_telegram_id") val creatorTelegramId: Long = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class GroupMember(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val role: String = "member", // "admin" or "member"
    val senderId: String = "",
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
data class VoiceMessage(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("sender_id") val senderId: Long = 0,
    @SerialName("sender_nick") val senderNick: String = "",
    @SerialName("file_path") val filePath: String = "",
    @SerialName("duration_sec") val durationSec: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Alert(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("sender_id") val senderId: Long = 0,
    @SerialName("sender_nick") val senderNick: String = "",
    @SerialName("alert_type") val alertType: String = "other",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    @SerialName("photo_path") val photoPath: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("resolved_at") val resolvedAt: String? = null,
    val expiresAt: Long = 0L
) {
    val typedCategory: AlertType
        get() = AlertType.fromRaw(alertType)
}

@Serializable
data class UserSettings(
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("alert_radius_km") val alertRadiusKm: Double = 2.0,
    @SerialName("sound_enabled") val soundEnabled: Boolean = true
)

// API Request/Response
@Serializable
data class AuthResponse(
    val token: String = "",
    val user: User = User()
)

@Serializable
data class ProfileRequest(
    val nickname: String,
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val password: String
)

@Serializable
data class JoinGroupRequest(
    @SerialName("invite_code") val inviteCode: String,
    val password: String
)

@Serializable
data class UpdateSettingsRequest(
    @SerialName("alert_radius_km") val alertRadiusKm: Double,
    @SerialName("sound_enabled") val soundEnabled: Boolean
)

// WebSocket Message
@Serializable
data class WSMessage(
    val type: String, // "voice", "alert", "alert_resolved"
    @SerialName("group_id") val groupId: Long = 0
)

@Serializable
data class WSVoicePayload(
    @SerialName("sender_id") val senderId: Long = 0,
    @SerialName("sender_nick") val senderNick: String = "",
    @SerialName("audio_url") val audioUrl: String = "",
    @SerialName("duration_sec") val durationSec: Int = 0,
    @SerialName("message_id") val messageId: Long = 0
)

@Serializable
data class WSAlertPayload(
    val alert: Alert = Alert()
)

@Serializable
data class WSResolvedPayload(
    @SerialName("alert_id") val alertId: Long = 0
)
