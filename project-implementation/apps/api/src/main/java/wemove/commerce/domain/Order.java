package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "CommerceOrder")
@Table(name = "commerce_orders")
public class Order {
    @Id public UUID id;
    public String orderNumber;
    public UUID userId;
    public UUID previewId;
    public String status;
    public long version = 1;
    public String currency = "CNY";
    public String mode = "SIMULATED";
    public long subtotalFen;
    public long shippingFen;
    public long taxFen;
    public long discountFen;
    public long totalFen;
    public String recipient;
    public String phone;
    public String countryOrRegion;
    public String region;
    public String city;
    public String addressLine;

    @Column(length = 2000)
    public String remark;

    public Instant createdAt;
    public Instant paidAt;
    public Instant cancelledAt;
    public Instant shippedAt;
    public Instant completedAt;
    public String logisticsName;
    public String trackingNumber;
    public UUID shippedBy;
}
