# Web 开发技术现状报告

> 课程：软件开发实践 2<br>
> 学生姓名：`【请填写】`<br>
> 学号：`【请填写】`<br>
> 班级：`【请填写】`<br>
> 团队角色：A（身份与工程底座）<br>
> 完成日期：2026 年 9 月 5 日

## 摘要

现代 Web 开发已经从“页面拼接”转向前后端协作、契约治理、安全身份、自动化验证和可持续演进并重的工程活动。本文结合 WEMOVE 网站重构中的成人注册、会话认证、个人资料与后台账户管理任务，调研前端工程化、Java 服务端、API 契约、数据迁移及身份安全的发展现状，并说明本项目采用 Vue 3、TypeScript、Vite、Spring Boot、MySQL、Flyway 与 OpenAPI 的理由。实践表明，框架本身只能提供机制；真正决定系统质量的是清晰的权限边界、服务端校验、事务与幂等策略、可追踪契约和可重复验证。

**关键词：** Web 工程化；Vue；Spring Boot；身份认证；OpenAPI；持续集成

## 1. Web 开发从功能实现走向工程系统

早期网站开发常把重点放在模板、表单提交和数据库增删改查。当前项目通常还需要处理多终端适配、前端状态、API 演进、并发写入、安全威胁、部署配置和团队并行开发。前后端分离提高了职责清晰度，也增加了契约漂移、重复校验和错误语义不一致的风险。因此，现代方案更强调“契约先行”和纵向切片：每个功能同时具备页面、接口、业务规则、持久化和测试，能够独立验证真实数据流。

WEMOVE 的六人分工正适合这种方式。角色 A 不只是制作登录页，还需要提供其他模块可信赖的当前身份、会话失效、统一错误与数据库迁移入口；这使身份模块从孤立功能转变为系统底座。

## 2. 前端技术现状

### 2.1 组件化与类型化

Vue 3 的组合式 API 让状态与业务逻辑按关注点组织，单文件组件把结构、行为与样式保持在可维护范围内。TypeScript 通过静态分析提前发现字段、路由和接口使用错误，尤其适合多人共享 DTO 的项目。Vue 官方说明其工具链原生支持 TypeScript，并建议 Vite 项目用 `vue-tsc` 对单文件组件执行命令行类型检查，而不是误以为打包转换等同于类型检查（[Vue：Using Vue with TypeScript](https://vuejs.org/guide/typescript/overview)）。

本项目使用严格 TypeScript，生产构建先运行 `vue-tsc --noEmit` 再运行 Vite。身份状态集中在 Pinia Store，页面不自行维护多个互相矛盾的“是否登录”标志。Vue 官方已将 Pinia 推荐为新应用的状态管理方案，并指出它相较 Vuex 具有更简洁的 API 和较好的类型推断（[Vue：State Management](https://vuejs.org/guide/scaling-up/state-management)）。

### 2.2 构建与测试

Vite 通过原生模块开发服务和面向生产的打包流程缩短反馈时间。由于 Vite 的 TypeScript 转换不负责完整类型检查，CI 中必须显式执行类型检查。单元测试采用 Vitest，与 Vite 共享转换配置；Vue 官方测试指南也把 Vitest 作为 Vite 项目的推荐选择（[Vue：Testing](https://vuejs.org/guide/scaling-up/testing.html)）。

前端测试不应只验证组件内部变量，还应关注用户可观察行为。本项目的状态徽标同时断言“已启用/已停用”文字与状态类，避免仅用颜色表达；后续团队集成应补充注册、登录、会话过期与管理员危险操作的浏览器级测试。

### 2.3 体验与可访问性

响应式设计已经从若干固定稿转向弹性布局、内容优先和设备能力适配。本项目在 390px、768px 和 1440px 验收宽度下采用可重排网格，后台宽表只允许局部横向滚动。表单始终提供可见标签、错误摘要、字段错误和键盘焦点，动画遵循 `prefers-reduced-motion`。这些实现并不追求展示性动效，而是保证身份流程在移动端和辅助使用场景中仍可完成。

## 3. 服务端与数据技术现状

### 3.1 Java 与 Spring Boot

Spring Boot 继续通过自动配置、Starter、嵌入式服务器和成熟生态降低 Java Web 服务的组装成本。官方当前同时维护 4.x 与 3.5.x 稳定线；最新系统要求页显示 4.1.1 至少需要 Java 17（[Spring Boot：System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)）。本项目选择 Spring Boot 3.5 与 Java 21，是因为 Java 21 是长期支持版本，且 3.5 线对现有 Spring Security、Session、JPA 教学资料与团队经验更友好。选用稳定线不等于固定不升级；依赖升级应通过自动测试和迁移演练逐步完成。

服务端采用 Controller—Service—Domain—Repository 分层，并以端口隔离经销身份和中心审计。这样既利用 Spring 生态，也避免业务规则散落在 Controller 或被框架类型完全绑死。

### 3.2 关系数据库与版本化迁移

身份数据需要唯一性、事务、一致读取和可审计状态历史，关系数据库仍然适合此类核心数据。MySQL 8 的 InnoDB 提供唯一约束、外键、事务与行级锁能力。本项目用规范化邮箱唯一约束保证最终一致性，用行锁和版本号处理管理员并发修改。

数据库结构必须和源代码共同版本化。Flyway 迁移负责建表及索引，JPA 设置为 `validate`，启动时只核对映射而不自动改表。团队不能修改已经执行的迁移，而应追加新版本；这使本地、CI 和交付环境能得到同样结构，也为回退和数据兼容评审留下依据。

### 3.3 API 契约

OpenAPI 把路径、参数、请求体、响应和安全方案变成机器可读契约。OpenAPI 3.1 的 Schema Object 与 JSON Schema Draft 2020-12 对齐，适合表达枚举、格式、组合和严格对象结构（[OpenAPI Specification 3.1](https://spec.openapis.org/oas/v3.1.0.html)）。本项目在编码时同步维护 `openapi.yaml`，采用 `/api/v1`、统一成功信封和 Problem Details 错误；其他成员可在实现未合并前据此开发消费者和契约测试。

契约不是“接口文档的截图”。任何字段或状态语义变化，都需要先判断兼容性，再同步实现、测试和调用方。版本号只能解决路径识别，不能替代变更治理。

## 4. 身份与 Web 安全现状

身份模块面临撞库、暴力登录、会话固定、跨站请求伪造、越权和敏感信息泄漏等问题。OWASP ASVS 将认证、会话、授权、安全架构及错误处理分别列为独立验证领域，说明安全不是登录接口上的单一校验（[OWASP Cheat Sheet Series：ASVS Index](https://cheatsheetseries.owasp.org/IndexASVS.html)）。

本项目采用以下组合措施：

- BCrypt 自适应哈希保存密码，源代码和响应中不出现明文或摘要。
- 服务端 Session 配合 HttpOnly、SameSite Cookie，登录时更新会话标识，退出、超时和停用均可撤销。
- 写请求同时检查 CSRF Token 与 Origin；生产环境要求 HTTPS 和 Secure Cookie。
- 登录按规范化邮箱统计失败次数，注册按来源限流；响应不区分“邮箱不存在”和“密码错误”。
- 后端从认证上下文取得用户 ID，普通用户不能通过请求参数访问他人资料；管理员操作也限制目标类型。
- 停用/恢复使用原因、幂等键、版本号、状态历史和审计端口，兼顾安全、并发与追踪。

需要注意，课程实现中的内存限流只适用于单实例。真实横向扩展环境应把计数与封禁时间放入 Redis 等共享存储，并正确处理反向代理后的可信客户端地址。高风险系统还应加入 MFA、凭证泄漏检测、密码重置与异常登录监测，这些属于本期明确排除的增强范围。

## 5. DevOps 与质量保障现状

持续集成的价值是让每次变更都经过同一组可重复检查，而不是在最终答辩前集中排错。本项目工作流分别执行 Maven `verify`、前端锁文件安装、组件测试和生产构建。环境变量模板只列键名与本地占位值；数据库通过 Compose 启动，Flyway 在应用启动时迁移。

当前测试以快速单元和组件测试为基础，仍需在全组版本中逐步形成测试金字塔：领域规则单测、使用 MySQL 8 的接口集成测试、关键业务浏览器测试，以及根据 SRS 数据量执行的性能与兼容性测试。CI 通过仅说明已配置检查通过，不能等价为需求全部验收；手工可用性、双浏览器兼容和跨模块联调仍需证据。

## 6. 项目实践与反思

角色 A 的实现体现了三点认识。第一，前端路由守卫只是体验层，权限最终必须在服务端验证。第二，“停用用户”不是更新一个状态字段，还涉及并发覆盖、旧会话撤销、业务历史保留、经销身份边界和重复请求。第三，公共底座要通过窄接口服务其他模块：直接共享表结构虽然短期方便，却会放大耦合和越权风险。

本项目仍有改进空间：用共享存储实现分布式限流；对 OpenAPI 执行自动 lint 与契约测试；使用隔离的 MySQL 测试库增加接口集成测试；按页面拆分 UI 组件库依赖以降低首屏包体；把默认日志审计替换为角色 F 的持久化实现；在反向代理和 HTTPS 环境完成 Cookie 与 Origin 回归。这些工作应按风险和验收价值排期，而不是无边界追求技术数量。

## 7. 结论

当前 Web 技术的核心趋势不是某个框架“一统天下”，而是类型化前端、契约化接口、版本化数据、默认安全和自动化交付逐渐成为工程基线。Vue 与 Spring Boot 为 WEMOVE 提供了成熟的开发效率，但高质量结果仍依赖需求追踪、权限边界、事务设计和真实验证。角色 A 的纵向切片把注册、会话、个人资料和管理员状态管理贯通，并给其他五个模块提供稳定接入点，为后续集成建立了可运行基础。

## 参考资料

1. Vue.js. *Using Vue with TypeScript*. https://vuejs.org/guide/typescript/overview
2. Vue.js. *State Management*. https://vuejs.org/guide/scaling-up/state-management
3. Vue.js. *Testing*. https://vuejs.org/guide/scaling-up/testing.html
4. Spring. *Spring Boot System Requirements*. https://docs.spring.io/spring-boot/system-requirements.html
5. OpenAPI Initiative. *OpenAPI Specification v3.1.0*. https://spec.openapis.org/oas/v3.1.0.html
6. OWASP Foundation. *Cheat Sheet Series — ASVS Index*. https://cheatsheetseries.owasp.org/IndexASVS.html
7. WEMOVE 项目组. [*WEMOVE 网站重构需求文档 V1.1*](../../../project-requirements/WEMOVE网站重构需求文档.md)。
8. WEMOVE 项目组. [*团队分工与协作规范*](../../../project-requirements/WEMOVE团队分工与协作规范.md)。
