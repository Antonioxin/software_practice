package com.wemove.identity.catalog.repository;

import com.wemove.identity.catalog.domain.CategoryEntity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    boolean existsByNameNormalized(String nameNormalized);
    boolean existsByNameNormalizedAndIdNot(String nameNormalized, UUID id);
    List<CategoryEntity> findByEnabledTrueOrderBySortOrderAscNameAsc();
    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();
}
