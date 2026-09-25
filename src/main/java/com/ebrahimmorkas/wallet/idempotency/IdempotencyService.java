package com.ebrahimmorkas.wallet.idempotency;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.ledger.TransactionRepository;
import com.ebrahimmorkas.wallet.ledger.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Makes money-moving requests safe to retry. The key is claimed in the same database transaction
 * as the money movement, so either both commit or neither does: a retried request either replays
 * the original transaction or runs for the first time, and can never run twice.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository keyRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public IdempotentResult<TransactionResponse> execute(UUID userId, String key, String requestFingerprint,
                                                         Supplier<TransactionResponse> operation) {
        String requestHash = sha256(requestFingerprint);

        if (keyRepository.tryClaim(userId, key, requestHash) == 1) {
            TransactionResponse response = operation.get();
            keyRepository.findByUserIdAndKey(userId, key).orElseThrow().completeWith(response.id());
            return new IdempotentResult<>(response, false);
        }

        IdempotencyKey existing = keyRepository.findByUserIdAndKey(userId, key).orElseThrow();
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key '%s' was already used with a different request".formatted(key));
        }
        TransactionResponse original = transactionRepository.findById(existing.getTransactionId())
                .map(TransactionResponse::from)
                .orElseThrow();
        return new IdempotentResult<>(original, true);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
