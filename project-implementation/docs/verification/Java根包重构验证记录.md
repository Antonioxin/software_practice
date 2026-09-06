# Java 根包重构验证记录

日期：2026-09-06。基线：远端主线 `932de37`；工作分支：`feature/flatten-wemove-modules`。

## 变更结果

- 已快进拉取 PR #4 的统一工程目录，应用继续位于 `project-implementation/apps/web` 和 `apps/api`。
- Java 根包改为 `wemove`，启动类为 `wemove.WemoveApplication`；身份 `identity` 与商品库存 `catalog` 平级，后续 C 使用 `wemove.commerce`。
- 公共 HTTP 响应/异常移入 `wemove.platform.api`，事务与限流等入口移入 `wemove.platform`，公共幂等实体及 repository 移入 `wemove.platform.idempotency`，全局请求过滤器移入 `wemove.platform.security`。
- 全局 SecurityConfig / WemoveProperties 移入 `wemove.config`；账户初始化 AdminBootstrap 留在身份模块。Maven groupId 为 `wemove`，应用名为 `wemove-api`。
- 原有 Java 源码与单测共 62 个文件调整包声明、路径及导入。商品、账户规则与 HTTP 契约保留；本次不处理 A/B 审核中列出的交易业务缺陷。
- C 方案从本地 `partC/docs` 移入 [commerce 模块资料](../modules/commerce/README.md)，并更新路径、包名、迁移版本与工程入口。
- 更新根 README、AGENTS、工程 README、协作规范及已有模块的源码链接，明确其他业务不得放入 `identity` 子包。

## 登录会话升级

Spring Session JDBC 保存的身份对象包含 Java 类名。包名改变后，旧登录会话中的 UserPrincipal 和角色枚举不能直接按新类名反序列化。

新增 [V3 迁移](../../apps/api/src/main/resources/db/migration/V3__invalidate_legacy_principal_sessions.sql)，只撤销带 `SPRING_SECURITY_CONTEXT` 的旧登录会话，关联属性通过既有外键级联清除。用户升级后重新登录；匿名会话、账户、商品、库存和幂等业务记录保留。V1/V2 不修改。

部署时先停止旧版本，再启动新版本执行迁移，不混跑旧包与新包应用。新环境同样按顺序执行 V1—V3，空会话库上的 V3 无副作用。下一业务迁移从 V4 或团队协调后的更高版本开始。

## 自动化验证

后端环境：WSL Ubuntu 的 OpenJDK 21 和 Maven；Windows PATH 原本没有 Maven，使用现有 WSL 工具完成验证，无需安装系统级工具。

| 检查 | 命令 / 内容 | 结果 |
| --- | --- | --- |
| 后端 | `mvn -B -ntp -f project-implementation/apps/api/pom.xml clean verify` | 通过，10 项测试，0 失败/错误，生成 `wemove-api-1.0.0.jar` |
| 原有单测 | 账户 2、限流 2、商品规则 3、库存实体 2 | 9 项通过 |
| 新增应用集成测试 | `wemove.ApplicationIntegrationTest`，由同一 Maven 入口执行 | 1 项通过；详见下文 |
| 前端单测 | `npm --prefix project-implementation/apps/web run test` | 4 项通过 |
| 前端构建 | `npm --prefix project-implementation/apps/web run build` | 类型检查和 Vite 构建通过，1471 个模块 |
| 源码、迁移和契约核对 | 原有 Java 方法体、V1/V2、HTTP 契约与基线比较 | 62 个原有 Java 文件除包名/导入/启动类改名外正文一致；V1/V2、契约、前端源码一致 |
| 文档与空白检查 | 10 份修改/新增文档相对链接、`git diff --check` | 通过 |
| 打包入口 | JAR manifest 和类文件检查 | Start-Class 为 `wemove.WemoveApplication`，没有旧包类，也没有将测试 H2 打入应用 |

新增集成测试使用仅测试依赖 H2，真实启动 Spring Boot / JPA / Spring Security / JDBC Session：

- 从 `wemove` 自动发现身份、商品、库存、事务及配置属性 Bean。
- 验证身份、商品、公共幂等实体均加入 JPA 元模型。
- 公开商品 API 可访问，匿名后台请求被拒绝。
- 管理员登录后，通过持久化会话读取当前身份与后台商品。
- 执行实际 V3 SQL，验证仅登录会话被撤销、匿名会话及账户保留，旧 Cookie 随后返回 401。

首次构建在添加 H2 依赖之前已启动，因此该轮测试类可编译但运行时缺少 H2 驱动；重新读取完整 POM 后执行 clean verify。此失败不作为通过记录。

## 验证边界

H2 验证了组件扫描、HTTP 安全、会话持久化和 V3 清理逻辑，不能替代 MySQL 的完整 Flyway 升级验收。本次 Docker 引擎不可用，未运行真实 MySQL API 冒烟或浏览器验收，也未执行全部成员交易用例。测试配置禁用 Flyway 并由 Hibernate 创建隔离内存业务表，实际生产仍由 Flyway 建表、Hibernate validate。
