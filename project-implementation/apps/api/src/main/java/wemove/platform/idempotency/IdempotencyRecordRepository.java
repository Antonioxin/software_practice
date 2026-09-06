package wemove.platform.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {
    java.util.List<IdempotencyRecordEntity> findByActorIdAndIdempotencyKey(
            UUID actorId, UUID idempotencyKey);

    Optional<IdempotencyRecordEntity> findByActorIdAndOperationIdAndIdempotencyKey(
            UUID actorId, String operationId, UUID idempotencyKey);
}
