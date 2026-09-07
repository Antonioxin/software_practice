package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "DealerApplicationVersion")
@Table(name = "dealer_application_versions")
public class DealerApplicationVersion {
    @Id public UUID id;
    @Column(nullable = false) public UUID applicationId;
    @Column(nullable = false) public int contentVersion;
    @Column(nullable = false, length = 100) public String companyName;
    @Column(nullable = false, length = 32) public String businessType;
    @Column(nullable = false, length = 100) public String countryOrRegion;
    @Column(nullable = false, length = 100) public String city;
    @Column(nullable = false, length = 50) public String contactName;
    @Column(nullable = false, length = 21) public String phone;
    @Column(nullable = false, length = 254) public String cooperationEmail;
    @Column(nullable = false, length = 2000) public String businessChannels;
    @Column(length = 2048) public String website;
    @Column(nullable = false, length = 2000) public String cooperationIntent;
    @Column(nullable = false) public boolean publicChannelConsent;
    @Column(nullable = false) public Instant submittedAt;
}
