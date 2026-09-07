# 工单与运营模块（角色 F）

在统一应用内实现 FR-19/20/21（文本工单全流程）与 FR-32（运营总览），并补齐审计记录的只读检索入口。状态机遵循 BR-05：`NEW →(管理员开始处理) PROCESSING`、`NEW/PROCESSING →(公开回复) REPLIED`、`REPLIED →(用户补充) PROCESSING`、任何未关闭状态 `→ CLOSED`（全只读）。处理方即 ADMIN，无独立 SUPPORT 角色。

- [OpenAPI 契约](../../../contracts/openapi/support.yaml)：工单 10 个端点 + dashboard + audit-logs。
- [后端源码](../../../apps/api/src/main/java/wemove/support/)、[审计检索](../../../apps/api/src/main/java/wemove/operations/audit/)、[V9 迁移](../../../apps/api/src/main/resources/db/migration/V9__support_tickets.sql)。
- [前端业务](../../../apps/web/src/features/support/)、[页面](../../../apps/web/src/pages/support/)、[后端测试](../../../apps/api/src/test/java/wemove/support/)。

## 模块结构

| 层 | 位置 | 说明 |
| --- | --- | --- |
| 数据库 | `V9__support_tickets.sql` | `support_tickets` / `support_messages` / `support_internal_notes` / `support_ticket_history` 四张表；不改动 V1—V8（`CommerceMigrationMySqlTest` 校验 checksum 不受影响） |
| 领域 | `wemove.support.domain` | `TicketType`/`TicketStatus` 枚举、`TicketRules` 纯函数（码点校验、状态机、allowedActions）、4 个贫血实体 |
| 仓储 | `wemove.support.repository` | 4 个 JpaRepository；票查询走 Specification；`findForUpdateById` 悲观锁 |
| 服务 | `wemove.support.service` | `TicketCommandService`（全部经 `IdempotencyExecutor`，事务外调用）、`TicketQueryService`（只读）、`TicketJournal`（同事务历史+审计）、`TicketAccess`、`DashboardService` |
| 接口 | `wemove.support.api` | 用户侧 `/api/v1/tickets`（管理员调用一律 403）；管理侧 `/api/v1/admin/tickets`；`DashboardController`；`wemove.operations.audit.AuditLogController` |
| 前端 | `features/support/` + `pages/support/` | 7 条路由：/contact、/account/tickets(+/:id)、/admin、/admin/tickets(+/:id)、/admin/audit-logs |

## 关键设计

- **状态机唯一真理**：所有转移集中在 `TicketRules`（纯函数、可单测），服务层只调用不重复实现；`allowedActions` 由状态推导，前端只按其渲染按钮。
- **内部备注**：不改变工单状态与版本（否则用户补充会无谓 409）、不写状态历史（`support_ticket_history` 有 `UNIQUE(ticket_id, ticket_version)`），仅写审计 `TICKET_NOTE_ADDED`；用户响应中 `internalNotes`/`history` 恒为空数组。
- **限流位置**：`CUSTOMER_CONTACT_WRITES`（20 次/10 分钟，与 D 的联系域共用桶）在幂等执行器**之前**消费——重试也计数，但被限流的请求不写任何业务数据。
- **AFTER_SALES 归属校验**：调用 C 的 `OrdersPort.requireOwnedReference(actor, orderId)`（非本人/不存在 404），与订单服务同事务可重入。
- **PRODUCT 关联**：直查 `catalog_products`（JdbcTemplate 只读）；TODO：B 的公开商品投影端口（`CatalogPort` 类）尚未提供，提供后替换直查。
- **审计写入**：复用 C 落地的 `DatabaseAuditAdapter`（同事务 MANDATORY，业务失败随回滚）；工单审计动作 `TICKET_CREATED/STARTED/REPLIED/FOLLOWED_UP/NOTE_ADDED/CLOSED`，`objectType=TICKET`。
- **审计检索（F 新增）**：`GET /api/v1/admin/audit-logs`（actorId/action/objectType/objectId/时间区间筛选 + 分页）与 `GET /{id}`，只读——不提供任何修改/删除接口，查询本身不写审计。
- **运营总览**：`DashboardService.read` 单个 `@Transactional(readOnly=true)` 事务保证 8 个指标同一快照、共用 `asOf`；`netPaidFen` 为整数分**字符串**（`CommerceMetricsPort` 原样透传，前端转元显示）；活跃用户含管理员（契约口径）；D 域两项（待审合作申请/待处理询价）在 D 接入前固定 0（代码内 TODO 标注）。
- **能力码占位（与 A 的约定）**：不修改 A 的 `UserAccountService` 能力码表。前端路由 `meta.capability` 复用现有码——用户页 `ORDERS_READ`、管理页 `ADMIN_ORDERS_READ`，仅作导航门面；真正授权由后端完成（`/admin/** hasRole(ADMIN)`、用户侧 `requireRole` + 归属校验）。待 A 下发 `TICKET_READ`/`ADMIN_TICKET_READ`/`ADMIN_AUDIT_READ` 等能力码后在 `features/support/routes.ts` 顶部注释处替换。

## 与其他成员的协作点

| 成员 | 事项 | 状态 |
| --- | --- | --- |
| C | `OrdersPort.requireOwnedReference` 用于售后工单归属校验；`CommerceMetricsPort` 提供区间订单数/净收款/待发货 | 已按契约调用，请 C 复核用法 |
| A | 审计检索直接读 `operations_audit_records`（C 建表、F 补查询入口）；能力码未改动 | 请 A 复核审计查询实现与“未改身份能力码”说明 |
| B | PRODUCT 工单需要商品存在性校验，当前直查 `catalog_products` | TODO：B 提供公开投影端口后替换 |
| D | 总览的待审合作申请/待处理询价两项固定 0；联系类写入限流共用 `CUSTOMER_CONTACT_WRITES` 桶 | D 接入后提供端口替换 |

## 测试与验证

- `TicketRulesTest`：码点边界（subject 1/2/100/101、body 9/10/2000/2001、emoji 码点）、电话标准化、类型关联 9 种组合、状态机全转移、allowedActions、版本冲突。
- `SupportMySqlTest`（opt-in，`@EnabledIfEnvironmentVariable MYSQL_TEST_URL` 含 `support_test`）：真实 Flyway + validate + 真实审计适配器；覆盖三类创建与关联校验、401/403/422、全链状态机（T-I-05 场景：NEW→PROCESSING→REPLIED→(备注)→PROCESSING→REPLIED→CLOSED）、归属与版本冲突、幂等重放、审计失败回滚业务、用户关闭默认原因、总览聚合、限流共用桶。
- 前端 `presentation.spec.ts`：查询串构建、标签/badge 映射、分转元、上海时区区间（含跨日界）。
- 验证记录：见[验证证据目录](../../verification/support/)（自动化摘要 + API 走查报告与原始响应）。

## 迁移说明

V9 为首个 support 迁移，全部为**新建表**，不触碰之前版本的任何对象；现有库执行 Flyway migrate 即升级，空库从 V1 顺序建到 V9。

**版本号说明**：本模块迁移最初编写为 V8，2026-09-07 发现 D 的经销合作分支（`codex/feat-dealership-cooperation`）已使用 `V8__dealership_cooperation.sql` 并先行推送，为避免 Flyway 版本撞号改为 V9；V8 由 D 的模块使用。
