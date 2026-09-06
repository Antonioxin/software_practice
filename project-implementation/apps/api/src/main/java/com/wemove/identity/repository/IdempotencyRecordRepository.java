package com.wemove.identity.repository;

import com.wemove.identity.domain.IdempotencyRecordEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {
    Optional<IdempotencyRecordEntity> findByActorIdAndOperationIdAndIdempotencyKey(
        UUID actorId, String operationId, UUID idempotencyKey);
}
