-- An order may have many simulated FAILURE attempts. Preserve the complete replay response.
ALTER TABLE idempotency_records MODIFY response_json LONGTEXT NOT NULL;
CREATE INDEX idx_inventory_source ON inventory_movements(source_type, source_id, product_id);
