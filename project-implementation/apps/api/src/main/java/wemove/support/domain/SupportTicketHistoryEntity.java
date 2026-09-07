package wemove.support.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 状态与操作历史（照 commerce_order_history 形状）。 */
@Entity(name = "SupportTicketHistory")
@Table(name = "support_ticket_history")
public class SupportTicketHistoryEntity {
    @Id public UUID id;
    public UUID ticketId;
    @Column(length = 64) public String action;
    @Column(length = 16) public String fromStatus;
    @Column(length = 16) public String toStatus;
    public long ticketVersion;
    public UUID actorId;
    @Column(length = 500) public String reason;
    @Column(length = 255) public String requestId;
    public Instant createdAt;
}
