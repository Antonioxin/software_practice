package wemove.content.assets;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="content_files")
public class DownloadFile {
    @Id public UUID id;
    @Column(nullable=false,length=100) public String title;
    @Column(nullable=false,length=50) public String type;
    @Column(nullable=false,length=500) public String versionNote;
    @Column(nullable=false,length=800) public String productIds="";
    @Column(nullable=false,length=16) public String visibility;
    @Column(nullable=false,length=16) public String status="DRAFT";
    @Column(nullable=false) public long version=1;
    @Column(nullable=false) public UUID downloadId;
    @Column(nullable=false,length=200) public String filename;
    @Column(nullable=false) public long sizeBytes;
    @Column(nullable=false) public Instant updatedAt;
    @Column(nullable=false) public Instant createdAt;
}
