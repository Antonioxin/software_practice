package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 订单状态历史实体。
 *
 * <p>记录每次订单操作的动作、前后状态、订单版本、操作者、原因、请求标识和发生时间，为订单状态追踪与审计关联提供不可变记录。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
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
