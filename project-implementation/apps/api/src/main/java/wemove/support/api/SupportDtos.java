package wemove.support.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 工单与总览的请求/响应契约；用户响应绝不包含内部备注。 */
public final class SupportDtos {
    private SupportDtos() {}

    public record CreateTicketRequest(
            String type, String subject, String body, String phone, UUID productId, UUID orderId) {}

    public record FollowUpRequest(long expectedVersion, String content) {}

    public record UserCloseRequest(long expectedVersion, String reason) {}

    public record StartRequest(long expectedVersion) {}

    public record ReplyRequest(long expectedVersion, String content) {}

    public record NoteRequest(String content) {}

    public record AdminCloseRequest(long expectedVersion, String reason) {}

    public record ProductReference(UUID id, String sku, String name) {}

    public record OrderReference(UUID id, String orderNumber, String status) {}

    public record MessageView(
            UUID id, String kind, String content, UUID actorId, Instant createdAt) {}

    public record NoteView(UUID id, String content, UUID actorId, Instant createdAt) {}

    public record HistoryView(
            UUID id,
            String action,
            String fromStatus,
            String toStatus,
            long ticketVersion,
            UUID actorId,
            String reason,
            Instant createdAt) {}

    public record TicketSummary(
            UUID id,
            String ticketNumber,
            String type,
            String status,
            String subject,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    public record TicketDetail(
            UUID id,
            String ticketNumber,
            String type,
            String status,
            String subject,
            String body,
            String phone,
            ProductReference product,
            OrderReference order,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt,
            UUID closedBy,
            String closeReason,
            List<MessageView> messages,
            List<NoteView> internalNotes,
            List<HistoryView> history,
            List<String> allowedActions) {}

    public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {}

    /** FR-32 总览快照；净成交金额为整数分字符串，asOf 为各指标共同的读取时点。 */
    public record DashboardSnapshot(
            long publishedProductCount,
            long activeUserCount,
            long pendingApplicationCount,
            long pendingInquiryCount,
            long pendingTicketCount,
            long pendingShipmentCount,
            long createdOrderCount,
            String netPaidFen,
            Instant asOf,
            Instant start,
            Instant end) {}
}
