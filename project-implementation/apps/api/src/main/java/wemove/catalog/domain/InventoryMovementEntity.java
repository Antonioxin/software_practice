package wemove.catalog.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovementEntity {
    @Id
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StockDirection direction;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "quantity_before", nullable = false)
    private int quantityBefore;
    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;
    @Column(name = "source_id", length = 64)
    private String sourceId;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryMovementEntity() {}

    public InventoryMovementEntity(UUID productId, StockDirection direction, int quantity,
                                   int before, int after, String reason, String sourceType,
                                   String sourceId, UUID actorId, Instant now) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.direction = direction;
        this.quantity = quantity;
        this.quantityBefore = before;
        this.quantityAfter = after;
        this.reason = reason;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.actorId = actorId;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public StockDirection getDirection() { return direction; }
    public int getQuantity() { return quantity; }
    public int getQuantityBefore() { return quantityBefore; }
    public int getQuantityAfter() { return quantityAfter; }
    public String getReason() { return reason; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public UUID getActorId() { return actorId; }
    public Instant getCreatedAt() { return createdAt; }
}
