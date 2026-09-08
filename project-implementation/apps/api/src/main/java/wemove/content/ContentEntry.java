package wemove.content;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name="content_entries")
public class ContentEntry {
    @Id UUID id;
    @Column(nullable=false, length=12) String kind;
    @Column(nullable=false, length=250) String title;
    @Column(nullable=false, length=500) String summary = "";
    @Column(nullable=false, columnDefinition="LONGTEXT") String body = "";
    @Column(nullable=false, length=80) String category = "";
    @Column(name="page_description", nullable=false, length=300) String pageDescription = "";
    @Column(name="product_ids", nullable=false, length=1000) String productIds = "";
    @Column(name="media_ids", nullable=false, length=1000) String mediaIds = "";
    @Column(name="image_id") UUID imageId;
    @Column(name="button_text", nullable=false, length=50) String buttonText = "";
    @Column(name="target_url", nullable=false, length=2048) String targetUrl = "";
    @Column(nullable=false, length=12) String status = "DRAFT";
    @Column(name="sort_order", nullable=false) int sortOrder;
    @Version long version;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    @Column(name="published_at") Instant publishedAt;
    protected ContentEntry() {}
    static ContentEntry create(String kind) {
        var entry = new ContentEntry(); entry.id = UUID.randomUUID(); entry.kind = kind;
        entry.createdAt = entry.updatedAt = Instant.now(); return entry;
    }
    boolean published() { return status.equals("PUBLISHED"); }
    static List<UUID> ids(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(UUID::fromString).toList();
    }
    static String pack(Collection<UUID> ids) {
        return ids == null ? "" : ids.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","));
    }
}
