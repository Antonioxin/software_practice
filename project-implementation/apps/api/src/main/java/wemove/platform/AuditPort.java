package wemove.platform;

import java.time.Instant;
import java.util.UUID;

public interface AuditPort {
    void append(AuditEvent event);
    record AuditEvent(UUID actorId, String action, String objectType, UUID objectId,
                      String result, String reason, Instant occurredAt) {}
}
