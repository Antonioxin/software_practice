package wemove.support.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wemove.support.api.SupportDtos.*;
import wemove.support.domain.*;
import wemove.support.repository.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 工单只读查询；用户视图不含内部备注，管理员视图含备注与历史。 */
@Service
public class TicketQueryService {
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final SupportInternalNoteRepository notes;
    private final SupportTicketHistoryRepository history;
    private final JdbcTemplate jdbc;

    public TicketQueryService(
            SupportTicketRepository tickets,
            SupportMessageRepository messages,
            SupportInternalNoteRepository notes,
            SupportTicketHistoryRepository history,
            JdbcTemplate jdbc) {
        this.tickets = tickets;
        this.messages = messages;
        this.notes = notes;
        this.history = history;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Page<TicketSummary> list(
            UUID actorId,
            TicketType type,
            TicketStatus status,
            Instant from,
            Instant to,
            Pageable pageable) {
        Specification<SupportTicketEntity> spec =
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (actorId != null) predicates.add(cb.equal(root.get("actorId"), actorId));
                    if (type != null) predicates.add(cb.equal(root.get("type"), type.name()));
                    if (status != null) predicates.add(cb.equal(root.get("status"), status.name()));
                    if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                    if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to));
                    return cb.and(predicates.toArray(Predicate[]::new));
                };
        return tickets.findAll(spec, pageable).map(this::summary);
    }

    /** 用户按 id 读取本人工单；他人/不存在统一 404，不回显归属。 */
    @Transactional(readOnly = true)
    public TicketDetail ownedDetail(UUID actorId, UUID ticketId) {
        SupportTicketEntity ticket =
                tickets.findById(ticketId).orElseThrow(TicketRules::notFound);
        if (!ticket.actorId.equals(actorId)) throw TicketRules.notFound();
        return detail(ticket, false);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<SupportTicketEntity> ticket(UUID ticketId) {
        return tickets.findById(ticketId);
    }

    @Transactional(readOnly = true)
    public TicketDetail detail(SupportTicketEntity ticket, boolean admin) {
        List<MessageView> conversation =
                messages.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.id).stream()
                        .map(this::message)
                        .toList();
        List<NoteView> internalNotes =
                admin
                        ? notes.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.id).stream()
                                .map(this::note)
                                .toList()
                        : List.of();
        List<HistoryView> timeline =
                admin
                        ? history.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.id).stream()
                                .map(this::historyRow)
                                .toList()
                        : List.of();
        return new TicketDetail(
                ticket.id,
                ticket.ticketNumber,
                ticket.type,
                ticket.status,
                ticket.subject,
                ticket.body,
                ticket.phone,
                product(ticket.productId),
                order(ticket.orderId),
                ticket.version,
                ticket.createdAt,
                ticket.updatedAt,
                ticket.closedAt,
                ticket.closedBy,
                ticket.closeReason,
                conversation,
                internalNotes,
                timeline,
                admin
                        ? TicketRules.adminActions(TicketStatus.valueOf(ticket.status))
                        : TicketRules.userActions(TicketStatus.valueOf(ticket.status)));
    }

    /** 关联信息只做展示投影，读取本人订单归属已在提交时经 OrdersPort 校验。 */
    private ProductReference product(UUID productId) {
        if (productId == null) return null;
        List<ProductReference> found =
                jdbc.query(
                        "SELECT BIN_TO_UUID(id) id, sku, name FROM catalog_products WHERE id = UUID_TO_BIN(?)",
                        (rs, i) ->
                                new ProductReference(
                                        UUID.fromString(rs.getString("id")),
                                        rs.getString("sku"),
                                        rs.getString("name")),
                        productId.toString());
        return found.isEmpty() ? null : found.get(0);
    }

    private OrderReference order(UUID orderId) {
        if (orderId == null) return null;
        List<OrderReference> found =
                jdbc.query(
                        "SELECT BIN_TO_UUID(id) id, order_number, status FROM commerce_orders WHERE id = UUID_TO_BIN(?)",
                        (rs, i) ->
                                new OrderReference(
                                        UUID.fromString(rs.getString("id")),
                                        rs.getString("order_number"),
                                        rs.getString("status")),
                        orderId.toString());
        return found.isEmpty() ? null : found.get(0);
    }

    private TicketSummary summary(SupportTicketEntity ticket) {
        return new TicketSummary(
                ticket.id,
                ticket.ticketNumber,
                ticket.type,
                ticket.status,
                ticket.subject,
                ticket.createdAt,
                ticket.updatedAt,
                ticket.version);
    }

    private MessageView message(SupportMessageEntity row) {
        return new MessageView(row.id, row.kind, row.content, row.actorId, row.createdAt);
    }

    private NoteView note(SupportInternalNoteEntity row) {
        return new NoteView(row.id, row.content, row.actorId, row.createdAt);
    }

    private HistoryView historyRow(SupportTicketHistoryEntity row) {
        return new HistoryView(
                row.id,
                row.action,
                row.fromStatus,
                row.toStatus,
                row.ticketVersion,
                row.actorId,
                row.reason,
                row.createdAt);
    }
}
