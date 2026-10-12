package models

import "time"

// User represents an app user with a custom nickname and avatar
type User struct {
	ID         int64     `json:"id"`
	TelegramID int64     `json:"telegram_id"`
	Nickname   string    `json:"nickname"`
	AvatarURL  string    `json:"avatar_url"`
	CreatedAt  time.Time `json:"created_at"`
}

// Group represents a closed community
type Group struct {
	ID                int64     `json:"id"`
	Name              string    `json:"name"`
	InviteCode        string    `json:"invite_code"`
	PasswordHash      string    `json:"-"`
	CreatorTelegramID int64     `json:"creator_telegram_id"`
	CreatedAt         time.Time `json:"created_at"`
}

// GroupMember links users to groups with a role
type GroupMember struct {
	ID       int64     `json:"id"`
	GroupID  int64     `json:"group_id"`
	UserID   int64     `json:"user_id"`
	Role     string    `json:"role"` // "admin" or "member"
	JoinedAt time.Time `json:"joined_at"`
}

// VoiceMessage stores walkie-talkie recordings
type VoiceMessage struct {
	ID          int64     `json:"id"`
	GroupID     int64     `json:"group_id"`
	SenderID    int64     `json:"sender_id"`
	SenderNick  string    `json:"sender_nick,omitempty"`
	FilePath    string    `json:"file_path"`
	DurationSec int       `json:"duration_sec"`
	CreatedAt   time.Time `json:"created_at"`
}

// AlertType enumerates danger types
type AlertType string

const (
	AlertShelling AlertType = "shelling"
	AlertFire     AlertType = "fire"
	AlertMedical  AlertType = "medical"
	AlertPolice   AlertType = "police"
	AlertMilitary AlertType = "military"
	AlertOther    AlertType = "other"
)

// AlertTypeInfo provides display metadata for each alert type
type AlertTypeInfo struct {
	Type  AlertType `json:"type"`
	Label string    `json:"label"`
	Icon  string    `json:"icon"`
	Color string    `json:"color"`
	Sound string    `json:"sound"`
}

// AllAlertTypes returns ordered metadata for all alert types
func AllAlertTypes() []AlertTypeInfo {
	return []AlertTypeInfo{
		{AlertShelling, "Обстрел", "💥", "#e74c3c", "siren_loud.mp3"},
		{AlertFire, "Пожар", "🔥", "#e67e22", "fire_alarm.mp3"},
		{AlertMedical, "Медпомощь", "🚑", "#2ecc71", "medical_beep.mp3"},
		{AlertPolice, "Полиция", "🚔", "#3498db", "police_siren.mp3"},
		{AlertMilitary, "Военная", "⚠️", "#f1c40f", "air_raid.mp3"},
		{AlertOther, "Другое", "❓", "#95a5a6", "alert_default.mp3"},
	}
}

// Alert represents a danger report with geolocation
type Alert struct {
	ID          int64     `json:"id"`
	GroupID     int64     `json:"group_id"`
	SenderID    int64     `json:"sender_id"`
	SenderNick  string    `json:"sender_nick,omitempty"`
	AlertType   AlertType `json:"alert_type"`
	Latitude    float64   `json:"latitude"`
	Longitude   float64   `json:"longitude"`
	Description string    `json:"description"`
	PhotoPath   string    `json:"photo_path,omitempty"`
	IsActive    bool      `json:"is_active"`
	CreatedAt   time.Time `json:"created_at"`
	ResolvedAt  *time.Time `json:"resolved_at,omitempty"`
}

// --- WebSocket message types ---

// WSMessage is the envelope for all WebSocket communication
type WSMessage struct {
	Type    string      `json:"type"`
	GroupID int64       `json:"group_id"`
	Payload interface{} `json:"payload"`
}

// WSVoicePayload carries voice broadcast data
type WSVoicePayload struct {
	SenderID    int64  `json:"sender_id"`
	SenderNick  string `json:"sender_nick"`
	AudioURL    string `json:"audio_url"`
	DurationSec int    `json:"duration_sec"`
	MessageID   int64  `json:"message_id"`
}

// WSAlertPayload carries alert broadcast data
type WSAlertPayload struct {
	Alert    Alert   `json:"alert"`
	Distance float64 `json:"distance_km,omitempty"` // filled client-side
}

// --- API request/response types ---

// ProfileRequest for creating/updating user profile
type ProfileRequest struct {
	Nickname  string `json:"nickname"`
	AvatarURL string `json:"avatar_url"`
}

// CreateGroupRequest for creating a new group
type CreateGroupRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
}

// JoinGroupRequest for joining a group
type JoinGroupRequest struct {
	InviteCode string `json:"invite_code"`
	Password   string `json:"password"`
}

// CreateAlertRequest for raising an alert
type CreateAlertRequest struct {
	AlertType   AlertType `json:"alert_type"`
	Latitude    float64   `json:"latitude"`
	Longitude   float64   `json:"longitude"`
	Description string    `json:"description"`
}

// UserSettings stores per-user notification preferences
type UserSettings struct {
	UserID       int64   `json:"user_id"`
	AlertRadius  float64 `json:"alert_radius_km"` // default 2.0
	SoundEnabled bool    `json:"sound_enabled"`   // default true
}
