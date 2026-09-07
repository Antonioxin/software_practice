package wemove.dealership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "DealerChannel")
@Table(name = "dealer_channels")
public class DealerChannel {
    @Id public UUID id;
    @Column(nullable = false, length = 100) public String name;
    @Column(nullable = false, length = 100) public String countryOrRegion;
    @Column(nullable = false, length = 100) public String city;
    @Column(nullable = false, length = 200) public String address;
    @Column(nullable = false, length = 21) public String phone;
    @Column(length = 2048) public String website;
    public UUID companyId;
    @Column(nullable = false) public boolean published;
    @Version public long version;
    @Column(nullable = false) public Instant createdAt;
    @Column(nullable = false) public Instant updatedAt;
}
