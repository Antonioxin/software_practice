package wemove.platform.idempotency;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"actor_id", "operation_id", "idempotency_key"}))
public class IdempotencyRecordEntity {
    @Id private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_json", nullable = false, columnDefinition = "longtext")
    private String responseJson;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "execution_state", nullable = false)
    private String executionState = "COMPLETED";

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @Column(name = "resource_reference")
    private String resourceReference;

    protected IdempotencyRecordEntity() {}

    public IdempotencyRecordEntity(
            UUID actorId,
            String operationId,
            UUID key,
            String requestHash,
            String responseJson,
            int httpStatus,
            Instant now) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.operationId = operationId;
        this.idempotencyKey = key;
        this.requestHash = requestHash;
        this.responseJson = responseJson;
        this.httpStatus = httpStatus;
        this.createdAt = now;
        this.completedAt = now;
        this.expiresAt = now.plusSeconds(86400);
    }

    public void begin(String resource) {
        executionState = "RUNNING";
        resourceReference = resource;
        schemaVersion = 2;
        completedAt = null;
        expiresAt = null;
    }

    public void complete(String json, int status, Instant now) {
        responseJson = json;
        httpStatus = status;
        executionState = "COMPLETED";
        completedAt = now;
        expiresAt = now.plusSeconds(86400);
    }

    public String getOperationId() {
        return operationId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getExecutionState() {
        return executionState;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
