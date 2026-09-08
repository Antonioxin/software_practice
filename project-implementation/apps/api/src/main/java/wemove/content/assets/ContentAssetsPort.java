package wemove.content.assets;
import java.util.Collection;
import java.util.UUID;
/** References participate in the caller's transaction; all references protect against deletion. */
public interface ContentAssetsPort {
    void replaceReferences(String sourceType, UUID sourceId, Collection<UUID> ids, boolean active);
}
