package com.wemove.identity.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wemove.identity.api.ApiException;
import com.wemove.identity.catalog.api.CatalogDtos;
import com.wemove.identity.catalog.domain.*;
import com.wemove.identity.catalog.repository.*;
import com.wemove.identity.domain.IdempotencyRecordEntity;
import com.wemove.identity.platform.AuditPort;
import com.wemove.identity.repository.IdempotencyRecordRepository;
import jakarta.persistence.criteria.Predicate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryBalanceRepository balances;
    private final InventoryMovementRepository movements;
    private final IdempotencyRecordRepository idempotency;
    private final AuditPort audit;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public CatalogService(CategoryRepository categories, ProductRepository products,
                          InventoryBalanceRepository balances, InventoryMovementRepository movements,
                          IdempotencyRecordRepository idempotency, AuditPort audit, ObjectMapper mapper) {
        this.categories = categories;
        this.products = products;
        this.balances = balances;
        this.movements = movements;
        this.idempotency = idempotency;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.ProductCard> publicProducts(String keyword, UUID categoryId, Integer age,
                                                        PlayType playType, ProductScene scene,
                                                        Pageable pageable) {
        String term = CatalogRules.trimToNull(keyword);
        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.equal(root.get("status"), ProductStatus.PUBLISHED));
            if (term != null) {
                String like = "%" + term.toLowerCase(Locale.ROOT) + "%";
                filters.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("sku")), like)));
            }
            if (categoryId != null) filters.add(cb.equal(root.get("category").get("id"), categoryId));
            if (age != null) {
                filters.add(cb.lessThanOrEqualTo(root.get("ageMin"), age));
                filters.add(cb.or(cb.isNull(root.get("ageMax")), cb.greaterThanOrEqualTo(root.get("ageMax"), age)));
            }
            if (playType != null) filters.add(cb.equal(root.get("playType"), playType));
            if (scene != null) {
                if (scene == ProductScene.BOTH) filters.add(cb.equal(root.get("scene"), ProductScene.BOTH));
                else filters.add(root.get("scene").in(scene, ProductScene.BOTH));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
        return products.findAll(spec, pageable).map(this::card);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PublicProductDetail publicDetail(UUID id) {
        ProductEntity product = requireProduct(id);
        if (product.getStatus() == ProductStatus.DRAFT) notFound();
        int stock = balance(id).getQuantity();
        boolean listed = product.getStatus() == ProductStatus.PUBLISHED;
        if (!listed) {
            return new CatalogDtos.PublicProductDetail(product.getId(), product.getSku(), product.getName(),
                product.getSummary(), null, category(product.getCategory()), product.getAgeMin(), product.getAgeMax(),
                product.getPlayType(), product.getScene(), null, null, null, null, null, product.getMainImageId(),
                List.of(), valueOrZero(product.getRetailUnitPriceFen()), "CNY", stock > 0,
                stock > 0 ? "IN_STOCK" : "OUT_OF_STOCK", ProductStatus.UNLISTED, false,
                "该商品已下架，暂时不能购买。", product.getUpdatedAt());
        }
        boolean inStock = stock > 0;
        return new CatalogDtos.PublicProductDetail(product.getId(), product.getSku(), product.getName(),
            product.getSummary(), product.getDescription(), category(product.getCategory()), product.getAgeMin(),
            product.getAgeMax(), product.getPlayType(), product.getScene(), product.getMaterial(),
            product.getDimensions(), product.getPackageContents(), product.getInstructions(), product.getSafetyNotes(),
            product.getMainImageId(), imageIds(product.getImageIds()), valueOrZero(product.getRetailUnitPriceFen()),
            "CNY", inStock, inStock ? "IN_STOCK" : "OUT_OF_STOCK", product.getStatus(), inStock,
            inStock ? "现货可售" : "暂时缺货，可稍后再来看看。", product.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryView> publicCategories() {
        return categories.findByEnabledTrueOrderBySortOrderAscNameAsc().stream().map(this::category).toList();
    }

    public CatalogDtos.ProductOptions productOptions() {
        return new CatalogDtos.ProductOptions(
            List.of(option(PlayType.BALANCE, "平衡能力"), option(PlayType.COORDINATION, "协调训练"),
                option(PlayType.THROWING, "投掷与瞄准"), option(PlayType.TEAM_PLAY, "团队游戏"),
                option(PlayType.OUTDOOR_EXPLORATION, "户外探索")),
            List.of(option(ProductScene.INDOOR, "室内"), option(ProductScene.OUTDOOR, "户外"),
                option(ProductScene.BOTH, "室内与户外")));
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.AdminProductView> adminProducts(String keyword, ProductStatus status,
                                                             UUID categoryId, Pageable pageable) {
        String term = CatalogRules.trimToNull(keyword);
        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (term != null) {
                String like = "%" + term.toLowerCase(Locale.ROOT) + "%";
                filters.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("sku")), like)));
            }
            if (status != null) filters.add(cb.equal(root.get("status"), status));
            if (categoryId != null) filters.add(cb.equal(root.get("category").get("id"), categoryId));
            return cb.and(filters.toArray(Predicate[]::new));
        };
        return products.findAll(spec, pageable).map(this::adminView);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.AdminProductView adminProduct(UUID id) { return adminView(requireProduct(id)); }

    @Transactional
    public CommandResult<CatalogDtos.AdminProductView> createProduct(UUID actorId, UUID key,
                                                                      CatalogDtos.CreateProductRequest request) {
        CatalogRules.validateDraft(request);
        String hash = hash(request);
        Optional<CatalogDtos.AdminProductView> replay = replay(actorId, "catalog.createProduct", key, hash,
            CatalogDtos.AdminProductView.class);
        if (replay.isPresent()) return new CommandResult<>(replay.get(), true);
        String sku = CatalogRules.trimToNull(request.sku());
        if (sku != null && products.existsBySku(sku)) unique("sku", "SKU 已存在，请使用其他编号。");
        Instant now = clock.instant();
        ProductEntity product = ProductEntity.draft(now);
        CategoryEntity category = request.categoryId() == null ? null : requireCategory(request.categoryId());
        apply(product, request, category, now);
        products.saveAndFlush(product);
        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        InventoryBalanceEntity balance = balances.save(new InventoryBalanceEntity(product.getId(), initialStock, now));
        movements.save(new InventoryMovementEntity(product.getId(), StockDirection.INCREASE, initialStock,
            0, initialStock, "商品建档初始库存", "INITIAL", product.getId().toString(), actorId, now));
        audit.append(new AuditPort.AuditEvent(actorId, "PRODUCT_CREATED", "PRODUCT", product.getId(),
            "SUCCESS", null, now));
        CatalogDtos.AdminProductView result = adminView(product, balance);
        saveReplay(actorId, "catalog.createProduct", key, hash, result, 201, now);
        return new CommandResult<>(result, false);
    }

    @Transactional
    public CatalogDtos.AdminProductView updateProduct(UUID actorId, UUID id,
                                                       CatalogDtos.UpdateProductRequest request) {
        CatalogRules.validateDraft(request);
        ProductEntity product = products.findForUpdateById(id).orElseThrow(this::notFoundException);
        if (product.getVersion() != request.expectedVersion()) versionConflict();
        String sku = CatalogRules.trimToNull(request.sku());
        if (product.getSku() != null && !Objects.equals(product.getSku(), sku)) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU_IMMUTABLE", "SKU 首次赋值后不可修改。");
        }
        if (sku != null && products.existsBySkuAndIdNot(sku, id)) unique("sku", "SKU 已存在，请使用其他编号。");
        CategoryEntity category = request.categoryId() == null ? null : requireCategory(request.categoryId());
        Instant now = clock.instant();
        apply(product, request, category, now);
        if (product.getStatus() == ProductStatus.PUBLISHED) ensurePublishable(product, balance(id));
        products.flush();
        audit.append(new AuditPort.AuditEvent(actorId, "PRODUCT_UPDATED", "PRODUCT", id,
            "SUCCESS", null, now));
        return adminView(product);
    }

    @Transactional
    public CommandResult<CatalogDtos.AdminProductView> changePublication(UUID actorId, UUID id, UUID key,
                                                                          CatalogDtos.VersionCommand request,
                                                                          boolean publish) {
        String operation = publish ? "catalog.publishProduct:" + id : "catalog.unpublishProduct:" + id;
        String hash = hash(request);
        Optional<CatalogDtos.AdminProductView> replay = replay(actorId, operation, key, hash,
            CatalogDtos.AdminProductView.class);
        if (replay.isPresent()) return new CommandResult<>(replay.get(), true);
        ProductEntity product = products.findForUpdateById(id).orElseThrow(this::notFoundException);
        if (product.getVersion() != request.expectedVersion()) versionConflict();
        if (publish && product.getStatus() == ProductStatus.PUBLISHED || !publish && product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "STATE_CONFLICT", "商品已处于目标状态。");
        }
        Instant now = clock.instant();
        if (publish) {
            ensurePublishable(product, balance(id));
            product.publish(now);
        } else product.unpublish(now);
        products.flush();
        String action = publish ? "PRODUCT_PUBLISHED" : "PRODUCT_UNPUBLISHED";
        audit.append(new AuditPort.AuditEvent(actorId, action, "PRODUCT", id, "SUCCESS", null, now));
        CatalogDtos.AdminProductView result = adminView(product);
        saveReplay(actorId, operation, key, hash, result, 200, now);
        return new CommandResult<>(result, false);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryView> adminCategories() {
        return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(this::category).toList();
    }

    @Transactional
    public CommandResult<CatalogDtos.CategoryView> createCategory(UUID actorId, UUID key,
                                                                   CatalogDtos.CategoryCreateRequest request) {
        validateCategory(request.name(), request.description());
        String hash = hash(request);
        Optional<CatalogDtos.CategoryView> replay = replay(actorId, "catalog.createCategory", key, hash,
            CatalogDtos.CategoryView.class);
        if (replay.isPresent()) return new CommandResult<>(replay.get(), true);
        String name = request.name().strip();
        String normalized = normalizeName(name);
        if (categories.existsByNameNormalized(normalized)) unique("name", "分类名称已存在。");
        Instant now = clock.instant();
        CategoryEntity entity = categories.save(new CategoryEntityBuilder().create(name, normalized,
            CatalogRules.trimToNull(request.description()), request.sortOrder(), request.enabled(), now));
        CatalogDtos.CategoryView result = category(entity);
        audit.append(new AuditPort.AuditEvent(actorId, "CATEGORY_CREATED", "CATEGORY", entity.getId(),
            "SUCCESS", null, now));
        saveReplay(actorId, "catalog.createCategory", key, hash, result, 201, now);
        return new CommandResult<>(result, false);
    }

    @Transactional
    public CatalogDtos.CategoryView updateCategory(UUID actorId, UUID id, CatalogDtos.CategoryUpdateRequest request) {
        validateCategory(request.name(), request.description());
        CategoryEntity entity = requireCategory(id);
        if (entity.getVersion() != request.expectedVersion()) versionConflict();
        if (!request.enabled() && entity.isEnabled() && products.countByCategoryId(id) > 0) inUse();
        String name = request.name().strip();
        String normalized = normalizeName(name);
        if (categories.existsByNameNormalizedAndIdNot(normalized, id)) unique("name", "分类名称已存在。");
        entity.update(name, normalized, CatalogRules.trimToNull(request.description()), request.sortOrder(),
            request.enabled(), clock.instant());
        audit.append(new AuditPort.AuditEvent(actorId, "CATEGORY_UPDATED", "CATEGORY", id,
            "SUCCESS", null, clock.instant()));
        return category(entity);
    }

    @Transactional
    public void deleteCategory(UUID actorId, UUID id, long expectedVersion) {
        CategoryEntity entity = requireCategory(id);
        if (entity.getVersion() != expectedVersion) versionConflict();
        if (products.countByCategoryId(id) > 0) inUse();
        categories.delete(entity);
        audit.append(new AuditPort.AuditEvent(actorId, "CATEGORY_DELETED", "CATEGORY", id,
            "SUCCESS", null, clock.instant()));
    }

    private CatalogDtos.ProductCard card(ProductEntity product) {
        int stock = balance(product.getId()).getQuantity();
        return new CatalogDtos.ProductCard(product.getId(), product.getSku(), product.getName(), product.getSummary(),
            category(product.getCategory()), product.getAgeMin(), product.getAgeMax(), product.getPlayType(),
            product.getScene(), product.getMainImageId(), valueOrZero(product.getRetailUnitPriceFen()), "CNY",
            stock > 0, stock > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
    }

    private CatalogDtos.AdminProductView adminView(ProductEntity product) { return adminView(product, balance(product.getId())); }

    private CatalogDtos.AdminProductView adminView(ProductEntity product, InventoryBalanceEntity balance) {
        return new CatalogDtos.AdminProductView(product.getId(), product.getSku(), product.getName(), product.getSummary(),
            product.getDescription(), product.getCategory() == null ? null : product.getCategory().getId(),
            product.getCategory() == null ? null : product.getCategory().getName(), product.getAgeMin(), product.getAgeMax(),
            product.getPlayType(), product.getScene(), product.getMaterial(), product.getDimensions(),
            product.getPackageContents(), product.getInstructions(), product.getSafetyNotes(), product.getMainImageId(),
            imageIds(product.getImageIds()), product.getRetailUnitPriceFen(), product.isDealerEnabled(),
            product.getDealerReferenceUnitPriceFen(), product.getMinInquiryQuantity(), product.getLeadTimeText(),
            product.getStatus(), product.getDisplayOrder(), balance.getQuantity(), balance.getVersion(),
            product.getVersion(), product.getCreatedAt(), product.getUpdatedAt());
    }

    private void apply(ProductEntity product, CatalogDtos.CreateProductRequest request,
                       CategoryEntity category, Instant now) {
        product.apply(CatalogRules.trimToNull(request.sku()), CatalogRules.trimToNull(request.name()), category,
            CatalogRules.trimToNull(request.summary()), CatalogRules.trimToNull(request.description()), request.ageMin(),
            request.ageMax(), request.playType(), request.scene(), CatalogRules.trimToNull(request.material()),
            CatalogRules.trimToNull(request.dimensions()), CatalogRules.trimToNull(request.packageContents()),
            CatalogRules.trimToNull(request.instructions()), CatalogRules.trimToNull(request.safetyNotes()),
            CatalogRules.trimToNull(request.mainImageId()), imageIdsText(request.imageIds()), request.retailUnitPriceFen(),
            Boolean.TRUE.equals(request.dealerEnabled()), request.dealerReferenceUnitPriceFen(), request.minInquiryQuantity(),
            CatalogRules.trimToNull(request.leadTimeText()), request.displayOrder() == null ? 0 : request.displayOrder(), now);
    }

    private void apply(ProductEntity product, CatalogDtos.UpdateProductRequest request,
                       CategoryEntity category, Instant now) {
        product.apply(CatalogRules.trimToNull(request.sku()), CatalogRules.trimToNull(request.name()), category,
            CatalogRules.trimToNull(request.summary()), CatalogRules.trimToNull(request.description()), request.ageMin(),
            request.ageMax(), request.playType(), request.scene(), CatalogRules.trimToNull(request.material()),
            CatalogRules.trimToNull(request.dimensions()), CatalogRules.trimToNull(request.packageContents()),
            CatalogRules.trimToNull(request.instructions()), CatalogRules.trimToNull(request.safetyNotes()),
            CatalogRules.trimToNull(request.mainImageId()), imageIdsText(request.imageIds()), request.retailUnitPriceFen(),
            Boolean.TRUE.equals(request.dealerEnabled()), request.dealerReferenceUnitPriceFen(), request.minInquiryQuantity(),
            CatalogRules.trimToNull(request.leadTimeText()), request.displayOrder() == null ? 0 : request.displayOrder(), now);
    }

    private void ensurePublishable(ProductEntity product, InventoryBalanceEntity balance) {
        CatalogRules.requirePublishable(product.getSku(), product.getName(), product.getCategory(), product.getSummary(),
            product.getAgeMin(), product.getPlayType(), product.getScene(), product.getMaterial(), product.getDimensions(),
            product.getPackageContents(), product.getInstructions(), product.getSafetyNotes(), product.getMainImageId(),
            product.getRetailUnitPriceFen(), product.isDealerEnabled(), product.getDealerReferenceUnitPriceFen(),
            product.getMinInquiryQuantity(), product.getLeadTimeText());
        if (!product.getCategory().isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_DISABLED", "商品分类已停用，不能发布。");
        }
        if (balance.getQuantity() < 0) throw new IllegalStateException("negative stock invariant violated");
    }

    private InventoryBalanceEntity balance(UUID productId) {
        return balances.findById(productId).orElseThrow(() ->
            new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVENTORY_NOT_INITIALIZED", "商品库存记录缺失。"));
    }

    private ProductEntity requireProduct(UUID id) {
        return products.findDetailedById(id).orElseThrow(this::notFoundException);
    }

    private CategoryEntity requireCategory(UUID id) {
        return categories.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "分类不存在。",
                List.of(new ApiException.FieldViolation("categoryId", "RESOURCE_NOT_FOUND", "请选择有效分类。"))));
    }

    private CatalogDtos.CategoryView category(CategoryEntity entity) {
        if (entity == null) return null;
        return new CatalogDtos.CategoryView(entity.getId(), entity.getName(), entity.getDescription(),
            entity.getSortOrder(), entity.isEnabled(), entity.getVersion());
    }

    private CatalogDtos.Option option(Enum<?> value, String label) { return new CatalogDtos.Option(value.name(), label); }
    private long valueOrZero(Long value) { return value == null ? 0 : value; }
    private String normalizeName(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private String imageIdsText(List<String> ids) { return ids == null || ids.isEmpty() ? null : String.join(",", ids); }
    private List<String> imageIds(String value) { return value == null || value.isBlank() ? List.of() : List.of(value.split(",")); }

    private void validateCategory(String name, String description) {
        int count = name.strip().codePointCount(0, name.strip().length());
        if (count < 2 || count > 100) validation("name", "分类名称需为 2—100 个字符。");
        if (description != null && description.strip().codePointCount(0, description.strip().length()) > 500) {
            validation("description", "分类说明最多 500 个字符。");
        }
    }

    private void validation(String field, String message) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。",
            List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", message)));
    }
    private void unique(String field, String message) {
        throw new ApiException(HttpStatus.CONFLICT, "UNIQUE_CONFLICT", message,
            List.of(new ApiException.FieldViolation(field, "UNIQUE_CONFLICT", message)));
    }
    private void versionConflict() { throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "记录已更新，请刷新后重试。"); }
    private void inUse() { throw new ApiException(HttpStatus.CONFLICT, "RESOURCE_IN_USE", "分类正在被商品引用，请先迁移商品。"); }
    private void notFound() { throw notFoundException(); }
    private ApiException notFoundException() { return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。"); }

    private String hash(Object input) {
        try {
            String json = mapper.writeValueAsString(input);
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private <T> Optional<T> replay(UUID actorId, String operation, UUID key, String hash, Class<T> type) {
        Optional<IdempotencyRecordEntity> existing = idempotency
            .findByActorIdAndOperationIdAndIdempotencyKey(actorId, operation, key);
        if (existing.isEmpty()) return Optional.empty();
        if (!existing.get().getRequestHash().equals(hash)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "同一请求标识不能用于不同内容。");
        }
        try { return Optional.of(mapper.readValue(existing.get().getResponseJson(), type)); }
        catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_DATA_INVALID", "已保存的请求结果无法读取。");
        }
    }

    private void saveReplay(UUID actorId, String operation, UUID key, String hash, Object result,
                            int status, Instant now) {
        try {
            idempotency.save(new IdempotencyRecordEntity(actorId, operation, key, hash,
                mapper.writeValueAsString(result), status, now));
        } catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }

    public record CommandResult<T>(T value, boolean replayed) {}

    private static final class CategoryEntityBuilder {
        CategoryEntity create(String name, String normalized, String description,
                              int sortOrder, boolean enabled, Instant now) {
            return CategoryEntity.create(name, normalized, description, sortOrder, enabled, now);
        }
    }
}
