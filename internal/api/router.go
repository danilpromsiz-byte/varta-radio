package api

import (
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"
	"strconv"

	"github.com/gorilla/mux"
	"safecomm/internal/db"
	"safecomm/internal/ws"
)

// Config holds configuration parameters for the API router.
type Config struct {
	DB        *db.DB
	Hub       *ws.Hub
	JWTSecret string
	BotToken  string
	WebDir    string
	UploadDir string
}

// writeJSON encodes data as JSON and sets Content-Type.
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(data)
}

// writeError encodes an error message in standard JSON format.
func writeError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]string{"error": message})
}

// SetupRouter creates and configures the Gorilla Mux HTTP router.
func SetupRouter(cfg Config) *mux.Router {
	r := mux.NewRouter()

	// Global Middlewares
	rateLimiter := NewRateLimiter(20.0, 40.0) // 20 req/s with burst of 40
	r.Use(CORSMiddleware)
	r.Use(rateLimiter.Middleware)

	// Handlers
	authHandler := NewAuthHandler(cfg.DB, cfg.JWTSecret, cfg.BotToken)
	groupsHandler := NewGroupsHandler(cfg.DB)
	voiceHandler := NewVoiceHandler(cfg.DB, cfg.Hub, cfg.UploadDir)
	alertsHandler := NewAlertsHandler(cfg.DB, cfg.Hub, cfg.UploadDir)

	// API subrouter
	apiRouter := r.PathPrefix("/api").Subrouter()

	// Public API endpoints
	apiRouter.HandleFunc("/auth/telegram", authHandler.TelegramLogin).Methods(http.MethodPost, http.MethodOptions)
	apiRouter.HandleFunc("/alerts/types", alertsHandler.GetAlertTypes).Methods(http.MethodGet, http.MethodOptions)

	// Protected API endpoints
	authMW := AuthMiddleware(cfg.DB, cfg.JWTSecret)
	protected := apiRouter.PathPrefix("").Subrouter()
	protected.Use(authMW)

	// User / Profile
	protected.HandleFunc("/auth/me", authHandler.GetMe).Methods(http.MethodGet)
	protected.HandleFunc("/auth/profile", authHandler.UpdateProfile).Methods(http.MethodPut)
	protected.HandleFunc("/auth/settings", authHandler.GetSettings).Methods(http.MethodGet)
	protected.HandleFunc("/auth/settings", authHandler.UpdateSettings).Methods(http.MethodPut)

	// Groups
	protected.HandleFunc("/groups", groupsHandler.ListUserGroups).Methods(http.MethodGet)
	protected.HandleFunc("/groups", groupsHandler.CreateGroup).Methods(http.MethodPost)
	protected.HandleFunc("/groups/join", groupsHandler.JoinGroup).Methods(http.MethodPost)
	protected.HandleFunc("/groups/{id:[0-9]+}", groupsHandler.GetGroup).Methods(http.MethodGet)
	protected.HandleFunc("/groups/{id:[0-9]+}/leave", groupsHandler.LeaveGroup).Methods(http.MethodPost)
	protected.HandleFunc("/groups/{id:[0-9]+}/members", groupsHandler.ListMembers).Methods(http.MethodGet)
	protected.HandleFunc("/groups/{id:[0-9]+}/members/{userId:[0-9]+}", groupsHandler.RemoveMember).Methods(http.MethodDelete)

	// Voice
	protected.HandleFunc("/groups/{id:[0-9]+}/voice", voiceHandler.GetVoiceHistory).Methods(http.MethodGet)
	protected.HandleFunc("/groups/{id:[0-9]+}/voice", voiceHandler.UploadVoice).Methods(http.MethodPost)

	// Alerts
	protected.HandleFunc("/groups/{id:[0-9]+}/alerts", alertsHandler.ListActiveAlerts).Methods(http.MethodGet)
	protected.HandleFunc("/groups/{id:[0-9]+}/alerts", alertsHandler.CreateAlert).Methods(http.MethodPost)
	protected.HandleFunc("/groups/{id:[0-9]+}/alerts/{alertId:[0-9]+}/resolve", alertsHandler.ResolveAlert).Methods(http.MethodPost)
	protected.HandleFunc("/alerts/{id:[0-9]+}", alertsHandler.GetAlert).Methods(http.MethodGet)

	// WebSocket endpoint: /ws?token=JWT&group_id=ID
	r.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		tokenStr := r.URL.Query().Get("token")
		if tokenStr == "" {
			http.Error(w, `{"error":"missing token query param"}`, http.StatusUnauthorized)
			return
		}

		claims, err := ValidateJWT(tokenStr, cfg.JWTSecret)
		if err != nil {
			http.Error(w, `{"error":"invalid or expired token"}`, http.StatusUnauthorized)
			return
		}

		groupIDStr := r.URL.Query().Get("group_id")
		groupID, err := strconv.ParseInt(groupIDStr, 10, 64)
		if err != nil || groupID <= 0 {
			http.Error(w, `{"error":"invalid group_id"}`, http.StatusBadRequest)
			return
		}

		isMember, err := cfg.DB.IsGroupMember(groupID, claims.UserID)
		if err != nil || !isMember {
			http.Error(w, `{"error":"not a member of this group"}`, http.StatusForbidden)
			return
		}

		ws.ServeWS(cfg.Hub, w, r, claims.UserID, groupID)
	})

	// Static files: /uploads/
	uploadFileServer := http.StripPrefix("/uploads/", http.FileServer(http.Dir(cfg.UploadDir)))
	r.PathPrefix("/uploads/").Handler(uploadFileServer)

	// Static files: web/ directory served at /
	if cfg.WebDir != "" {
		spa := spaHandler{staticPath: cfg.WebDir, indexPath: "index.html"}
		r.PathPrefix("/").Handler(spa)
	}

	return r
}

// spaHandler implements http.Handler for serving Single Page Applications.
type spaHandler struct {
	staticPath string
	indexPath  string
}

func (h spaHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	path := filepath.Join(h.staticPath, filepath.Clean(r.URL.Path))

	fi, err := os.Stat(path)
	if os.IsNotExist(err) || fi.IsDir() {
		// Fallback to index.html
		indexPath := filepath.Join(h.staticPath, h.indexPath)
		if _, err := os.Stat(indexPath); err == nil {
			http.ServeFile(w, r, indexPath)
			return
		}
		http.NotFound(w, r)
		return
	} else if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	http.FileServer(http.Dir(h.staticPath)).ServeHTTP(w, r)
}
