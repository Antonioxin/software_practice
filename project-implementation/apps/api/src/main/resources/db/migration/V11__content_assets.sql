-- Binaries are separate from metadata so lists never load file content. Each upload,
-- replacement and audit commits in one InnoDB transaction; no static storage URL exists.
CREATE TABLE content_binaries (
    id BINARY(16) NOT NULL PRIMARY KEY,
    bytes LONGBLOB NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE content_assets (
    id BINARY(16) NOT NULL PRIMARY KEY,
    alt_text VARCHAR(200) NOT NULL,
    filename VARCHAR(200) NOT NULL,
    mime_type VARCHAR(32) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    size_bytes BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT ck_content_asset_mime CHECK (mime_type IN ('image/jpeg','image/png','image/webp')),
    CONSTRAINT ck_content_asset_size CHECK (size_bytes BETWEEN 1 AND 5242880)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE content_asset_references (
    id BINARY(16) NOT NULL PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    source_id BINARY(16) NOT NULL,
    asset_id BINARY(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_content_asset_source UNIQUE(source_type,source_id,asset_id),
    CONSTRAINT fk_content_asset_reference FOREIGN KEY (asset_id) REFERENCES content_assets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_content_asset_public ON content_asset_references(asset_id,active);
CREATE TABLE content_files (
    id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    version_note VARCHAR(500) NOT NULL,
    product_ids VARCHAR(800) NOT NULL DEFAULT '',
    visibility VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 1,
    download_id BINARY(16) NOT NULL,
    filename VARCHAR(200) NOT NULL,
    size_bytes BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_content_file_download UNIQUE(download_id),
    CONSTRAINT ck_content_file_visibility CHECK (visibility IN ('PUBLIC','DEALER','INTERNAL')),
    CONSTRAINT ck_content_file_status CHECK (status IN ('DRAFT','PUBLISHED','OFFLINE')),
    CONSTRAINT ck_content_file_size CHECK (size_bytes BETWEEN 1 AND 10485760)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_content_file_visible ON content_files(status,visibility,updated_at);
