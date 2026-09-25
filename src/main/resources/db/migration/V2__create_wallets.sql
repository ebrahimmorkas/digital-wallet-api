CREATE TABLE wallets
(
    id         UUID PRIMARY KEY,
    owner_id   UUID REFERENCES users (id),
    currency   VARCHAR(3)     NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    type       VARCHAR(10)    NOT NULL,
    status     VARCHAR(10)    NOT NULL,
    version    BIGINT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL,
    -- User wallets can never go negative; SYSTEM wallets mirror money entering/leaving the platform.
    CONSTRAINT chk_user_wallet_non_negative CHECK (type = 'SYSTEM' OR balance >= 0),
    CONSTRAINT chk_user_wallet_has_owner CHECK (type = 'SYSTEM' OR owner_id IS NOT NULL)
);

-- One wallet per currency per user, and exactly one system wallet per currency.
CREATE UNIQUE INDEX uq_wallets_owner_currency ON wallets (owner_id, currency) WHERE type = 'USER';
CREATE UNIQUE INDEX uq_wallets_system_currency ON wallets (currency) WHERE type = 'SYSTEM';

-- A system wallet per supported currency is the counter-account for deposits and withdrawals.
INSERT INTO wallets (id, owner_id, currency, balance, type, status, created_at)
VALUES (gen_random_uuid(), NULL, 'USD', 0, 'SYSTEM', 'ACTIVE', now()),
       (gen_random_uuid(), NULL, 'EUR', 0, 'SYSTEM', 'ACTIVE', now()),
       (gen_random_uuid(), NULL, 'GBP', 0, 'SYSTEM', 'ACTIVE', now()),
       (gen_random_uuid(), NULL, 'INR', 0, 'SYSTEM', 'ACTIVE', now());
