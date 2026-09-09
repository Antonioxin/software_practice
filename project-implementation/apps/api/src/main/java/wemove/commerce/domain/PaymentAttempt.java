package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 模拟付款尝试实体。
 *
 * <p>记录订单每次成功或失败的模拟付款结果、金额、引用标识和操作者，不连接真实支付渠道，也不保存真实支付敏感信息。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
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
