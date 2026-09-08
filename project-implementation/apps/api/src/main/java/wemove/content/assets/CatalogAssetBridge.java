package wemove.content.assets;

import org.springframework.stereotype.Component;
import java.util.*;

/** Compatibility for pre-media-service records. New arbitrary strings are never accepted. */
@Component
public class CatalogAssetBridge {
    private static final Set<String> BUNDLED_IMAGES=Set.of("product-balance-stones","product-rainbow-arch","product-ring-toss","product-team-board","product-forest-kit","product-skip-rope");
    private final ContentAssetsPort assets;
    public CatalogAssetBridge(ContentAssetsPort assets) { this.assets=assets; }
    public void replace(UUID productId,Collection<String> values,Collection<String> previous,boolean published) {
        Set<String> existing=new HashSet<>(previous);
        List<UUID> ids=new ArrayList<>();
        for(String value:values) {
            if(value==null||value.isBlank()) continue;
            try { ids.add(UUID.fromString(value)); }
            catch(IllegalArgumentException error) {
                if(!BUNDLED_IMAGES.contains(value)&&!existing.contains(value))
                    throw AssetRules.invalid("mainImageId","请使用素材库上传后取得的图片 ID。");
            }
        }
        assets.replaceReferences("PRODUCT",productId,ids.stream().distinct().toList(),published);
    }
}
