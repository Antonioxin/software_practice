package wemove.support.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import wemove.commerce.platform.OrdersPort;
import wemove.platform.ActorContext;
import wemove.platform.RateLimitPort;
import wemove.platform.idempotency.IdempotencyExecutor;
import wemove.support.api.SupportDtos.*;
import wemove.support.domain.*;
import wemove.support.repository.*;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * 工单写命令。创建与用户补充共用 CUSTOMER_CONTACT_WRITES 限流桶（含同键重试）；
 * 全部写命令经 IdempotencyExecutor 与业务/审计同事务提交。
 */
@Service
public class TicketCommandService {
    private final IdempotencyExecutor executor;
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final SupportInternalNoteRepository notes;
    private final TicketQueryService queries;
    private final TicketJournal journal;
    private final TicketAccess access;
    private final RateLimitPort rateLimit;
    private final OrdersPort orders;
    private final JdbcTemplate jdbc;

    public TicketCommandService(
            IdempotencyExecutor executor,
            SupportTicketRepository tickets,
            SupportMessageRepository messages,
            SupportInternalNoteRepository notes,
            TicketQueryService queries,
            TicketJournal journal,
            TicketAccess access,
            RateLimitPort rateLimit,
            OrdersPort orders,
            JdbcTemplate jdbc) {
        this.executor = executor;
        this.tickets = tickets;
        this.messages = messages;
        this.notes = notes;
        this.queries = queries;
        this.journal = journal;
        this.access = access;
        this.rateLimit = rateLimit;
        this.orders = orders;
        this.jdbc = jdbc;
    }

    public IdempotencyExecutor.Result<TicketDetail> create(UUID actor, UUID key, CreateTicketRequest r) {
        rateLimit.consume(actor.toString(), RateLimitPort.Bucket.CUSTOMER_CONTACT_WRITES);
        TicketType type = parseType(r.type());
        String subject = TicketRules.text("subject", r.subject(), 2, 100);
        String body = TicketRules.text("body", r.body(), 10, 2000);
        String phone = TicketRules.phone(r.phone());
        TicketRules.association(type, r.productId(), r.orderId());
        CreateTicketRequest normalized =
                new CreateTicketRequest(type.name(), subject, body, phone, r.productId(), r.orderId());
        return executor.execute(
                actor,
                "support.createTicket",
                key,
                "/api/v1/tickets",
                normalized,
                TicketDetail.class,
                201,
                null,
                () -> {
                    ActorContext current = access.lock(actor, false);
                    if (type == TicketType.PRODUCT) requireProduct(r.productId());
                    if (type == TicketType.AFTER_SALES) orders.requireOwnedReference(current, r.orderId());
                    Instant now = Instant.now();
                    SupportTicketEntity ticket = new SupportTicketEntity();
                    ticket.id = UUID.randomUUID();
                    ticket.ticketNumber = "TK" + ticket.id.toString().replace("-", "").toUpperCase(Locale.ROOT);
                    ticket.actorId = actor;
                    ticket.type = type.name();
                    ticket.status = TicketStatus.NEW.name();
                    ticket.subject = subject;
                    ticket.body = body;
                    ticket.phone = phone;
                    ticket.productId = r.productId();
                    ticket.orderId = r.orderId();
                    ticket.version = 1;
                    ticket.createdAt = now;
                    ticket.updatedAt = now;
                    tickets.saveAndFlush(ticket);
                    journal.append(ticket, actor, "CREATED", null, null);
                    return queries.detail(ticket, false);
                });
    }

    public IdempotencyExecutor.Result<TicketDetail> followUp(
            UUID actor, UUID id, UUID key, FollowUpRequest r) {
        rateLimit.consume(actor.toString(), RateLimitPort.Bucket.CUSTOMER_CONTACT_WRITES);
        String content = TicketRules.text("content", r.content(), 1, 2000);
        FollowUpRequest normalized = new FollowUpRequest(r.expectedVersion(), content);
        return executor.execute(
                actor,
                "support.followUp",
                key,
                "/api/v1/tickets/" + id + "/messages",
                normalized,
                TicketDetail.class,
                200,
                null,
                () -> {
                    access.lock(actor, false);
                    SupportTicketEntity ticket = requireOwnedTicket(actor, id, false);
                    TicketRules.version(ticket.version, r.expectedVersion());
                    TicketRules.requireOpen(TicketStatus.valueOf(ticket.status));
                    String previous = ticket.status;
                    ticket.status = TicketRules.afterFollowUp(TicketStatus.valueOf(previous)).name();
                    appendMessage(ticket, actor, "USER_FOLLOWUP", content);
                    return touchAndJournal(ticket, actor, "FOLLOWED_UP", previous, null, false);
                });
    }

    public IdempotencyExecutor.Result<TicketDetail> closeByUser(
            UUID actor, UUID id, UUID key, UserCloseRequest r) {
        String reason =
                r.reason() == null || r.reason().isBlank()
                        ? "用户确认关闭"
                        : TicketRules.text("reason", r.reason(), 2, 500);
        UserCloseRequest normalized = new UserCloseRequest(r.expectedVersion(), reason);
        return executor.execute(
                actor,
                "support.closeTicket",
                key,
                "/api/v1/tickets/" + id + "/close",
                normalized,
                TicketDetail.class,
                200,
                null,
                () -> {
                    access.lock(actor, false);
                    SupportTicketEntity ticket = requireOwnedTicket(actor, id, false);
                    close(ticket, actor, r.expectedVersion(), reason);
                    return queries.detail(ticket, false);
                });
    }

    public IdempotencyExecutor.Result<TicketDetail> start(UUID admin, UUID id, UUID key, StartRequest r) {
        return executor.execute(
                admin,
                "support.startTicket",
                key,
                "/api/v1/admin/tickets/" + id + "/start",
                r,
                TicketDetail.class,
                200,
                null,
                () -> {
                    access.lock(admin, true);
                    SupportTicketEntity ticket = requireAdminTicket(id);
                    TicketRules.version(ticket.version, r.expectedVersion());
                    TicketRules.requireStartable(TicketStatus.valueOf(ticket.status));
                    String previous = ticket.status;
                    ticket.status = TicketStatus.PROCESSING.name();
                    return touchAndJournal(ticket, admin, "STARTED", previous, null, true);
                });
    }

    public IdempotencyExecutor.Result<TicketDetail> reply(
            UUID admin, UUID id, UUID key, ReplyRequest r) {
        String content = TicketRules.text("content", r.content(), 1, 2000);
        ReplyRequest normalized = new ReplyRequest(r.expectedVersion(), content);
        return executor.execute(
                admin,
                "support.replyTicket",
                key,
                "/api/v1/admin/tickets/" + id + "/replies",
                normalized,
                TicketDetail.class,
                200,
                null,
                () -> {
                    access.lock(admin, true);
                    SupportTicketEntity ticket = requireAdminTicket(id);
                    TicketRules.version(ticket.version, r.expectedVersion());
                    TicketRules.requireOpen(TicketStatus.valueOf(ticket.status));
                    String previous = ticket.status;
                    ticket.status = TicketRules.afterReply(TicketStatus.valueOf(previous)).name();
                    appendMessage(ticket, admin, "PUBLIC_REPLY", content);
                    return touchAndJournal(ticket, admin, "REPLIED", previous, null, true);
                });
    }

    /** 内部备注：不改变状态、不递增版本、不写状态历史；仅追加备注行与审计。 */
    public IdempotencyExecutor.Result<TicketDetail> addNote(UUID admin, UUID id, UUID key, NoteRequest r) {
        String content = TicketRules.text("content", r.content(), 1, 2000);
        NoteRequest normalized = new NoteRequest(content);
        return executor.execute(
                admin,
                "support.addTicketNote",
                key,
                "/api/v1/admin/tickets/" + id + "/internal-notes",
                normalized,
                TicketDetail.class,
                200,
                null,
                () -> {
                    access.lock(admin, true);
                    SupportTicketEntity ticket = requireAdminTicket(id);
                    TicketRules.requireOpen(TicketStatus.valueOf(ticket.status));
                    SupportInternalNoteEntity note = new SupportInternalNoteEntity();
                    note.id = UUID.randomUUID();
                    note.ticketId = ticket.id;
                    note.content = content;
                    note.actorId = admin;
                    note.createdAt = Instant.now();
                    notes.saveAndFlush(note);
                    journal.auditOnly(ticket, admin, "NOTE_ADDED", null);
                    return queries.detail(ticket, true);
                });
    }

    public IdempotencyExecutor.Result<TicketDetail> closeByAdmin(
            UUID admin, UUID id, UUID key, AdminCloseRequest r) {
        String reason = TicketRules.text("reason", r.reason(), 2, 500);
        AdminCloseRequest normalized = new AdminCloseRequest(r.expectedVersion(), reason);
        return executor.execute(
                admin,
                "support.adminCloseTicket",
                key,
                "/api/v1/admin/tickets/" + id + "/close",
                normalized,
                TicketDetail.class,
                200,
                null,
                () -> {
                    access.lock(admin, true);
                    SupportTicketEntity ticket = requireAdminTicket(id);
                    close(ticket, admin, r.expectedVersion(), reason);
                    return queries.detail(ticket, true);
                });
    }

    private void close(SupportTicketEntity ticket, UUID actor, long expectedVersion, String reason) {
        TicketRules.version(ticket.version, expectedVersion);
        TicketRules.requireOpen(TicketStatus.valueOf(ticket.status));
        String previous = ticket.status;
        ticket.status = TicketStatus.CLOSED.name();
        ticket.closedAt = Instant.now();
        ticket.closedBy = actor;
        ticket.closeReason = reason;
        ticket.version = Math.incrementExact(ticket.version);
        ticket.updatedAt = Instant.now();
        tickets.saveAndFlush(ticket);
        journal.append(ticket, actor, "CLOSED", previous, reason);
    }

    private TicketDetail touchAndJournal(
            SupportTicketEntity ticket,
            UUID actor,
            String action,
            String previous,
            String reason,
            boolean admin) {
        ticket.version = Math.incrementExact(ticket.version);
        ticket.updatedAt = Instant.now();
        tickets.saveAndFlush(ticket);
        journal.append(ticket, actor, action, previous, reason);
        return queries.detail(ticket, admin);
    }

    private void appendMessage(
            SupportTicketEntity ticket, UUID actor, String kind, String content) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.id = UUID.randomUUID();
        message.ticketId = ticket.id;
        message.kind = kind;
        message.content = content;
        message.actorId = actor;
        message.createdAt = Instant.now();
        messages.saveAndFlush(message);
    }

    private SupportTicketEntity requireOwnedTicket(UUID actor, UUID id, boolean admin) {
        SupportTicketEntity ticket =
                tickets.findForUpdateById(id).orElseThrow(TicketRules::notFound);
        TicketAccess.owned(ticket, actor, admin);
        return ticket;
    }

    private SupportTicketEntity requireAdminTicket(UUID id) {
        return tickets.findForUpdateById(id).orElseThrow(TicketRules::notFound);
    }

    /**
     * 产品咨询关联校验。TODO(B/F)：B 的 Catalog 公开投影端口（getPublicProducts）尚未提供，
     * 当前接受存在的任意状态商品（含历史下架），待端口落地后按公开/历史口径收紧。
     */
    private void requireProduct(UUID productId) {
        Integer found =
                jdbc.queryForObject(
                        "select count(*) from catalog_products where id = UUID_TO_BIN(?)",
                        Integer.class,
                        productId.toString());
        if (found == null || found == 0) throw TicketRules.notFound();
    }

    private TicketType parseType(String value) {
        if (value == null || value.isBlank())
            throw TicketRules.invalid("type", "请选择咨询类型。");
        try {
            return TicketType.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw TicketRules.invalid("type", "咨询类型不合法。");
        }
    }
}
