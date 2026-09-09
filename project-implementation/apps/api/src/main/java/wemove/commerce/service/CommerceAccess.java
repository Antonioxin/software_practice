package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.notFound;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import wemove.commerce.domain.Order;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import wemove.platform.api.ApiException;

import java.util.UUID;

/**
 * 交易模块的身份与资源访问辅助组件。
 *
 * <p>锁定当前有效身份并检查普通用户或管理员角色，同时对订单执行本人归属校验；角色不符返回禁止访问，归属不符按资源不存在处理。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
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
