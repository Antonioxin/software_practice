package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "DealerCompany")
@Table(name = "dealer_companies")
public class DealerCompany {
    @Id public UUID id;
    @Column(nullable = false, unique = true) public UUID ownerUserId;
    @Column(nullable = false, unique = true) public UUID sourceApplicationId;
    @Column(nullable = false) public boolean sourcePublicConsent;
    @Column(nullable = false, length = 100) public String companyName;
    @Column(nullable = false, length = 32) public String businessType;
    @Column(nullable = false, length = 100) public String countryOrRegion;
    @Column(nullable = false, length = 100) public String city;
    @Column(nullable = false, length = 50) public String contactName;
    @Column(nullable = false, length = 21) public String phone;
    @Column(nullable = false, length = 254) public String cooperationEmail;
    @Column(length = 2048) public String website;
    @Column(nullable = false, length = 16) public String cooperationStatus;
    @Column(length = 2000) public String internalNote;
    @Version public long version;
    @Column(nullable = false) public Instant createdAt;
    @Column(nullable = false) public Instant updatedAt;
}
