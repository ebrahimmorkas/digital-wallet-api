package com.ebrahimmorkas.wallet.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    /**
     * Claims the key. Returns 1 if this request owns it, 0 if it already existed. If another
     * in-flight transaction holds the same key, PostgreSQL blocks here until that transaction
     * commits (→ 0, replay) or rolls back (→ 1, we proceed), so duplicates can't slip through.
     */
    @Modifying
    @Query(value = """
            insert into idempotency_keys (user_id, idempotency_key, request_hash, created_at)
            values (:userId, :key, :requestHash, now())
            on conflict (user_id, idempotency_key) do nothing""", nativeQuery = true)
    int tryClaim(@Param("userId") UUID userId, @Param("key") String key, @Param("requestHash") String requestHash);

    Optional<IdempotencyKey> findByUserIdAndKey(UUID userId, String key);

    @Modifying
    @Query("delete from IdempotencyKey k where k.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
