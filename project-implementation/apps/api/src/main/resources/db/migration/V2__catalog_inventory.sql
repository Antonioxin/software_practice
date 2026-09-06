CREATE TABLE catalog_categories (
    id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    name_normalized VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_catalog_categories PRIMARY KEY (id),
    CONSTRAINT uk_catalog_categories_name UNIQUE (name_normalized),
    CONSTRAINT chk_catalog_categories_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE catalog_products (
    id BINARY(16) NOT NULL,
    sku VARCHAR(40) NULL,
    name VARCHAR(100) NULL,
    category_id BINARY(16) NULL,
    summary VARCHAR(200) NULL,
    description TEXT NULL,
    age_min INT NULL,
    age_max INT NULL,
    play_type VARCHAR(40) NULL,
    scene VARCHAR(16) NULL,
    material VARCHAR(2000) NULL,
    dimensions VARCHAR(2000) NULL,
    package_contents VARCHAR(2000) NULL,
    instructions VARCHAR(2000) NULL,
    safety_notes VARCHAR(2000) NULL,
    main_image_id VARCHAR(64) NULL,
    image_ids TEXT NULL,
    retail_unit_price_fen BIGINT NULL,
    dealer_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    dealer_reference_unit_price_fen BIGINT NULL,
    min_inquiry_quantity INT NULL,
    lead_time_text VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_catalog_products PRIMARY KEY (id),
    CONSTRAINT uk_catalog_products_sku UNIQUE (sku),
    CONSTRAINT fk_catalog_products_category FOREIGN KEY (category_id) REFERENCES catalog_categories(id),
    CONSTRAINT chk_catalog_products_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNLISTED')),
    CONSTRAINT chk_catalog_products_scene CHECK (scene IS NULL OR scene IN ('INDOOR', 'OUTDOOR', 'BOTH')),
    CONSTRAINT chk_catalog_products_play_type CHECK (play_type IS NULL OR play_type IN ('BALANCE', 'COORDINATION', 'THROWING', 'TEAM_PLAY', 'OUTDOOR_EXPLORATION')),
    CONSTRAINT chk_catalog_products_age CHECK (age_min IS NULL OR (age_min BETWEEN 0 AND 18 AND (age_max IS NULL OR age_max BETWEEN age_min AND 18))),
    CONSTRAINT chk_catalog_products_retail_price CHECK (retail_unit_price_fen IS NULL OR retail_unit_price_fen BETWEEN 1 AND 99999999),
    CONSTRAINT chk_catalog_products_dealer_price CHECK (dealer_reference_unit_price_fen IS NULL OR dealer_reference_unit_price_fen BETWEEN 1 AND 99999999),
    CONSTRAINT chk_catalog_products_min_inquiry CHECK (min_inquiry_quantity IS NULL OR min_inquiry_quantity BETWEEN 1 AND 9999),
    CONSTRAINT chk_catalog_products_display_order CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_catalog_products_public ON catalog_products(status, display_order, id);
CREATE INDEX idx_catalog_products_category ON catalog_products(category_id, status, display_order);
CREATE INDEX idx_catalog_products_price ON catalog_products(status, retail_unit_price_fen, id);

CREATE TABLE inventory_balances (
    product_id BINARY(16) NOT NULL,
    quantity INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_inventory_balances PRIMARY KEY (product_id),
    CONSTRAINT fk_inventory_balances_product FOREIGN KEY (product_id) REFERENCES catalog_products(id),
    CONSTRAINT chk_inventory_balances_nonnegative CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory_movements (
    id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    quantity INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NULL,
    actor_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_inventory_movements_product FOREIGN KEY (product_id) REFERENCES catalog_products(id),
    CONSTRAINT fk_inventory_movements_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT uk_inventory_business_effect UNIQUE (product_id, source_type, source_id),
    CONSTRAINT chk_inventory_movements_direction CHECK (direction IN ('INCREASE', 'DECREASE')),
    CONSTRAINT chk_inventory_movements_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_movements_balances CHECK (quantity_before >= 0 AND quantity_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_inventory_movements_product_time ON inventory_movements(product_id, created_at DESC);

SET @now = UTC_TIMESTAMP(6);
SET @cat_balance = UUID_TO_BIN('10000000-0000-0000-0000-000000000101');
SET @cat_throw = UUID_TO_BIN('10000000-0000-0000-0000-000000000102');
SET @cat_team = UUID_TO_BIN('10000000-0000-0000-0000-000000000103');

INSERT INTO catalog_categories (id, name, name_normalized, description, sort_order, enabled, version, created_at, updated_at) VALUES
(@cat_balance, '平衡与协调', '平衡与协调', '面向家庭与活动空间的平衡、身体控制类运动游戏。', 10, TRUE, 0, @now, @now),
(@cat_throw, '投掷与瞄准', '投掷与瞄准', '训练投掷、瞄准与手眼协调的轻量运动玩具。', 20, TRUE, 0, @now, @now),
(@cat_team, '团队与户外', '团队与户外', '适合亲子、同伴协作和户外探索的组合游戏。', 30, TRUE, 0, @now, @now);

INSERT INTO catalog_products (
    id, sku, name, category_id, summary, description, age_min, age_max, play_type, scene,
    material, dimensions, package_contents, instructions, safety_notes, main_image_id, image_ids,
    retail_unit_price_fen, dealer_enabled, dealer_reference_unit_price_fen, min_inquiry_quantity,
    lead_time_text, status, display_order, version, created_at, updated_at
) VALUES
(UUID_TO_BIN('20000000-0000-0000-0000-000000001001'), 'WM-BAL-001', '木纹平衡步道', @cat_balance, '六块可自由排列的低台，练习步幅与重心转换。', '以不同间距组合一条家庭平衡路线，支持由易到难调整。', 3, 10, 'BALANCE', 'BOTH', '测试用木纹复合材料与防滑垫', '单块 28 × 12 × 6 cm', '平衡台 6 块、连接标记 8 枚', '在平整地面排列，从短间距开始依次通过。', '需成人看护；使用前确认地面干燥、组件无破损。', 'seed-img-balance-trail', 'seed-img-balance-trail-side', 29900, TRUE, 20900, 12, '测试交期：7—10 个工作日', 'PUBLISHED', 10, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001002'), 'WM-RING-002', '软质投环组', @cat_throw, '轻量软环与三档目标柱，适合室内外轮流挑战。', '通过距离和目标分值变化练习投掷控制。', 4, NULL, 'THROWING', 'BOTH', '测试用 EPP 软质材料', '目标底座 42 × 18 cm', '软环 8 个、目标柱 3 个、底座 1 个', '从一米距离开始，每轮三次投掷。', '请远离台阶和易碎物品；不可套向人体。', 'seed-img-ring-toss', NULL, 12900, TRUE, 8900, 24, '测试交期：5—8 个工作日', 'PUBLISHED', 20, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001003'), 'WM-BOWL-003', '家庭软式保龄球', @cat_throw, '安静柔软的十瓶套装，让客厅也能成为小球道。', '轻量球瓶降低碰撞噪音，适合亲子记分游戏。', 3, 9, 'THROWING', 'INDOOR', '测试用软质泡棉', '球瓶高 18 cm，球直径 12 cm', '球瓶 10 个、软球 2 个、收纳袋 1 个', '划定起点，轮流滚动软球并记录击倒数量。', '仅限滚动使用，不可抛向他人。', 'seed-img-soft-bowling', NULL, 18900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 30, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001004'), 'WM-STEP-004', '节奏脚印垫', @cat_balance, '十二枚方向脚印，组合跳跃、转向和节奏路线。', '使用颜色与方向提示设计连续动作路线。', 3, 12, 'COORDINATION', 'INDOOR', '测试用 TPE 防滑材料', '单枚 22 × 10 cm', '脚印垫 12 枚、玩法卡 10 张', '根据玩法卡排列方向，依次踩踏完成路线。', '确认垫片完全贴合地面，避免在湿滑表面使用。', 'seed-img-footsteps', NULL, 9900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 40, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001005'), 'WM-PARA-005', '合作彩虹伞', @cat_team, '多人共同抬升与传球，建立节奏和团队配合。', '适合亲子活动与小组热身的合作游戏。', 5, NULL, 'TEAM_PLAY', 'BOTH', '测试用高密度涤纶布', '展开直径 3 m', '彩虹伞 1 件、轻球 6 个、活动卡 8 张', '参与者均匀站立握住把手，按口令协作抬升。', '保持周围无障碍物；不可缠绕身体。', 'seed-img-parachute', NULL, 35900, TRUE, 24900, 8, '测试交期：10—15 个工作日', 'PUBLISHED', 50, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001006'), 'WM-BEAN-006', '触感豆袋标靶', @cat_throw, '六种纹理豆袋配合折叠标靶，练习力量控制。', '可按颜色、分值或触感设计多种回合。', 4, 11, 'THROWING', 'BOTH', '测试用织物与安全填充颗粒', '标靶展开 60 × 60 cm', '豆袋 12 个、折叠标靶 1 个', '摆放标靶后从近到远分轮投掷。', '发现破损立即停止使用，避免填充物外泄。', 'seed-img-beanbag', NULL, 15900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 60, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001007'), 'WM-CONE-007', '路径标志桶套装', @cat_team, '柔韧标志桶与数字贴，快速搭建绕行和接力路线。', '适合家庭、学校与活动机构设计运动路径。', 5, NULL, 'OUTDOOR_EXPLORATION', 'OUTDOOR', '测试用柔性 PE', '标志桶高 23 cm', '标志桶 20 个、数字贴 20 张、收纳网袋 1 个', '按路线需求摆放，使用后擦拭并收纳。', '设置在可见平整区域，避免阻塞通道。', 'seed-img-cones', NULL, 21900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 70, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001008'), 'WM-ROPE-008', '团队协作绳', @cat_team, '带色彩握位的环形软绳，用于节奏与方向协作。', '参与者在统一节奏下完成传递、转向和形状变化。', 6, NULL, 'TEAM_PLAY', 'BOTH', '测试用柔软编织绳', '环长 8 m', '环形协作绳 1 条、任务卡 12 张', '每人握住一个色彩区域，按任务卡共同移动。', '不得缠绕颈部或用于拉拽；需成人组织。', 'seed-img-team-rope', NULL, 26900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 80, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001009'), 'WM-DISC-009', '软飞盘三色组', @cat_throw, '边缘柔软、易抓握的三色飞盘，适合初学者。', '通过近距离双人传接逐步练习飞行方向。', 5, NULL, 'THROWING', 'OUTDOOR', '测试用柔性硅胶', '直径 22 cm', '软飞盘 3 个、收纳袋 1 个', '从三米内开始双人传接，逐步增加距离。', '仅在开阔区域使用，避开道路和人群。', 'seed-img-soft-disc', NULL, 7900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 90, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001010'), 'WM-BLOCK-010', '平衡积木挑战', @cat_balance, '不同坡度与纹理的模块，可组合低高度平衡任务。', '在保持低高度的前提下自由设计路线。', 4, 12, 'BALANCE', 'INDOOR', '测试用 EVA 泡棉', '模块最大 30 × 20 × 8 cm', '平衡模块 10 个、挑战卡 12 张', '先铺设防滑垫，再按挑战卡组合模块。', '不可堆叠超过说明高度；需成人检查连接。', 'seed-img-balance-blocks', NULL, 32900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 100, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001011'), 'WM-LADDER-011', '地面敏捷梯', @cat_balance, '可调节横档间距，完成跨步、侧移和节奏训练。', '地面铺设后用于低冲击敏捷小游戏。', 6, NULL, 'COORDINATION', 'BOTH', '测试用织带与柔性横档', '展开长 4 m', '敏捷梯 1 条、固定钉 4 枚、收纳袋 1 个', '在平整区域展开，按脚步图示依次通过。', '室内不使用固定钉；户外确认地面允许固定。', 'seed-img-agility-ladder', NULL, 13900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 110, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001012'), 'WM-BALL-012', '慢弹力训练球', @cat_team, '低弹、易控制的轻量球，支持接传与目标游戏。', '降低初学者接球压力，适合多种团队玩法。', 4, 10, 'TEAM_PLAY', 'BOTH', '测试用低弹 TPR', '直径 18 cm', '训练球 4 个、充气管 1 个', '按说明充气至适中硬度，使用双手近距离传接。', '远离尖锐物和火源；不可过度充气。', 'seed-img-slow-ball', NULL, 11900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 120, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001013'), 'WM-MARK-013', '户外探索标记', @cat_team, '可重复使用的任务标记，组织寻路和观察挑战。', '通过编号、颜色和方向箭头建立探索路线。', 6, NULL, 'OUTDOOR_EXPLORATION', 'OUTDOOR', '测试用耐候 PP', '单枚直径 15 cm', '任务标记 16 枚、可擦写任务卡 8 张', '成人提前勘察并布置安全路线，完成后全部回收。', '不得布置在道路、水边或视线盲区。', 'seed-img-trail-marks', NULL, 14900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 130, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001014'), 'WM-TOWER-014', '协作叠塔挑战', @cat_team, '多人牵引软绳控制夹具，共同搭建彩色积木塔。', '强调沟通、节奏与共同决策的桌地两用游戏。', 7, NULL, 'TEAM_PLAY', 'INDOOR', '测试用木纹复合块与棉绳', '积木 8 × 5 × 3 cm', '积木 12 块、协作夹具 1 套、任务卡 10 张', '参与者分别握住绳端，共同控制夹具移动积木。', '夹具仅用于配套积木；避免绳索缠绕。', 'seed-img-team-tower', NULL, 24900, FALSE, NULL, NULL, NULL, 'PUBLISHED', 140, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001015'), 'WM-HOP-015', '数字跳格垫', @cat_balance, '草稿测试商品：待补充正式图片与安全说明。', NULL, 4, 10, 'COORDINATION', 'INDOOR', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 8900, FALSE, NULL, NULL, NULL, 'DRAFT', 150, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001016'), NULL, '户外接力包', @cat_team, '草稿测试商品：SKU 与资料尚待确认。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, FALSE, NULL, NULL, NULL, 'DRAFT', 160, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001017'), 'WM-TARGET-017', '折叠数字标靶（已下架）', @cat_throw, '历史链接保留的下架测试商品。', '该测试商品已下架，仅用于验证旧链接行为。', 5, NULL, 'THROWING', 'BOTH', '测试用织物', '展开 80 × 60 cm', '标靶 1 件', '按标记位置固定后投掷豆袋。', '固定稳妥后使用。', 'seed-img-retired-target', NULL, 10900, FALSE, NULL, NULL, NULL, 'UNLISTED', 170, 0, @now, @now),
(UUID_TO_BIN('20000000-0000-0000-0000-000000001018'), 'WM-BAL-018', '弧形平衡板（已下架）', @cat_balance, '历史链接保留的下架测试商品。', '该测试商品已下架，仅用于验证不可购买状态。', 6, NULL, 'BALANCE', 'INDOOR', '测试用木纹复合材料', '70 × 28 × 12 cm', '平衡板 1 件、防滑垫 1 件', '双脚站稳后缓慢转移重心。', '需成人看护，远离坚硬尖角。', 'seed-img-retired-board', NULL, 27900, FALSE, NULL, NULL, NULL, 'UNLISTED', 180, 0, @now, @now);

INSERT INTO inventory_balances (product_id, quantity, version, updated_at)
SELECT id,
    CASE sku
        WHEN 'WM-BAL-001' THEN 1
        WHEN 'WM-BOWL-003' THEN 0
        WHEN 'WM-TARGET-017' THEN 0
        ELSE 24
    END,
    0, @now
FROM catalog_products;

INSERT INTO inventory_movements (
    id, product_id, direction, quantity, quantity_before, quantity_after,
    reason, source_type, source_id, actor_id, created_at
)
SELECT UUID_TO_BIN(UUID()), product_id, 'INCREASE', quantity, 0, quantity,
       '初始化测试库存', 'INITIAL', BIN_TO_UUID(product_id), NULL, @now
FROM inventory_balances;
