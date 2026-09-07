package wemove.operations.audit;

import jakarta.persistence.*;

import wemove.platform.AuditPort;

import java.time.Instant;
import java.util.UUID;

/** F-owned durable audit record. No address, credential or raw command body is stored. */
@Entity
@Table(name = "operations_audit_records")
public class AuditRecord {
    @Id private UUID id;
    private UUID actorId;
    private String action;
    private String objectType;
    private UUID objectId;
    private String result;

    @Column(length = 500)
    private String reason;

    private Instant occurredAt;

    @Column(length = 100)
    private String requestId;

    @Column(length = 1000)
    private String changeSummary;

    protected AuditRecord() {}

    public AuditRecord(AuditPort.AuditEvent e, String requestId) {
        id = UUID.randomUUID();
        actorId = e.actorId();
        action = e.action();
        objectType = e.objectType();
        objectId = e.objectId();
        result = e.result();
        reason = e.reason();
        occurredAt = e.occurredAt();
        this.requestId = requestId;
        changeSummary = e.changeSummary();
    }

    /** F 的检索入口（只读）使用的访问器。 */
    public UUID getId() { return id; }

    public UUID getActorId() { return actorId; }

    public String getAction() { return action; }

    public String getObjectType() { return objectType; }

    public UUID getObjectId() { return objectId; }

    public String getResult() { return result; }

    public String getReason() { return reason; }

    public Instant getOccurredAt() { return occurredAt; }

    public String getRequestId() { return requestId; }

    public String getChangeSummary() { return changeSummary; }
}
