package wemove.catalog.repository;

import wemove.catalog.domain.CategoryEntity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    boolean existsByNameNormalized(String nameNormalized);
    boolean existsByNameNormalizedAndIdNot(String nameNormalized, UUID id);
    List<CategoryEntity> findByEnabledTrueOrderBySortOrderAscNameAsc();
    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();
}
