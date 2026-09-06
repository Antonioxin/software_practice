# 身份与账户模块

本模块由成员 A 负责，覆盖 FR-09、FR-10、FR-11、FR-12、FR-31，并提供认证、会话、权限和统一错误等公共设施。本目录保存模块设计及交付记录；所有成员在同一套前后端工程中开发。

## 代码与工程入口

- [前端身份路由](../../../apps/web/src/features/identity/)：注册、登录、账户和管理页面的路由入口。
- [后端身份模块](../../../apps/api/src/main/java/wemove/identity/)：身份服务、权限与账户数据访问；共用能力位于 [platform](../../../apps/api/src/main/java/wemove/platform/)，全局配置位于 [config](../../../apps/api/src/main/java/wemove/config/)。
- [数据库迁移](../../../apps/api/src/main/resources/db/migration/)：V1 身份底座及后续全局有序迁移。
- [身份接口契约](../../../contracts/openapi/identity.yaml)：身份与账户 HTTP 定义。

## 快速启动

统一启动和构建入口见 [工程说明](../../../README.md)，数据库初始化、环境配置与身份验收步骤见 [运行与验证手册](运行与验证手册.md)。前端通过 `/api` 代理访问后端，浏览器使用同站 Cookie 会话。

## 交付索引

- [角色 A 模块设计](角色A模块设计.md)：边界、数据、安全决策与跨模块端口。
- [接口与集成说明](接口与集成说明.md)：Cookie、CSRF、能力名和接入方式。
- [角色 A 验收追踪](角色A验收追踪.md)：FR/公共底座到实现及验证的映射。
- [角色 A 进度计划](角色A进度计划.md)：阶段、判据与集成里程碑。
- [运行与验证手册](运行与验证手册.md)：从干净环境启动、测试和手工验收。
- [验证记录](验证记录.md)：本次已执行、通过及受环境限制未执行的检查。
- [Web 开发技术现状报告](Web开发技术现状报告.md)：个人报告，姓名、学号和班级保留待填占位符。
- [OpenAPI 契约](../../../contracts/openapi/identity.yaml)：11 条身份与账户管理路径。
- [身份 API 冒烟脚本](../../../scripts/smoke/identity-api.sh)：使用测试 MySQL 启动后端并执行身份链路。
- [设计工具资料](../../../.superdesign/)：设计系统、页面清单和可恢复的工具元数据。
- [页面参考图](../../design/references/identity/)：保留的身份页面设计参考。
- [商品与库存模块](../catalog/README.md)：共享应用中的商品、分类、价格及库存功能。
