package wemove.identity.repository;

import wemove.identity.domain.AccountStatusHistoryEntity;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistoryEntity, UUID> {
    List<AccountStatusHistoryEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
