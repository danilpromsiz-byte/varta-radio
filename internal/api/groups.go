package api

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gorilla/mux"
	"golang.org/x/crypto/bcrypt"
	"safecomm/internal/db"
	"safecomm/internal/models"
)

// GroupsHandler handles group operations.
type GroupsHandler struct {
	db *db.DB
}

// NewGroupsHandler creates a new GroupsHandler.
func NewGroupsHandler(database *db.DB) *GroupsHandler {
	return &GroupsHandler{db: database}
}

// GroupDetailResponse includes group details along with the caller's role.
type GroupDetailResponse struct {
	Group models.Group `json:"group"`
	Role  string       `json:"role"`
}

// MemberDetailResponse includes group member info with user nickname and avatar.
type MemberDetailResponse struct {
	ID        int64     `json:"id"`
	GroupID   int64     `json:"group_id"`
	UserID    int64     `json:"user_id"`
	Nickname  string    `json:"nickname"`
	AvatarURL string    `json:"avatar_url"`
	Role      string    `json:"role"`
	JoinedAt  time.Time `json:"joined_at"`
}

func generateInviteCode() string {
	b := make([]byte, 4) // 8 hex characters
	_, _ = rand.Read(b)
	return strings.ToUpper(hex.EncodeToString(b))
}

// CreateGroup creates a new group and assigns the caller as admin.
func (h *GroupsHandler) CreateGroup(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req models.CreateGroupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	name := strings.TrimSpace(req.Name)
	if name == "" {
		writeError(w, http.StatusBadRequest, "group name is required")
		return
	}

	if len(req.Password) < 4 {
		writeError(w, http.StatusBadRequest, "password must be at least 4 characters")
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to process password")
		return
	}

	inviteCode := generateInviteCode()
	group, err := h.db.CreateGroup(name, inviteCode, string(hash), user.TelegramID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to create group: "+err.Error())
		return
	}

	writeJSON(w, http.StatusCreated, GroupDetailResponse{
		Group: *group,
		Role:  "admin",
	})
}

// ListUserGroups returns all groups the user is a member of.
func (h *GroupsHandler) ListUserGroups(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	groups, err := h.db.GetUserGroups(user.ID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "database error")
		return
	}

	if groups == nil {
		groups = []models.Group{}
	}

	writeJSON(w, http.StatusOK, groups)
}

// GetGroup returns group details if caller is a member.
func (h *GroupsHandler) GetGroup(w http.ResponseWriter, r *http.Request) {
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

	role, err := h.db.GetMemberRole(groupID, user.ID)
	if err != nil || role == "" {
		writeError(w, http.StatusForbidden, "not a member of this group")
		return
	}

	group, err := h.db.GetGroupByID(groupID)
	if err != nil || group == nil {
		writeError(w, http.StatusNotFound, "group not found")
		return
	}

	writeJSON(w, http.StatusOK, GroupDetailResponse{
		Group: *group,
		Role:  role,
	})
}

// JoinGroup joins a group by invite code and password.
func (h *GroupsHandler) JoinGroup(w http.ResponseWriter, r *http.Request) {
	user := UserFromContext(r.Context())
	if user == nil {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req models.JoinGroupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	inviteCode := strings.TrimSpace(req.InviteCode)
	group, err := h.db.GetGroupByInviteCode(inviteCode)
	if err != nil || group == nil {
		writeError(w, http.StatusNotFound, "group not found")
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(group.PasswordHash), []byte(req.Password)); err != nil {
		writeError(w, http.StatusUnauthorized, "invalid group password")
		return
	}

	if err := h.db.AddGroupMember(group.ID, user.ID, "member"); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to join group")
		return
	}

	writeJSON(w, http.StatusOK, GroupDetailResponse{
		Group: *group,
		Role:  "member",
	})
}

// LeaveGroup removes caller from group.
func (h *GroupsHandler) LeaveGroup(w http.ResponseWriter, r *http.Request) {
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

	if err := h.db.RemoveGroupMember(groupID, user.ID); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to leave group")
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

// ListMembers returns all members of the group.
func (h *GroupsHandler) ListMembers(w http.ResponseWriter, r *http.Request) {
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

	members, err := h.db.GetGroupMembers(groupID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to get members")
		return
	}

	var detailedMembers []MemberDetailResponse
	for _, m := range members {
		u, _ := h.db.GetUserByID(m.UserID)
		dm := MemberDetailResponse{
			ID:       m.ID,
			GroupID:  m.GroupID,
			UserID:   m.UserID,
			Role:     m.Role,
			JoinedAt: m.JoinedAt,
		}
		if u != nil {
			dm.Nickname = u.Nickname
			dm.AvatarURL = u.AvatarURL
		}
		detailedMembers = append(detailedMembers, dm)
	}

	if detailedMembers == nil {
		detailedMembers = []MemberDetailResponse{}
	}

	writeJSON(w, http.StatusOK, detailedMembers)
}

// RemoveMember removes a member (admin only).
func (h *GroupsHandler) RemoveMember(w http.ResponseWriter, r *http.Request) {
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

	targetUserID, err := strconv.ParseInt(vars["userId"], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid user id")
		return
	}

	role, err := h.db.GetMemberRole(groupID, user.ID)
	if err != nil || role != "admin" {
		writeError(w, http.StatusForbidden, "admin privileges required")
		return
	}

	if err := h.db.RemoveGroupMember(groupID, targetUserID); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to remove member")
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}
