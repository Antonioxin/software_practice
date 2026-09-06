package wemove.catalog.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_products")
public class ProductEntity {
    @Id
    private UUID id;
    @Column(unique = true, length = 40)
    private String sku;
    @Column(length = 100)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;
    @Column(length = 200)
    private String summary;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "age_min")
    private Integer ageMin;
    @Column(name = "age_max")
    private Integer ageMax;
    @Enumerated(EnumType.STRING)
    @Column(name = "play_type", length = 40)
    private PlayType playType;
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ProductScene scene;
    @Column(length = 2000)
    private String material;
    @Column(length = 2000)
    private String dimensions;
    @Column(name = "package_contents", length = 2000)
    private String packageContents;
    @Column(length = 2000)
    private String instructions;
    @Column(name = "safety_notes", length = 2000)
    private String safetyNotes;
    @Column(name = "main_image_id", length = 64)
    private String mainImageId;
    @Column(name = "image_ids", columnDefinition = "text")
    private String imageIds;
    @Column(name = "retail_unit_price_fen")
    private Long retailUnitPriceFen;
    @Column(name = "dealer_enabled", nullable = false)
    private boolean dealerEnabled;
    @Column(name = "dealer_reference_unit_price_fen")
    private Long dealerReferenceUnitPriceFen;
    @Column(name = "min_inquiry_quantity")
    private Integer minInquiryQuantity;
    @Column(name = "lead_time_text", length = 500)
    private String leadTimeText;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductStatus status;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {}

    public static ProductEntity draft(Instant now) {
        ProductEntity product = new ProductEntity();
        product.id = UUID.randomUUID();
        product.status = ProductStatus.DRAFT;
        product.dealerEnabled = false;
        product.displayOrder = 0;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }

    public void apply(String sku, String name, CategoryEntity category, String summary, String description,
                      Integer ageMin, Integer ageMax, PlayType playType, ProductScene scene,
                      String material, String dimensions, String packageContents, String instructions,
                      String safetyNotes, String mainImageId, String imageIds, Long retailUnitPriceFen,
                      boolean dealerEnabled, Long dealerReferenceUnitPriceFen, Integer minInquiryQuantity,
                      String leadTimeText, int displayOrder, Instant now) {
        if (this.sku != null && sku != null && !this.sku.equals(sku)) {
            throw new IllegalStateException("SKU_IMMUTABLE");
        }
        if (this.sku == null) this.sku = sku;
        this.name = name;
        this.category = category;
        this.summary = summary;
        this.description = description;
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.playType = playType;
        this.scene = scene;
        this.material = material;
        this.dimensions = dimensions;
        this.packageContents = packageContents;
        this.instructions = instructions;
        this.safetyNotes = safetyNotes;
        this.mainImageId = mainImageId;
        this.imageIds = imageIds;
        this.retailUnitPriceFen = retailUnitPriceFen;
        this.dealerEnabled = dealerEnabled;
        this.dealerReferenceUnitPriceFen = dealerReferenceUnitPriceFen;
        this.minInquiryQuantity = minInquiryQuantity;
        this.leadTimeText = leadTimeText;
        this.displayOrder = displayOrder;
        this.updatedAt = now;
    }

    public void publish(Instant now) { this.status = ProductStatus.PUBLISHED; this.updatedAt = now; }
    public void unpublish(Instant now) { this.status = ProductStatus.UNLISTED; this.updatedAt = now; }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public CategoryEntity getCategory() { return category; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public Integer getAgeMin() { return ageMin; }
    public Integer getAgeMax() { return ageMax; }
    public PlayType getPlayType() { return playType; }
    public ProductScene getScene() { return scene; }
    public String getMaterial() { return material; }
    public String getDimensions() { return dimensions; }
    public String getPackageContents() { return packageContents; }
    public String getInstructions() { return instructions; }
    public String getSafetyNotes() { return safetyNotes; }
    public String getMainImageId() { return mainImageId; }
    public String getImageIds() { return imageIds; }
    public Long getRetailUnitPriceFen() { return retailUnitPriceFen; }
    public boolean isDealerEnabled() { return dealerEnabled; }
    public Long getDealerReferenceUnitPriceFen() { return dealerReferenceUnitPriceFen; }
    public Integer getMinInquiryQuantity() { return minInquiryQuantity; }
    public String getLeadTimeText() { return leadTimeText; }
    public ProductStatus getStatus() { return status; }
    public int getDisplayOrder() { return displayOrder; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
