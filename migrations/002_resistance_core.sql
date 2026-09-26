-- +goose Up

CREATE TABLE resistance_profiles (
    user_id INTEGER PRIMARY KEY,
    address TEXT NOT NULL DEFAULT '',
    gold INTEGER NOT NULL DEFAULT 0 CHECK (gold >= 0),
    high_score INTEGER NOT NULL DEFAULT 0 CHECK (high_score >= 0),
    energy INTEGER NOT NULL DEFAULT 100 CHECK (energy BETWEEN 0 AND 10000),
    scenario INTEGER NOT NULL DEFAULT 0 CHECK (scenario >= 0),
    head INTEGER NOT NULL DEFAULT 0 CHECK (head >= 0),
    body INTEGER NOT NULL DEFAULT 0 CHECK (body >= 0),
    arm INTEGER NOT NULL DEFAULT 0 CHECK (arm >= 0),
    health INTEGER NOT NULL DEFAULT 100 CHECK (health BETWEEN 0 AND 100000),
    attack INTEGER NOT NULL DEFAULT 10 CHECK (attack BETWEEN 0 AND 100000),
    critical INTEGER NOT NULL DEFAULT 0 CHECK (critical BETWEEN 0 AND 10000),
    durability INTEGER NOT NULL DEFAULT 0 CHECK (durability BETWEEN 0 AND 100000),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE resistance_friend_requests (
    requester_id INTEGER NOT NULL,
    addressee_id INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (requester_id, addressee_id),
    CHECK (requester_id <> addressee_id),
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (addressee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_resistance_friend_requests_addressee ON resistance_friend_requests (addressee_id, created_at DESC);

CREATE TABLE resistance_friendships (
    user_low_id INTEGER NOT NULL,
    user_high_id INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (user_low_id, user_high_id),
    CHECK (user_low_id < user_high_id),
    FOREIGN KEY (user_low_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user_high_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_resistance_friendships_high ON resistance_friendships (user_high_id, created_at DESC);

-- +goose Down

DROP TABLE resistance_friendships;
DROP TABLE resistance_friend_requests;
DROP TABLE resistance_profiles;
