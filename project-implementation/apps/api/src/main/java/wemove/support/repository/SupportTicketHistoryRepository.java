package wemove.support.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wemove.support.domain.SupportTicketHistoryEntity;

import java.util.List;
import java.util.UUID;

public interface SupportTicketHistoryRepository extends JpaRepository<SupportTicketHistoryEntity, UUID> {
    List<SupportTicketHistoryEntity> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
