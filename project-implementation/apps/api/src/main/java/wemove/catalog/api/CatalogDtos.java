package wemove.catalog.api;

import wemove.catalog.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record CategoryView(UUID id, String name, String description, int sortOrder,
                               boolean enabled, long version) {}

    public record ProductCard(UUID id, String sku, String name, String summary,
                              CategoryView category, Integer ageMin, Integer ageMax,
                              PlayType playType, ProductScene scene, String mainImageId,
                              long retailUnitPriceFen, String currency, boolean inStock,
                              String stockStatus) {}

    public record PublicProductDetail(UUID id, String sku, String name, String summary,
                                      String description, CategoryView category, Integer ageMin,
                                      Integer ageMax, PlayType playType, ProductScene scene,
                                      String material, String dimensions, String packageContents,
                                      String instructions, String safetyNotes, String mainImageId,
                                      List<String> imageIds, long retailUnitPriceFen, String currency,
                                      boolean inStock, String stockStatus, ProductStatus status,
                                      boolean purchasable, String availabilityMessage,
                                      Instant updatedAt) {}

    public record AdminProductView(UUID id, String sku, String name, String summary,
                                   String description, UUID categoryId, String categoryName,
                                   Integer ageMin, Integer ageMax, PlayType playType,
                                   ProductScene scene, String material, String dimensions,
                                   String packageContents, String instructions, String safetyNotes,
                                   String mainImageId, List<String> imageIds,
                                   Long retailUnitPriceFen, boolean dealerEnabled,
                                   Long dealerReferenceUnitPriceFen, Integer minInquiryQuantity,
                                   String leadTimeText, ProductStatus status, int displayOrder,
                                   int stock, long stockVersion, long version,
                                   Instant createdAt, Instant updatedAt) {}

    public record CreateProductRequest(
        String sku, String name, UUID categoryId, String summary, String description,
        Integer ageMin, Integer ageMax, PlayType playType, ProductScene scene,
        String material, String dimensions, String packageContents, String instructions,
        String safetyNotes, String mainImageId, List<String> imageIds,
        Long retailUnitPriceFen, Boolean dealerEnabled, Long dealerReferenceUnitPriceFen,
        Integer minInquiryQuantity, String leadTimeText, Integer displayOrder,
        @PositiveOrZero Integer initialStock) {}

    public record UpdateProductRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        String sku, String name, UUID categoryId, String summary, String description,
        Integer ageMin, Integer ageMax, PlayType playType, ProductScene scene,
        String material, String dimensions, String packageContents, String instructions,
        String safetyNotes, String mainImageId, List<String> imageIds,
        Long retailUnitPriceFen, Boolean dealerEnabled, Long dealerReferenceUnitPriceFen,
        Integer minInquiryQuantity, String leadTimeText, Integer displayOrder) {}

    public record VersionCommand(@NotNull @PositiveOrZero Long expectedVersion) {}

    public record CategoryCreateRequest(
        @NotBlank String name, String description,
        @NotNull @PositiveOrZero Integer sortOrder, @NotNull Boolean enabled) {}

    public record CategoryUpdateRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank String name, String description,
        @NotNull @PositiveOrZero Integer sortOrder, @NotNull Boolean enabled) {}

    public record StockAdjustmentRequest(
        @NotNull StockDirection direction,
        @NotNull @Positive Integer quantity,
        @NotBlank String reason) {}

    public record StockMovementView(UUID id, UUID productId, StockDirection direction,
                                    int quantity, int quantityBefore, int quantityAfter,
                                    String reason, String sourceType, String sourceId,
                                    UUID actorId, Instant createdAt) {}

    public record ProductOptions(List<Option> playTypes, List<Option> scenes) {}
    public record Option(String value, String label) {}
    public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {}
}
