package com.wemove.identity.catalog.platform;

import java.util.*;

public interface CatalogPort {
    List<PublicProductProjection> getPublicProducts(Collection<UUID> productIds);
    List<RetailProductSnapshot> getRetailSnapshot(Collection<RequestedItem> items);
    List<DealerProductProjection> getDealerProducts(Collection<UUID> productIds);

    record RequestedItem(UUID productId, int quantity) {}
    record PublicProductProjection(UUID id, String sku, String name, long retailUnitPriceFen,
                                   boolean published, boolean inStock) {}
    record RetailProductSnapshot(UUID id, String sku, String name, long retailUnitPriceFen,
                                 int requestedQuantity, int availableQuantity, boolean published) {}
    record DealerProductProjection(UUID id, String sku, String name, long retailUnitPriceFen,
                                   long dealerReferenceUnitPriceFen, int minInquiryQuantity,
                                   int availableQuantity, String leadTimeText) {}
}
