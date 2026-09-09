package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 模拟退款实体。
 *
 * <p>关联订单和成功付款尝试，保存整单模拟退款的金额、原因、引用标识及操作者；退款金额由已记录的成功付款事实确定。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
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
