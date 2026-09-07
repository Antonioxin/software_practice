package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "DealerInquiryHistory")
@Table(name = "dealer_inquiry_history")
public class DealerInquiryHistory {
    @Id public UUID id;
    @Column(nullable = false) public UUID inquiryId;
    @Column(nullable = false, length = 32) public String action;
    @Column(length = 16) public String fromStatus;
    @Column(nullable = false, length = 16) public String toStatus;
    @Column(nullable = false) public long inquiryVersion;
    @Column(nullable = false) public UUID actorId;
    @Column(length = 500) public String reason;
    @Column(nullable = false) public Instant createdAt;
}
