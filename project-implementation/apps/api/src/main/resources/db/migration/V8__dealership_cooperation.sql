CREATE TABLE dealer_applications (
    id BINARY(16) NOT NULL,
    application_number VARCHAR(32) NOT NULL,
    user_id BINARY(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    current_content_version INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    public_reason VARCHAR(500) NULL,
    internal_note VARCHAR(2000) NULL,
    reviewed_by BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_applications PRIMARY KEY (id),
    CONSTRAINT uk_dealer_applications_number UNIQUE (application_number),
    CONSTRAINT uk_dealer_applications_user UNIQUE (user_id),
    CONSTRAINT fk_dealer_applications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_dealer_applications_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
    CONSTRAINT chk_dealer_applications_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT chk_dealer_applications_content_version CHECK (current_content_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_dealer_applications_queue ON dealer_applications(status, updated_at DESC, id);

CREATE TABLE dealer_application_versions (
    id BINARY(16) NOT NULL,
    application_id BINARY(16) NOT NULL,
    content_version INT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    country_or_region VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(21) NOT NULL,
    cooperation_email VARCHAR(254) NOT NULL,
    business_channels VARCHAR(2000) NOT NULL,
    website VARCHAR(2048) NULL,
    cooperation_intent VARCHAR(2000) NOT NULL,
    public_channel_consent BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_application_versions PRIMARY KEY (id),
    CONSTRAINT uk_dealer_application_versions UNIQUE (application_id, content_version),
    CONSTRAINT fk_dealer_application_versions_application FOREIGN KEY (application_id) REFERENCES dealer_applications(id),
    CONSTRAINT chk_dealer_application_versions_business_type CHECK (business_type IN ('RETAIL','WHOLESALE','IMPORT','EDUCATION_ACTIVITY','OTHER')),
    CONSTRAINT chk_dealer_application_versions_number CHECK (content_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dealer_application_reviews (
    id BINARY(16) NOT NULL,
    application_id BINARY(16) NOT NULL,
    content_version INT NOT NULL,
    decision VARCHAR(16) NOT NULL,
    public_reason VARCHAR(500) NULL,
    internal_note VARCHAR(2000) NULL,
    reviewer_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_application_reviews PRIMARY KEY (id),
    CONSTRAINT uk_dealer_application_reviews_version UNIQUE (application_id, content_version),
    CONSTRAINT fk_dealer_application_reviews_application FOREIGN KEY (application_id) REFERENCES dealer_applications(id),
    CONSTRAINT fk_dealer_application_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_dealer_application_reviews_decision CHECK (decision IN ('APPROVE','REJECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dealer_companies (
    id BINARY(16) NOT NULL,
    owner_user_id BINARY(16) NOT NULL,
    source_application_id BINARY(16) NOT NULL,
    source_public_consent BOOLEAN NOT NULL DEFAULT FALSE,
    company_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    country_or_region VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(21) NOT NULL,
    cooperation_email VARCHAR(254) NOT NULL,
    website VARCHAR(2048) NULL,
    cooperation_status VARCHAR(16) NOT NULL,
    internal_note VARCHAR(2000) NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_companies PRIMARY KEY (id),
    CONSTRAINT uk_dealer_companies_owner UNIQUE (owner_user_id),
    CONSTRAINT uk_dealer_companies_application UNIQUE (source_application_id),
    CONSTRAINT fk_dealer_companies_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_dealer_companies_application FOREIGN KEY (source_application_id) REFERENCES dealer_applications(id),
    CONSTRAINT chk_dealer_companies_status CHECK (cooperation_status IN ('ACTIVE','SUSPENDED')),
    CONSTRAINT chk_dealer_companies_business_type CHECK (business_type IN ('RETAIL','WHOLESALE','IMPORT','EDUCATION_ACTIVITY','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_dealer_companies_status ON dealer_companies(cooperation_status, updated_at DESC, id);

CREATE TABLE dealer_channels (
    id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    country_or_region VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    phone VARCHAR(21) NOT NULL,
    website VARCHAR(2048) NULL,
    company_id BINARY(16) NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_channels PRIMARY KEY (id),
    CONSTRAINT fk_dealer_channels_company FOREIGN KEY (company_id) REFERENCES dealer_companies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_dealer_channels_public ON dealer_channels(published, country_or_region, city, name, id);
CREATE INDEX idx_dealer_channels_company ON dealer_channels(company_id, published);

CREATE TABLE dealer_inquiries (
    id BINARY(16) NOT NULL,
    inquiry_number VARCHAR(32) NOT NULL,
    company_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expected_delivery_date DATE NULL,
    delivery_notes VARCHAR(2000) NULL,
    purpose VARCHAR(2000) NULL,
    remark VARCHAR(2000) NULL,
    public_reply VARCHAR(2000) NULL,
    close_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_inquiries PRIMARY KEY (id),
    CONSTRAINT uk_dealer_inquiries_number UNIQUE (inquiry_number),
    CONSTRAINT fk_dealer_inquiries_company FOREIGN KEY (company_id) REFERENCES dealer_companies(id),
    CONSTRAINT fk_dealer_inquiries_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_dealer_inquiries_status CHECK (status IN ('NEW','PROCESSING','REPLIED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_dealer_inquiries_owner ON dealer_inquiries(user_id, updated_at DESC, id);
CREATE INDEX idx_dealer_inquiries_queue ON dealer_inquiries(status, updated_at DESC, id);

CREATE TABLE dealer_inquiry_items (
    id BINARY(16) NOT NULL,
    inquiry_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    sku_snapshot VARCHAR(40) NOT NULL,
    name_snapshot VARCHAR(100) NOT NULL,
    reference_unit_price_fen_snapshot BIGINT NOT NULL,
    min_inquiry_quantity_snapshot INT NOT NULL,
    quantity INT NOT NULL,
    reply_reference_unit_price_fen BIGINT NULL,
    reply_lead_time_text VARCHAR(500) NULL,
    CONSTRAINT pk_dealer_inquiry_items PRIMARY KEY (id),
    CONSTRAINT uk_dealer_inquiry_items_product UNIQUE (inquiry_id, product_id),
    CONSTRAINT fk_dealer_inquiry_items_inquiry FOREIGN KEY (inquiry_id) REFERENCES dealer_inquiries(id),
    CONSTRAINT fk_dealer_inquiry_items_product FOREIGN KEY (product_id) REFERENCES catalog_products(id),
    CONSTRAINT chk_dealer_inquiry_items_quantity CHECK (quantity BETWEEN 1 AND 9999),
    CONSTRAINT chk_dealer_inquiry_items_min CHECK (min_inquiry_quantity_snapshot BETWEEN 1 AND 9999),
    CONSTRAINT chk_dealer_inquiry_items_price CHECK (reference_unit_price_fen_snapshot BETWEEN 1 AND 99999999),
    CONSTRAINT chk_dealer_inquiry_items_reply_price CHECK (reply_reference_unit_price_fen IS NULL OR reply_reference_unit_price_fen BETWEEN 1 AND 99999999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dealer_inquiry_history (
    id BINARY(16) NOT NULL,
    inquiry_id BINARY(16) NOT NULL,
    action VARCHAR(32) NOT NULL,
    from_status VARCHAR(16) NULL,
    to_status VARCHAR(16) NOT NULL,
    inquiry_version BIGINT NOT NULL,
    actor_id BINARY(16) NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_dealer_inquiry_history PRIMARY KEY (id),
    CONSTRAINT fk_dealer_inquiry_history_inquiry FOREIGN KEY (inquiry_id) REFERENCES dealer_inquiries(id),
    CONSTRAINT fk_dealer_inquiry_history_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @now = UTC_TIMESTAMP(6);
INSERT INTO dealer_channels (id, name, country_or_region, city, address, phone, website, company_id, published, version, created_at, updated_at) VALUES
(UUID_TO_BIN('40000000-0000-0000-0000-000000000001'), 'WEMOVE 上海体验合作点', '中国', '上海市', '上海市徐汇区测试运动街 18 号', '+862100000018', 'https://www.wemovetoy.com', NULL, TRUE, 1, @now, @now),
(UUID_TO_BIN('40000000-0000-0000-0000-000000000002'), 'WEMOVE 北京活动采购点', '中国', '北京市', '北京市朝阳区测试活力路 6 号', '+861000000006', NULL, NULL, TRUE, 1, @now, @now),
(UUID_TO_BIN('40000000-0000-0000-0000-000000000003'), 'WEMOVE 杭州渠道（待发布）', '中国', '杭州市', '杭州市西湖区测试创意路 9 号', '+8657100000009', NULL, NULL, FALSE, 1, @now, @now);
