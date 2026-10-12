package db

import (
	"database/sql"
	"fmt"
	"safecomm/internal/models"
	"time"
)

// --- Users ---

// CreateUser inserts a new user
func (d *DB) CreateUser(telegramID int64, nickname, avatarURL string) (*models.User, error) {
	res, err := d.Exec(
		`INSERT INTO users (telegram_id, nickname, avatar_url) VALUES (?, ?, ?)`,
		telegramID, nickname, avatarURL,
	)
	if err != nil {
		return nil, fmt.Errorf("insert user: %w", err)
	}
	id, _ := res.LastInsertId()

	// Create default settings
	_, _ = d.Exec(`INSERT OR IGNORE INTO user_settings (user_id) VALUES (?)`, id)

	return &models.User{
		ID: id, TelegramID: telegramID, Nickname: nickname,
		AvatarURL: avatarURL, CreatedAt: time.Now(),
	}, nil
}

// GetUserByTelegramID retrieves a user by Telegram ID
func (d *DB) GetUserByTelegramID(tgID int64) (*models.User, error) {
	u := &models.User{}
	err := d.QueryRow(
		`SELECT id, telegram_id, nickname, avatar_url, created_at FROM users WHERE telegram_id = ?`, tgID,
	).Scan(&u.ID, &u.TelegramID, &u.Nickname, &u.AvatarURL, &u.CreatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	return u, err
}

// GetUserByID retrieves a user by internal ID
func (d *DB) GetUserByID(id int64) (*models.User, error) {
	u := &models.User{}
	err := d.QueryRow(
		`SELECT id, telegram_id, nickname, avatar_url, created_at FROM users WHERE id = ?`, id,
	).Scan(&u.ID, &u.TelegramID, &u.Nickname, &u.AvatarURL, &u.CreatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	return u, err
}

// NicknameExists checks if a nickname is already taken
func (d *DB) NicknameExists(nickname string) (bool, error) {
	var count int
	err := d.QueryRow(`SELECT COUNT(*) FROM users WHERE nickname = ?`, nickname).Scan(&count)
	return count > 0, err
}

// UpdateProfile updates nickname and avatar
func (d *DB) UpdateProfile(userID int64, nickname, avatarURL string) error {
	_, err := d.Exec(
		`UPDATE users SET nickname = ?, avatar_url = ? WHERE id = ?`,
		nickname, avatarURL, userID,
	)
	return err
}

// --- Groups ---

// CreateGroup inserts a new group and adds the creator as admin
func (d *DB) CreateGroup(name, inviteCode, passwordHash string, creatorTgID int64) (*models.Group, error) {
	res, err := d.Exec(
		`INSERT INTO groups_ (name, invite_code, password_hash, creator_telegram_id) VALUES (?, ?, ?, ?)`,
		name, inviteCode, passwordHash, creatorTgID,
	)
	if err != nil {
		return nil, fmt.Errorf("insert group: %w", err)
	}
	groupID, _ := res.LastInsertId()

	// Add creator as admin
	user, err := d.GetUserByTelegramID(creatorTgID)
	if err != nil || user == nil {
		return nil, fmt.Errorf("creator not found: %w", err)
	}
	_, err = d.Exec(
		`INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'admin')`,
		groupID, user.ID,
	)
	if err != nil {
		return nil, fmt.Errorf("add creator as admin: %w", err)
	}

	return &models.Group{
		ID: groupID, Name: name, InviteCode: inviteCode,
		CreatorTelegramID: creatorTgID, CreatedAt: time.Now(),
	}, nil
}

// GetGroupByInviteCode finds a group by invite code
func (d *DB) GetGroupByInviteCode(code string) (*models.Group, error) {
	g := &models.Group{}
	err := d.QueryRow(
		`SELECT id, name, invite_code, password_hash, creator_telegram_id, created_at
		 FROM groups_ WHERE invite_code = ?`, code,
	).Scan(&g.ID, &g.Name, &g.InviteCode, &g.PasswordHash, &g.CreatorTelegramID, &g.CreatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	return g, err
}

// GetGroupByID retrieves a group by ID
func (d *DB) GetGroupByID(id int64) (*models.Group, error) {
	g := &models.Group{}
	err := d.QueryRow(
		`SELECT id, name, invite_code, password_hash, creator_telegram_id, created_at
		 FROM groups_ WHERE id = ?`, id,
	).Scan(&g.ID, &g.Name, &g.InviteCode, &g.PasswordHash, &g.CreatorTelegramID, &g.CreatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	return g, err
}

// AddGroupMember adds a user to a group
func (d *DB) AddGroupMember(groupID, userID int64, role string) error {
	_, err := d.Exec(
		`INSERT OR IGNORE INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)`,
		groupID, userID, role,
	)
	return err
}

// RemoveGroupMember removes a user from a group
func (d *DB) RemoveGroupMember(groupID, userID int64) error {
	_, err := d.Exec(
		`DELETE FROM group_members WHERE group_id = ? AND user_id = ?`,
		groupID, userID,
	)
	return err
}

// IsGroupMember checks membership
func (d *DB) IsGroupMember(groupID, userID int64) (bool, error) {
	var count int
	err := d.QueryRow(
		`SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?`,
		groupID, userID,
	).Scan(&count)
	return count > 0, err
}

// GetGroupMembers returns all members of a group
func (d *DB) GetGroupMembers(groupID int64) ([]models.GroupMember, error) {
	rows, err := d.Query(
		`SELECT gm.id, gm.group_id, gm.user_id, gm.role, gm.joined_at
		 FROM group_members gm WHERE gm.group_id = ? ORDER BY gm.joined_at`, groupID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var members []models.GroupMember
	for rows.Next() {
		var m models.GroupMember
		if err := rows.Scan(&m.ID, &m.GroupID, &m.UserID, &m.Role, &m.JoinedAt); err != nil {
			return nil, err
		}
		members = append(members, m)
	}
	return members, nil
}

// GetMemberRole returns the role of a user in a group (empty string if not a member)
func (d *DB) GetMemberRole(groupID, userID int64) (string, error) {
	var role string
	err := d.QueryRow(
		`SELECT role FROM group_members WHERE group_id = ? AND user_id = ?`,
		groupID, userID,
	).Scan(&role)
	if err == sql.ErrNoRows {
		return "", nil
	}
	return role, err
}

// GetUserGroups returns all groups a user belongs to
func (d *DB) GetUserGroups(userID int64) ([]models.Group, error) {
	rows, err := d.Query(
		`SELECT g.id, g.name, g.invite_code, '', g.creator_telegram_id, g.created_at
		 FROM groups_ g
		 JOIN group_members gm ON gm.group_id = g.id
		 WHERE gm.user_id = ?
		 ORDER BY g.name`, userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var groups []models.Group
	for rows.Next() {
		var g models.Group
		if err := rows.Scan(&g.ID, &g.Name, &g.InviteCode, &g.PasswordHash, &g.CreatorTelegramID, &g.CreatedAt); err != nil {
			return nil, err
		}
		groups = append(groups, g)
	}
	return groups, nil
}

// --- Voice Messages ---

// SaveVoiceMessage stores a voice message record
func (d *DB) SaveVoiceMessage(groupID, senderID int64, filePath string, durationSec int) (*models.VoiceMessage, error) {
	res, err := d.Exec(
		`INSERT INTO voice_messages (group_id, sender_id, file_path, duration_sec) VALUES (?, ?, ?, ?)`,
		groupID, senderID, filePath, durationSec,
	)
	if err != nil {
		return nil, err
	}
	id, _ := res.LastInsertId()
	return &models.VoiceMessage{
		ID: id, GroupID: groupID, SenderID: senderID,
		FilePath: filePath, DurationSec: durationSec, CreatedAt: time.Now(),
	}, nil
}

// GetVoiceHistory returns recent voice messages for a group
func (d *DB) GetVoiceHistory(groupID int64, limit int) ([]models.VoiceMessage, error) {
	rows, err := d.Query(
		`SELECT vm.id, vm.group_id, vm.sender_id, u.nickname, vm.file_path, vm.duration_sec, vm.created_at
		 FROM voice_messages vm
		 JOIN users u ON u.id = vm.sender_id
		 WHERE vm.group_id = ?
		 ORDER BY vm.created_at DESC
		 LIMIT ?`, groupID, limit,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var msgs []models.VoiceMessage
	for rows.Next() {
		var m models.VoiceMessage
		if err := rows.Scan(&m.ID, &m.GroupID, &m.SenderID, &m.SenderNick, &m.FilePath, &m.DurationSec, &m.CreatedAt); err != nil {
			return nil, err
		}
		msgs = append(msgs, m)
	}
	return msgs, nil
}

// --- Alerts ---

// CreateAlert stores a new alert
func (d *DB) CreateAlert(groupID, senderID int64, alertType models.AlertType, lat, lng float64, desc, photoPath string) (*models.Alert, error) {
	res, err := d.Exec(
		`INSERT INTO alerts (group_id, sender_id, alert_type, latitude, longitude, description, photo_path)
		 VALUES (?, ?, ?, ?, ?, ?, ?)`,
		groupID, senderID, string(alertType), lat, lng, desc, photoPath,
	)
	if err != nil {
		return nil, err
	}
	id, _ := res.LastInsertId()
	return &models.Alert{
		ID: id, GroupID: groupID, SenderID: senderID,
		AlertType: alertType, Latitude: lat, Longitude: lng,
		Description: desc, PhotoPath: photoPath,
		IsActive: true, CreatedAt: time.Now(),
	}, nil
}

// GetActiveAlerts returns all active alerts for a group
func (d *DB) GetActiveAlerts(groupID int64) ([]models.Alert, error) {
	rows, err := d.Query(
		`SELECT a.id, a.group_id, a.sender_id, u.nickname,
		        a.alert_type, a.latitude, a.longitude, a.description, a.photo_path,
		        a.is_active, a.created_at, a.resolved_at
		 FROM alerts a
		 JOIN users u ON u.id = a.sender_id
		 WHERE a.group_id = ? AND a.is_active = 1
		 ORDER BY a.created_at DESC`, groupID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var alerts []models.Alert
	for rows.Next() {
		var a models.Alert
		if err := rows.Scan(&a.ID, &a.GroupID, &a.SenderID, &a.SenderNick,
			&a.AlertType, &a.Latitude, &a.Longitude, &a.Description, &a.PhotoPath,
			&a.IsActive, &a.CreatedAt, &a.ResolvedAt); err != nil {
			return nil, err
		}
		alerts = append(alerts, a)
	}
	return alerts, nil
}

// ResolveAlert marks an alert as resolved
func (d *DB) ResolveAlert(alertID, groupID int64) error {
	_, err := d.Exec(
		`UPDATE alerts SET is_active = 0, resolved_at = CURRENT_TIMESTAMP
		 WHERE id = ? AND group_id = ?`,
		alertID, groupID,
	)
	return err
}

// GetAlertByID retrieves a single alert
func (d *DB) GetAlertByID(id int64) (*models.Alert, error) {
	a := &models.Alert{}
	err := d.QueryRow(
		`SELECT a.id, a.group_id, a.sender_id, u.nickname,
		        a.alert_type, a.latitude, a.longitude, a.description, a.photo_path,
		        a.is_active, a.created_at, a.resolved_at
		 FROM alerts a
		 JOIN users u ON u.id = a.sender_id
		 WHERE a.id = ?`, id,
	).Scan(&a.ID, &a.GroupID, &a.SenderID, &a.SenderNick,
		&a.AlertType, &a.Latitude, &a.Longitude, &a.Description, &a.PhotoPath,
		&a.IsActive, &a.CreatedAt, &a.ResolvedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	return a, err
}

// --- Settings ---

// GetUserSettings retrieves notification preferences
func (d *DB) GetUserSettings(userID int64) (*models.UserSettings, error) {
	s := &models.UserSettings{}
	err := d.QueryRow(
		`SELECT user_id, alert_radius, sound_enabled FROM user_settings WHERE user_id = ?`, userID,
	).Scan(&s.UserID, &s.AlertRadius, &s.SoundEnabled)
	if err == sql.ErrNoRows {
		return &models.UserSettings{UserID: userID, AlertRadius: 2.0, SoundEnabled: true}, nil
	}
	return s, err
}

// UpdateUserSettings saves notification preferences
func (d *DB) UpdateUserSettings(userID int64, radius float64, soundEnabled bool) error {
	_, err := d.Exec(
		`INSERT INTO user_settings (user_id, alert_radius, sound_enabled) VALUES (?, ?, ?)
		 ON CONFLICT(user_id) DO UPDATE SET alert_radius = excluded.alert_radius, sound_enabled = excluded.sound_enabled`,
		userID, radius, soundEnabled,
	)
	return err
}
