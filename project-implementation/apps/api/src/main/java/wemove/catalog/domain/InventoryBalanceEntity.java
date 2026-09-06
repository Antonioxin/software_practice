package wemove.catalog.domain;

import wemove.platform.api.ApiException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "inventory_balances")
public class InventoryBalanceEntity {
    @Id
    @Column(name = "product_id")
    private UUID productId;
    @Column(nullable = false)
    private int quantity;
    @Version
    private long version;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryBalanceEntity() {}

    public InventoryBalanceEntity(UUID productId, int quantity, Instant now) {
        if (quantity < 0) throw new IllegalArgumentException("quantity must be non-negative");
        this.productId = productId;
        this.quantity = quantity;
        this.updatedAt = now;
    }

    public StockChange adjust(StockDirection direction, int amount, Instant now) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        int before = quantity;
        long candidate = direction == StockDirection.INCREASE ? (long) quantity + amount : (long) quantity - amount;
        if (candidate < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "本次调整将使库存不足，请减少调整数量。");
        }
        if (candidate > Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.CONFLICT, "STOCK_LIMIT_EXCEEDED", "库存数量超过系统允许范围。");
        }
        quantity = (int) candidate;
        updatedAt = now;
        return new StockChange(before, quantity);
    }

    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public record StockChange(int before, int after) {}
}
