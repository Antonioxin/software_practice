CREATE TABLE users (
    id BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    email_normalized VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    phone VARCHAR(21) NULL,
    base_role VARCHAR(16) NOT NULL,
    account_status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email_normalized UNIQUE (email_normalized),
    CONSTRAINT chk_users_base_role CHECK (base_role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_users_status_created ON users(account_status, created_at DESC);

CREATE TABLE user_consents (
    user_id BINARY(16) NOT NULL,
    adult_confirmed_at DATETIME(6) NOT NULL,
    terms_version VARCHAR(32) NOT NULL,
    terms_accepted_at DATETIME(6) NOT NULL,
    privacy_version VARCHAR(32) NOT NULL,
    privacy_accepted_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_user_consents PRIMARY KEY (user_id),
    CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE account_status_history (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    operator_id BINARY(16) NOT NULL,
    action VARCHAR(16) NOT NULL,
    previous_status VARCHAR(16) NOT NULL,
    new_status VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_account_status_history PRIMARY KEY (id),
    CONSTRAINT fk_account_history_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_account_history_operator FOREIGN KEY (operator_id) REFERENCES users(id),
    CONSTRAINT chk_account_history_action CHECK (action IN ('DISABLE', 'RESTORE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_account_history_user ON account_status_history(user_id, created_at DESC);

CREATE TABLE idempotency_records (
    id BINARY(16) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    operation_id VARCHAR(100) NOT NULL,
    idempotency_key BINARY(16) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    http_status INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (id),
    CONSTRAINT fk_idempotency_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT uk_idempotency_actor_operation_key UNIQUE (actor_id, operation_id, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_idempotency_created ON idempotency_records(created_at);

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES LONGBLOB NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;
