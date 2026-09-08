package wemove.content.assets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public final class AssetDtos {
    private AssetDtos() {}
    public record FileMetadata(String title, String type, String versionNote, List<UUID> productIds, String visibility) {}
    public record FileUpdate(String title, String type, String versionNote, List<UUID> productIds, String visibility, Long expectedVersion) {
        public FileMetadata metadata() { return new FileMetadata(title,type,versionNote,productIds,visibility); }
    }
    public record VersionCommand(Long expectedVersion) {}
    public record FileView(UUID id, String title, String type, String versionNote, List<UUID> productIds,
            String visibility, String status, long version, UUID downloadId, String downloadUrl,
            String filename, String mimeType, long sizeBytes, Instant updatedAt, Instant createdAt) {}
    public record MediaView(UUID id, String url, String altText, String mimeType, int width, int height,
            String filename, long sizeBytes, long version, long referenceCount, long publicReferenceCount, Instant createdAt) {}
    public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {}
    public record Page<T>(List<T> items, PageMeta meta) {}
    public record Download(byte[] bytes, String filename, String mimeType) {}
}
