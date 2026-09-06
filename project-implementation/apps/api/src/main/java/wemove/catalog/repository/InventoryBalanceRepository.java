package wemove.catalog.repository;

import wemove.catalog.domain.InventoryBalanceEntity;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalanceEntity, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBalanceEntity b where b.productId = :productId")
    Optional<InventoryBalanceEntity> findForUpdateByProductId(@Param("productId") UUID productId);
}
