package wemove.dealership.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity(name = "DealerInquiryItem")
@Table(name = "dealer_inquiry_items")
public class DealerInquiryItem {
    @Id public UUID id;
    @Column(nullable = false) public UUID inquiryId;
    @Column(nullable = false) public UUID productId;
    @Column(nullable = false, length = 40) public String skuSnapshot;
    @Column(nullable = false, length = 100) public String nameSnapshot;
    @Column(nullable = false) public long referenceUnitPriceFenSnapshot;
    @Column(nullable = false) public int minInquiryQuantitySnapshot;
    @Column(nullable = false) public int quantity;
    public Long replyReferenceUnitPriceFen;
    @Column(length = 500) public String replyLeadTimeText;
}
