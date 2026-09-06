package wemove.commerce.platform;

import wemove.platform.ActorContext;

import java.util.UUID;

public interface OrdersPort {
    Reference requireOwnedReference(ActorContext actor, UUID orderId);

    record Reference(UUID id, String orderNumber, String status) {}
}
