package wemove.support.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wemove.support.domain.SupportTicketEntity;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository
        extends JpaRepository<SupportTicketEntity, UUID>, JpaSpecificationExecutor<SupportTicketEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SupportTicket t where t.id = :id")
    Optional<SupportTicketEntity> findForUpdateById(@Param("id") UUID id);
}
