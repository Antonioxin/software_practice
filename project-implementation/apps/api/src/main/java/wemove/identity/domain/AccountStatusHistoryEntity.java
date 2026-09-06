package wemove.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_status_history")
public class AccountStatusHistoryEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;
    @Column(nullable = false, length = 16)
    private String action;
    @Column(name = "previous_status", nullable = false, length = 16)
    private String previousStatus;
    @Column(name = "new_status", nullable = false, length = 16)
    private String newStatus;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountStatusHistoryEntity() {}

    public AccountStatusHistoryEntity(UUID userId, UUID operatorId, String action,
                                      AccountStatus previous, AccountStatus next, String reason, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.operatorId = operatorId;
        this.action = action;
        this.previousStatus = previous.name();
        this.newStatus = next.name();
        this.reason = reason;
        this.createdAt = now;
    }

    public String getAction() { return action; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
