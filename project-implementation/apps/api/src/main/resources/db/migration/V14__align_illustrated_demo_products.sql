-- The six illustrated products used by the web application were previously
-- created only through one developer's local admin API. Seed them through
-- Flyway so a fresh or upgraded database exposes the same SKU contract as the
-- checked-in product image and guide mappings.
--
-- Keep the legacy V2 rows for historical foreign-key references, but remove
-- them from the public catalogue. Existing illustrated products created by an
-- administrator are retained and win by SKU.

SET @now = UTC_TIMESTAMP(6);
SET @cat_balance = UUID_TO_BIN('10000000-0000-0000-0000-000000000101');
SET @cat_throw = UUID_TO_BIN('10000000-0000-0000-0000-000000000102');
SET @cat_team = UUID_TO_BIN('10000000-0000-0000-0000-000000000103');

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
)
SELECT UUID_TO_BIN('21000000-0000-0000-0000-000000001001'),
       'WM-BALANCE-STONES', '平衡石', @cat_balance,
       '用六块柔和色彩的平衡石，搭一条自己的小路。',
       '用六块柔和色彩的平衡石，搭一条自己的小路。本资料为课程演示商品，用于检查排版、筛选与详情布局。',
       3, 10, 'BALANCE', 'BOTH', '圆角木质构件与水性涂层',
       '320 × 240 × 80 mm；以实际产品资料为准。', '主体 1 套、收纳袋 1 件、玩法说明 1 份。',
       '由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。',
       '课程演示商品；使用前请检查器材完整性，并由成人陪同。',
       'product-balance-stones', 'product-balance-stones',
       25900, TRUE, 20720, 10, '示例：7—10 个工作日', 'PUBLISHED', 1, 0, @now, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_products
    WHERE sku = 'WM-BALANCE-STONES'
       OR id = UUID_TO_BIN('21000000-0000-0000-0000-000000001001')
);

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
)
SELECT UUID_TO_BIN('21000000-0000-0000-0000-000000001002'),
       'WM-RAINBOW-ARCH', '彩虹拱桥', @cat_balance,
       '堆叠、穿越、想象，让简单的形状连接更多玩法。',
       '堆叠、穿越、想象，让简单的形状连接更多玩法。本资料为课程演示商品，用于检查排版、筛选与详情布局。',
       3, 12, 'COORDINATION', 'INDOOR', '圆角木质构件与水性涂层',
       '320 × 240 × 80 mm；以实际产品资料为准。', '主体 1 套、收纳袋 1 件、玩法说明 1 份。',
       '由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。',
       '课程演示商品；使用前请检查器材完整性，并由成人陪同。',
       'product-rainbow-arch', 'product-rainbow-arch',
       32900, TRUE, 26320, 10, '示例：7—10 个工作日', 'PUBLISHED', 2, 0, @now, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_products
    WHERE sku = 'WM-RAINBOW-ARCH'
       OR id = UUID_TO_BIN('21000000-0000-0000-0000-000000001002')
);

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
)
SELECT UUID_TO_BIN('21000000-0000-0000-0000-000000001003'),
       'WM-RING-TOSS', '环环投掷', @cat_throw,
       '循序渐进的投掷挑战，练习专注与手眼协调。',
       '循序渐进的投掷挑战，练习专注与手眼协调。本资料为课程演示商品，用于检查排版、筛选与详情布局。',
       4, 14, 'THROWING', 'BOTH', '圆角木质构件与柔软编织绳',
       '320 × 240 × 80 mm；以实际产品资料为准。', '投掷底座 1 套、绳圈 6 个、玩法说明 1 份。',
       '由成人陪同，在平坦开阔的空间内使用。从近距离开始，再逐步增加挑战。',
       '课程演示商品；投掷前确认前方无人，并由成人陪同。',
       'product-ring-toss', 'product-ring-toss',
       16900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 3, 0, @now, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_products
    WHERE sku = 'WM-RING-TOSS'
       OR id = UUID_TO_BIN('21000000-0000-0000-0000-000000001003')
);

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
)
SELECT UUID_TO_BIN('21000000-0000-0000-0000-000000001004'),
       'WM-TEAM-BOARD', '伙伴协作板', @cat_team,
       '一起出发、一起保持平衡，把合作变成游戏。',
       '一起出发、一起保持平衡，把合作变成游戏。本资料为课程演示商品，用于检查排版、筛选与详情布局。',
       6, 16, 'TEAM_PLAY', 'BOTH', '圆角木质构件、防滑垫与编织绳',
       '单板约 900 × 120 mm；以实际产品资料为准。', '协作板 2 块、玩法说明 1 份。',
       '参与者站稳后握住绳环，先练习同时抬脚，再共同缓慢前进。',
       '课程演示商品；需成人组织，在平坦、无障碍的地面使用。',
       'product-team-board', 'product-team-board',
       28900, TRUE, 23120, 10, '示例：7—10 个工作日', 'PUBLISHED', 4, 0, @now, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_products
    WHERE sku = 'WM-TEAM-BOARD'
       OR id = UUID_TO_BIN('21000000-0000-0000-0000-000000001004')
);

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
)
SELECT UUID_TO_BIN('21000000-0000-0000-0000-000000001005'),
       'WM-FOREST-KIT', '森林探索套装', @cat_team,
       '收集色彩、观察纹理，为一次散步增添新发现。',
       '收集色彩、观察纹理，为一次散步增添新发现。本资料为课程演示商品，用于检查排版、筛选与详情布局。',
       5, 14, 'OUTDOOR_EXPLORATION', 'OUTDOOR', '木质观察工具、纸质记录册与棉布收纳袋',
       '收纳袋约 220 × 260 mm；以实际产品资料为准。', '放大镜、指南针、记录册、观察卡与收纳袋。',
       '由成人提前确认安全路线，在开阔区域观察自然物，结束后带走全部器材。',
       '课程演示商品；远离道路、水边和视线盲区，并由成人全程陪同。',
       'product-forest-kit', 'product-forest-kit',
       21900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 5, 0, @now, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_products
    WHERE sku = 'WM-FOREST-KIT'
       OR id = UUID_TO_BIN('21000000-0000-0000-0000-000000001005')
);

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
)
SELECT UUID_TO_BIN('21000000-0000-0000-0000-000000001006'),
       'WM-SKIP-ROPE', '轻盈跳绳', @cat_balance,
       '轻巧握柄与适合练习的绳长，找到自己的节奏。',
       '轻巧握柄与适合练习的绳长，找到自己的节奏。本资料为课程演示商品，用于检查排版、筛选与详情布局。',
       6, 18, 'COORDINATION', 'BOTH', '木质握柄与柔软耐磨编织绳',
       '绳长可调；以实际产品资料为准。', '跳绳 1 根、收纳袋 1 件、玩法说明 1 份。',
       '由成人陪同，在平坦开阔的空间内使用。先调整绳长，再从慢速连续动作开始。',
       '课程演示商品；与他人保持安全距离，不得将绳索缠绕身体。',
       'product-skip-rope', 'product-skip-rope',
       8900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 6, 0, @now, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_products
    WHERE sku = 'WM-SKIP-ROPE'
       OR id = UUID_TO_BIN('21000000-0000-0000-0000-000000001006')
);

-- Only the fixed rows inserted above need seeded balances. A database that
-- already owns one of these SKUs keeps its existing product id and stock.
INSERT INTO inventory_balances (product_id, quantity, version, updated_at)
SELECT product.id,
       CASE product.sku
           WHEN 'WM-BALANCE-STONES' THEN 42
           WHEN 'WM-RAINBOW-ARCH' THEN 18
           WHEN 'WM-RING-TOSS' THEN 35
           WHEN 'WM-TEAM-BOARD' THEN 12
           WHEN 'WM-FOREST-KIT' THEN 27
           ELSE 0
       END,
       0, @now
FROM catalog_products product
LEFT JOIN inventory_balances balance ON balance.product_id = product.id
WHERE product.id IN (
    UUID_TO_BIN('21000000-0000-0000-0000-000000001001'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001002'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001003'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001004'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001005'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001006')
)
  AND balance.product_id IS NULL;

INSERT INTO inventory_movements (
    id, product_id, direction, quantity, quantity_before, quantity_after,
    reason, source_type, source_id, actor_id, created_at
)
SELECT UUID_TO_BIN(UUID()), product.id, 'INCREASE', balance.quantity, 0, balance.quantity,
       '初始化配图示例库存', 'INITIAL', BIN_TO_UUID(product.id), NULL, @now
FROM catalog_products product
JOIN inventory_balances balance ON balance.product_id = product.id
LEFT JOIN inventory_movements movement
       ON movement.product_id = product.id
      AND movement.source_type = 'INITIAL'
      AND movement.source_id = BIN_TO_UUID(product.id)
WHERE product.id IN (
    UUID_TO_BIN('21000000-0000-0000-0000-000000001001'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001002'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001003'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001004'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001005'),
    UUID_TO_BIN('21000000-0000-0000-0000-000000001006')
)
  AND movement.id IS NULL;

-- Do not delete legacy rows: orders, inquiries, tickets or carts in an upgraded
-- member database may still reference them. Hiding only the known V2 seeds
-- gives every member the same six-item public catalogue while preserving history.
UPDATE catalog_products
SET status = 'UNLISTED', version = version + 1, updated_at = @now
WHERE id IN (
    UUID_TO_BIN('20000000-0000-0000-0000-000000001001'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001002'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001003'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001004'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001005'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001006'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001007'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001008'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001009'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001010'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001011'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001012'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001013'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001014'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001015'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001016'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001017'),
    UUID_TO_BIN('20000000-0000-0000-0000-000000001018')
)
  AND status <> 'UNLISTED';
