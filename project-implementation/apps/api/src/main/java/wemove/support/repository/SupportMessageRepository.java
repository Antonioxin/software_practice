package wemove.support.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wemove.support.domain.SupportMessageEntity;

import java.util.List;
import java.util.UUID;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, UUID> {
    List<SupportMessageEntity> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
