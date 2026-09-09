package wemove.commerce.domain;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * 订单商品快照实体。
 *
 * <p>在建单时固化商品的 SKU、名称、成交单价、数量和行小计，使订单展示、金额核对及后续交易处理不依赖商品当前主数据。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
@Entity(name = "CommerceOrderItem")
@Table(name = "commerce_order_items")
public class OrderItem {
    @Id public UUID id;
    public UUID orderId;
    public UUID productId;
    public String sku;
    public String name;
    public long unitPriceFen;
    public int quantity;
    public long subtotalFen;
}
