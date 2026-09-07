package wemove.support.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "SupportTicket")
@Table(name = "support_tickets")
public class SupportTicketEntity {
    @Id public UUID id;
    @Column(length = 40) public String ticketNumber;
    public UUID actorId;
    @Column(length = 16) public String type;
    @Column(length = 16) public String status;
    @Column(length = 255) public String subject;
    @Column(length = 2000) public String body;
    @Column(length = 255) public String phone;
    public UUID productId;
    public UUID orderId;
    public long version = 1;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant closedAt;
    public UUID closedBy;
    @Column(length = 500) public String closeReason;
}
