-- F：客户支持工单（FR-19/20/21）。BR-05 状态机：NEW/PROCESSING/REPLIED/CLOSED，
-- 关闭后全只读；公开消息与内部备注分表保存。
CREATE TABLE support_tickets (
    id BINARY(16) NOT NULL,
    ticket_number VARCHAR(40) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    phone VARCHAR(255) NULL,
    product_id BINARY(16) NULL,
    order_id BINARY(16) NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6) NULL,
    closed_by BINARY(16) NULL,
    close_reason VARCHAR(500) NULL,
    CONSTRAINT pk_support_tickets PRIMARY KEY (id),
    CONSTRAINT uk_support_tickets_number UNIQUE (ticket_number),
    CONSTRAINT fk_support_tickets_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT fk_support_tickets_product FOREIGN KEY (product_id) REFERENCES catalog_products(id),
    CONSTRAINT fk_support_tickets_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id),
    CONSTRAINT fk_support_tickets_closed_by FOREIGN KEY (closed_by) REFERENCES users(id),
    CONSTRAINT chk_support_tickets_type CHECK (type IN ('GENERAL', 'PRODUCT', 'AFTER_SALES')),
    CONSTRAINT chk_support_tickets_status CHECK (status IN ('NEW', 'PROCESSING', 'REPLIED', 'CLOSED')),
    CONSTRAINT chk_support_tickets_version CHECK (version >= 1),
    CONSTRAINT chk_support_tickets_general CHECK (type <> 'GENERAL' OR (product_id IS NULL AND order_id IS NULL)),
    CONSTRAINT chk_support_tickets_product CHECK (type <> 'PRODUCT' OR (product_id IS NOT NULL AND order_id IS NULL)),
    CONSTRAINT chk_support_tickets_after_sales CHECK (type <> 'AFTER_SALES' OR (order_id IS NOT NULL AND product_id IS NULL)),
    CONSTRAINT chk_support_tickets_closed CHECK (
        (status <> 'CLOSED' AND closed_at IS NULL AND closed_by IS NULL AND close_reason IS NULL)
        OR (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_by IS NOT NULL AND close_reason IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_support_tickets_actor ON support_tickets(actor_id, created_at DESC, id);
CREATE INDEX idx_support_tickets_status ON support_tickets(status, created_at DESC, id);
CREATE INDEX idx_support_tickets_type ON support_tickets(type, created_at DESC, id);
CREATE INDEX idx_support_tickets_created ON support_tickets(created_at DESC, id);
CREATE INDEX idx_support_tickets_order ON support_tickets(order_id);

-- 公开对话：用户补充与管理员公开回复，append-only，不覆盖历史。
CREATE TABLE support_messages (
    id BINARY(16) NOT NULL,
    ticket_id BINARY(16) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_support_messages PRIMARY KEY (id),
    CONSTRAINT fk_support_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    CONSTRAINT fk_support_messages_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT chk_support_messages_kind CHECK (kind IN ('USER_FOLLOWUP', 'PUBLIC_REPLY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_support_messages_ticket ON support_messages(ticket_id, created_at, id);

-- 内部备注：仅管理员写入与读取，绝不出现在用户响应中。
CREATE TABLE support_internal_notes (
    id BINARY(16) NOT NULL,
    ticket_id BINARY(16) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_support_internal_notes PRIMARY KEY (id),
    CONSTRAINT fk_support_internal_notes_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    CONSTRAINT fk_support_internal_notes_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_support_internal_notes_ticket ON support_internal_notes(ticket_id, created_at, id);

-- 状态与操作历史（照 commerce_order_history 形状），关闭后仍是审计事实来源。
CREATE TABLE support_ticket_history (
    id BINARY(16) NOT NULL,
    ticket_id BINARY(16) NOT NULL,
    action VARCHAR(64) NOT NULL,
    from_status VARCHAR(16) NULL,
    to_status VARCHAR(16) NOT NULL,
    ticket_version BIGINT NOT NULL,
    actor_id BINARY(16) NOT NULL,
    reason VARCHAR(500) NULL,
    request_id VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_support_ticket_history PRIMARY KEY (id),
    CONSTRAINT uk_support_history_version UNIQUE (ticket_id, ticket_version),
    CONSTRAINT fk_support_history_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    CONSTRAINT fk_support_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT chk_support_history_version CHECK (ticket_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_support_history_ticket ON support_ticket_history(ticket_id, created_at, id);
