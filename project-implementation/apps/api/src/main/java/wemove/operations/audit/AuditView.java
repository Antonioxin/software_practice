package wemove.operations.audit;

import java.time.Instant;
import java.util.UUID;

/** 审计检索响应视图；不含任何凭证或完整个人数据。 */
public record AuditView(
        UUID id,
        UUID actorId,
        String action,
        String objectType,
        UUID objectId,
        String result,
        String reason,
        Instant occurredAt,
        String requestId,
        String changeSummary) {

    public static AuditView from(AuditRecord record) {
        return new AuditView(
                record.getId(),
                record.getActorId(),
                record.getAction(),
                record.getObjectType(),
                record.getObjectId(),
                record.getResult(),
                record.getReason(),
                record.getOccurredAt(),
                record.getRequestId(),
                record.getChangeSummary());
    }
}
