package wemove.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import wemove.platform.api.ApiException;
import wemove.catalog.api.CatalogDtos;
import wemove.catalog.domain.*;
import wemove.catalog.platform.InventoryPort;
import wemove.catalog.repository.*;
import wemove.platform.idempotency.IdempotencyRecordEntity;
import wemove.platform.AuditPort;
import wemove.platform.idempotency.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService implements InventoryPort {
    private final ProductRepository products;
    private final InventoryBalanceRepository balances;
    private final InventoryMovementRepository movements;
    private final IdempotencyRecordRepository idempotency;
    private final AuditPort audit;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public InventoryService(ProductRepository products, InventoryBalanceRepository balances,
                            InventoryMovementRepository movements, IdempotencyRecordRepository idempotency,
                            AuditPort audit, ObjectMapper mapper) {
        this.products = products;
        this.balances = balances;
        this.movements = movements;
        this.idempotency = idempotency;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional
    public CommandResult<CatalogDtos.AdminProductView> adjust(UUID actorId, UUID productId, UUID key,
                                                               CatalogDtos.StockAdjustmentRequest request) {
        String reason = request.reason().strip();
        if (reason.codePointCount(0, reason.length()) < 2 || reason.codePointCount(0, reason.length()) > 500) {
            throw validation("reason", "调整原因需为 2—500 个字符。");
        }
        String operation = "catalog.adjustStock:" + productId;
        String hash = hash(request.direction() + "|" + request.quantity() + "|" + reason);
        Optional<IdempotencyRecordEntity> existing = idempotency
            .findByActorIdAndOperationIdAndIdempotencyKey(actorId, operation, key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "同一请求标识不能用于不同内容。");
            }
            try {
                return new CommandResult<>(mapper.readValue(existing.get().getResponseJson(), CatalogDtos.AdminProductView.class), true);
            } catch (JsonProcessingException ex) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_DATA_INVALID", "已保存的请求结果无法读取。");
            }
        }
        ProductEntity product = products.findForUpdateById(productId).orElseThrow(this::notFound);
        InventoryBalanceEntity balance = balances.findForUpdateByProductId(productId)
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVENTORY_NOT_INITIALIZED", "商品库存记录缺失。"));
        Instant now = clock.instant();
        InventoryBalanceEntity.StockChange change = balance.adjust(request.direction(), request.quantity(), now);
        movements.save(new InventoryMovementEntity(productId, request.direction(), request.quantity(), change.before(),
            change.after(), reason, "ADMIN_ADJUSTMENT", key.toString(), actorId, now));
        balances.flush();
        CatalogDtos.AdminProductView result = adminView(product, balance);
        audit.append(new AuditPort.AuditEvent(actorId, "INVENTORY_ADJUSTED", "PRODUCT", productId,
            "SUCCESS", reason, now));
        try {
            idempotency.save(new IdempotencyRecordEntity(actorId, operation, key, hash,
                mapper.writeValueAsString(result), 200, now));
        } catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
        return new CommandResult<>(result, false);
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.StockMovementView> movements(UUID productId, Pageable pageable) {
        if (!products.existsById(productId)) throw notFound();
        return movements.findByProductId(productId, pageable).map(this::movement);
    }

    @Override
    @Transactional
    public void deductForPayment(UUID orderId, Collection<InventoryItem> items) {
        List<InventoryItem> ordered = validateAndSort(items);
        String sourceId = orderId.toString();
        long alreadyApplied = ordered.stream().filter(item -> movements.existsByProductIdAndSourceTypeAndSourceId(
            item.productId(), "PAYMENT_DEDUCT", sourceId)).count();
        if (alreadyApplied == ordered.size()) return;
        if (alreadyApplied > 0) throw new IllegalStateException("partial payment inventory effect detected");
        Instant now = clock.instant();
        for (InventoryItem item : ordered) {
            ProductEntity product = products.findForUpdateById(item.productId()).orElseThrow(this::unavailable);
            if (product.getStatus() != ProductStatus.PUBLISHED) throw unavailable();
            InventoryBalanceEntity balance = balances.findForUpdateByProductId(item.productId())
                .orElseThrow(() -> new IllegalStateException("inventory balance missing"));
            InventoryBalanceEntity.StockChange change = balance.adjust(StockDirection.DECREASE, item.quantity(), now);
            movements.save(new InventoryMovementEntity(item.productId(), StockDirection.DECREASE, item.quantity(),
                change.before(), change.after(), "模拟付款成功扣减", "PAYMENT_DEDUCT", sourceId, null, now));
        }
    }

    @Override
    @Transactional
    public void restoreForCancellation(UUID orderId, Collection<InventoryItem> items) {
        List<InventoryItem> ordered = validateAndSort(items);
        String sourceId = orderId.toString();
        long restored = ordered.stream().filter(item -> movements.existsByProductIdAndSourceTypeAndSourceId(
            item.productId(), "CANCELLATION_RESTORE", sourceId)).count();
        if (restored == ordered.size()) return;
        if (restored > 0) throw new IllegalStateException("partial cancellation inventory effect detected");
        for (InventoryItem item : ordered) {
            if (!movements.existsByProductIdAndSourceTypeAndSourceId(item.productId(), "PAYMENT_DEDUCT", sourceId)) {
                throw new ApiException(HttpStatus.CONFLICT, "STOCK_EFFECT_NOT_FOUND", "未找到该订单的库存扣减记录。");
            }
        }
        Instant now = clock.instant();
        for (InventoryItem item : ordered) {
            products.findForUpdateById(item.productId()).orElseThrow(this::notFound);
            InventoryBalanceEntity balance = balances.findForUpdateByProductId(item.productId())
                .orElseThrow(() -> new IllegalStateException("inventory balance missing"));
            InventoryBalanceEntity.StockChange change = balance.adjust(StockDirection.INCREASE, item.quantity(), now);
            movements.save(new InventoryMovementEntity(item.productId(), StockDirection.INCREASE, item.quantity(),
                change.before(), change.after(), "已付款订单取消返还", "CANCELLATION_RESTORE", sourceId, null, now));
        }
    }

    private List<InventoryItem> validateAndSort(Collection<InventoryItem> items) {
        if (items == null || items.isEmpty()) throw validation("items", "商品明细不能为空。");
        Set<UUID> ids = new HashSet<>();
        List<InventoryItem> result = new ArrayList<>();
        for (InventoryItem item : items) {
            if (item == null || item.productId() == null || item.quantity() < 1 || !ids.add(item.productId())) {
                throw validation("items", "商品明细包含无效或重复项目。");
            }
            result.add(item);
        }
        result.sort(Comparator.comparing(item -> item.productId().toString()));
        return result;
    }

    private CatalogDtos.StockMovementView movement(InventoryMovementEntity entity) {
        return new CatalogDtos.StockMovementView(entity.getId(), entity.getProductId(), entity.getDirection(),
            entity.getQuantity(), entity.getQuantityBefore(), entity.getQuantityAfter(), entity.getReason(),
            entity.getSourceType(), entity.getSourceId(), entity.getActorId(), entity.getCreatedAt());
    }

    private CatalogDtos.AdminProductView adminView(ProductEntity p, InventoryBalanceEntity balance) {
        List<String> imageIds = p.getImageIds() == null || p.getImageIds().isBlank()
            ? List.of() : List.of(p.getImageIds().split(","));
        return new CatalogDtos.AdminProductView(p.getId(), p.getSku(), p.getName(), p.getSummary(), p.getDescription(),
            p.getCategory() == null ? null : p.getCategory().getId(), p.getCategory() == null ? null : p.getCategory().getName(),
            p.getAgeMin(), p.getAgeMax(), p.getPlayType(), p.getScene(), p.getMaterial(), p.getDimensions(),
            p.getPackageContents(), p.getInstructions(), p.getSafetyNotes(), p.getMainImageId(), imageIds,
            p.getRetailUnitPriceFen(), p.isDealerEnabled(), p.getDealerReferenceUnitPriceFen(), p.getMinInquiryQuantity(),
            p.getLeadTimeText(), p.getStatus(), p.getDisplayOrder(), balance.getQuantity(), balance.getVersion(),
            p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private String hash(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private ApiException validation(String field, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。",
            List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", message)));
    }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。"); }
    private ApiException unavailable() { return new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", "商品不存在或已下架。"); }
    public record CommandResult<T>(T value, boolean replayed) {}
}
