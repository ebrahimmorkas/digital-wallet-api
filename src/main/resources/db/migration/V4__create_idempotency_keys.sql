CREATE TABLE idempotency_keys
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id),
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    transaction_id  UUID REFERENCES transactions (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Keys are scoped per user; the unique index is what serialises concurrent retries.
    CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys (created_at);
