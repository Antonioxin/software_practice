package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "DealerApplication")
@Table(name = "dealer_applications")
public class DealerApplication {
    @Id public UUID id;
    @Column(nullable = false, unique = true, length = 32) public String applicationNumber;
    @Column(nullable = false, unique = true) public UUID userId;
    @Column(nullable = false, length = 16) public String status;
    @Column(nullable = false) public int currentContentVersion;
    @Version public long version;
    @Column(length = 500) public String publicReason;
    @Column(length = 2000) public String internalNote;
    public UUID reviewedBy;
    @Column(nullable = false) public Instant createdAt;
    @Column(nullable = false) public Instant updatedAt;
}
