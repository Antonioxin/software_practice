package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "DealerReview")
@Table(name = "dealer_application_reviews")
public class DealerReview {
    @Id public UUID id;
    @Column(nullable = false) public UUID applicationId;
    @Column(nullable = false) public int contentVersion;
    @Column(nullable = false, length = 16) public String decision;
    @Column(length = 500) public String publicReason;
    @Column(length = 2000) public String internalNote;
    @Column(nullable = false) public UUID reviewerId;
    @Column(nullable = false) public Instant createdAt;
}
