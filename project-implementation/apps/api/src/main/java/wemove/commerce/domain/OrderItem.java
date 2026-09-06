package wemove.commerce.domain;

import jakarta.persistence.*;

import java.util.UUID;

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
