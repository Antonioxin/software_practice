# WEMOVE 角色 C 实现方案与 A/B 实现检查

> 语境说明：本文保留2026-09-06方案编写轮次的判断；下文“本轮/未实现”属于该方案快照。本任务已依授权实施，当前进展与新测试结果见[验证记录](验证记录.md)。

原方案状态：**方案待用户审核，未实现业务代码**。复核日期：2026-09-06。

## 1. 主线、依据与建议

本轮执行 `git fetch origin` 后，远端 `master` 为 `cb969283d5a0feb31f38571fb500e5443f5a3f5d`。GitHub PR API 确认 [PR #5](https://github.com/Antonioxin/software_practice/pull/5) 为 closed、merged=true，merged_at 为 `2026-09-06T03:55:22Z`，合并提交为上述主线，包含 `d6a3324`。当前干净工作区由 `932de37` 快进至该基线；没有自行合并 PR、提交或推送。

已读取根 AGENTS、根 README、工程 README，以及 [SRS V1.1](../../../project-requirements/WEMOVE网站重构需求文档.md)、[接口协作契约](../../../project-requirements/WEMOVE接口与数据协作契约.md) §5.4/6/7/9/10、[验收追踪表](../../../project-requirements/WEMOVE需求责任与验收追踪表.md)、[成员 C 的 26 条测试设计](../../../tests/cases/成员C测试用例.md)及 [B 审核记录](../catalog/成员B完成情况审核.md)。SRS 已标记评审通过；接口设计、成员用例和本文仍待评审，不能当作已部署能力。

**建议先修公共交易边界，再按购物车→结算建单→付款取消→履约及 F 接口推进。** 目录重构已完成，但受保护快照、返还核验、并发幂等和持久化审计仍需实现。可以先写设计和隔离的 C 规则测试；不能绕过这些前置项宣布交易链路完成。

业务范围为 FR-13—18，以及 F 所需订单引用与指标。普通用户、经销商均按零售价购买；管理员只能管理订单，不能个人购物。整车结算、整单退款/发货；下单不扣减或预占库存；付款使用订单快照价，但重新检查当前可售和库存，任一商品失败全单回滚。支付、退款、物流都明确为模拟，不采集真实支付信息。运费、额外税费和优惠均为 0；无待付款自动关闭、拆单、部分退款或发货后退款执行。

[原方案与历史检查](角色C原方案与历史检查.md)逐字保存上轮版本：业务检查始于 `0157ab2`，后来随 `932de37`/`d6a3324` 调整路径。其“本次测试”和行号只代表历史轮次；本轮结论以下列主线源码与方法名为准。

## 2. A/B 主线复核与修复边界

### 2.1 已能复用的能力

- [WemoveApplication](../../../apps/api/src/main/java/wemove/WemoveApplication.java) 位于 `wemove` 根包；A/B 平级，公共设施在 `wemove.platform`，全局配置在 `wemove.config`。C 分层骨架存在；D 只有根目录，本轮不扩展 D。
- [IdentityService.requireActiveActor](../../../apps/api/src/main/java/wemove/identity/service/IdentityService.java) 每次读取当前账户状态；[SecurityConfig](../../../apps/api/src/main/java/wemove/config/SecurityConfig.java) 和 [http.ts](../../../apps/web/src/services/http.ts) 提供会话、CSRF、统一请求基础。C 仍需检查消费者角色、管理员角色和资源归属。
- [SpringUnitOfWork.run](../../../apps/api/src/main/java/wemove/platform/SpringUnitOfWork.java) 使用 TransactionTemplate；[ProductRepository](../../../apps/api/src/main/java/wemove/catalog/repository/ProductRepository.java) 与 [InventoryBalanceRepository](../../../apps/api/src/main/java/wemove/catalog/repository/InventoryBalanceRepository.java) 已有排他行锁。B 库存服务按 UUID 字符串排序，商品后库存逐项获取锁。
- [V2](../../../apps/api/src/main/resources/db/migration/V2__catalog_inventory.sql) 已有非负余额和 `(product_id,source_type,source_id)` 库存流水唯一约束。它们防重复效果，但不能替代订单状态锁和内容核验。

### 2.2 仍成立的发现及最晚完成阶段

P1/P2 是工程优先级，非 SRS 需求等级。表中是本轮静态复核；B 审核中的 HTTP 复现属于历史证据，本轮未重新运行。

| 项目 | 主线证据与判断 | 责任及开发门槛 |
| --- | --- | --- |
| P1 结算快照未受保护 | [CatalogIntegrationService.getRetailSnapshot](../../../apps/api/src/main/java/wemove/catalog/service/CatalogIntegrationService.java) 仍普通 `findById`，`stock` 也普通读，输入循环未排序；readOnly 事务不会自动取得写保护 | B 在 C2 建单前提供事务内 `lockRetailSnapshot`；C1 加购数量检查也复用该保证。普通展示读可保留。验收必须并发改价/下架，不能只加 C 的外层事务 |
| P1 取消返还信任调用数量 | [InventoryService.restoreForCancellation](../../../apps/api/src/main/java/wemove/catalog/service/InventoryService.java) 只以 exists 检查原扣减，按传入 quantity 返还；deduct/restore 已执行判断也只计输入集合命中数 | B 在 C3 前读取该订单完整扣减/返还流水，比较完整商品集合、数量、方向；原扣 1 返 2、漏项、额外项和异内容重试均拒绝，零额外写入。已下架商品仍可返还 |
| P1 公共并发幂等缺失 | [IdempotencyRecordEntity](../../../apps/api/src/main/java/wemove/platform/idempotency/IdempotencyRecordEntity.java) 只有结果存储；[UserAccountService.changeStatus](../../../apps/api/src/main/java/wemove/identity/service/UserAccountService.java)、[CatalogService.replay/saveReplay](../../../apps/api/src/main/java/wemove/catalog/service/CatalogService.java) 和 InventoryService.adjust 仍先查、业务执行、末尾保存 | A 在 C1 的 K 命令前提供公开执行器、事务外重试/重放；唯一约束冲突不能直接当成业务 UNIQUE_CONFLICT。历史 B 并发是一次效果、其余冲突，并非已证实重复扣库存 |
| P1 B operationId 含资源 | InventoryService.adjust 为 `catalog.adjustStock:<id>`；CatalogService.changePublication 使用 `catalog.publishProduct:<id>` / `catalog.unpublishProduct:<id>` | A/B 在公共幂等切换阶段一并修正；稳定模板标识，具体路径进入摘要。C 不复制旧实现，旧记录兼容见 §7 |
| P1 成功审计只写日志 | [AuditPort](../../../apps/api/src/main/java/wemove/platform/AuditPort.java) 缺 requestId/changeSummary；[LoggingAuditAdapter.append](../../../apps/api/src/main/java/wemove/platform/LoggingAuditAdapter.java) 仅 log.info | A/F 在 C0 固定接口，F 的数据库适配器最迟 C2 事务验收前交付；失败要使业务回滚。可用假适配做隔离开发，不能用日志通过 TC-17/37 |
| P2 能力与加购入口缺失 | UserAccountService.actor 无 C 能力；[ProductDetailPage.addToCart](../../../apps/web/src/pages/ProductDetailPage.vue) 仅提示；[router.ts](../../../apps/web/src/router.ts) 仅注册 A/B | 随 C1 由 A/B/C 接入能力、路由、商品按钮、PublicShell/SiteShell 及个人中心入口，不必阻塞规则设计 |
| P2 业务版本契约差异 | [UserEntity](../../../apps/api/src/main/java/wemove/identity/domain/UserEntity.java) 直接暴露未设初值的 JPA version，B/V2 初始版本也为 0；总契约 §6.3 要求正整数 | C0 登记兼容：旧 A/B DTO 暂按原值不转换，C 独立业务 version 从 1 开始。A/B 版本统一另作迁移/兼容评审，不修改 V1/V2、不在前端统一加 1 |
| P2 输入与错误边界差异 | [application.yml](../../../apps/api/src/main/resources/application.yml) 只显式禁止未知字段，未配置严格整数转换；[ApiExceptionHandler](../../../apps/api/src/main/java/wemove/platform/api/ApiExceptionHandler.java) 不可读 JSON/未知字段为 400，查询类型异常无专用映射 | A 在 C1 API 验收前提供严格数量/金额/版本类型和 422 字段错误；坏 JSON 语法仍 400。小数、字符串数字、未知字段按 C 用例拒绝，避免 DTO 转换后才校验；共享配置变更跑 A/B 回归 |
| P2 库存页面未知结果换键 | [AdminProductsPage.adjustStock](../../../apps/web/src/pages/AdminProductsPage.vue) 每次提交生成新键，catch 显示失败；[http.ts](../../../apps/web/src/services/http.ts) 也没有命令恢复层且不返回成功响应头 | C1 建立可复用命令恢复状态，B 同步修库存入口；联合库存竞争/恢复验收前完成。扩展 HTTP 返回元数据须兼容原 api 调用方 |

B 历史审核中的分类响应旧版本、0 岁筛选、流水仅 20 条、LIKE 通配语义等也没有被包名重构修复。它们继续由 B 按原审核处理；不把这些后台/检索修复扩大为 C 的业务职责。分类修改缺 flush 属于 B API 版本修复；C 响应必须在 flush 后生成。CI 仍未配置，但不阻塞本轮方案评审。

账户并发边界也需 C0 明确：当前 requireActiveActor 是普通读。建议 A 提供事务内受保护的启用身份读取，使停用与交易有明确串行先后；身份锁应在幂等和业务锁之前获取，A 停用流程同样遵守，避免公共执行器插入后再反向取身份锁。交易先取得锁可先完成；停用先提交后交易必须拒绝。经销资格不决定零售价格，不需要锁 D 关系。

## 3. 落盘路径和模块职责

以下路径均从仓库根目录开始，标注文件均为计划新增，不代表实现已存在。

| 路径 | 职责 |
| --- | --- |
| `project-implementation/apps/api/src/main/java/wemove/commerce/api/` | CartController、CheckoutController、OrderController、AdminOrderController、CommerceDtos；请求校验、权限入口、DTO，无任意状态写入 |
| `project-implementation/apps/api/src/main/java/wemove/commerce/domain/` | Cart、CartItem、CheckoutPreview、Order、OrderItem、PaymentAttempt、Refund、OrderHistory 实体及金额/状态规则 |
| `project-implementation/apps/api/src/main/java/wemove/commerce/repository/` | C 自有表、车头/订单行锁、归属过滤和聚合查询 |
| `project-implementation/apps/api/src/main/java/wemove/commerce/service/` | CartService、CheckoutService、OrderCommandService、OrderQueryService；事务编排，通过 B 端口读商品/改库存 |
| `project-implementation/apps/api/src/main/java/wemove/commerce/platform/` | OrdersPort、CommerceMetricsPort 及只读投影；由 C 服务实现，供 F 调用 |
| `project-implementation/apps/web/src/features/commerce/` | api.ts、types.ts、routes.ts、cartStore.ts、commandRecovery.ts 与相邻 `*.spec.ts` |
| `project-implementation/apps/web/src/pages/commerce/` | CartPage、CheckoutPage、OrdersPage、OrderDetailPage、AdminOrdersPage、AdminOrderDetailPage 的 `.vue` 文件 |
| `project-implementation/apps/api/src/test/java/wemove/commerce/` | 规则与 MySQL 集成 `*Test.java`，共用 Maven 入口 |
| `project-implementation/contracts/openapi/commerce.yaml` | 所有 C HTTP schema、K/V、权限、错误、示例；目前尚不存在 |
| `project-implementation/scripts/smoke/commerce-api.ps1` | 独立测试库上的正常链路/恢复/并发脚本；记录启动条件和数据影响 |
| `project-implementation/docs/modules/commerce/` | 本方案、后续运行与验证手册、角色C验收追踪、验证记录；证据进入 `project-implementation/docs/verification/commerce/` |

只复用统一应用、pom、package.json、会话和迁移入口。C 不引用 A/B repository/entity，不直接写商品库存或幂等表；通过 `wemove.platform` 公共服务和 `wemove.catalog.platform` 端口调用。同一应用内 Java 调用参与同一事务，不通过 HTTP 调 B。公共执行器放 `wemove.platform.idempotency`；审计接口继续公共，持久化实现归 F。D 的 `wemove/dealership/` 仅根目录保持不动，不恢复任何 partA/partB/partC 目录。

## 4. 数据库方案

主线迁移只有 V1、V2、V3。V3 是 [一次性旧登录会话撤销](../../../apps/api/src/main/resources/db/migration/V3__invalidate_legacy_principal_sessions.sql)，不是 C 表；本轮比较确认 PR #5 未改 V1/V2。**下一可用号为 V4**，尚未占号：建议公共幂等 V4、审计由 F 协调后续版本、C 使用届时下一个版本 `V<n>__commerce_baseline.sql`，实施前再次 fetch 并核对所有成员在途迁移。不得把旧方案的 V3 当 commerce 迁移，也不改已应用的 V1—V3。

统一 UUID 对应 BINARY(16)，UTC 时间 DATETIME(6)，金额 BIGINT 整数分，业务版本 BIGINT ≥1，文本 UTF-8。业务外键不级联删除用户、商品或历史订单；停用/下架只影响新动作。C 子表外键指向 C 父表，历史表不提供普通删除入口。

| 表 | 字段、约束与索引 |
| --- | --- |
| commerce_carts | id PK、user_id FK users 且 UNIQUE、version 默认 1、updated_at；空车车头保留。首次写入创建车头，唯一冲突后整事务重新读取；GET 无车返回空车逻辑版本 1，不制造 GET 写入 |
| commerce_cart_items | id PK、cart_id FK、product_id FK catalog_products、quantity CHECK 1—99、last_confirmed_unit_price_fen；UNIQUE(cart_id,product_id)。车头锁内保证 ≤20 行，所有增删改更新车头版本 |
| commerce_checkout_previews | id PK、token_hash UNIQUE、user_id FK、cart_id FK、cart_version、snapshot_json（结构固定的完整行与金额）、created_at、expires_at；INDEX(expires_at)，INDEX(user_id,created_at)。随机不透明 token 原值只给当前用户，持久化摘要可在重启后验证；15 分钟有效 |
| commerce_orders | id PK、order_number UNIQUE、user_id FK、preview_id UNIQUE/FK（消费过的预览永不再次建单）、status CHECK 五状态、version、currency 固定 CNY、mode 固定 SIMULATED、subtotal/shipping/tax/discount/total_fen；shipping 地址六字段与 remark 快照；created/paid/cancelled/shipped/completed_at、物流名/运单号、操作者。INDEX(user_id,created_at,id)、INDEX(user_id,status,created_at,id)、INDEX(status,created_at,id)、INDEX(created_at,id) |
| commerce_order_items | id PK、order_id FK、product_id FK、sku/name/unit_price_fen/quantity/subtotal_fen 快照；UNIQUE(order_id,product_id)，CHECK 数量1—99、单价1—99999999、小计=单价×数量 |
| commerce_payment_attempts | id PK、order_id FK、outcome SUCCESS/FAILURE、mode、amount_fen、simulation_reference UNIQUE、actor_id、created_at；success_order_id 可空且 UNIQUE，CHECK 仅 SUCCESS 时等于 order_id，FAILURE 必须为空；INDEX(order_id,created_at,id)、INDEX(outcome,created_at,order_id) |
| commerce_refunds | id PK、order_id UNIQUE/FK、payment_attempt_id UNIQUE/FK、amount_fen、simulation_reference UNIQUE、mode、actor_id、reason、created_at；只保存成功整单模拟退款，金额由成功付款记录读取，不接收客户端退款额 |
| commerce_order_history | id PK、order_id FK、action、from_status/to_status、order_version、actor_id、reason、request_id、created_at；UNIQUE(order_id,order_version)，INDEX(order_id,created_at,id)。创建记录版本1；合法 FAILURE 也递增版本并记录同状态历史 |

表级 CHECK 验证非负金额、固定零费用和头部金额公式；跨行总和、退款等于成功付款额由持订单锁的服务核验。单订单上限 `99999999×99×20=197999998020` 分，Java 用 Math.multiplyExact/addExact，前端只展示，不用浮点金额回算成交额。内部 JPA @Version 若另设命名为 lockVersion，不替代 API business version；响应在 flush 后构建。

付款允许多次显式 FAILURE，所以不能 UNIQUE(order_id,outcome)。发货字段仅在订单 PAID→SHIPPED 时写一次，历史唯一键和订单锁保护，不设部分发货表。预览不存地址；未消费过期预览可清理，已被订单引用的预览按订单保留，不能因清理释放一次消费约束。订单、快照、支付、退款、状态历史长期保留，审计由 F 管理。

## 5. API、DTO 与前端交互

省略前缀 `/api/v1`。U=当前启用非管理员用户（包括经销身份），A=当前启用管理员；私人资源必须本人，其他人同不存在统一 404。K=UUID Idempotency-Key；V=最近读取的业务版本。所有写操作复用 CSRF，身份信息从会话取得，禁止 userId/role/status/price 等未定义字段。

| API | 权限/并发 | 请求与响应要点 |
| --- | --- | --- |
| GET /cart | U | cartVersion、items、当前零售价/数量/行小计、valid/reason、priceChanged、canCheckout；下架/不足项保留，不静默剔除 |
| POST /cart/items | U/K | productId、quantity（累加）；返回更新车及版本，同商品累计 ≤99、≤可售库存、≤20行 |
| PATCH /cart/items/{productId} | U/V | quantity 为绝对值，cartVersion 放 JSON；锁车后校验库存与数量，返回更新车 |
| DELETE /cart/items/{productId}、DELETE /cart/items | U/V | cartVersion 作为必填查询参数（避免 DELETE body）；返回更新车。无实际变化不递增版本，版本过旧仍冲突 |
| POST /checkout-previews | U | 无选择项请求，读取整车；返回 previewToken、cartVersion、expiresAt、currency、完整行和金额构成；空/无效车拒绝 |
| POST /orders | U/K | previewToken、cartVersion、shippingAddress、remark?、clientTotalFen?；首次201，重放200；id/orderNumber/status/version/currency/totalFen/mode/createdAt |
| GET /orders、GET /orders/{id} | U/本人 | 列表 page≥1、pageSize 1—50、status?，默认20；详情为完整不可变快照、尝试/退款/历史、allowedActions |
| POST /orders/{id}/mock-payments | U/K/V | expectedVersion、outcome=SUCCESS/FAILURE；金额由订单读取；返回操作后的订单和本次尝试摘要 |
| POST /orders/{id}/cancel | U/K/V | expectedVersion、reason；返回订单及可能产生的模拟退款摘要 |
| POST /orders/{id}/confirm-receipt | U/K/V | 仅 expectedVersion；只允许本人已发货订单 |
| GET /admin/orders、GET /admin/orders/{id} | A | 分页、status、start/end（UTC [start,end)，过滤创建时间）；详情含履约所需地址 |
| POST /admin/orders/{id}/cancel | A/K/V | expectedVersion、reason；与本人取消共用领域规则 |
| POST /admin/orders/{id}/mock-shipment | A/K/V | expectedVersion、logisticsName、trackingNumber；一次整单发货 |

DTO 名建议为 CartView/CartItemView、CheckoutPreviewView、CreateOrderRequest、ShippingAddress、OrderSummary/OrderDetail、MockPaymentRequest、CancelOrderRequest、ShipmentRequest。返回 ApiEnvelope，错误沿用 Problem Details（code/requestId/errors）；409 区分 CART_CHANGED、PRICE_CHANGED、PRODUCT_UNAVAILABLE、INSUFFICIENT_STOCK、VERSION_CONFLICT、STATE_CONFLICT、IDEMPOTENCY_CONFLICT、REQUEST_IN_PROGRESS；预览失效建议 CHECKOUT_PREVIEW_EXPIRED，需写入 OpenAPI。不可见预览统一404。422 指明字段，401 失效会话，403 动作无权限。库存问题可列出本人本次商品 ID，不泄露他人资源。

地址字段为 recipient/phone/countryOrRegion/region/city/addressLine。收件人 2—50 码点，国家/城市为受控值或2—100，详细地址5—200；电话移除允许的空格、连字符和括号后匹配可选加号及6—20数字。省州是否必填用共同地区配置，非空 region 建议2—100。remark 最多2000，reason 2—500；物流名2—50，运单号3—50且只含字母/数字/连字符。严格校验 JSON 数值类型和未知字段，不能把小数或字符串转为整数后通过。

新增能力建议 CART_READ/CART_WRITE/ORDERS_READ/ORDERS_WRITE（U），ADMIN_ORDERS_READ/ADMIN_ORDERS_WRITE（A），由 A/C 同步契约与 session DTO。路由依次为 `/cart`、`/checkout`、`/account/orders`、`/account/orders/:id`、`/admin/orders`、`/admin/orders/:id`，全部注册到现有 router，能力提示不能替代后端校验。

页面采用已有 PublicShell/SiteShell；桌面和移动导航、个人中心同步接入。商品按钮调用统一 Cart API；游客登录后回原商品页再次确认，管理员隐藏购买动作并由后端拒绝。服务端购物车为权威，操作后失效缓存，退出/切换账户清缓存。整车无勾选结算，失效项可移除；改价显示新旧价并重新预览确认，不自动创建订单。地址错误保留输入，显示零运费/零税费/零优惠。列表含空态、筛选和详情；详情按 allowedActions 展示操作、历史和模拟说明。取消、发货、收货有确认步骤，成功后重新 GET；F 咨询入口只在 F 路由可用时启用。

## 6. 可信预览、事务和状态机

### 6.1 预览与建单

预览在短事务内锁车头，再经 B 受保护端口读取全车商品及库存，核验并服务端精确计价，保存绑定用户/版本/完整内容的预览。没有库存预占；释放锁后库存仍可能变化。15分钟是待评审设计参数，不是订单超时。

建单顺序：认证/授权及输入格式 → 公共幂等执行权或已完成结果重放 → 锁车头，校验预览归属/有效期、车版本/完整商品数量 → B 锁定快照 → 比较预览零售价与当前价、当前可售与库存 → 重新计算金额 → 写订单与商品/地址快照、历史、消费预览关联 → 移除本次整车项并递增版本 → 同事务成功审计及幂等结果 → flush/提交。clientTotalFen 合法但被篡改为100，仍应得到14870分，不能拿客户端总额判断改价。

建单等待期间的新加购和改量由同车头锁排序：加购先提交使旧预览 CART_CHANGED；建单先提交后新加的商品仍保留。失败不删车、不留半份订单；首次创建绝不扣库存。成功重放在车是否为空、预览是否过期等业务条件之前进行。预览一单唯一约束进一步防换键/去重期后误复用，不能取代24小时成功结果重放。

### 6.2 状态及同事务效果

| 原状态 | 合法动作/操作者 | 新状态与原子写入 |
| --- | --- | --- |
| PENDING_PAYMENT | 模拟失败/U本人 | 状态不变，新增 FAILURE 尝试、版本+1、历史/幂等/审计；不扣库存 |
| PENDING_PAYMENT | 模拟成功/U本人 | PAID；按快照金额写唯一成功尝试，B 检查当前 PUBLISHED 和全部库存后扣减，版本/历史/幂等/审计一起提交 |
| PENDING_PAYMENT | 取消/U本人或A | CANCELLED；原因/版本/历史/幂等/审计，无退款、无库存变动 |
| PAID | 取消/U本人或A | CANCELLED；B 依原扣减完整返还、整单模拟退款、版本/历史/幂等/审计全部提交 |
| PAID | 模拟发货/A | SHIPPED；物流名/运单号/操作者/时间、版本/历史/幂等/审计；库存不变 |
| SHIPPED | 确认收货/U本人 | COMPLETED；版本/时间/历史/幂等/审计；库存不变 |
| CANCELLED、COMPLETED | 无新写动作 | 新键拒绝；已成功同键命令经当前授权仍可重放原结果 |

订单命令先取得同一订单行锁，核对归属、expectedVersion、状态，再执行。不同键同版本竞争至多一方生效；同键成功重放不再次校验旧版本。已付后新键再 SUCCESS/FAILURE 均 STATE_CONFLICT，不新增尝试。显式 FAILURE 是完成的业务操作，与库存不足/系统异常导致整个事务回滚不同。

付款先成功，旧版本取消冲突；用户读取新版本并重新确认后仍可合法取消。PAID 取消与发货竞争只可有一种结果：取消/返还/退款全部成立，或发货/原库存全部成立。已下架只阻止新付款，不阻止已付取消的原量返还。付款不因当前零售价变化重新计价。

### 6.3 锁顺序与库存端口修订

统一：必要账户状态 → 幂等执行权 → 车头或订单 → 按 `UUID.toString()` 升序逐商品取“商品锁→该商品库存锁” → 附属表/审计。不要一条路径先锁所有库存、另一条先锁商品。建单只需车头，不锁已有订单；支付只需订单，不反向锁车。B 编辑/上下架、人工库存调整与 C 使用同一商品/库存锁。

B 建议新增 `lockRetailSnapshot(items)`，要求调用方已有读写事务（MANDATORY 或等价断言），返回当前 SKU/名称/零售价/数量/库存/可售投影，持锁至外层提交；缺失商品按 ID 给出不可售结果，不让普通展示读替代建单检查。库存扣减/返还端口也明确必须参与 C 外层事务，禁止 REQUIRES_NEW、异步线程或外部 HTTP 提前提交。

扣减/返还在订单锁保护下读取该 sourceId 的**完整**原流水，不仅查输入项。首次扣减后唯一流水留存；重复调用必须全量匹配后无操作，部分历史作为一致性错误拒绝。返还先核对原扣减集合和数量，写入量取原记录，再在相同商品锁内核查已返还集合，完整匹配才无操作；部分返还/额外项均回滚报错。C 永远从订单快照提供 items，不能让客户端指定库存明细。B 不可反向获取 C 订单锁；它的公开协议要声明订单串行化由调用方负责。

真实 MySQL 下同一事务中先做普通读再锁读，不能使用持久化上下文中旧实体构建快照；受保护查询需确保读取刷新后的数据库状态并验证。死锁/锁超时只能在整事务回滚后有界重试原命令，不能从第二件商品继续。

## 7. 公共幂等与结果未知恢复

A 提供 `IdempotencyExecutor` 作为事务外协调入口，内部每次尝试通过 UnitOfWork 建立新事务；C 传入稳定 operationId、目标资源、规范化请求及业务回调。建议 `commerce.addCartItem/createOrder/mockPayment/cancelOwnOrder/cancelAdminOrder/mockShipment/confirmReceipt`。摘要覆盖方法、具体路径/业务查询和规范化请求体（含版本）；JSON 对象键稳定排序，数组保留顺序，原始敏感体不写日志。same key 改目标/地址/版本均冲突。

具体执行方案：事务内先受保护读取身份，插入唯一 `(actor,operation,key)` 占位并 flush；取得执行权后业务执行，更新完成响应，与业务和审计一起提交。占位绝不单独提交；新增执行状态、可空待完成响应/状态码、completedAt/expiresAt、结果schema版本与资源引用，迁移将旧行标为已完成。并发唯一键等待者在冲突事务完整退出后新事务重新鉴权、读取已提交结果；原执行回滚则可竞争执行。不能捕获唯一异常后继续用 rollback-only 的 EntityManager，也不能用 MySQL REPEATABLE READ 旧快照查不到结果便断言未执行。

只将幂等唯一键冲突转重放，其他唯一约束不能吞掉。建议单次锁等待预算2秒、死锁至多3次整事务尝试，耗尽返回409 REQUEST_IN_PROGRESS/可理解并发冲突和 Retry-After；具体参数在 C0 压测前确认。已提交响应丢失时不自动判断失败。所有完成 K 结果至少保存完成时间后24小时；模拟 FAILURE 也是完成结果。系统回滚不保存成功占位；本期不缓存业务拒绝，环境改变后重新确认使用新键。

B 历史 operationId 兼容不能简单改名/清表。建议迁移旧键为稳定模板并把旧资源标识纳入版本化摘要；若同 actor/key 在多个资源已有历史结果，不能任意挑一个响应，应保留 legacy 记录和对应冲突标记，新入口阻止该键再次产生效果并要求查询现有结果。过渡期须有明确旧摘要校验适配、碰撞清单与回放测试；停止旧版本后一次切换，不能混跑两套去重空间。V3 保留幂等数据，不能借撤销会话规避此兼容。

前端每次用户确认固定 `{actorId,operation,path,key,body,version,startedAt}`；发送中/结果未知不生成新键、不换版本、不修改原体。网络错误、超时、非明确业务拒绝的5xx提示“结果暂未确认”，提供原请求重试和列表/详情查询；同键成功重放200并带 Idempotency-Replayed:true，随后GET当前订单。409 REQUEST_IN_PROGRESS 保留原命令，其他已知冲突刷新并重新确认。PATCH/DELETE 未标K，未知结果先读车对照原绝对目标与版本，不用新版本静默重做。

建议 sessionStorage 按账户隔离短期保存待恢复命令，完成/退出清除，不持久保存会话凭据；涉及地址的原请求仅用于当前会话恢复，不用长期 localStorage。跨会话无原请求时通过订单列表人工核对，不盲目另建单。超过去重期的未知加购等可重复动作禁止自动重发；创建还可依预览唯一关联核查，已有订单动作靠永久状态/唯一约束防重复效果。C1 与 B 共享该恢复模式，但不强迫其他 HTTP 调用全部改成自动重试。

## 8. 给 F 的端口

`OrdersPort.requireOwnedReference(ActorContext ctx, UUID orderId)` 返回 `{id,orderNumber,status}` 最小投影；先当前身份再本人归属，不存在/他人统一404，不返回地址或付款详情。历史取消/完成订单可关联咨询；管理员工单处理按 F 已授权的工单上下文读取其最小关联信息，不扩大此“本人”方法为任意订单查询。

`CommerceMetricsPort.read(start,end)` 必须加入 F 的同一只读事务/数据库快照，不另开事务；调用上下文由 F 管理且仅管理员仪表盘可达。返回 `pendingShipmentCount`（当前全部PAID，与区间无关）、`createdOrderCount`（创建时间落在UTC [start,end)，所有状态）、`netPaidFen`（成功付款在区间内订单原付款额减这些订单截至该快照已完成退款额，十进制整数分字符串）。F 统一生成 asOf；以 BigDecimal/精确SQL聚合后转整数字符串，不能用可能溢出的 long 累加长时间汇总。空值返回0/"0"，start必须早于end。

9月5日付100元、6日退款，重查5日净额为0；不是区间收款减区间退款。两域联测半开时间边界、取消/付款并发时同一快照、待发货总量及无数据；F 的 TC-37 完整通过仍需其他域，不以 C 指标单独代替。

## 9. 分阶段执行和验收

| 阶段 | 可执行交付顺序 | 门槛及用例 |
| --- | --- | --- |
| C0 公共边界 | 再fetch确认迁移号；A/B/C/F评审 OpenAPI/DTO、身份锁、幂等和审计接口；A提供执行器与严格输入，B提供快照/库存核验及旧键兼容方案 | 同键同体并发能重放、异资源冲突；锁快照改价等待/冲突可解释；扣1返2/漏项不写。可先写隔离规则，未通过不接交易持久化 |
| C1 购物车 | C迁移车头/明细，API与页面；能力/商品加购/导航/命令恢复共同接入 | T-C-01—04、数量严格类型、20/21行、99/100、重登录/重启、账户隔离；B未知结果换键修复联测 |
| C2 结算建单 | 预览/订单/明细/历史迁移，可信计价、清车事务、本人查询页；F持久审计到位 | T-C-05—10、12、24—26；14870及最大金额、改价再次确认、创建失败不清车、过期预览成功重放、并发新加购不丢 |
| C3 付款取消 | 支付尝试/退款，订单状态机，B同事务扣减返还 | T-C-11、13—21、26；最后一件、同单竞争、人工调整/下架竞争、第一项后故障全回滚；退款与原扣减精确对应 |
| C4 履约/F | 管理员列表/详情/模拟发货、本人收货；OrdersPort/MetricsPort与F联调 | T-C-20、22—24；取消/发货只有合法终态，管理员不能代收，指标半开区间及历史退款口径准确 |
| C5 联合交付 | MySQL空库/升级/恢复证据、A/B回归、浏览器、运行手册、用例审核执行记录 | TC-11、13—24、34、37、40及相关TC-38/39/41；用例按执行人、版本、环境、时间和证据逐条登记 |

每阶段使用现有 Maven verify 和前端 test/build 入口。集成测试用 `*Test` 纳入 Surefire；若改用 `*IT`，先配置 Failsafe，不能只放文件。MySQL 测试必须真正启用 Flyway：从空库执行全部迁移，从有登录会话及A/B数据的V2升级经V3到最新，以及现有V3到新增版本；核对旧会话撤销、V1/V2校验和、业务/幂等数据保留。不用 H2 模拟替代 MySQL 锁与迁移验收。

并发采用独立会话/连接与屏障，至少覆盖同键建单、同单不同键付款、最后1件、付款/取消、取消/发货、反向商品输入、后台调整/下架。各竞争建议复位后10轮，核对订单/支付/退款/全部库存/历史/幂等/审计的前后值，而非只看响应。故障注入点包括订单头后、清车后、首商品扣减/返还后、支付/退款/历史/幂等/审计写入；失败无任何部分效果，移除故障原键恢复一次。注入只在隔离测试配置开放。

前端验证390/768/1440宽、键盘、两浏览器及模拟标识；覆盖正常收货与已付取消两条链路，响应已丢/请求未到两种未知恢复。性能按追踪表：本人订单预热后20次至少19次≤1秒，购物车5次至少4次≤3秒可操作，由F汇总。26条成员设计仍全部未执行；历史单测通过不改变其状态。

## 10. 评审待定项与本轮验证边界

需要用户/团队审核的设计选择：

1. 采用本文15分钟持久预览、预览一次消费约束、DELETE查询版本，以及具体DTO/能力名/错误码；业务上的整车、零售价、无预占、整单退款和模拟范围已经固定。
2. A/B/F公共边界负责人及交付顺序，尤其旧幂等记录碰撞兼容、身份锁、F数据库审计最晚C2到位；C初期测试替身不算正式验收。
3. A/B旧0起始版本的契约兼容注记、严格转换对旧调用方的影响；实施时同时更新协作契约及OpenAPI，本文不擅自改已部署语义。
4. 地区配置来源/无省州样本、幂等等待与重试预算、sessionStorage短期恢复策略，以及隔离MySQL/可控时钟/故障屏障测试环境。

本轮实际执行：fetch、Git提交/祖先与PR API合并状态核对、工作区快进、源码/契约静态检查、V1/V2与重构前差异检查；仅修改文档，完成Markdown相对链接/表格结构和diff空白检查。未运行应用、npm测试/构建、Maven、MySQL升级、冒烟、并发或浏览器业务验收。

历史证据另见 [Java根包重构验证记录](../../verification/Java根包重构验证记录.md)：9项后端单测+1项H2应用集成、4项前端测试及构建通过；H2禁用Flyway且非真实MySQL升级。原B审核有HTTP复现记录，本文只确认对应代码仍存在，不冒充本轮重现。交付状态仍为“方案待审核”，不登记业务实现完成。
