package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "CommerceRefund")
@Table(name = "commerce_refunds")
public class Refund {
    @Id public UUID id;
    public UUID orderId;
    public UUID paymentAttemptId;
    public long amountFen;
    public String simulationReference;
    public String mode = "SIMULATED";
    public UUID actorId;

    @Column(length = 500)
    public String reason;

    public Instant createdAt;
}
