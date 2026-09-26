-- +goose Up

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account TEXT NOT NULL COLLATE NOCASE UNIQUE,
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL,
    display_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'PLAYER' CHECK (role IN ('PLAYER', 'ADMIN')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE refresh_sessions (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    token_hash BLOB NOT NULL UNIQUE,
    parent_session_id TEXT NULL,
    replaced_by_session_id TEXT NULL,
    expires_at TEXT NOT NULL,
    revoked_at TEXT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_session_id) REFERENCES refresh_sessions(id),
    FOREIGN KEY (replaced_by_session_id) REFERENCES refresh_sessions(id)
);

CREATE INDEX idx_refresh_sessions_user_expiry
    ON refresh_sessions (user_id, expires_at);

CREATE INDEX idx_refresh_sessions_family
    ON refresh_sessions (family_id);

CREATE TABLE notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('SYSTEM', 'RESISTANCE')),
    title TEXT NOT NULL CHECK (length(title) BETWEEN 1 AND 160),
    body TEXT NOT NULL CHECK (length(body) BETWEEN 1 AND 4000),
    deep_link TEXT NULL,
    read_at TEXT NULL,
    expires_at TEXT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_inbox
    ON notifications (user_id, read_at, created_at DESC, id DESC);

CREATE INDEX idx_notifications_expiry
    ON notifications (expires_at)
    WHERE expires_at IS NOT NULL;

-- +goose Down

DROP TABLE notifications;
DROP TABLE refresh_sessions;
DROP TABLE users;
