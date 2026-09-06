package wemove.catalog.service;


import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import wemove.catalog.api.CatalogDtos;
import wemove.catalog.domain.*;
import wemove.catalog.platform.InventoryPort;
import wemove.catalog.repository.*;
import wemove.platform.AuditPort;
import wemove.platform.api.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class InventoryService implements InventoryPort {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entities;

    @org.springframework.beans.factory.annotation.Autowired
    private wemove.platform.TransactionProbe probe;

    private final ProductRepository products;
    private final InventoryBalanceRepository balances;
    private final InventoryMovementRepository movements;
    private final AuditPort audit;

    private final wemove.platform.idempotency.IdempotencyExecutor executor;

    private final Clock clock = Clock.systemUTC();

    public InventoryService(
            ProductRepository products,
            InventoryBalanceRepository balances,
            InventoryMovementRepository movements,
            AuditPort audit,
            wemove.platform.idempotency.IdempotencyExecutor executor) {
        this.products = products;
        this.balances = balances;
        this.movements = movements;
        this.audit = audit;
        this.executor = executor;
    }

    public CommandResult<CatalogDtos.AdminProductView> adjust(
            UUID actorId, UUID productId, UUID key, CatalogDtos.StockAdjustmentRequest request) {
        var execution =
                executor.execute(
                        actorId,
                        "catalog.adjustStock",
                        key,
                        "/api/v1/admin/products/" + productId + "/stock-adjustments",
                        request,
                        CatalogDtos.AdminProductView.class,
                        200,
                        hash(
                                request.direction()
                                        + "|"
                                        + request.quantity()
                                        + "|"
                                        + request.reason().strip()),
                        () -> {
                            String reason = request.reason().strip();
                            if (reason.codePointCount(0, reason.length()) < 2
                                    || reason.codePointCount(0, reason.length()) > 500)
                                throw validation("reason", "原因需为2—500字符。");
                            ProductEntity product =
                                    products.findForUpdateById(productId)
                                            .orElseThrow(this::notFound);
                            InventoryBalanceEntity balance =
                                    balances.findForUpdateByProductId(productId)
                                            .orElseThrow(
                                                    () ->
                                                            new ApiException(
                                                                    HttpStatus
                                                                            .INTERNAL_SERVER_ERROR,
                                                                    "INVENTORY_NOT_INITIALIZED",
                                                                    "商品库存记录缺失。"));
                            Instant now = clock.instant();
                            InventoryBalanceEntity.StockChange change =
                                    balance.adjust(request.direction(), request.quantity(), now);
                            movements.save(
                                    new InventoryMovementEntity(
                                            productId,
                                            request.direction(),
                                            request.quantity(),
                                            change.before(),
                                            change.after(),
                                            reason,
                                            "ADMIN_ADJUSTMENT",
                                            key.toString(),
                                            actorId,
                                            now));
                            balances.flush();
                            CatalogDtos.AdminProductView result = adminView(product, balance);
                            audit.append(
                                    new AuditPort.AuditEvent(
                                            actorId,
                                            "INVENTORY_ADJUSTED",
                                            "PRODUCT",
                                            productId,
                                            "SUCCESS",
                                            reason,
                                            now));
                            return result;
                        });
        return new CommandResult<>(execution.value(), execution.replayed());
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.StockMovementView> movements(UUID productId, Pageable pageable) {
        if (!products.existsById(productId)) throw notFound();
        return movements.findByProductId(productId, pageable).map(this::movement);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void deductForPayment(UUID orderId, Collection<InventoryItem> items) {
        List<InventoryItem> ordered = validateAndSort(items);
        List<LockedItem> locked = lockItems(ordered);
        String sourceId = orderId.toString();
        List<InventoryMovementEntity> original =
                movements.findBySourceTypeAndSourceId("PAYMENT_DEDUCT", sourceId);
        if (!original.isEmpty()) {
            requireExactEffect(ordered, original, StockDirection.DECREASE);
            return;
        }
        Instant now = clock.instant();
        for (LockedItem item : locked) {
            if (item.product().getStatus() != ProductStatus.PUBLISHED) throw unavailable();
            var change =
                    item.balance().adjust(StockDirection.DECREASE, item.request().quantity(), now);
            movements.save(
                    new InventoryMovementEntity(
                            item.request().productId(),
                            StockDirection.DECREASE,
                            item.request().quantity(),
                            change.before(),
                            change.after(),
                            "模拟付款成功扣减",
                            "PAYMENT_DEDUCT",
                            sourceId,
                            null,
                            now));
            probe.hit("inventory.deducted");
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void restoreForCancellation(UUID orderId, Collection<InventoryItem> items) {
        List<InventoryItem> ordered = validateAndSort(items);
        List<LockedItem> locked = lockItems(ordered);
        String sourceId = orderId.toString();
        List<InventoryMovementEntity> original =
                movements.findBySourceTypeAndSourceId("PAYMENT_DEDUCT", sourceId);
        requireExactEffect(ordered, original, StockDirection.DECREASE);
        List<InventoryMovementEntity> restored =
                movements.findBySourceTypeAndSourceId("CANCELLATION_RESTORE", sourceId);
        if (!restored.isEmpty()) {
            requireExactEffect(ordered, restored, StockDirection.INCREASE);
            return;
        }
        Map<UUID, Integer> originalQuantities = new HashMap<>();
        original.forEach(m -> originalQuantities.put(m.getProductId(), m.getQuantity()));
        Instant now = clock.instant();
        for (LockedItem item : locked) {
            int quantity = originalQuantities.get(item.request().productId());
            var change = item.balance().adjust(StockDirection.INCREASE, quantity, now);
            movements.save(
                    new InventoryMovementEntity(
                            item.request().productId(),
                            StockDirection.INCREASE,
                            quantity,
                            change.before(),
                            change.after(),
                            "已付款订单取消返还",
                            "CANCELLATION_RESTORE",
                            sourceId,
                            null,
                            now));
            probe.hit("inventory.restored");
        }
    }

    private List<LockedItem> lockItems(List<InventoryItem> ordered) {
        List<LockedItem> result = new ArrayList<>();
        for (InventoryItem item : ordered) {
            ProductEntity product =
                    products.findForUpdateById(item.productId()).orElseThrow(this::unavailable);
            entities.refresh(product, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            InventoryBalanceEntity balance =
                    balances.findForUpdateByProductId(item.productId())
                            .orElseThrow(
                                    () -> new IllegalStateException("inventory balance missing"));
            entities.refresh(balance, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            result.add(new LockedItem(item, product, balance));
        }
        return result;
    }

    private record LockedItem(
            InventoryItem request, ProductEntity product, InventoryBalanceEntity balance) {}

    private void requireExactEffect(
            List<InventoryItem> expected,
            List<InventoryMovementEntity> actual,
            StockDirection direction) {
        Map<UUID, Integer> quantities = new HashMap<>();
        for (InventoryMovementEntity movement : actual) {
            if (movement.getDirection() != direction
                    || quantities.put(movement.getProductId(), movement.getQuantity()) != null)
                throw effectConflict();
        }
        if (quantities.size() != expected.size()) throw effectConflict();
        for (InventoryItem item : expected)
            if (!Objects.equals(quantities.get(item.productId()), item.quantity()))
                throw effectConflict();
    }

    private ApiException effectConflict() {
        return new ApiException(
                HttpStatus.CONFLICT, "STOCK_EFFECT_MISMATCH", "库存操作必须与原订单完整明细及数量一致。");
    }

    private List<InventoryItem> validateAndSort(Collection<InventoryItem> items) {
        if (items == null || items.isEmpty()) throw validation("items", "商品明细不能为空。");
        Set<UUID> ids = new HashSet<>();
        List<InventoryItem> result = new ArrayList<>();
        for (InventoryItem item : items) {
            if (item == null
                    || item.productId() == null
                    || item.quantity() < 1
                    || !ids.add(item.productId())) {
                throw validation("items", "商品明细包含无效或重复项目。");
            }
            result.add(item);
        }
        result.sort(Comparator.comparing(item -> item.productId().toString()));
        return result;
    }

    private CatalogDtos.StockMovementView movement(InventoryMovementEntity entity) {
        return new CatalogDtos.StockMovementView(
                entity.getId(),
                entity.getProductId(),
                entity.getDirection(),
                entity.getQuantity(),
                entity.getQuantityBefore(),
                entity.getQuantityAfter(),
                entity.getReason(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getActorId(),
                entity.getCreatedAt());
    }

    private CatalogDtos.AdminProductView adminView(
            ProductEntity p, InventoryBalanceEntity balance) {
        List<String> imageIds =
                p.getImageIds() == null || p.getImageIds().isBlank()
                        ? List.of()
                        : List.of(p.getImageIds().split(","));
        return new CatalogDtos.AdminProductView(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getSummary(),
                p.getDescription(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName(),
                p.getAgeMin(),
                p.getAgeMax(),
                p.getPlayType(),
                p.getScene(),
                p.getMaterial(),
                p.getDimensions(),
                p.getPackageContents(),
                p.getInstructions(),
                p.getSafetyNotes(),
                p.getMainImageId(),
                imageIds,
                p.getRetailUnitPriceFen(),
                p.isDealerEnabled(),
                p.getDealerReferenceUnitPriceFen(),
                p.getMinInquiryQuantity(),
                p.getLeadTimeText(),
                p.getStatus(),
                p.getDisplayOrder(),
                balance.getQuantity(),
                balance.getVersion(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private String hash(String input) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ApiException validation(String field, String message) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_ERROR",
                "请检查标记的字段。",
                List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", message)));
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。");
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", "商品不存在或已下架。");
    }

    public record CommandResult<T>(T value, boolean replayed) {}
}
