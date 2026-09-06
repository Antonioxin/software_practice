package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "CommerceOrderHistory")
@Table(name = "commerce_order_history")
public class OrderHistory {
    @Id public UUID id;
    public UUID orderId;
    public String action;
    public String fromStatus;
    public String toStatus;
    public long orderVersion;
    public UUID actorId;

    @Column(length = 500)
    public String reason;

    public String requestId;
    public Instant createdAt;
}
