package wemove.catalog.repository;

import wemove.catalog.domain.InventoryMovementEntity;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID> {
    Page<InventoryMovementEntity> findByProductId(UUID productId, Pageable pageable);
    boolean existsByProductIdAndSourceTypeAndSourceId(UUID productId, String sourceType, String sourceId);
}
