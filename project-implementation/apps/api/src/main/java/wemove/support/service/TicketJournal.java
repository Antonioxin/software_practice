package wemove.support.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import wemove.platform.AuditPort;
import wemove.platform.TransactionProbe;
import wemove.support.domain.SupportTicketEntity;
import wemove.support.domain.SupportTicketHistoryEntity;
import wemove.support.repository.SupportTicketHistoryRepository;

import java.time.Instant;
import java.util.UUID;

/** 工单状态历史与关键动作审计，与业务同事务提交（失败随业务回滚）。 */
@Component
public class TicketJournal {
    @org.springframework.beans.factory.annotation.Autowired
    private TransactionProbe probe;

    private final SupportTicketHistoryRepository history;
    private final AuditPort audit;

    public TicketJournal(SupportTicketHistoryRepository history, AuditPort audit) {
        this.history = history;
        this.audit = audit;
    }

    public void append(
            SupportTicketEntity ticket, UUID actor, String action, String previous, String reason) {
        String requestId =
                RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                        ? (String) attrs.getRequest().getAttribute("requestId")
                        : null;
        SupportTicketHistoryEntity row = new SupportTicketHistoryEntity();
        row.id = UUID.randomUUID();
        row.ticketId = ticket.id;
        row.action = action;
        row.fromStatus = previous;
        row.toStatus = ticket.status;
        row.ticketVersion = ticket.version;
        row.actorId = actor;
        row.reason = reason;
        row.requestId = requestId;
        row.createdAt = Instant.now();
        history.save(row);
        history.flush();
        probe.hit("ticket.history.created");
        audit.append(
                new AuditPort.AuditEvent(
                        actor,
                        "TICKET_" + action,
                        "TICKET",
                        ticket.id,
                        "SUCCESS",
                        reason,
                        row.createdAt,
                        requestId,
                        "status="
                                + previous
                                + " -> "
                                + ticket.status
                                + "; version="
                                + ticket.version));
        probe.hit("ticket.audit.created");
    }

    /** 仅审计不写状态历史的动作（内部备注不改变工单状态与版本）。 */
    public void auditOnly(SupportTicketEntity ticket, UUID actor, String action, String reason) {
        String requestId =
                RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                        ? (String) attrs.getRequest().getAttribute("requestId")
                        : null;
        Instant occurredAt = Instant.now();
        audit.append(
                new AuditPort.AuditEvent(
                        actor,
                        "TICKET_" + action,
                        "TICKET",
                        ticket.id,
                        "SUCCESS",
                        reason,
                        occurredAt,
                        requestId,
                        "status=" + ticket.status + "; version=" + ticket.version + "; note"));
        probe.hit("ticket.audit.created");
    }
}
