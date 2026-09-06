package wemove.catalog.service;

import wemove.platform.api.ApiException;
import wemove.catalog.domain.*;
import wemove.catalog.platform.CatalogPort;
import wemove.catalog.repository.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogIntegrationService implements CatalogPort {
    private final ProductRepository products;
    private final InventoryBalanceRepository balances;

    public CatalogIntegrationService(ProductRepository products, InventoryBalanceRepository balances) {
        this.products = products;
        this.balances = balances;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProductProjection> getPublicProducts(Collection<UUID> productIds) {
        return orderedIds(productIds).stream().map(products::findById).flatMap(Optional::stream)
            .filter(product -> product.getStatus() == ProductStatus.PUBLISHED)
            .map(product -> new PublicProductProjection(product.getId(), product.getSku(), product.getName(),
                price(product.getRetailUnitPriceFen()), true,
                stock(product.getId()) > 0)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetailProductSnapshot> getRetailSnapshot(Collection<RequestedItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        Set<UUID> seen = new HashSet<>();
        List<RetailProductSnapshot> result = new ArrayList<>();
        for (RequestedItem item : items) {
            if (item == null || item.productId() == null || item.quantity() < 1 || item.quantity() > 99 || !seen.add(item.productId())) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "商品明细包含无效或重复项目。");
            }
            ProductEntity product = products.findById(item.productId()).orElseThrow(this::unavailable);
            int available = stock(product.getId());
            result.add(new RetailProductSnapshot(product.getId(), product.getSku(), product.getName(),
                price(product.getRetailUnitPriceFen()), item.quantity(), available,
                product.getStatus() == ProductStatus.PUBLISHED));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealerProductProjection> getDealerProducts(Collection<UUID> productIds) {
        return orderedIds(productIds).stream().map(products::findById).flatMap(Optional::stream)
            .filter(product -> product.getStatus() == ProductStatus.PUBLISHED && product.isDealerEnabled()
                && product.getDealerReferenceUnitPriceFen() != null && product.getMinInquiryQuantity() != null
                && product.getLeadTimeText() != null)
            .map(product -> new DealerProductProjection(product.getId(), product.getSku(), product.getName(),
                price(product.getRetailUnitPriceFen()), product.getDealerReferenceUnitPriceFen(),
                product.getMinInquiryQuantity(), stock(product.getId()), product.getLeadTimeText())).toList();
    }

    private List<UUID> orderedIds(Collection<UUID> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().sorted(Comparator.comparing(UUID::toString)).toList();
    }
    private int stock(UUID id) { return balances.findById(id).map(balance -> balance.getQuantity()).orElse(0); }
    private long price(Long value) { return value == null ? 0 : value; }
    private ApiException unavailable() { return new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", "商品不存在或已下架。"); }
}
