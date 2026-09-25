package com.ebrahimmorkas.wallet.wallet;

public enum WalletType {
    /** Belongs to a customer; balance can never go negative. */
    USER,
    /** Platform counter-account per currency; its balance mirrors money that entered or left the platform. */
    SYSTEM
}
