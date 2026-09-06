# WEMOVE 角色 B：商品与库存

角色 B 的实现直接接入角色 A 已建立的单体应用，源码位于 `../partA/frontend` 和 `../partA/backend`，避免复制登录、会话、安全与错误处理底座。本目录保存 B 独立负责的契约、设计、验收和复现脚本。

本轮覆盖 FR-03、FR-04、FR-05、FR-26、FR-27 及 BR-02 的 B 边界：

- 公开商品分页、稳定排序、搜索、组合筛选与详情。
- 草稿、发布、下架、分类、两类价格及库存流水管理。
- 向 C 提供零售快照和付款扣减／取消返还端口，向 D 提供经销商商品投影。
- 向 E 保留受控媒体 ID 引用；当前使用明确的占位图，不伪造文件服务。

## 交付索引

- [角色 B 模块设计](docs/角色B模块设计.md)
- [角色 B 验收追踪](docs/角色B验收追踪.md)
- [运行与验证手册](docs/运行与验证手册.md)
- [验证记录](docs/验证记录.md)
- [Catalog OpenAPI 契约](contracts/catalog.openapi.yaml)
- `scripts/catalog-api-smoke.ps1`：针对已启动的 MySQL 应用执行 B 核心 API 冒烟验收。

## 代码入口

- 前端：`../partA/frontend/src/features/catalog`、`pages/ProductsPage.vue`、`pages/ProductDetailPage.vue` 与 `pages/Admin*`。
- 后端：`../partA/backend/src/main/java/com/wemove/identity/catalog`。
- 数据库：`../partA/backend/src/main/resources/db/migration/V2__catalog_inventory.sql`。

完整启动步骤见运行手册。
