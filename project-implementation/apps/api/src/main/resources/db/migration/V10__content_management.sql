-- E: editorial content, public site identity, and ordered home recommendations.
CREATE TABLE content_entries (
    id BINARY(16) NOT NULL PRIMARY KEY,
    kind VARCHAR(12) NOT NULL,
    title VARCHAR(250) NOT NULL,
    summary VARCHAR(500) NOT NULL DEFAULT '',
    body LONGTEXT NOT NULL,
    category VARCHAR(80) NOT NULL DEFAULT '',
    page_description VARCHAR(300) NOT NULL DEFAULT '',
    product_ids VARCHAR(1000) NOT NULL DEFAULT '',
    media_ids VARCHAR(1000) NOT NULL DEFAULT '',
    image_id BINARY(16) NULL,
    button_text VARCHAR(50) NOT NULL DEFAULT '',
    target_url VARCHAR(2048) NOT NULL DEFAULT '',
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    CONSTRAINT chk_content_kind CHECK (kind IN ('ARTICLE','FAQ','BANNER')),
    CONSTRAINT chk_content_status CHECK (status IN ('DRAFT','PUBLISHED','OFFLINE')),
    CONSTRAINT chk_content_sort CHECK (sort_order >= 0),
    CONSTRAINT chk_content_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_content_public ON content_entries(kind,status,sort_order,created_at);
CREATE INDEX idx_content_category ON content_entries(kind,category,status);

CREATE TABLE content_site_settings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    brand_name VARCHAR(100) NOT NULL,
    brand_description LONGTEXT NOT NULL,
    contact_email VARCHAR(250) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    contact_address VARCHAR(250) NOT NULL,
    logo_media_id BINARY(16) NULL,
    terms_text LONGTEXT NOT NULL,
    terms_version VARCHAR(80) NOT NULL,
    privacy_text LONGTEXT NOT NULL,
    privacy_version VARCHAR(80) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE content_home_settings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    recommended_product_ids VARCHAR(1000) NOT NULL DEFAULT '',
    featured_article_ids VARCHAR(500) NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO content_site_settings (id,brand_name,brand_description,contact_email,contact_phone,contact_address,
    terms_text,terms_version,privacy_text,privacy_version)
VALUES (UNHEX('e0000000000040008000000000000001'),'WEMOVE',
    '<p>让陪伴发生，让成长可见。</p><p>WEMOVE 与家庭一起，在运动和游戏中发现成长的可能。我们关注适龄玩法、循序渐进的探索，以及大人与孩子共同参与的快乐。</p>',
    '', '', '',
    '<p>欢迎使用 WEMOVE。请提供真实准确的注册信息，妥善保管账户，并遵守站点使用规则。本网站为软件开发实践课程项目；演示订单不构成真实支付。</p>',
    '2026-09-05',
    '<p>我们仅为账户管理、订单和客户服务处理必要信息。账户资料仅向当前用户和获授权管理员提供。请勿在课程演示站点提交真实敏感资料。</p>',
    '2026-09-05');
INSERT INTO content_home_settings (id,featured_article_ids)
VALUES (UNHEX('e0000000000040008000000000000002'),'e1000000-0000-4000-8000-000000000001');

INSERT INTO content_entries (id,kind,title,summary,body,category,status,sort_order,created_at,updated_at,published_at) VALUES
(UNHEX('e1000000000040008000000000000001'),'ARTICLE','把客厅变成一场小小探险',
 '用简单的路线和亲子约定，让每天十分钟成为探索平衡、协作与想象的游戏时间。',
 '<h2>从一条安全的小路开始</h2><p>在平整、不打滑的地面留出活动空间，移开尖锐和易碎物品。由大人示范，再邀请孩子自己选择起点和终点。</p><h2>让规则跟着孩子成长</h2><p>可以从慢慢走、停下来和绕过标记开始。熟悉后，再加入颜色口令或轮流带路。每次只增加一个小挑战，并允许孩子随时休息。</p><h2>陪伴比闯关更重要</h2><p>关注孩子如何思考、尝试和表达。活动难度应结合孩子的年龄、能力和具体产品说明调整，全程由成人看护。</p>',
 '亲子玩法','PUBLISHED',10,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
(UNHEX('e1000000000040008000000000000002'),'ARTICLE','平衡游戏的进阶思路',
 '从熟悉动作到组合路线，整理下一次亲子活动的灵感。',
 '<h2>先熟悉，再组合</h2><p>本文为待审核草稿。发布前需要补充适龄条件、所需器材和成人看护要点。</p>',
 '运动探索','DRAFT',20,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),NULL),
(UNHEX('e1000000000040008000000000000003'),'ARTICLE','夏日户外活动备忘',
 '已结束的季节活动内容，保留供后台编辑参考。',
 '<p>该季节活动已经结束，文章已下线。重新发布前请核对活动信息和适用条件。</p>',
 '活动资讯','OFFLINE',30,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),NULL),
(UNHEX('e2000000000040008000000000000001'),'FAQ','如何选择适合孩子的产品？','',
 '<p>请先查看商品页面标注的适龄范围、尺寸和安全说明，再结合孩子当前能力与可用空间选择。年龄范围是参考，活动中仍需成人看护。</p>',
 '产品选购','PUBLISHED',10,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
(UNHEX('e2000000000040008000000000000002'),'FAQ','在哪里查看产品使用说明？','',
 '<p>商品详情页提供玩法和注意事项，资料中心提供已经发布的 PDF 说明。可在资料中心按产品筛选；需要特定身份的资料会按权限显示。</p>',
 '产品使用','PUBLISHED',20,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
(UNHEX('e2000000000040008000000000000003'),'FAQ','亲子活动开始前需要做哪些准备？','',
 '<p>检查器材是否完整、地面是否平整防滑，清理周围障碍并留足活动空间。按照产品说明组装，先由成人示范，全程关注孩子的状态。</p>',
 '产品使用','PUBLISHED',30,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
(UNHEX('e2000000000040008000000000000004'),'FAQ','如何咨询订单或售后问题？','',
 '<p>登录后可在客户服务页面提交工单。订单相关问题请关联对应订单，并说明需要帮助的事项；回复会保存在工单对话中。</p>',
 '订单售后','PUBLISHED',40,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
(UNHEX('e2000000000040008000000000000005'),'FAQ','如何了解合作渠道与经销资料？','',
 '<p>可先浏览合作渠道页面。经销专属资料仅对当前合作状态有效的经销账户开放；尚未取得资格时可按页面指引提交合作申请。</p>',
 '合作咨询','PUBLISHED',50,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
(UNHEX('e3000000000040008000000000000001'),'BANNER','一起发现新的玩法','',
 '', '', 'DRAFT',10,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),NULL);
