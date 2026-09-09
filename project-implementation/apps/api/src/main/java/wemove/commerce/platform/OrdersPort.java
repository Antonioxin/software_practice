package wemove.commerce.platform;

import wemove.platform.ActorContext;

import java.util.UUID;

/**
 * 面向其他模块的订单归属引用端口。
 *
 * <p>验证当前身份对订单的访问权限后，仅返回订单 ID、订单号和状态等跨模块所需的最小引用信息。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
public interface OrdersPort {
    Reference requireOwnedReference(ActorContext actor, UUID orderId);

    record Reference(UUID id, String orderNumber, String status) {}
}
