package db

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"path/filepath"

	_ "modernc.org/sqlite"
)

// DB wraps the sql.DB connection
type DB struct {
	*sql.DB
}

// New opens (or creates) the SQLite database and runs migrations
func New(dbPath string) (*DB, error) {
	dir := filepath.Dir(dbPath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return nil, fmt.Errorf("create db dir: %w", err)
	}

	sqlDB, err := sql.Open("sqlite", dbPath)
	if err != nil {
		return nil, fmt.Errorf("open db: %w", err)
	}

	// Enable WAL mode for concurrent reads
	if _, err := sqlDB.Exec("PRAGMA journal_mode=WAL"); err != nil {
		return nil, fmt.Errorf("set WAL: %w", err)
	}
	if _, err := sqlDB.Exec("PRAGMA foreign_keys=ON"); err != nil {
		return nil, fmt.Errorf("enable FK: %w", err)
	}

	d := &DB{sqlDB}
	if err := d.migrate(); err != nil {
		return nil, fmt.Errorf("migrate: %w", err)
	}

	log.Println("[DB] Database ready at", dbPath)
	return d, nil
}

func (d *DB) migrate() error {
	migrations := []string{
		`CREATE TABLE IF NOT EXISTS users (
			id            INTEGER PRIMARY KEY AUTOINCREMENT,
			telegram_id   INTEGER UNIQUE NOT NULL,
			nickname      TEXT UNIQUE NOT NULL,
			avatar_url    TEXT DEFAULT '',
			created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
		)`,

		`CREATE TABLE IF NOT EXISTS groups_ (
			id                    INTEGER PRIMARY KEY AUTOINCREMENT,
			name                  TEXT NOT NULL,
			invite_code           TEXT UNIQUE NOT NULL,
			password_hash         TEXT NOT NULL,
			creator_telegram_id   INTEGER NOT NULL,
			created_at            DATETIME DEFAULT CURRENT_TIMESTAMP
		)`,

		`CREATE TABLE IF NOT EXISTS group_members (
			id        INTEGER PRIMARY KEY AUTOINCREMENT,
			group_id  INTEGER NOT NULL REFERENCES groups_(id) ON DELETE CASCADE,
			user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
			role      TEXT NOT NULL DEFAULT 'member',
			joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			UNIQUE(group_id, user_id)
		)`,

		`CREATE TABLE IF NOT EXISTS voice_messages (
			id           INTEGER PRIMARY KEY AUTOINCREMENT,
			group_id     INTEGER NOT NULL REFERENCES groups_(id) ON DELETE CASCADE,
			sender_id    INTEGER NOT NULL REFERENCES users(id),
			file_path    TEXT NOT NULL,
			duration_sec INTEGER DEFAULT 0,
			created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
		)`,

		`CREATE TABLE IF NOT EXISTS alerts (
			id          INTEGER PRIMARY KEY AUTOINCREMENT,
			group_id    INTEGER NOT NULL REFERENCES groups_(id) ON DELETE CASCADE,
			sender_id   INTEGER NOT NULL REFERENCES users(id),
			alert_type  TEXT NOT NULL,
			latitude    REAL NOT NULL,
			longitude   REAL NOT NULL,
			description TEXT DEFAULT '',
			photo_path  TEXT DEFAULT '',
			is_active   INTEGER DEFAULT 1,
			created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
			resolved_at DATETIME
		)`,

		`CREATE TABLE IF NOT EXISTS user_settings (
			user_id       INTEGER PRIMARY KEY REFERENCES users(id),
			alert_radius  REAL DEFAULT 2.0,
			sound_enabled INTEGER DEFAULT 1
		)`,

		`CREATE INDEX IF NOT EXISTS idx_alerts_group_active ON alerts(group_id, is_active)`,
		`CREATE INDEX IF NOT EXISTS idx_voice_group ON voice_messages(group_id, created_at DESC)`,
		`CREATE INDEX IF NOT EXISTS idx_gm_group ON group_members(group_id)`,
		`CREATE INDEX IF NOT EXISTS idx_gm_user ON group_members(user_id)`,
	}

	for i, m := range migrations {
		if _, err := d.Exec(m); err != nil {
			return fmt.Errorf("migration %d: %w", i, err)
		}
	}
	return nil
}
