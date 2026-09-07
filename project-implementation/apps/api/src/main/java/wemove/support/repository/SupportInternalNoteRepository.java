package wemove.support.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wemove.support.domain.SupportInternalNoteEntity;

import java.util.List;
import java.util.UUID;

public interface SupportInternalNoteRepository extends JpaRepository<SupportInternalNoteEntity, UUID> {
    List<SupportInternalNoteEntity> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
