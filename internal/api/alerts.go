package api

import (
	"encoding/json"
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

// AlertsHandler handles alert creation, retrieval, and resolution.
type AlertsHandler struct {
	db        *db.DB
	hub       *ws.Hub
	uploadDir string
}

// NewAlertsHandler creates a new AlertsHandler.
func NewAlertsHandler(database *db.DB, hub *ws.Hub, uploadDir string) *AlertsHandler {
	return &AlertsHandler{
		db:        database,
		hub:       hub,
		uploadDir: uploadDir,
	}
}

// GetAlertTypes returns supported alert types and metadata.
func (h *AlertsHandler) GetAlertTypes(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, models.AllAlertTypes())
}

// CreateAlert creates a new alert with optional photo upload.
func (h *AlertsHandler) CreateAlert(w http.ResponseWriter, r *http.Request) {
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

	var alertType models.AlertType
	var lat, lng float64
	var description, photoURL string

	contentType := r.Header.Get("Content-Type")
	if strings.HasPrefix(contentType, "multipart/form-data") {
		// 15MB limit for photos
		if err := r.ParseMultipartForm(15 << 20); err != nil {
			writeError(w, http.StatusBadRequest, "failed to parse multipart form")
			return
		}

		alertType = models.AlertType(r.FormValue("alert_type"))
		lat, _ = strconv.ParseFloat(r.FormValue("latitude"), 64)
		lng, _ = strconv.ParseFloat(r.FormValue("longitude"), 64)
		description = strings.TrimSpace(r.FormValue("description"))

		file, header, err := r.FormFile("photo")
		if err == nil {
			defer file.Close()

			ext := strings.ToLower(filepath.Ext(header.Filename))
			if ext == "" || (!strings.EqualFold(ext, ".jpg") && !strings.EqualFold(ext, ".jpeg") &&
				!strings.EqualFold(ext, ".png") && !strings.EqualFold(ext, ".webp")) {
				ext = ".jpg"
			}

			fileName := fmt.Sprintf("%s%s", uuid.New().String(), ext)
			targetDir := filepath.Join(h.uploadDir, "photos")
			if err := os.MkdirAll(targetDir, 0755); err != nil {
				writeError(w, http.StatusInternalServerError, "failed to create photo directory")
				return
			}

			dstPath := filepath.Join(targetDir, fileName)
			dst, err := os.Create(dstPath)
			if err != nil {
				writeError(w, http.StatusInternalServerError, "failed to save photo")
				return
			}
			defer dst.Close()

			if _, err := io.Copy(dst, file); err != nil {
				writeError(w, http.StatusInternalServerError, "failed to write photo file")
				return
			}

			photoURL = fmt.Sprintf("/uploads/photos/%s", fileName)
		}
	} else {
		var req models.CreateAlertRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeError(w, http.StatusBadRequest, "invalid request body")
			return
		}
		alertType = req.AlertType
		lat = req.Latitude
		lng = req.Longitude
		description = strings.TrimSpace(req.Description)
	}

	if alertType == "" {
		alertType = models.AlertOther
	}

	alert, err := h.db.CreateAlert(groupID, user.ID, alertType, lat, lng, description, photoURL)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to create alert: "+err.Error())
		return
	}
	alert.SenderNick = user.Nickname

	// Broadcast alert to group members
	_ = h.hub.BroadcastToGroup(groupID, &models.WSMessage{
		Type:    "alert",
		GroupID: groupID,
		Payload: models.WSAlertPayload{
			Alert: *alert,
		},
	})

	writeJSON(w, http.StatusCreated, alert)
}

// ListActiveAlerts returns all active alerts in the group.
func (h *AlertsHandler) ListActiveAlerts(w http.ResponseWriter, r *http.Request) {
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

	alerts, err := h.db.GetActiveAlerts(groupID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to fetch alerts")
		return
	}

	if alerts == nil {
		alerts = []models.Alert{}
	}

	writeJSON(w, http.StatusOK, alerts)
}

// GetAlert retrieves a single alert by ID.
func (h *AlertsHandler) GetAlert(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	vars := mux.Vars(r)
	alertID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid alert id")
		return
	}

	alert, err := h.db.GetAlertByID(alertID)
	if err != nil || alert == nil {
		writeError(w, http.StatusNotFound, "alert not found")
		return
	}

	isMember, err := h.db.IsGroupMember(alert.GroupID, user.ID)
	if err != nil || !isMember {
		writeError(w, http.StatusForbidden, "not a member of this group")
		return
	}

	writeJSON(w, http.StatusOK, alert)
}

// ResolveAlert marks an alert as resolved and broadcasts resolution to group.
func (h *AlertsHandler) ResolveAlert(w http.ResponseWriter, r *http.Request) {
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

	alertID, err := strconv.ParseInt(vars["alertId"], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid alert id")
		return
	}

	isMember, err := h.db.IsGroupMember(groupID, user.ID)
	if err != nil || !isMember {
		writeError(w, http.StatusForbidden, "not a member of this group")
		return
	}

	if err := h.db.ResolveAlert(alertID, groupID); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to resolve alert")
		return
	}

	// Broadcast alert resolved event
	_ = h.hub.BroadcastToGroup(groupID, &models.WSMessage{
		Type:    "alert_resolved",
		GroupID: groupID,
		Payload: map[string]interface{}{
			"alert_id":    alertID,
			"resolved_by": user.ID,
		},
	})

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"status":   "resolved",
		"alert_id": alertID,
	})
}
