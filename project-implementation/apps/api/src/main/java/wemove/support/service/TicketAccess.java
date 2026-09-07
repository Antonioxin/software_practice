package wemove.support.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import wemove.identity.domain.BaseRole;
import wemove.platform.ActorContext;
import wemove.platform.IdentityPort;
import wemove.platform.api.ApiException;
import wemove.support.domain.SupportTicketEntity;
import wemove.support.domain.TicketRules;

import java.util.UUID;

/** 工单的角色与归属检查；管理员不发起个人咨询，用户只能访问本人工单。 */
@Component
public class TicketAccess {
    private final IdentityPort identity;

    public TicketAccess(IdentityPort identity) {
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

    public static void owned(SupportTicketEntity ticket, UUID actor, boolean admin) {
        if (!admin && !ticket.actorId.equals(actor)) throw TicketRules.notFound();
    }
}
