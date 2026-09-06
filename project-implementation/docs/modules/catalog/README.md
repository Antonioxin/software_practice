# 商品与库存模块

本模块由成员 B 负责，接入共用的前后端应用。本目录保存模块设计与交付记录，接口契约、脚本和截图各自存放在工程的对应目录中。

本轮覆盖 FR-03、FR-04、FR-05、FR-26、FR-27 及 BR-02 的 B 边界：

- 公开商品分页、稳定排序、搜索、组合筛选与详情。
- 草稿、发布、下架、分类、两类价格及库存流水管理。
- 向 C 提供零售快照和付款扣减／取消返还端口，向 D 提供经销商商品投影。
- 向 E 保留受控媒体 ID 引用；当前使用明确的占位图，不伪造文件服务。

## 交付索引

- [角色 B 模块设计](角色B模块设计.md)
- [角色 B 验收追踪](角色B验收追踪.md)
- [运行与验证手册](运行与验证手册.md)
- [验证记录](验证记录.md)
- [Catalog OpenAPI 契约](../../../contracts/openapi/catalog.yaml)
- [商品库存冒烟脚本](../../../scripts/smoke/catalog-api.ps1)：针对已启动的测试 MySQL 应用执行核心 API 验收。
- [验证截图](../../verification/catalog/README.md)
- [成员 B 完成情况审核](成员B完成情况审核.md)

## 代码入口

- 前端：[商品路由和展示逻辑](../../../apps/web/src/features/catalog/)；[商品页面](../../../apps/web/src/pages/ProductsPage.vue)、[商品详情](../../../apps/web/src/pages/ProductDetailPage.vue) 与同目录的商品管理页面。
- 后端：[商品库存业务包](../../../apps/api/src/main/java/wemove/catalog/)。
- 数据库：[V2 商品库存迁移](../../../apps/api/src/main/resources/db/migration/V2__catalog_inventory.sql)。

统一工程入口见 [工程说明](../../../README.md)，商品模块的 PowerShell 命令及验收步骤见 [运行手册](运行与验证手册.md)。
