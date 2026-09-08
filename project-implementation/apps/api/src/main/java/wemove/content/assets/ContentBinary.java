package wemove.content.assets;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="content_binaries")
public class ContentBinary {
    @Id public UUID id;
    @Lob @Column(nullable=false,columnDefinition="LONGBLOB") public byte[] bytes;
    protected ContentBinary() {}
    ContentBinary(UUID id, byte[] bytes) { this.id=id; this.bytes=bytes; }
}
