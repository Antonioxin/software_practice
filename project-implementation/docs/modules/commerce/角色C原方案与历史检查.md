# WEMOVE 角色 C 实现方案与 A/B 实现检查

日期：2026-09-06。业务检查基线：`0157ab2`；路径已按远端 `932de37` 及本次 Java 根包重构更新。原检查记录保留，源码行号仅对应原基线。

本文依据现有源码、SRS、接口契约及 A/B 验证记录制定实施建议。原检查仅做设计；后续包名重构不改变业务逻辑，C 尚未实现。代码中可确认的问题与尚未执行的运行验证分别记录。

## 1. 结论与实现边界

**A/B 已形成 C 可复用的应用底座，但交易所需的锁定快照、幂等并发和库存返还核验仍需补齐。建议 C 直接接入当前单体应用。**

- A 已实现注册、登录、账户资料、用户停用/恢复、Cookie 会话、CSRF、权限和统一错误，以及事务与身份端口。
- B 已在同一应用中实现商品列表/详情、分类、商品管理、价格、库存调整与流水，并提供 `CatalogPort`、`InventoryPort`。
- C 负责 FR-13—FR-18：持久化购物车、整车结算、订单、模拟付款、取消退款、模拟发货和本人收货；承担金额、状态机和交易一致性验证。
- 普通用户与经销商均按零售价购买；管理员只管理订单，不发起个人加购、结算和付款。
- 运费、额外税费、优惠均为 0；不接真实支付或物流，不拆单、不部分退款，不自动关闭待付款订单。
- D/E/F 尚未接入的能力通过公开端口衔接；F 的真实审计是最终一致性验收依赖。

参考：[SRS](../../../project-requirements/WEMOVE网站重构需求文档.md)、[接口契约](../../../project-requirements/WEMOVE接口与数据协作契约.md)、[验收追踪表](../../../project-requirements/WEMOVE需求责任与验收追踪表.md)。

## 2. A/B 检查结果

### 2.1 可以复用的实现

| 范围 | 代码证据 | 对 C 的意义 |
| --- | --- | --- |
| 单一应用 | [A README](../identity/README.md)、[B README](../catalog/README.md) | Vue 3 / TypeScript / Element Plus，Java 21 / Spring Boot / MySQL / Flyway；无需复制应用 |
| 当前身份 | [IdentityService](../../../apps/api/src/main/java/wemove/identity/service/IdentityService.java) | 每次请求读取当前账户状态；C 再校验角色与订单归属 |
| 请求安全 | [SecurityConfig](../../../apps/api/src/main/java/wemove/config/SecurityConfig.java)、[http.ts](../../../apps/web/src/services/http.ts) | 复用会话、CSRF、Problem Details、请求 ID 和幂等请求头 |
| 事务 | [SpringUnitOfWork](../../../apps/api/src/main/java/wemove/platform/SpringUnitOfWork.java) | C 用例建立外层事务，B 的 Spring 事务调用加入同一事务 |
| 商品写锁 | [ProductRepository](../../../apps/api/src/main/java/wemove/catalog/repository/ProductRepository.java)、[CatalogService](../../../apps/api/src/main/java/wemove/catalog/service/CatalogService.java) | 商品编辑和上下架已锁定商品，C 需通过 B 的受保护快照使用同一锁边界 |
| 库存保护 | [InventoryService](../../../apps/api/src/main/java/wemove/catalog/service/InventoryService.java)、[V2 迁移](../../../apps/api/src/main/resources/db/migration/V2__catalog_inventory.sql) | 按商品 ID 排序扣减、商品/库存行锁、非负约束、业务流水唯一约束已具备 |

### 2.2 接入前应处理的问题

以下 P1 表示交易正确性或契约问题，P2 表示接入和交付缺口；不是已经复现全部并发故障的声明。

| 优先级 | 发现与证据 | 影响及处理建议 |
| --- | --- | --- |
| P1 | `CatalogIntegrationService.getRetailSnapshot` 第 34—48 行使用普通 `findById`，库存也是普通读取，且未排序锁定 | C 创建订单时可能读完后遭遇改价/下架。由 B 增加事务内锁定快照端口，按统一商品 ID 顺序锁商品及库存，保持至 C 提交；不能只给 C 外层加 `@Transactional` 就视为解决 |
| P1 | `InventoryService.restoreForCancellation` 第 111—131 行只检查扣减流水存在，按调用参数数量返还 | 原扣 1 件、传入 2 件也会返还 2 件，未满足“仅按原扣减记录返还”。B 应读取该订单完整原扣减明细，以原记录为权威并核对商品集合及数量；重复扣减/返还也应核对原始内容 |
| P1 | A `changeStatus`、B `adjust` / `saveReplay` 都是先查幂等记录、执行业务、最后保存；没有公共幂等执行器 | 数据库唯一约束能让冲突事务回滚，但并发同键请求未保证重放成功结果，可能先遇版本冲突或 `UNIQUE_CONFLICT`。C 不应照搬；先补统一的并发协调及整事务重试/重放入口 |
| P1 | B 的 `catalog.adjustStock:<productId>` 和发布操作把目标 ID 放进 operationId | 同键用于另一商品会变成另一去重作用域，不能按契约报同键异内容冲突。改为稳定 operationId，具体路径/资源 ID 纳入摘要；已有记录需要兼容策略，不能直接改名导致历史重试再次执行 |
| P1 | [LoggingAuditAdapter](../../../apps/api/src/main/java/wemove/platform/LoggingAuditAdapter.java) 第 11 行只写日志；AuditEvent 缺 `requestId`、`changeSummary` | 日志不能随数据库事务回滚，也不能证明成功审计已持久化。通过 F 所有的 `AuditPort` 接入同事务数据库适配器；本地日志适配器只能用于开发，不能作为 TC-17/37 完成证据 |
| P2 | `UserAccountService.actor` 第 171—175 行仅分配身份和商品相关能力；商品详情第 36—38 行加购只是消息提示 | 增加 C 能力、路由和导航；商品按钮调用 C 的统一购物车服务，未登录先登录再回原商品页 |
| P2 | B 的 JPA `version` 和 V2 初值为 0，而总契约规定正整数版本 | 联调前明确现有 A/B 版本兼容口径，C 新资源使用从 1 开始的业务版本；不要在 C 前端统一加减 1，也不要直接改已应用 V1/V2 |
| P2 | 原检查时根 README/AGENTS 仍称无应用；A 历史记录提到 CI，但未发现 `.github` 工作流 | 远端目录重组已更新入口并澄清 CI 未配置；CI 本身仍待落实 |

另一个重要边界：B 的库存去重检查在商品锁之前，不能替代 C 对订单的串行保护。C 必须先锁定订单再校验版本/状态，所有订单动作都走同一用例入口。

### 2.3 本次验证与证据边界

- 已阅读 A/B 服务、端口、迁移、前端接入点、单测及验证脚本/文档。
- 已执行 `project-implementation/apps/web` 的 `npm run test`：2 个测试文件、4 个用例通过。测试覆盖状态标签和商品展示辅助函数，不代表购物车/交易已验证。
- 前端生产构建结果见本文末尾的补充记录。
- 当前 PATH 无 `mvn`，仓库无 Maven Wrapper；Docker Desktop Linux 引擎连接失败。本次未运行后端 `mvn verify` 或真实 MySQL API 冒烟，未变更环境和业务数据库。
- A 文档历史记录 4 个后端测试通过；B 文档历史记录集成后共 9 个后端测试及 MySQL API/浏览器验证通过。本次未复现这些历史结果。
- 现有 B 测试验证字段规则与单个库存实体运算，没有订单服务、多商品事务回滚或交易竞争测试；C 必须补真实数据库证据。

## 3. 工程组织与协作改动

沿用最新统一工程：代码进入 `project-implementation/apps`，C 的设计与验收材料放入 `docs/modules/commerce`，契约和脚本分别进入工程的 `contracts/openapi` 与 `scripts/smoke`。Java 根包为 `wemove`，C 与 A/B 平级。下列是计划位置，尚不代表 C 已实现。

```text
project-implementation/apps/api/src/main/java/wemove/commerce/
    api/          CartController、CheckoutController、OrderController、AdminOrderController、DTO
    domain/       购物车、预览、订单、付款、退款、状态历史
    repository/   C 自有数据访问、订单与车头行锁
    service/      CartService、CheckoutService、OrderCommandService、OrderQueryService
    platform/     OrdersPort、CommerceMetricsPort
project-implementation/apps/web/src/features/commerce/
    routes.ts、api.ts、types.ts、购物车缓存与命令重试状态
project-implementation/apps/web/src/pages/
    CartPage.vue、CheckoutPage.vue、OrdersPage.vue、OrderDetailPage.vue
    AdminOrdersPage.vue、AdminOrderDetailPage.vue
project-implementation/apps/api/src/main/resources/db/migration/
    V<下一版本>__commerce_baseline.sql
project-implementation/apps/api/src/test/java/wemove/commerce/
project-implementation/contracts/openapi/commerce.yaml
project-implementation/docs/modules/commerce/角色C模块设计.md、角色C验收追踪.md、运行与验证手册.md、验证记录.md
project-implementation/scripts/smoke/commerce-api.ps1
```

本次包名重构新增 V3 撤销旧登录会话；C 从下一可用版本开始。若公共幂等/审计先新增迁移，C 顺延版本。所有升级新增迁移，不改已应用版本。

协作改动分开评审：A 的公共幂等/能力/错误处理；B 的锁定快照和返还核验；F 的审计适配；C 的交易实现。C 只导入其他域公开端口与 DTO，不直接操作商品库存表，也不直接使用 A 幂等 repository。

## 4. 数据设计

| 表（建议名） | 关键字段和约束 |
| --- | --- |
| `commerce_carts` | id、user_id 唯一、业务 version、更新时间；首次建车也必须处理并发唯一约束 |
| `commerce_cart_items` | cart_id、product_id、quantity，(cart_id, product_id) 唯一，数量 1—99；最近确认价格用于提示变化，展示仍以当前价为准 |
| `commerce_checkout_previews` | 不可猜测 token、user_id、cart_version、完整商品/数量/零售价快照、created_at、expires_at；15 分钟有效，重启后仍可验证 |
| `commerce_orders` | id、唯一 order_number、user_id、状态、业务 version、币种、金额构成、完整收货快照、备注、模拟标识、各动作时间、发货信息 |
| `commerce_order_items` | order_id、product_id、SKU/名称/零售单价/数量/小计快照，(order_id, product_id) 唯一 |
| `commerce_payment_attempts` | order_id、结果、模拟交易标识、快照付款额、时间；允许多次失败；成功效果有数据库唯一约束 |
| `commerce_refunds` | order_id 唯一、模拟退款标识、整单原付款额、时间、操作人 |
| `commerce_order_history` | order_id、动作、前后状态、业务版本、操作人、原因、时间、request_id；每次有效动作仅一条 |

订单上的成功付款引用/唯一成功记录可用于实现“一单一次成功”；不要对 `(order_id, outcome)` 简单加唯一约束，否则会禁止不同请求的多次失败记录。可选实现是付款表的可空成功订单键：仅 SUCCESS 填入 order_id 并加唯一约束，FAILURE 填空。

- 金额使用 Java `long`、MySQL `BIGINT`，单位分；用精确整数乘加并检查溢出。最大整单金额为 197,999,998,020 分。
- 商品最多 20 行是跨行规则，在车头锁内检查；数据库负责行数量、唯一键、非负金额、引用完整性等约束。
- 购物车及订单业务版本从 1 开始；购物车行的增删也更新车头版本。若另设 JPA `@Version` 作为内部锁版本，应与 API 业务版本清楚区分。
- 用户列表索引 `(user_id, created_at, id)`，状态列表补相应复合索引；稳定按创建时间和 ID 倒序分页，每页 1—50。
- 订单快照永不从用户资料或当前商品反向覆盖。后台不提供订单任意编辑接口。

## 5. API、权限与页面

API 统一前缀 `/api/v1`，沿用契约 5.4 的路径，K 表示 UUID 幂等键，V 表示版本校验。

| API | 主要实现 |
| --- | --- |
| `GET /cart` | 本人车、当前零售价、行有效性及原因、总额、cartVersion；下架/缺货行保留 |
| `POST /cart/items`（K） | 同商品累加，检查合并后的数量、库存及 20 行上限 |
| `PATCH /cart/items/{productId}`（V） | quantity 表示设置值，不是增量 |
| `DELETE /cart/items/{productId}`、`DELETE /cart/items`（V） | 删除或清空；版本随请求传入，具体位置在 OpenAPI 固定 |
| `POST /checkout-previews` | 整车有效才返回服务器预览、token、版本、金额与过期时间 |
| `POST /orders`（K） | token、cartVersion、地址、备注；首次 201，同键成功重放 200 + `Idempotency-Replayed: true` |
| `GET /orders`、`GET /orders/{id}` | 本人分页/状态筛选、完整快照、付款/退款/历史及 allowedActions |
| `POST /orders/{id}/mock-payments`（K/V） | outcome 为 SUCCESS 或 FAILURE |
| `POST /orders/{id}/cancel`、`/confirm-receipt`（K/V） | 取消原因必填；本人操作与状态限制 |
| `GET /admin/orders`、`GET /admin/orders/{id}` | 管理员按状态/时间筛选和核对订单 |
| `POST /admin/orders/{id}/cancel`、`/mock-shipment`（K/V） | 管理员整单取消或模拟发货，记录物流名称及运单号 |

所有入口调用 `IdentityPort` 检查当前启用状态；消费者端明确拒绝 ADMIN，查询按 actorId 限制归属。他人的私有订单统一 404。管理员权限不能只依赖页面按钮。

建议新增能力名 `CART_READ`、`CART_WRITE`、`ORDERS_READ`、`ORDERS_WRITE`、`ADMIN_ORDERS_READ`、`ADMIN_ORDERS_WRITE`，由 A 和 C 写入共同契约后实现。用户拥有前四项，管理员拥有后两项。

页面路由：`/cart`、`/checkout`、`/account/orders`、`/account/orders/:id`、`/admin/orders`、`/admin/orders/:id`。复用 PublicShell/SiteShell，补桌面/移动端购物车、我的订单、订单管理入口。

- B 的加购按钮调用 C 的 `api.ts`；服务端是购物车权威，客户端成功后刷新/失效缓存；退出或账户切换清理缓存。
- 结算显示全部金额构成、地址字段错误、价格变化再次确认；无效项保留且阻止整车提交。
- 订单详情集成付款、取消、收货及历史，不必为每个动作另建页面；所有支付/退款/发货明确显示 SIMULATED 和“不发生真实扣款”。
- 超时视为结果未知：保留原键和原请求，不自动改版本或换键重发；用同键恢复创建结果，再 GET 订单当前状态。已知版本/价格冲突则刷新并经用户再次确认后建立新命令。
- 收货地址和发货字段逐项继承 SRS/契约限制；省州必填依据同一地区配置，取消原因 2—500 字符，备注最多 2000 字符。

## 6. 事务、幂等与状态机

### 6.1 公共幂等实现建议

由 A 提供公开 `IdempotencyExecutor`，C 传入稳定 operationId、目标资源、规范化请求、业务回调和返回类型。规范化摘要包含 HTTP 方法、具体路径和业务参数；不只对 DTO 拼接字符串。

一种可实施方案：在业务同一事务中插入唯一占位记录并立即 flush，取得执行权；随后完成业务，更新为成功结果后一起提交。占位未完成不得独立提交。此方案需要新增迁移支持执行状态及尚无结果的记录。

并发唯一冲突必须让当前事务完整结束，再在新事务中重读原结果；不能在已标记回滚的 JPA 事务里继续操作。锁等待超出预算返回 `409 REQUEST_IN_PROGRESS`；死锁仅有界重试整个事务并保留原键。A 的异常映射需识别这些情形，避免统一变成 500 或普通唯一冲突。

每次重放前重新鉴权并核对归属；成功订单创建的重放先于购物车非空、版本和预览有效性检查。结果至少保留 24 小时，订单动作的永久一次效果由状态和业务唯一键保证。

### 6.2 创建订单

1. 检查当前启用用户，进入公共幂等和外层事务；已完成同键请求直接重放。
2. 验证预览归属与有效期，锁车头，比较 cartVersion、完整商品集合及数量。
3. 调 B 锁定快照，核验当前上架、库存和预览价格；分别返回 `CART_CHANGED`、`PRICE_CHANGED`、`PRODUCT_UNAVAILABLE`、`INSUFFICIENT_STOCK` 等错误。
4. 服务端计算金额，写待付款订单、商品及地址快照、初始历史；移除本次结算项，递增车头版本，保存幂等结果和成功审计。
5. 一起提交。任一写失败全部回滚；不扣减、不预占库存。`clientTotalFen` 无论传什么合法值均不参与计价。

### 6.3 付款、取消与履约

| 原状态 | 动作 | 新状态 / 同事务效果 |
| --- | --- | --- |
| PENDING_PAYMENT | 本人模拟失败 | 保持原状态，记录 FAILURE 和有效修改版本，不扣库存 |
| PENDING_PAYMENT | 本人模拟成功 | 锁订单，再调 B 扣全单库存；付款成功、PAID、历史、幂等、审计一起提交 |
| PENDING_PAYMENT | 本人/管理员取消 | CANCELLED，记录原因，不动库存 |
| PAID | 本人/管理员取消 | CANCELLED，B 按原扣减返还一次，整单模拟退款与历史一起提交 |
| PAID | 管理员模拟发货 | SHIPPED，保存模拟物流与操作人，不动库存 |
| SHIPPED | 本人确认收货 | COMPLETED，不动库存 |
| CANCELLED / COMPLETED | 状态写入 | 拒绝；同键已成功命令仍按授权重放 |

每次动作都在订单行锁内检查 expectedVersion 和合法状态。付款使用订单快照价，但必须检查当前可售状态和库存。库存不足等业务拒绝整体回滚；显式 FAILURE 是合法的模拟失败记录，与系统故障回滚区分。

同键同内容重放不新增记录；新键对不允许状态执行动作返回 `STATE_CONFLICT`，并发陈旧版本返回 `VERSION_CONFLICT`。失败模拟会使版本变化，下一次成功付款须读取新版本并使用新键。

锁顺序统一为：必要身份状态 → 幂等 → 车头/订单 → 按 ID 排序的商品与库存 → 附属记录/审计。C/B 使用相同数据库、事务管理器与线程，不另开事务提交库存，不在事务中发 HTTP 请求调用 B。

## 7. 给 F 的最小接口

- `OrdersPort.requireOwnedReference(ctx, orderId)`：返回订单 ID、编号及必要状态的最小投影，验证本人归属；不把完整地址交给工单。
- `CommerceMetricsPort.read(start, end)`：待发货总量、区间创建订单数、区间模拟净成交额；参与 F 的同一只读数据库快照。
- `netPaidFen` 返回整数分字符串。口径为区间内成功付款订单原额，减这些订单截至统计时已退款额；不是“区间付款减区间退款”。
- 保留 F 售后咨询入口接入位置；F 未实现时不指向不存在的可提交页面。

## 8. 开发顺序与完成判据

以下阶段按依赖顺序推进，每阶段拆成可独立评审的小增量；不以工期估算代替完成证据。

| 阶段 | 交付物 | 完成判据 |
| --- | --- | --- |
| C0 接入基线 | OpenAPI、数据库方案；A 公共幂等、B 受保护快照/返还核验、能力与审计对接约定 | 端口契约可调用；同键并发、数量不匹配和快照锁测试通过；正式审计未到位时明确仅可开发 |
| C1 购物车 | 车头/明细迁移、API、页面、商品加购入口 | TC-13；重复加购、数量/库存/20 行边界、重登录、两用户隔离 |
| C2 结算建单 | 持久预览、可信金额、订单快照、清车事务、订单列表/详情 | TC-14—17、TC-21；篡改合计仍 14870 分，同键仅一单，失败不清车 |
| C3 模拟支付和取消 | 状态机、支付尝试、退款、B 库存联动 | TC-18—22；竞争库存不为负，整单回滚，取消仅返还一次 |
| C4 履约与 F 接口 | 后台列表/详情、模拟发货、本人收货、订单引用与统计 | TC-23—24；发货/取消竞争只产生合法结果；本人归属与统计口径正确 |
| C5 联合验收 | 自动化/浏览器证据、运行手册、追踪表、讨论结论 | TC-11、13—24、34、37、40 相关部分，全部关键交易测试及 A/B 回归通过 |

## 9. 必须执行的测试

沿用现有正式入口：前端 `npm run test`、`npm run build`，后端 `mvn verify`。新增测试应让该入口实际执行；若使用 `*IT` 命名，需配置 Maven Failsafe，不能仅放文件就声称已覆盖。

| 类型 | 场景与断言 |
| --- | --- |
| 规则单测 | 金额 2990×3+5900=14870、最大整单金额、数量/地址边界、合法与非法状态迁移 |
| MySQL 集成 | 从空库跑全部迁移、从 V2 升级、真实 Spring 事务及约束、重启后购物车/订单/预览保留 |
| 幂等 | 并发同键建单、同键异内容/异订单冲突、成功清车后重放、预览过期后成功结果重放、旧键在停用后不能访问 |
| 同订单竞争 | 同单两次支付、支付与取消、取消与发货；检查订单、付款/退款、全部库存流水、历史、幂等、审计一致 |
| 库存竞争 | 库存 1 的两用户付款、付款与后台减库存/下架竞争、多商品反向输入；至多一个有效扣减且余额非负 |
| 故障注入 | 清车后、首项扣库存后、退款/历史/幂等/审计写入失败；核对全部相关表回滚，注入只在测试配置启用 |
| 权限 | 未登录、停用、普通用户访问后台、管理员加购/结算/付款、交叉用户订单查询及动作、CSRF 缺失 |
| 快照与返还 | 建单后商品改名改价仍保留原订单；下架后可取消已付款单；扣 1 返 2、漏项、异明细重试被拒绝且无额外效果 |
| 浏览器 | 加购→结算→付款失败→成功→发货→收货；另走付款后取消；改价再次确认、超时原键恢复、移动端及键盘操作 |

并发测试用同步屏障在不同连接/事务上同时提交，保留请求、响应及数据库前后值；不以串行循环代替竞争。测试使用隔离 MySQL 库，勿在日常业务库执行写入冒烟。F 的 TC-37 完整验收仍需其他域加入，C 只提交本域证据。

## 10. 本次交付检查补充

- `npm run build` 通过，包含 `vue-tsc --noEmit` 类型检查和 Vite 生产构建，1471 个模块完成转换。
- 本文相对链接全部可解析，无行尾空白；`git diff --check` 通过。本文为新文件，另行扫描了其正文，避免仅依赖不包含未跟踪文件的 diff 检查。
- 以上为原方案检查结果；后续包名迁移与验证另见 [根包重构验证记录](../../verification/Java根包重构验证记录.md)。
