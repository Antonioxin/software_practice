package wemove.commerce.domain;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * 持久化购物车商品行实体。
 *
 * <p>保存购物车、商品、数量和最近确认的零售价；商品当前是否可售以及库存是否充足由商品目录服务在业务操作中核验。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
@Entity(name = "CommerceCartItem")
@Table(name = "commerce_cart_items")
public class CartItem {
    @Id public UUID id;
    public UUID cartId;
    public UUID productId;
    public int quantity;
    public long lastConfirmedUnitPriceFen;
}
