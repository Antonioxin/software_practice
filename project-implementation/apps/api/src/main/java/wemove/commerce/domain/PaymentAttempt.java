package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "CommercePaymentAttempt")
@Table(name = "commerce_payment_attempts")
public class PaymentAttempt {
    @Id public UUID id;
    public UUID orderId;
    public String outcome;
    public String mode = "SIMULATED";
    public long amountFen;
    public String simulationReference;
    public UUID actorId;
    public Instant createdAt;
    public UUID successOrderId;
}
