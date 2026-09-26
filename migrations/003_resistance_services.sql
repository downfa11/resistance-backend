-- +goose Up

CREATE TABLE resistance_currency_rates (
    code TEXT PRIMARY KEY,
    rate INTEGER NOT NULL CHECK (rate > 0),
    uses INTEGER NOT NULL DEFAULT 0 CHECK (uses >= 0),
    updated_at TEXT NOT NULL
);

CREATE TABLE resistance_currency_balances (
    user_id INTEGER NOT NULL,
    code TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity BETWEEN 0 AND 1000000),
    updated_at TEXT NOT NULL,
    PRIMARY KEY (user_id, code),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (code) REFERENCES resistance_currency_rates(code)
);

CREATE TABLE resistance_exchange_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    currency_code TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    rate INTEGER NOT NULL CHECK (rate > 0),
    gold_granted INTEGER NOT NULL CHECK (gold_granted > 0),
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (user_id, idempotency_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (currency_code) REFERENCES resistance_currency_rates(code)
);

CREATE TABLE resistance_supporter_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    details TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE resistance_supporter_codes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    kind TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    reward_gold INTEGER NOT NULL DEFAULT 0 CHECK (reward_gold BETWEEN 0 AND 1000000000000),
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'REDEEMED')),
    redeemed_by INTEGER NULL,
    redeemed_at TEXT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (redeemed_by) REFERENCES users(id)
);

CREATE TABLE resistance_supporter_redemptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    code_id INTEGER NOT NULL UNIQUE,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    reward_gold INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (user_id, idempotency_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (code_id) REFERENCES resistance_supporter_codes(id)
);

CREATE TABLE resistance_notices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    published_at TEXT NULL,
    created_by_user_id INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE resistance_content_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version TEXT NOT NULL UNIQUE,
    published_at TEXT NULL,
    created_by_user_id INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE resistance_content_entries (
    version_id INTEGER NOT NULL,
    chapter_id TEXT NOT NULL,
    content_id TEXT NOT NULL,
    media_type TEXT NOT NULL,
    body TEXT NOT NULL,
    sha256_hex TEXT NOT NULL CHECK (length(sha256_hex) = 64),
    ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
    PRIMARY KEY (version_id, content_id),
    FOREIGN KEY (version_id) REFERENCES resistance_content_versions(id) ON DELETE CASCADE
);

INSERT INTO resistance_currency_rates (code, rate, uses, updated_at) VALUES
    ('JPY', 1090, 0, '2026-09-21T00:00:00Z'),
    ('XAG', 27, 0, '2026-09-21T00:00:00Z'),
    ('XPT', 1201, 0, '2026-09-21T00:00:00Z'),
    ('CNY', 177, 0, '2026-09-21T00:00:00Z'),
    ('SPD', 1, 0, '2026-09-21T00:00:00Z'),
    ('KRW', 1335, 0, '2026-09-21T00:00:00Z');

-- +goose Down

DROP TABLE resistance_content_entries;
DROP TABLE resistance_content_versions;
DROP TABLE resistance_notices;
DROP TABLE resistance_supporter_redemptions;
DROP TABLE resistance_supporter_codes;
DROP TABLE resistance_supporter_details;
DROP TABLE resistance_exchange_transactions;
DROP TABLE resistance_currency_balances;
DROP TABLE resistance_currency_rates;
