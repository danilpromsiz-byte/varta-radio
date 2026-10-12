package ws

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"safecomm/internal/models"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 512 * 1024 // 512KB
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true // Allow all origins for Telegram Mini App
	},
}

// Client represents a connected WebSocket user for a specific group.
type Client struct {
	Hub     *Hub
	Conn    *websocket.Conn
	Send    chan []byte
	UserID  int64
	GroupID int64
}

// Hub maintains active client connections and broadcasts messages to groups.
type Hub struct {
	// Registered clients mapped by group_id -> set of clients
	groups     map[int64]map[*Client]bool
	clients    map[*Client]bool
	broadcast  chan broadcastMessage
	register   chan *Client
	unregister chan *Client
	mu         sync.RWMutex
}

type broadcastMessage struct {
	groupID      int64
	exceptUserID int64
	data         []byte
}

// NewHub creates a new Hub instance.
func NewHub() *Hub {
	return &Hub{
		groups:     make(map[int64]map[*Client]bool),
		clients:    make(map[*Client]bool),
		broadcast:  make(chan broadcastMessage, 256),
		register:   make(chan *Client),
		unregister: make(chan *Client),
	}
}

// Run executes the main loop of the Hub.
func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.mu.Lock()
			h.clients[client] = true
			if _, ok := h.groups[client.GroupID]; !ok {
				h.groups[client.GroupID] = make(map[*Client]bool)
			}
			h.groups[client.GroupID][client] = true
			h.mu.Unlock()
			log.Printf("[WS] User %d joined group %d. Total clients: %d", client.UserID, client.GroupID, len(h.clients))

		case client := <-h.unregister:
			h.mu.Lock()
			if _, ok := h.clients[client]; ok {
				delete(h.clients, client)
				if groupClients, ok := h.groups[client.GroupID]; ok {
					delete(groupClients, client)
					if len(groupClients) == 0 {
						delete(h.groups, client.GroupID)
					}
				}
				close(client.Send)
				log.Printf("[WS] User %d disconnected from group %d. Remaining clients: %d", client.UserID, client.GroupID, len(h.clients))
			}
			h.mu.Unlock()

		case msg := <-h.broadcast:
			h.mu.RLock()
			groupClients := h.groups[msg.groupID]
			for client := range groupClients {
				if msg.exceptUserID > 0 && client.UserID == msg.exceptUserID {
					continue
				}
				select {
				case client.Send <- msg.data:
				default:
					// Slow client; mark for cleanup
					close(client.Send)
					delete(h.clients, client)
					delete(groupClients, client)
				}
			}
			h.mu.RUnlock()
		}
	}
}

// BroadcastToGroup sends a message to all connected clients in a group.
func (h *Hub) BroadcastToGroup(groupID int64, msg *models.WSMessage) error {
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	h.broadcast <- broadcastMessage{
		groupID: groupID,
		data:    data,
	}
	return nil
}

// BroadcastToGroupExcept sends a message to all connected clients in a group except the specified user.
func (h *Hub) BroadcastToGroupExcept(groupID int64, exceptUserID int64, msg *models.WSMessage) error {
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	h.broadcast <- broadcastMessage{
		groupID:      groupID,
		exceptUserID: exceptUserID,
		data:         data,
	}
	return nil
}

// ReadPump pumps messages from the websocket connection to the hub.
func (c *Client) ReadPump() {
	defer func() {
		c.Hub.unregister <- c
		c.Conn.Close()
	}()

	c.Conn.SetReadLimit(maxMessageSize)
	_ = c.Conn.SetReadDeadline(time.Now().Add(pongWait))
	c.Conn.SetPongHandler(func(string) error {
		_ = c.Conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := c.Conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("[WS] Read error: %v", err)
			}
			break
		}

		// Handle client-originated messages if any (e.g. ping or client echo)
		var wsMsg models.WSMessage
		if err := json.Unmarshal(message, &wsMsg); err == nil {
			if wsMsg.Type == "ping" {
				pongMsg, _ := json.Marshal(models.WSMessage{
					Type:    "pong",
					GroupID: c.GroupID,
				})
				c.Send <- pongMsg
			}
		}
	}
}

// WritePump pumps messages from the hub to the websocket connection.
func (c *Client) WritePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.Conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.Send:
			_ = c.Conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				// Channel was closed by hub
				_ = c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			w, err := c.Conn.NextWriter(websocket.TextMessage)
			if err != nil {
				return
			}
			if _, err := w.Write(message); err != nil {
				return
			}

			// Add queued messages if any
			n := len(c.Send)
			for i := 0; i < n; i++ {
				_, _ = w.Write([]byte{'\n'})
				_, _ = w.Write(<-c.Send)
			}

			if err := w.Close(); err != nil {
				return
			}

		case <-ticker.C:
			_ = c.Conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// ServeWS upgrades the HTTP connection to a WebSocket connection and registers client.
func ServeWS(hub *Hub, w http.ResponseWriter, r *http.Request, userID, groupID int64) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("[WS] Upgrade failed: %v", err)
		return
	}

	client := &Client{
		Hub:     hub,
		Conn:    conn,
		Send:    make(chan []byte, 256),
		UserID:  userID,
		GroupID: groupID,
	}

	hub.register <- client

	go client.WritePump()
	go client.ReadPump()
}
