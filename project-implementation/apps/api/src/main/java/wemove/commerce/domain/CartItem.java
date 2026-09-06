package wemove.commerce.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity(name = "CommerceCartItem")
@Table(name = "commerce_cart_items")
public class CartItem {
    @Id public UUID id;
    public UUID cartId;
    public UUID productId;
    public int quantity;
    public long lastConfirmedUnitPriceFen;
}
