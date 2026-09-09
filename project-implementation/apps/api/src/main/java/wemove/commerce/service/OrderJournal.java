package wemove.commerce.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import wemove.commerce.domain.*;
import wemove.commerce.repository.CommerceRepository;
import wemove.platform.AuditPort;

import java.time.Instant;
import java.util.UUID;

/**
 * 订单状态历史与审计编排组件。
 *
 * <p>将订单状态变化写入历史表，并在同一事务中发送带有请求标识、版本和状态变化摘要的持久审计事件。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
@Component
public class OrderJournal {
    @org.springframework.beans.factory.annotation.Autowired
    private wemove.platform.TransactionProbe probe;

    private final CommerceRepository db;
    private final AuditPort audit;

    public OrderJournal(CommerceRepository db, AuditPort audit) {
        this.db = db;
        this.audit = audit;
    }

    public void append(Order o, UUID actor, String action, String previous, String reason) {
        String requestId =
                RequestContextHolder.getRequestAttributes()
                                instanceof ServletRequestAttributes attrs
                        ? (String) attrs.getRequest().getAttribute("requestId")
                        : null;
        OrderHistory h = new OrderHistory();
        h.id = UUID.randomUUID();
        h.orderId = o.id;
        h.action = action;
        h.fromStatus = previous;
        h.toStatus = o.status;
        h.orderVersion = o.version;
        h.actorId = actor;
        h.reason = reason;
        h.requestId = requestId;
        h.createdAt = Instant.now();
        db.save(h);
        db.flush();
        probe.hit("history.created");
        audit.append(
                new AuditPort.AuditEvent(
                        actor,
                        "ORDER_" + action,
                        "ORDER",
                        o.id,
                        "SUCCESS",
                        reason,
                        h.createdAt,
                        requestId,
                        "status="
                                + previous
                                + " -> "
                                + o.status
                                + "; version="
                                + o.version
                                + "; mode=SIMULATED"));
        probe.hit("audit.created");
    }
}
