package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.notFound;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import wemove.commerce.domain.Order;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import wemove.platform.api.ApiException;

import java.util.UUID;

@Component
public class CommerceAccess {
    private final IdentityPort identity;

    public CommerceAccess(IdentityPort identity) {
        this.identity = identity;
    }

    public ActorContext lock(UUID actor, boolean admin) {
        ActorContext current = identity.lockActiveActor(actor);
        requireRole(current, admin);
        return current;
    }

    public static void requireRole(ActorContext actor, boolean admin) {
        if ((actor.baseRole() == BaseRole.ADMIN) != admin)
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前身份无权执行此操作。");
    }

    public static void owned(Order order, UUID actor, boolean admin) {
        if (!admin && !order.userId.equals(actor)) throw notFound();
    }
}
