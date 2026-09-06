# 角色 A 模块设计

## 1. 交付范围

本模块实现需求基线 v1.1 中角色 A 负责的 FR-09、FR-10、FR-11、FR-12、FR-31，并提供六人协作所需的身份和工程公共底座。系统只创建成年普通用户与管理员两种基础角色；经销商是角色 D 维护的派生身份，角色 A 只通过 `DealerIdentityPort` 查询，不修改其状态。

本期明确不包含儿童档案、真实支付、邮件验证、找回密码、多因素认证和在线角色编辑。个人中心中的订单、咨询、经销申请和询价仅提供接入入口，不伪造其他成员的数据。

## 2. 架构边界

```text
Vue 身份页面
      │ JSON / Cookie / CSRF
      ▼
Spring MVC 控制器 ── IdentityPort ── 当前操作者与能力
      │
      ├── UserAccountService ── JPA / MySQL 8
      ├── RateLimitPort ─────── 当前为单实例内存实现
      ├── DealerIdentityPort ── 默认返回 USER，角色 D 可替换
      └── AuditPort ─────────── 默认结构化日志，角色 F 可替换
```

后端采用分层结构：`api` 只处理 HTTP 语义，`service` 编排用例，`domain` 保存核心状态，`repository` 管理持久化，`platform` 暴露团队集成端口。所有表由 Flyway 迁移创建，JPA 仅执行 `validate`，避免运行时偷偷修改数据库。

## 3. 关键决策

| 决策 | 实现 | 原因 |
|---|---|---|
| 身份凭证 | 服务端 Spring Session + `HttpOnly` Cookie | 避免令牌暴露给前端脚本，支持管理员立即撤销会话 |
| 会话时限 | 空闲 30 分钟 | 与需求一致，访问会刷新最后访问时间 |
| 写请求防护 | SameSite=Lax、Origin 白名单、CSRF Header 三层校验 | 降低跨站请求伪造风险 |
| 密码 | BCrypt cost 12 | 数据库只保存不可逆哈希 |
| 权限 | 基础角色 + 服务端能力集合 | 路由守卫改善体验，后端始终作为最终授权点 |
| 状态修改 | 悲观锁 + JPA `@Version` + `expectedVersion` | 避免两名管理员覆盖彼此操作 |
| 重复提交 | `Idempotency-Key` 与请求摘要持久化 | 网络重试返回原结果，键复用不同内容返回冲突 |
| 删除策略 | 停用而非删除 | 保留订单、工单和合作等业务历史 |
| 错误格式 | RFC 9457 风格 Problem Details + 稳定 `code` | 便于前端字段映射、日志检索和跨模块统一 |

## 4. 数据模型

- `users`：登录邮箱、标准化邮箱、密码哈希、昵称、电话、基础角色、账户状态和乐观锁版本。
- `user_consents`：成人确认时间、使用说明版本、隐私说明版本及同意时间。
- `account_status_history`：停用/恢复前后状态、操作者、原因和时间。
- `idempotency_records`：操作者、操作名、请求键、请求摘要和已返回结果。
- `SPRING_SESSION*`：服务端会话及其属性；可按过期时间清理。

邮箱用 `strip + lower-case` 生成唯一身份键，但保留用户输入的展示邮箱。手机号可为空，保存前移除空格、短横线与括号。昵称按 Unicode code point 校验 2—30 个字符。

## 5. 安全与数据隔离

1. 注册只产生 `USER`，外部请求不能提交角色或状态；未知 JSON 字段会被拒绝。
2. 登录连续失败 5 次（10 分钟窗口）后封禁 10 分钟；注册按来源限制为 5 次/10 分钟。
3. 每个受保护用例都从服务端 `Authentication` 解析操作者，不接受客户端提供的用户 ID。
4. 普通用户资料接口只能更新本人；管理员列表/详情/状态命令要求 `ADMIN`。
5. 管理员状态命令只能作用于基础角色 `USER`，不能停用管理员。
6. 停用事务在提交前删除目标主体的所有 Spring Session；恢复不会重建旧会话，也不会改变角色 D 的经销状态。
7. 响应设置 `Cache-Control: no-store`，每个请求生成 `X-Request-Id`；未知异常只向客户端返回通用信息。

当前 `InMemoryRateLimitService` 适合课程单实例演示。多实例部署时必须用 Redis 等共享存储替换 `RateLimitPort`，否则各实例计数相互独立。

## 6. 前端设计

界面延续原站的暖木色、深绿和编辑式排版，复用 `material` 中的 WEMOVE 标志并复制到模块公共资源。登录与注册采用品牌叙事/表单双栏；个人中心强调本人身份与必要资料；管理员页面用筛选表格、状态徽标、详情时间线和二次确认完成高风险操作。

所有关键状态同时使用文字与颜色表达；表单控件具有可见标签、错误摘要和键盘焦点；移动端在 760px 以下切换为单列。`prefers-reduced-motion` 会关闭非必要动画。

## 7. 团队集成接口

- 角色 B/C/D/E/F 获取当前操作者：依赖 `IdentityPort.requireActiveActor(authentication)`，不要信任请求体中的 `userId`。
- 角色 D 接入派生身份：实现 `DealerIdentityPort` 并替换默认 `NoDealerIdentityAdapter`。
- 角色 F 接入中心审计：实现 `AuditPort`；A 自有的 `account_status_history` 继续作为状态事实记录。
- 事务协作：需要原子操作的模块可注入 `UnitOfWork`，但跨服务流程应使用事件/补偿，不能假设分布式 ACID。
- 路由注册：前端入口集中在 `identityRouteRegistrations`，团队合并时可将数组挂到主路由。

完整 HTTP 契约见 `project-implementation/contracts/openapi/identity.yaml`。
