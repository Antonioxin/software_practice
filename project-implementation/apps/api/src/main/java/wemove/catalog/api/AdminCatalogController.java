package wemove.catalog.api;

import wemove.platform.api.ApiException;
import wemove.platform.api.ApiEnvelope;
import wemove.catalog.domain.ProductStatus;
import wemove.catalog.service.*;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final IdentityPort identity;
    private final CatalogService catalog;
    private final InventoryService inventory;

    public AdminCatalogController(IdentityPort identity, CatalogService catalog, InventoryService inventory) {
        this.identity = identity;
        this.catalog = catalog;
        this.inventory = inventory;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiEnvelope<List<CatalogDtos.AdminProductView>>> products(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        Authentication authentication) {
        requireAdmin(authentication);
        validatePage(page, pageSize);
        ProductStatus parsedStatus = parse(status, ProductStatus.class, "status");
        Pageable pageable = PageRequest.of(page - 1, pageSize,
            Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by("id")));
        Page<CatalogDtos.AdminProductView> result = catalog.adminProducts(keyword, parsedStatus, categoryId, pageable);
        return ok(ApiEnvelope.page(result.getContent(), new CatalogDtos.PageMeta(page, pageSize,
            result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> product(
        @PathVariable UUID id, Authentication authentication) {
        requireAdmin(authentication);
        return ok(ApiEnvelope.of(catalog.adminProduct(id)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> createProduct(
        @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody CatalogDtos.CreateProductRequest request,
        Authentication authentication) {
        ActorContext actor = requireAdmin(authentication);
        CatalogService.CommandResult<CatalogDtos.AdminProductView> result = catalog.createProduct(actor.actorId(), key, request);
        ResponseEntity.BodyBuilder response = result.replayed()
            ? ResponseEntity.ok() : ResponseEntity.created(URI.create("/api/v1/admin/products/" + result.value().id()));
        if (result.replayed()) response.header("Idempotency-Replayed", "true");
        return response.cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(result.value()));
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> updateProduct(
        @PathVariable UUID id, @Valid @RequestBody CatalogDtos.UpdateProductRequest request,
        Authentication authentication) {
        ActorContext actor = requireAdmin(authentication);
        return ok(ApiEnvelope.of(catalog.updateProduct(actor.actorId(), id, request)));
    }

    @PostMapping("/products/{id}/publish")
    public ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> publish(
        @PathVariable UUID id, @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody CatalogDtos.VersionCommand request, Authentication authentication) {
        return publication(id, key, request, authentication, true);
    }

    @PostMapping("/products/{id}/unpublish")
    public ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> unpublish(
        @PathVariable UUID id, @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody CatalogDtos.VersionCommand request, Authentication authentication) {
        return publication(id, key, request, authentication, false);
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiEnvelope<List<CatalogDtos.CategoryView>>> categories(Authentication authentication) {
        requireAdmin(authentication);
        return ok(ApiEnvelope.of(catalog.adminCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiEnvelope<CatalogDtos.CategoryView>> createCategory(
        @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody CatalogDtos.CategoryCreateRequest request,
        Authentication authentication) {
        ActorContext actor = requireAdmin(authentication);
        CatalogService.CommandResult<CatalogDtos.CategoryView> result = catalog.createCategory(actor.actorId(), key, request);
        ResponseEntity.BodyBuilder response = result.replayed()
            ? ResponseEntity.ok() : ResponseEntity.created(URI.create("/api/v1/admin/categories/" + result.value().id()));
        if (result.replayed()) response.header("Idempotency-Replayed", "true");
        return response.cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(result.value()));
    }

    @PatchMapping("/categories/{id}")
    public ResponseEntity<ApiEnvelope<CatalogDtos.CategoryView>> updateCategory(
        @PathVariable UUID id, @Valid @RequestBody CatalogDtos.CategoryUpdateRequest request,
        Authentication authentication) {
        ActorContext actor = requireAdmin(authentication);
        return ok(ApiEnvelope.of(catalog.updateCategory(actor.actorId(), id, request)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiEnvelope<Map<String, Object>>> deleteCategory(
        @PathVariable UUID id, @RequestParam long expectedVersion, Authentication authentication) {
        ActorContext actor = requireAdmin(authentication);
        catalog.deleteCategory(actor.actorId(), id, expectedVersion);
        return ok(ApiEnvelope.of(Map.of("id", id, "deleted", true)));
    }

    @PostMapping("/products/{id}/stock-adjustments")
    public ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> adjustStock(
        @PathVariable UUID id, @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody CatalogDtos.StockAdjustmentRequest request,
        Authentication authentication) {
        ActorContext actor = requireAdmin(authentication);
        InventoryService.CommandResult<CatalogDtos.AdminProductView> result = inventory.adjust(actor.actorId(), id, key, request);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (result.replayed()) response.header("Idempotency-Replayed", "true");
        return response.body(ApiEnvelope.of(result.value()));
    }

    @GetMapping("/products/{id}/stock-movements")
    public ResponseEntity<ApiEnvelope<List<CatalogDtos.StockMovementView>>> stockMovements(
        @PathVariable UUID id, @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize, Authentication authentication) {
        requireAdmin(authentication);
        validatePage(page, pageSize);
        Page<CatalogDtos.StockMovementView> result = inventory.movements(id, PageRequest.of(page - 1, pageSize,
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return ok(ApiEnvelope.page(result.getContent(), new CatalogDtos.PageMeta(page, pageSize,
            result.getTotalElements(), result.getTotalPages())));
    }

    private ResponseEntity<ApiEnvelope<CatalogDtos.AdminProductView>> publication(
        UUID id, UUID key, CatalogDtos.VersionCommand request, Authentication authentication, boolean publish) {
        ActorContext actor = requireAdmin(authentication);
        CatalogService.CommandResult<CatalogDtos.AdminProductView> result = catalog.changePublication(
            actor.actorId(), id, key, request, publish);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (result.replayed()) response.header("Idempotency-Replayed", "true");
        return response.body(ApiEnvelope.of(result.value()));
    }

    private ActorContext requireAdmin(Authentication authentication) {
        ActorContext actor = identity.requireActiveActor(authentication);
        if (actor.baseRole() != BaseRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "您无权访问商品管理后台。");
        }
        return actor;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50) throw validation("page", "分页参数无效。");
    }
    private <E extends Enum<E>> E parse(String value, Class<E> type, String field) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw validation(field, field + " 参数无效。"); }
    }
    private ApiException validation(String field, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查查询条件。",
            List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", message)));
    }
    private <T> ResponseEntity<ApiEnvelope<T>> ok(ApiEnvelope<T> body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
