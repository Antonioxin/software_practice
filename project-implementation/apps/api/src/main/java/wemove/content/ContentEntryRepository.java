package wemove.content;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ContentEntryRepository extends JpaRepository<ContentEntry, UUID>, JpaSpecificationExecutor<ContentEntry> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ContentEntry e where e.id = :id and e.kind = :kind")
    Optional<ContentEntry> lock(@Param("id") UUID id, @Param("kind") String kind);
    Optional<ContentEntry> findByIdAndKind(UUID id, String kind);
    List<ContentEntry> findByKindAndStatusOrderBySortOrderAscCreatedAtDesc(String kind, String status);
}
