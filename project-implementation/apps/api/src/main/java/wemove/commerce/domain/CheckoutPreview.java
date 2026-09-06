package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "CommerceCheckoutPreview")
@Table(name = "commerce_checkout_previews")
public class CheckoutPreview {
    @Id public UUID id;
    public String tokenHash;
    public UUID userId;
    public UUID cartId;
    public long cartVersion;

    @Column(columnDefinition = "text")
    public String snapshotJson;

    public Instant createdAt;
    public Instant expiresAt;
}
