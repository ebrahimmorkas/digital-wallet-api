package com.ebrahimmorkas.wallet.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Idempotency keys only need to outlive client retry windows; expired keys are purged hourly. */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyKeyCleanupJob {

    private final IdempotencyKeyRepository keyRepository;
    private final Clock clock;

    @Value("${app.idempotency.retention:24h}")
    private Duration retention;

    @Scheduled(cron = "${app.idempotency.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void purgeExpiredKeys() {
        int deleted = keyRepository.deleteCreatedBefore(clock.instant().minus(retention));
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
