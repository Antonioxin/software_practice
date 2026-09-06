package com.wemove.identity.catalog.repository;

import com.wemove.identity.catalog.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    long countByCategoryId(UUID categoryId);
    long countByStatus(ProductStatus status);

    @Query("select p from ProductEntity p where p.id = :id")
    Optional<ProductEntity> findDetailedById(@Param("id") UUID id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id = :id")
    Optional<ProductEntity> findForUpdateById(@Param("id") UUID id);
}
