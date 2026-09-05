# 角色 A 验收追踪

| 需求 | 验收点 | 实现位置 | 验证方式 |
|---|---|---|---|
| FR-09 | 成人声明、版本化同意、邮箱唯一、弱密码拒绝 | `RegisterPage.vue`、`UserAccountService.register`、`user_consents` | 注册正反例、重复邮箱、过期版本 |
| FR-10 | 登录、服务端会话、30 分钟空闲、注销、失败封禁 | `AuthController`、`SecurityConfig`、`InMemoryRateLimitService` | 登录/注销手测、限流单测 |
| FR-11 | 本人边界、管理员能力、停用实时生效 | `IdentityPort`、`IdentityService`、Spring Security | 普通用户访问 admin 返回 403；停用后旧会话 401 |
| FR-12 | 查看身份、修改昵称/电话、业务入口 | `ProfilePage.vue`、`AccountController` | 修改/刷新、非法电话、入口呈现 |
| FR-31 | 搜索筛选、详情、停用/恢复、原因、历史 | `AdminUserController`、管理端页面、`account_status_history` | 管理员主路径、管理员目标保护 |
| 公共安全 | Origin、CSRF、Cookie、密码哈希、no-store | 过滤器、安全配置、BCrypt、控制器 | 缺失 Header、Cookie 属性、响应头 |
| 公共一致性 | 统一响应、Problem Details、requestId | `ApiEnvelope`、`ApiExceptionHandler`、`RequestIdFilter` | 字段校验与未知异常响应 |
| 并发/重试 | 乐观版本、行锁、幂等请求键 | `@Version`、`findForUpdateById`、`idempotency_records` | 旧版本 409、同键重放/冲突 |
| 工程化 | 迁移、环境模板、CI、构建说明 | Flyway、`.env.example`、workflow、运行手册 | `mvn verify`、`npm run build` |

## 交付自检

- [x] 课程原始资料未修改。
- [x] 所有角色 A 新产物位于 `partA/`，根目录仅包含获准的 CI 工作流与忽略规则调整。
- [x] 没有硬编码真实账号或生产密钥。
- [x] 经销状态与中心审计通过端口留给角色 D/F，不越权实现。
- [x] 页面具有加载、空、错误、禁用和成功反馈。
- [x] 管理员危险操作要求原因、二次确认、版本号和幂等键。
