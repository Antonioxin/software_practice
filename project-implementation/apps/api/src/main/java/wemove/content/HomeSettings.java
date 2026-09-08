package wemove.content;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="content_home_settings")
public class HomeSettings {
    static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000002");
    @Id UUID id = ID;
    @Column(name="recommended_product_ids", nullable=false, length=1000) String recommendedProductIds = "";
    @Column(name="featured_article_ids", nullable=false, length=500) String featuredArticleIds = "";
    @Version long version;
    public HomeSettings() {}
}
