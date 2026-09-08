package wemove.content;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import wemove.catalog.platform.CatalogPort;

public final class ContentDtos {
    private ContentDtos() {}
    public record ArticleView(UUID id, String title, String summary, String body, String category,
            String pageDescription, List<UUID> productIds, List<UUID> mediaIds, String status,
            int sortOrder, long version, Instant createdAt, Instant updatedAt, Instant publishedAt) {}
    public record FaqView(UUID id, String question, String answer, String category,
            List<UUID> productIds, String status, int sortOrder, long version,
            Instant createdAt, Instant updatedAt, Instant publishedAt) {}
    public record BannerView(UUID id, String title, UUID imageId, String buttonText, String targetUrl,
            int sortOrder, String status, long version, Instant createdAt, Instant updatedAt, Instant publishedAt) {}
    public record ArticleRequest(Long expectedVersion, String title,
            @Size(max=500) String summary, @Size(max=200000) String body,
            @Size(max=80) String category, @Size(max=300) String pageDescription,
            @Size(max=20) List<@NotNull UUID> productIds, @Size(max=20) List<@NotNull UUID> mediaIds,
            @PositiveOrZero Integer sortOrder) {}
    public record FaqRequest(Long expectedVersion, String question,
            @Size(max=60000) String answer, @Size(max=160) String category,
            @Size(max=20) List<@NotNull UUID> productIds, @PositiveOrZero Integer sortOrder) {}
    public record BannerRequest(Long expectedVersion, String title,
            UUID imageId, @Size(max=50) String buttonText, String targetUrl,
            @PositiveOrZero Integer sortOrder) {}
    public record VersionCommand(@NotNull @PositiveOrZero Long expectedVersion) {}
    public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {}
    public record SettingsView(String brandName, String brandDescription, String contactEmail,
            String contactPhone, String contactAddress, UUID logoMediaId, String termsText,
            String termsVersion, String privacyText, String privacyVersion, long version) {}
    public record SettingsRequest(@NotNull @PositiveOrZero Long expectedVersion,
            @NotBlank @Size(max=100) String brandName, @Size(max=20000) String brandDescription,
            @Email @Size(max=250) String contactEmail, @Size(max=50) String contactPhone,
            @Size(max=250) String contactAddress, UUID logoMediaId,
            @NotBlank @Size(max=50000) String termsText, @NotBlank @Size(max=32) String termsVersion,
            @NotBlank @Size(max=50000) String privacyText, @NotBlank @Size(max=32) String privacyVersion) {}
    public record HomeAdminView(List<UUID> recommendedProductIds, List<UUID> featuredArticleIds, long version) {}
    public record HomeRequest(@NotNull @PositiveOrZero Long expectedVersion,
            @NotNull @Size(max=12) List<@NotNull UUID> recommendedProductIds,
            @NotNull @Size(max=6) List<@NotNull UUID> featuredArticleIds) {}
    public record HomeView(List<BannerView> banners,
            List<CatalogPort.PublicProductProjection> recommendedProducts, List<ArticleView> featuredArticles) {}
}
