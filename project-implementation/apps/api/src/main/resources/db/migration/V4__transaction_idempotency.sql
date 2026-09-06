-- Preserve legacy records verbatim. The executor checks the whole legacy operation family
-- before creating a stable-operation claim. Multiple legacy targets block new effects.
ALTER TABLE idempotency_records
    ADD execution_state VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    ADD completed_at DATETIME(6) NULL,
    ADD expires_at DATETIME(6) NULL,
    ADD schema_version INT NOT NULL DEFAULT 1,
    ADD resource_reference VARCHAR(255) NULL;
UPDATE idempotency_records SET completed_at = created_at, expires_at = DATE_ADD(created_at, INTERVAL 24 HOUR);
CREATE INDEX idx_idempotency_actor_key ON idempotency_records(actor_id, idempotency_key);
