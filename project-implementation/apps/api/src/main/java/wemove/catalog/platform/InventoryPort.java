package wemove.catalog.platform;

import java.util.*;

public interface InventoryPort {
    void deductForPayment(UUID orderId, Collection<InventoryItem> items);
    void restoreForCancellation(UUID orderId, Collection<InventoryItem> items);

    record InventoryItem(UUID productId, int quantity) {}
}
