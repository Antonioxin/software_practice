package wemove.catalog.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import wemove.catalog.domain.InventoryMovementEntity;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    java.util.List<InventoryMovementEntity> findBySourceTypeAndSourceId(
            String sourceType, String sourceId);

    Page<InventoryMovementEntity> findByProductId(UUID productId, Pageable pageable);

    boolean existsByProductIdAndSourceTypeAndSourceId(
            UUID productId, String sourceType, String sourceId);
}
