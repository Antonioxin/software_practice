package com.wemove.identity.catalog.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_categories")
public class CategoryEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "name_normalized", nullable = false, unique = true, length = 100)
    private String nameNormalized;
    @Column(length = 500)
    private String description;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(nullable = false)
    private boolean enabled;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CategoryEntity() {}

    public static CategoryEntity create(String name, String normalizedName, String description,
                                        int sortOrder, boolean enabled, Instant now) {
        CategoryEntity category = new CategoryEntity();
        category.id = UUID.randomUUID();
        category.name = name;
        category.nameNormalized = normalizedName;
        category.description = description;
        category.sortOrder = sortOrder;
        category.enabled = enabled;
        category.createdAt = now;
        category.updatedAt = now;
        return category;
    }

    public void update(String name, String normalizedName, String description,
                       int sortOrder, boolean enabled, Instant now) {
        this.name = name;
        this.nameNormalized = normalizedName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getNameNormalized() { return nameNormalized; }
    public String getDescription() { return description; }
    public int getSortOrder() { return sortOrder; }
    public boolean isEnabled() { return enabled; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
