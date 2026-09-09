package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 结算预览持久化实体。
 *
 * <p>关联用户、购物车版本和短期有效期，仅保存预览令牌摘要及订单创建所需的商品快照；创建订单时据此校验令牌只能被当前用户消费一次。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
@Entity(name = "CommerceCheckoutPreview")
@Table(name = "commerce_checkout_previews")
public class CheckoutPreview {
    @Id public UUID id;
    public String tokenHash;
    public UUID userId;
    public UUID cartId;
    public long cartVersion;

    @Column(columnDefinition = "text")
    public String snapshotJson;

    public Instant createdAt;
    public Instant expiresAt;
}
