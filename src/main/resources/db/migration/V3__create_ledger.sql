-- A transaction is one business operation; its ledger entries are the immutable double-entry postings.
CREATE TABLE transactions
(
    id               UUID PRIMARY KEY,
    type             VARCHAR(20)    NOT NULL,
    amount           NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3)     NOT NULL,
    debit_wallet_id  UUID           NOT NULL REFERENCES wallets (id),
    credit_wallet_id UUID           NOT NULL REFERENCES wallets (id),
    description      VARCHAR(255),
    initiated_by     UUID           NOT NULL REFERENCES users (id),
    created_at       TIMESTAMPTZ    NOT NULL,
    CHECK (debit_wallet_id <> credit_wallet_id)
);

CREATE TABLE ledger_entries
(
    id             BIGSERIAL PRIMARY KEY,
    transaction_id UUID           NOT NULL REFERENCES transactions (id),
    wallet_id      UUID           NOT NULL REFERENCES wallets (id),
    direction      VARCHAR(6)     NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount         NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    balance_after  NUMERIC(19, 2) NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_ledger_entries_wallet_created ON ledger_entries (wallet_id, created_at DESC, id DESC);
CREATE INDEX idx_ledger_entries_transaction ON ledger_entries (transaction_id);
