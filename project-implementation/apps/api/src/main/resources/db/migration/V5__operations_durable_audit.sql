CREATE TABLE operations_audit_records (
    id BINARY(16) PRIMARY KEY,
    actor_id BINARY(16) NULL,
    action VARCHAR(255) NOT NULL,
    object_type VARCHAR(255) NOT NULL,
    object_id BINARY(16) NOT NULL,
    result VARCHAR(255) NOT NULL,
    reason VARCHAR(500) NULL,
    occurred_at DATETIME(6) NOT NULL,
    request_id VARCHAR(100) NULL,
    change_summary VARCHAR(1000) NULL,
    FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_audit_object (object_type, object_id, occurred_at),
    INDEX idx_audit_time (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
