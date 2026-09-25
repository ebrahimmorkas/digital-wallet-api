package com.ebrahimmorkas.wallet.wallet;

public enum WalletStatus {
    ACTIVE,
    /** Frozen wallets can neither send nor receive money (e.g. suspected fraud). */
    FROZEN
}
