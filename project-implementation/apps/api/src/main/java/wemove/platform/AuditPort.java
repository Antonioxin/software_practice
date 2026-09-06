package wemove.platform;

import java.time.Instant;
import java.util.UUID;

public interface AuditPort {
    void append(AuditEvent event);

    record AuditEvent(
            UUID actorId,
            String action,
            String objectType,
            UUID objectId,
            String result,
            String reason,
            Instant occurredAt,
            String requestId,
            String changeSummary) {
        public AuditEvent(
                UUID actorId,
                String action,
                String objectType,
                UUID objectId,
                String result,
                String reason,
                Instant occurredAt) {
            this(actorId, action, objectType, objectId, result, reason, occurredAt, null, action);
        }
    }
}
