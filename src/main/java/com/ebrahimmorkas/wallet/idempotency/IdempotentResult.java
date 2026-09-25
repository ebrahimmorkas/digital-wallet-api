package com.ebrahimmorkas.wallet.idempotency;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** The outcome of an idempotent request, and whether it was served from a previous execution. */
public record IdempotentResult<T>(T body, boolean replayed) {

    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    public ResponseEntity<T> toResponse(HttpStatus status) {
        return ResponseEntity.status(status).header(REPLAYED_HEADER, String.valueOf(replayed)).body(body);
    }
}
