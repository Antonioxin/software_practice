CREATE TABLE commerce_carts (
 id BINARY(16) PRIMARY KEY, user_id BINARY(16) NOT NULL UNIQUE,
 version BIGINT NOT NULL DEFAULT 1 CHECK(version >= 1), updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE commerce_cart_items (
 id BINARY(16) PRIMARY KEY, cart_id BINARY(16) NOT NULL, product_id BINARY(16) NOT NULL,
 quantity INT NOT NULL CHECK(quantity BETWEEN 1 AND 99), last_confirmed_unit_price_fen BIGINT NOT NULL,
 UNIQUE(cart_id,product_id), FOREIGN KEY(cart_id) REFERENCES commerce_carts(id),
 FOREIGN KEY(product_id) REFERENCES catalog_products(id)
) ENGINE=InnoDB;
CREATE TABLE commerce_checkout_previews (
 id BINARY(16) PRIMARY KEY, token_hash VARCHAR(255) NOT NULL UNIQUE, user_id BINARY(16) NOT NULL,
 cart_id BINARY(16) NOT NULL, cart_version BIGINT NOT NULL, snapshot_json TEXT NOT NULL,
 created_at DATETIME(6) NOT NULL, expires_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(cart_id) REFERENCES commerce_carts(id),
 INDEX idx_preview_expiry(expires_at), INDEX idx_preview_user(user_id,created_at)
) ENGINE=InnoDB;
CREATE TABLE commerce_orders (
 id BINARY(16) PRIMARY KEY, order_number VARCHAR(255) NOT NULL UNIQUE,
 user_id BINARY(16) NOT NULL, preview_id BINARY(16) NOT NULL UNIQUE,
 status VARCHAR(255) NOT NULL CHECK(status IN ('PENDING_PAYMENT','PAID','CANCELLED','SHIPPED','COMPLETED')),
 version BIGINT NOT NULL CHECK(version >= 1),
 currency VARCHAR(255) NOT NULL CHECK(currency='CNY'), mode VARCHAR(255) NOT NULL CHECK(mode='SIMULATED'),
 subtotal_fen BIGINT NOT NULL CHECK(subtotal_fen > 0),
 shipping_fen BIGINT NOT NULL CHECK(shipping_fen=0), tax_fen BIGINT NOT NULL CHECK(tax_fen=0),
 discount_fen BIGINT NOT NULL CHECK(discount_fen=0),
 total_fen BIGINT NOT NULL,
 CHECK(total_fen=subtotal_fen+shipping_fen+tax_fen-discount_fen),
 recipient VARCHAR(255) NOT NULL, phone VARCHAR(255) NOT NULL, country_or_region VARCHAR(255) NOT NULL,
 region VARCHAR(255), city VARCHAR(255) NOT NULL, address_line VARCHAR(255) NOT NULL, remark VARCHAR(2000),
 created_at DATETIME(6) NOT NULL, paid_at DATETIME(6), cancelled_at DATETIME(6), shipped_at DATETIME(6), completed_at DATETIME(6),
 logistics_name VARCHAR(255), tracking_number VARCHAR(255), shipped_by BINARY(16),
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(preview_id) REFERENCES commerce_checkout_previews(id),
 INDEX idx_order_user(user_id,created_at,id), INDEX idx_order_user_status(user_id,status,created_at,id),
 INDEX idx_order_status(status,created_at,id), INDEX idx_order_created(created_at,id)
) ENGINE=InnoDB;
CREATE TABLE commerce_order_items (
 id BINARY(16) PRIMARY KEY, order_id BINARY(16) NOT NULL, product_id BINARY(16) NOT NULL,
 sku VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL,
 unit_price_fen BIGINT NOT NULL CHECK(unit_price_fen BETWEEN 1 AND 99999999),
 quantity INT NOT NULL CHECK(quantity BETWEEN 1 AND 99), subtotal_fen BIGINT NOT NULL,
 CHECK(subtotal_fen=unit_price_fen*quantity),
 UNIQUE(order_id,product_id), FOREIGN KEY(order_id) REFERENCES commerce_orders(id), FOREIGN KEY(product_id) REFERENCES catalog_products(id)
) ENGINE=InnoDB;
CREATE TABLE commerce_payment_attempts (
 id BINARY(16) PRIMARY KEY, order_id BINARY(16) NOT NULL, outcome VARCHAR(255) NOT NULL CHECK(outcome IN ('SUCCESS','FAILURE')),
 mode VARCHAR(255) NOT NULL CHECK(mode='SIMULATED'), amount_fen BIGINT NOT NULL CHECK(amount_fen>0),
 simulation_reference VARCHAR(255) NOT NULL UNIQUE, actor_id BINARY(16) NOT NULL, created_at DATETIME(6) NOT NULL,
 success_order_id BINARY(16) UNIQUE,
 CHECK((outcome='SUCCESS' AND success_order_id IS NOT NULL AND success_order_id=order_id) OR (outcome='FAILURE' AND success_order_id IS NULL)),
 FOREIGN KEY(order_id) REFERENCES commerce_orders(id), FOREIGN KEY(actor_id) REFERENCES users(id),
 INDEX idx_payment_order(order_id,created_at,id), INDEX idx_payment_time(outcome,created_at,order_id)
) ENGINE=InnoDB;
CREATE TABLE commerce_refunds (
 id BINARY(16) PRIMARY KEY, order_id BINARY(16) NOT NULL UNIQUE, payment_attempt_id BINARY(16) NOT NULL UNIQUE,
 amount_fen BIGINT NOT NULL CHECK(amount_fen>0), simulation_reference VARCHAR(255) NOT NULL UNIQUE,
 mode VARCHAR(255) NOT NULL CHECK(mode='SIMULATED'), actor_id BINARY(16) NOT NULL, reason VARCHAR(500) NOT NULL,
 created_at DATETIME(6) NOT NULL, FOREIGN KEY(order_id) REFERENCES commerce_orders(id),
 FOREIGN KEY(payment_attempt_id) REFERENCES commerce_payment_attempts(id), FOREIGN KEY(actor_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE commerce_order_history (
 id BINARY(16) PRIMARY KEY, order_id BINARY(16) NOT NULL, action VARCHAR(255) NOT NULL,
 from_status VARCHAR(255), to_status VARCHAR(255) NOT NULL, order_version BIGINT NOT NULL CHECK(order_version>=1),
 actor_id BINARY(16) NOT NULL, reason VARCHAR(500), request_id VARCHAR(255), created_at DATETIME(6) NOT NULL,
 UNIQUE(order_id,order_version), FOREIGN KEY(order_id) REFERENCES commerce_orders(id), FOREIGN KEY(actor_id) REFERENCES users(id),
 INDEX idx_history_order(order_id,created_at,id)
) ENGINE=InnoDB;
