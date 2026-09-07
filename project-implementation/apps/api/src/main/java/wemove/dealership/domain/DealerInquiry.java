package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity(name = "DealerInquiry")
@Table(name = "dealer_inquiries")
public class DealerInquiry {
    @Id public UUID id;
    @Column(nullable = false, unique = true, length = 32) public String inquiryNumber;
    @Column(nullable = false) public UUID companyId;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false, length = 16) public String status;
    public LocalDate expectedDeliveryDate;
    @Column(length = 2000) public String deliveryNotes;
    @Column(length = 2000) public String purpose;
    @Column(length = 2000) public String remark;
    @Column(length = 2000) public String publicReply;
    @Column(length = 500) public String closeReason;
    @Version public long version;
    @Column(nullable = false) public Instant createdAt;
    @Column(nullable = false) public Instant updatedAt;
}
