package api

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"time"

	"safecomm/internal/db"
	"safecomm/internal/models"
)

// TelegramUser contains Telegram user metadata passed in initData.
type TelegramUser struct {
	ID        int64  `json:"id"`
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name,omitempty"`
	Username  string `json:"username,omitempty"`
	PhotoURL  string `json:"photo_url,omitempty"`
}

// TelegramAuthRequest is the payload sent from the frontend Telegram Mini App.
type TelegramAuthRequest struct {
	InitData string `json:"init_data"`
	Nickname string `json:"nickname,omitempty"`
}

// AuthResponse returns the auth token and user information.
type AuthResponse struct {
	Token string       `json:"token"`
	User  *models.User `json:"user"`
}

// AuthHandler handles authentication and user profiles.
type AuthHandler struct {
	db        *db.DB
	jwtSecret string
	botToken  string
}

// NewAuthHandler creates an AuthHandler instance.
func NewAuthHandler(database *db.DB, jwtSecret, botToken string) *AuthHandler {
	return &AuthHandler{
		db:        database,
		jwtSecret: jwtSecret,
		botToken:  botToken,
	}
}

// ValidateTelegramInitData verifies the Telegram Mini App initData HMAC-SHA256 signature.
func ValidateTelegramInitData(initData, botToken string) (*TelegramUser, error) {
	if initData == "" {
		return nil, errors.New("empty init_data")
	}

	values, err := url.ParseQuery(initData)
	if err != nil {
		return nil, fmt.Errorf("parse init_data query: %w", err)
	}

	hash := values.Get("hash")
	if hash == "" {
		return nil, errors.New("missing hash in init_data")
	}
	values.Del("hash")

	// Check auth_date for expiration (allow up to 24 hours)
	authDateStr := values.Get("auth_date")
	if authDateStr != "" {
		authDateUnix, err := strconv.ParseInt(authDateStr, 10, 64)
		if err == nil {
			authDate := time.Unix(authDateUnix, 0)
			if time.Since(authDate) > 24*time.Hour {
				return nil, errors.New("init_data expired")
			}
		}
	}

	// Telegram HMAC verification:
	// Secret key is HMAC-SHA256 of botToken with secret key "WebAppData"
	// Data check string is key=value pairs sorted alphabetically, joined by '\n'
	if botToken != "" {
		var pairs []string
		for k, v := range values {
			if len(v) > 0 {
				pairs = append(pairs, fmt.Sprintf("%s=%s", k, v[0]))
			}
		}
		sort.Strings(pairs)
		dataCheckString := strings.Join(pairs, "\n")

		macKey := hmac.New(sha256.New, []byte("WebAppData"))
		macKey.Write([]byte(botToken))
		secretKey := macKey.Sum(nil)

		macHash := hmac.New(sha256.New, secretKey)
		macHash.Write([]byte(dataCheckString))
		expectedHash := hex.EncodeToString(macHash.Sum(nil))

		if !hmac.Equal([]byte(expectedHash), []byte(hash)) {
			return nil, errors.New("invalid init_data signature")
		}
	}

	userJSON := values.Get("user")
	if userJSON == "" {
		return nil, errors.New("missing user in init_data")
	}

	var tgUser TelegramUser
	if err := json.Unmarshal([]byte(userJSON), &tgUser); err != nil {
		return nil, fmt.Errorf("unmarshal telegram user: %w", err)
	}

	return &tgUser, nil
}

// TelegramLogin authenticates or creates a user using Telegram initData.
func (h *AuthHandler) TelegramLogin(w http.ResponseWriter, r *http.Request) {
	var req TelegramAuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	tgUser, err := ValidateTelegramInitData(req.InitData, h.botToken)
	if err != nil {
		writeError(w, http.StatusUnauthorized, "telegram validation failed: "+err.Error())
		return
	}

	user, err := h.db.GetUserByTelegramID(tgUser.ID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "database error")
		return
	}

	if user == nil {
		// New user: determine default nickname
		nickname := strings.TrimSpace(req.Nickname)
		if nickname == "" {
			if tgUser.Username != "" {
				nickname = tgUser.Username
			} else {
				nickname = tgUser.FirstName
			}
		}

		// Ensure uniqueness if nickname already taken
		baseNick := nickname
		suffix := 1
		for {
			exists, err := h.db.NicknameExists(nickname)
			if err != nil {
				writeError(w, http.StatusInternalServerError, "database error")
				return
			}
			if !exists {
				break
			}
			nickname = fmt.Sprintf("%s_%d", baseNick, suffix)
			suffix++
		}

		user, err = h.db.CreateUser(tgUser.ID, nickname, tgUser.PhotoURL)
		if err != nil {
			writeError(w, http.StatusInternalServerError, "failed to create user: "+err.Error())
			return
		}
	}

	token, err := GenerateJWT(user, h.jwtSecret, 30*24*time.Hour)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to generate token")
		return
	}

	writeJSON(w, http.StatusOK, AuthResponse{
		Token: token,
		User:  user,
	})
}

// GetMe returns current authenticated user and their settings.
func (h *AuthHandler) GetMe(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	settings, err := h.db.GetUserSettings(user.ID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to get settings")
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"user":     user,
		"settings": settings,
	})
}

// UpdateProfile updates user nickname and avatar.
func (h *AuthHandler) UpdateProfile(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req models.ProfileRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	nickname := strings.TrimSpace(req.Nickname)
	if nickname == "" {
		writeError(w, http.StatusBadRequest, "nickname cannot be empty")
		return
	}

	if nickname != user.Nickname {
		exists, err := h.db.NicknameExists(nickname)
		if err != nil {
			writeError(w, http.StatusInternalServerError, "database error")
			return
		}
		if exists {
			writeError(w, http.StatusConflict, "nickname already taken")
			return
		}
	}

	avatarURL := req.AvatarURL
	if avatarURL == "" {
		avatarURL = user.AvatarURL
	}

	if err := h.db.UpdateProfile(user.ID, nickname, avatarURL); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to update profile")
		return
	}

	user.Nickname = nickname
	user.AvatarURL = avatarURL

	writeJSON(w, http.StatusOK, user)
}

// GetSettings returns current user settings.
func (h *AuthHandler) GetSettings(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	settings, err := h.db.GetUserSettings(user.ID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to get settings")
		return
	}

	writeJSON(w, http.StatusOK, settings)
}

// UpdateSettings updates user alert radius and sound preferences.
func (h *AuthHandler) UpdateSettings(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req models.UserSettings
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if req.AlertRadius <= 0 {
		req.AlertRadius = 2.0
	}

	if err := h.db.UpdateUserSettings(user.ID, req.AlertRadius, req.SoundEnabled); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to update settings")
		return
	}

	req.UserID = user.ID
	writeJSON(w, http.StatusOK, req)
}
