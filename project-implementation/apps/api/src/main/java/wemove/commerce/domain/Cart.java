package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 持久化购物车头实体。
 *
 * <p>每个用户对应一个购物车头，保存购物车版本和最近更新时间；服务层使用版本配合行锁检测并发变更。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
@Entity(name = "CommerceCart")
@Table(name = "commerce_carts")
public class Cart {
    @Id public UUID id;
    public UUID userId;
    public long version = 1;
    public Instant updatedAt;
}
