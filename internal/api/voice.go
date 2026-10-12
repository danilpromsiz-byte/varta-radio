package api

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"safecomm/internal/db"
	"safecomm/internal/models"
	"safecomm/internal/ws"
)

// VoiceHandler handles voice recordings and walkie-talkie history.
type VoiceHandler struct {
	db        *db.DB
	hub       *ws.Hub
	uploadDir string
}

// NewVoiceHandler creates a new VoiceHandler.
func NewVoiceHandler(database *db.DB, hub *ws.Hub, uploadDir string) *VoiceHandler {
	return &VoiceHandler{
		db:        database,
		hub:       hub,
		uploadDir: uploadDir,
	}
}

// UploadVoice handles multipart voice file uploads, saves file, and broadcasts via WS.
func (h *VoiceHandler) UploadVoice(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	vars := mux.Vars(r)
	groupID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid group id")
		return
	}

	isMember, err := h.db.IsGroupMember(groupID, user.ID)
	if err != nil || !isMember {
		writeError(w, http.StatusForbidden, "not a member of this group")
		return
	}

	// Max upload size 10MB
	if err := r.ParseMultipartForm(10 << 20); err != nil {
		writeError(w, http.StatusBadRequest, "failed to parse multipart form")
		return
	}

	file, header, err := r.FormFile("voice")
	if err != nil {
		file, header, err = r.FormFile("audio")
		if err != nil {
			writeError(w, http.StatusBadRequest, "voice or audio file is required")
			return
		}
	}
	defer file.Close()

	durationSec := 0
	if durStr := r.FormValue("duration_sec"); durStr != "" {
		if d, err := strconv.Atoi(durStr); err == nil && d >= 0 {
			durationSec = d
		}
	}

	ext := strings.ToLower(filepath.Ext(header.Filename))
	if ext == "" || (!strings.EqualFold(ext, ".ogg") && !strings.EqualFold(ext, ".mp3") &&
		!strings.EqualFold(ext, ".wav") && !strings.EqualFold(ext, ".m4a") &&
		!strings.EqualFold(ext, ".webm")) {
		ext = ".ogg"
	}

	fileName := fmt.Sprintf("%s%s", uuid.New().String(), ext)
	targetDir := filepath.Join(h.uploadDir, "voice")
	if err := os.MkdirAll(targetDir, 0755); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to create voice directory")
		return
	}

	dstPath := filepath.Join(targetDir, fileName)
	dst, err := os.Create(dstPath)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to save voice file")
		return
	}
	defer dst.Close()

	if _, err := io.Copy(dst, file); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to write file")
		return
	}

	publicURL := fmt.Sprintf("/uploads/voice/%s", fileName)
	vm, err := h.db.SaveVoiceMessage(groupID, user.ID, publicURL, durationSec)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to record voice message")
		return
	}
	vm.SenderNick = user.Nickname

	// Broadcast to WebSocket listeners in this group
	_ = h.hub.BroadcastToGroup(groupID, &models.WSMessage{
		Type:    "voice",
		GroupID: groupID,
		Payload: models.WSVoicePayload{
			SenderID:    user.ID,
			SenderNick:  user.Nickname,
			AudioURL:    publicURL,
			DurationSec: durationSec,
			MessageID:   vm.ID,
		},
	})

	writeJSON(w, http.StatusCreated, vm)
}

// GetVoiceHistory returns recent voice messages for a group.
func (h *VoiceHandler) GetVoiceHistory(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	vars := mux.Vars(r)
	groupID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid group id")
		return
	}

	isMember, err := h.db.IsGroupMember(groupID, user.ID)
	if err != nil || !isMember {
		writeError(w, http.StatusForbidden, "not a member of this group")
		return
	}

	limit := 50
	if l := r.URL.Query().Get("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 {
			if parsed > 100 {
				parsed = 100
			}
			limit = parsed
		}
	}

	history, err := h.db.GetVoiceHistory(groupID, limit)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to fetch voice history")
		return
	}

	if history == nil {
		history = []models.VoiceMessage{}
	}

	writeJSON(w, http.StatusOK, history)
}
