package wemove.commerce.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "CommerceCart")
@Table(name = "commerce_carts")
public class Cart {
    @Id public UUID id;
    public UUID userId;
    public long version = 1;
    public Instant updatedAt;
}
