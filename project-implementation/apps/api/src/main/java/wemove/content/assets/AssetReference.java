package wemove.content.assets;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="content_asset_references",uniqueConstraints=@UniqueConstraint(columnNames={"source_type","source_id","asset_id"}))
public class AssetReference {
    @Id public UUID id;
    @Column(nullable=false,length=32) public String sourceType;
    @Column(nullable=false) public UUID sourceId;
    @Column(nullable=false) public UUID assetId;
    @Column(nullable=false) public boolean active;
}
