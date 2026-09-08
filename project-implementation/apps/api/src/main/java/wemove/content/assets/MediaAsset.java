package wemove.content.assets;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="content_assets")
public class MediaAsset {
    @Id public UUID id;
    @Column(nullable=false,length=200) public String altText;
    @Column(nullable=false,length=200) public String filename;
    @Column(nullable=false,length=32) public String mimeType;
    @Column(nullable=false) public int width;
    @Column(nullable=false) public int height;
    @Column(nullable=false) public long sizeBytes;
    @Column(nullable=false) public long version=1;
    @Column(nullable=false) public Instant createdAt;
}
