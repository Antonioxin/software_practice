package wemove.catalog.api;

import wemove.platform.api.ApiException;
import wemove.platform.api.ApiEnvelope;
import wemove.catalog.domain.*;
import wemove.catalog.service.CatalogService;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PublicCatalogController {
    private final CatalogService catalog;
    public PublicCatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/products")
    public ResponseEntity<ApiEnvelope<List<CatalogDtos.ProductCard>>> products(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) Integer age,
        @RequestParam(required = false) String playType,
        @RequestParam(required = false) String scene,
        @RequestParam(defaultValue = "recommended") String sort,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int pageSize) {
        validatePage(page, pageSize);
        if (keyword != null && keyword.strip().codePointCount(0, keyword.strip().length()) > 100) {
            throw validation("keyword", "关键词最多 100 个字符。");
        }
        if (age != null && (age < 0 || age > 18)) throw validation("age", "年龄需为 0—18 的整数。");
        PlayType parsedPlayType = parse(playType, PlayType.class, "playType");
        ProductScene parsedScene = parse(scene, ProductScene.class, "scene");
        Sort ordering = switch (sort) {
            case "recommended" -> Sort.by("displayOrder").ascending().and(Sort.by("id").ascending());
            case "priceAsc" -> Sort.by("retailUnitPriceFen").ascending().and(Sort.by("id").ascending());
            case "priceDesc" -> Sort.by("retailUnitPriceFen").descending().and(Sort.by("id").ascending());
            default -> throw validation("sort", "排序参数无效。");
        };
        Page<CatalogDtos.ProductCard> result = catalog.publicProducts(keyword, categoryId, age,
            parsedPlayType, parsedScene, PageRequest.of(page - 1, pageSize, ordering));
        CatalogDtos.PageMeta meta = new CatalogDtos.PageMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiEnvelope.page(result.getContent(), meta));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiEnvelope<CatalogDtos.PublicProductDetail>> product(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(catalog.publicDetail(id)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiEnvelope<List<CatalogDtos.CategoryView>>> categories() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(catalog.publicCategories()));
    }

    @GetMapping("/product-options")
    public ResponseEntity<ApiEnvelope<CatalogDtos.ProductOptions>> options() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(catalog.productOptions()));
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
}
