-- Auth hardening: hash every bearer secret at rest, record why a session ended,
-- and replace the index set with one that matches how the auth queries actually
-- filter. Sized for six figures of users and their sessions.

-- ---------------------------------------------------------------------------
-- 1. Bearer secrets are stored as SHA-256 digests, never as plaintext
-- ---------------------------------------------------------------------------
-- Existing values are plaintext and cannot be converted into their own digest
-- without the original, so they are cleared first. Any outstanding verification
-- or reset link stops working and has to be requested again.
UPDATE users
   SET verification_token = NULL,
       verification_expires_at = NULL,
       password_reset_token = NULL,
       password_reset_expires_at = NULL;

ALTER TABLE users RENAME COLUMN verification_token TO verification_token_hash;
ALTER TABLE users RENAME COLUMN password_reset_token TO password_reset_token_hash;

ALTER TABLE users ALTER COLUMN verification_token_hash TYPE VARCHAR(64);
ALTER TABLE users ALTER COLUMN password_reset_token_hash TYPE VARCHAR(64);

-- refresh_tokens.token_hash was misnamed: it held the token itself, so every
-- existing row is a plaintext credential that can no longer be matched against
-- an incoming digest. They are deleted rather than migrated, which signs every
-- session out exactly once; clients re-authenticate and receive hashed sessions.
DELETE FROM refresh_tokens;
ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);

-- ---------------------------------------------------------------------------
-- 2. Session revocation metadata (drives refresh-token reuse detection)
-- ---------------------------------------------------------------------------
ALTER TABLE refresh_tokens
ADD COLUMN revoked_at TIMESTAMPTZ NULL,
ADD COLUMN revoked_reason VARCHAR(50) NULL;

-- ---------------------------------------------------------------------------
-- 3. Accounts provisioned by an identity provider have no password of their own
-- ---------------------------------------------------------------------------
-- Existing accounts all chose a password, so they keep password login. Only
-- accounts created by the Google flow are provisioned with it switched off.
ALTER TABLE users
ADD COLUMN password_login_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- ---------------------------------------------------------------------------
-- 4. Index rework
-- ---------------------------------------------------------------------------
-- These duplicate the b-tree PostgreSQL already builds for the UNIQUE
-- constraint on the same column. They served no read and cost a write on every
-- insert and update.
DROP INDEX IF EXISTS idx_users_public_id;
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_refresh_tokens_public_id;
DROP INDEX IF EXISTS idx_refresh_tokens_token_hash;

-- Backs "list my live sessions", ordered the way the endpoint returns them.
-- Partial, so it stays small as revoked sessions accumulate between sweeps.
DROP INDEX IF EXISTS idx_refresh_tokens_user_id;
DROP INDEX IF EXISTS idx_refresh_tokens_deleted_at;
CREATE INDEX idx_refresh_tokens_user_active
    ON refresh_tokens (user_id, last_accessed_at DESC)
    WHERE is_revoked = FALSE AND deleted_at IS NULL;

-- Backs the nightly retention sweep.
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- Verification and reset lookups only ever target rows that hold a token, which
-- is a small fraction of the table.
DROP INDEX IF EXISTS idx_users_verification_token;
DROP INDEX IF EXISTS idx_users_password_reset_token;
CREATE INDEX idx_users_verification_token_hash
    ON users (verification_token_hash)
    WHERE verification_token_hash IS NOT NULL;
CREATE INDEX idx_users_password_reset_token_hash
    ON users (password_reset_token_hash)
    WHERE password_reset_token_hash IS NOT NULL;

-- Login history is read as "latest N for this user" and swept by age.
DROP INDEX IF EXISTS idx_login_history_user_id;
CREATE INDEX idx_login_history_user_recent
    ON login_history (user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Backs the provider lookup on every Google sign-in.
DROP INDEX IF EXISTS idx_oauth_accounts_provider_user;
CREATE INDEX idx_oauth_accounts_provider_lookup
    ON oauth_accounts (provider, provider_user_id)
    WHERE deleted_at IS NULL;
