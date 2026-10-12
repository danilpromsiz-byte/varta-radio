package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"
	"time"

	"safecomm/internal/api"
	"safecomm/internal/bot"
	"safecomm/internal/db"
	"safecomm/internal/ws"
)

func getEnv(key, defaultVal string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}
	return defaultVal
}

func main() {
	log.Println("======================================")
	log.Println("       SafeComm Server Starting       ")
	log.Println("======================================")

	port := getEnv("PORT", "8080")
	botToken := os.Getenv("BOT_TOKEN")
	jwtSecret := getEnv("JWT_SECRET", "safecomm-dev-secret-key-min-32-chars-long!")
	dbPath := getEnv("DB_PATH", filepath.Join("data", "safecomm.db"))
	webAppURL := getEnv("WEBAPP_URL", "")
	webDir := getEnv("WEB_DIR", "web")
	uploadDir := getEnv("UPLOADS_DIR", "uploads")

	if jwtSecret == "safecomm-dev-secret-key-min-32-chars-long!" {
		log.Println("[WARN] Using default JWT_SECRET. Set JWT_SECRET in production!")
	}

	// Ensure required directories exist
	dirs := []string{
		filepath.Dir(dbPath),
		filepath.Join(uploadDir, "voice"),
		filepath.Join(uploadDir, "photos"),
		webDir,
	}
	for _, d := range dirs {
		if err := os.MkdirAll(d, 0755); err != nil {
			log.Fatalf("[FATAL] Failed to create directory %s: %v", d, err)
		}
	}

	// Initialize Database
	database, err := db.New(dbPath)
	if err != nil {
		log.Fatalf("[FATAL] Failed to initialize database: %v", err)
	}
	defer database.Close()

	// Initialize WebSocket Hub
	hub := ws.NewHub()
	go hub.Run()

	// Context for graceful shutdown
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Initialize Telegram Bot if token provided
	if botToken != "" {
		tgBot, err := bot.NewBot(botToken, webAppURL)
		if err != nil {
			log.Printf("[WARN] Failed to start Telegram Bot: %v", err)
		} else {
			go tgBot.Start(ctx)
			log.Println("[Bot] Telegram Bot listener running")
		}
	} else {
		log.Println("[INFO] BOT_TOKEN not set. Telegram bot polling disabled.")
	}

	// Setup API Router
	router := api.SetupRouter(api.Config{
		DB:        database,
		Hub:       hub,
		JWTSecret: jwtSecret,
		BotToken:  botToken,
		WebDir:    webDir,
		UploadDir: uploadDir,
	})

	server := &http.Server{
		Addr:         ":" + port,
		Handler:      router,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// Start HTTP server in a goroutine
	go func() {
		log.Printf("[Server] Listening on http://localhost:%s", port)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatalf("[FATAL] Server error: %v", err)
		}
	}()

	// Listen for termination signals
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	<-sigChan

	log.Println("[Server] Shutting down gracefully...")
	cancel()

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("[Server] Shutdown forced: %v", err)
	}

	log.Println("[Server] SafeComm server stopped successfully")
}
