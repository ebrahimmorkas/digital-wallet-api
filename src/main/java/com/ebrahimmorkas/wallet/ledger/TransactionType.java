package com.ebrahimmorkas.wallet.ledger;

public enum TransactionType {
    /** Money entering the platform: SYSTEM wallet → user wallet. */
    DEPOSIT,
    /** Money leaving the platform: user wallet → SYSTEM wallet. */
    WITHDRAWAL,
    /** Peer-to-peer: user wallet → user wallet. */
    TRANSFER
}
