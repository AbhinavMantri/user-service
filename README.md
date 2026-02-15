# user-service
User service to manage authentication and authorization

Entities
users
- id (UUID PK)
- email (unique, indexed)
- password_hash
- full_name
- phone (optional)
- role (varchar)
- status (ACTIVE, DISABLED)
- created_at
- updated_at

refresh_tokens
- id (UUID PK)
- user_id (FK)
- token_hash (store hash, not raw token)
- expires_at
- revoked (boolean)
- created_at

REST API contract
Auth

POST /user-service/v1/auth/register

POST /user-service/v1/auth/login

POST /user-service/v1/auth/refresh

POST /user-service/v1/auth/logout (optional: invalidate refresh token)

Users

GET /user-service/v1/users/me (current user profile)

PATCH /user-service/v1/users/me (optional: name, phone)

GET /user-service/v1/admin/users (ADMIN only)

PATCH /user-service/v1/admin/users/{id}/status (ADMIN only)

PATCH /user-service/v1/admin/users/{id}/role (ADMIN only)

User-service DB scripts

select current_database(); 
SET search_path TO auth_db;
-- PostgreSQL DDL for User Service (users + refresh_tokens)
-- Notes:
-- 1) Uses UUID primary keys (requires pgcrypto for gen_random_uuid()).
-- 2) Stores refresh token HASH (never store raw refresh token).
-- 3) Includes basic status/role constraints, indexes, and FK cascade.

BEGIN;

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -------------------------
-- USERS
-- -------------------------
CREATE TABLE IF NOT EXISTS users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(320) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(150),
  phone         VARCHAR(32),

  role          VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',

  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER','ORGANIZER','ADMIN','GATE_AGENT')),
  CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','DISABLED'))
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_users_role   ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- -------------------------
-- REFRESH TOKENS
-- -------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL,

  -- Store a hash of the refresh token (e.g., SHA-256 hex, or bcrypt string).
  -- If using SHA-256 hex: 64 chars. If using bcrypt: up to ~60 chars.
  -- We'll allow up to 255 to be flexible.
  token_hash  VARCHAR(255) NOT NULL,

  expires_at  TIMESTAMPTZ NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT FALSE,

  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
);

-- Ensure we don't store duplicate token hashes
CREATE UNIQUE INDEX IF NOT EXISTS uq_refresh_tokens_token_hash
  ON refresh_tokens(token_hash);

-- Fast lookups by user and token state
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
  ON refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at
  ON refresh_tokens(expires_at);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked_expires
  ON refresh_tokens(user_id, revoked, expires_at);

-- Add system admin user   
INSERT INTO auth_db.users (
  id,
  email,
  password_hash,
  full_name,
  role,
  status,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'admin@capstone.com',
  '$2a$10$sR.dcpItW5sJ3623WYdWLeXEiiVN/z4EgVCPhd5sQE9T7Kqk6gnfK',
  'System Administrator',
  'ADMIN',
  'ACTIVE',
  NOW(),
  NOW()
);
  
COMMIT;