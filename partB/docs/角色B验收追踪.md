# 角色 B 验收追踪

| 需求 | 本轮验收点 | 主要实现 | 验证 |
| --- | --- | --- | --- |
| FR-03 | 仅已发布；12 项默认分页、最大 50；推荐／价格排序稳定；加载、空、错误状态 | `ProductsPage.vue`、`PublicCatalogController`、`CatalogService.publicProducts` | TC-02；API 冒烟确认 14 条已发布、2 条草稿不泄漏 |
| FR-04 | 名称／SKU、分类、年龄、玩法、场景交集；查询保存在 URL | `ProductsPage.vue`、`presentation.ts`、JPA `Specification` | TC-02、TC-03；前端查询辅助函数单测 |
| FR-05 | 公开详情允许字段清单；缺货／下架不可购；占位图；经销价不返回 | `ProductDetailPage.vue`、`PublicProductDetail` | TC-04；烟测检查缺货、下架和经销字段隔离 |
| FR-26 | 草稿可不完整；发布完整校验；SKU 固定；上下架保留数据 | `AdminProductEditorPage.vue`、`AdminProductsPage.vue`、`CatalogRules`、`CatalogService` | TC-04、TC-33；后端规则单测与真实 MySQL 启动 |
| FR-27 | 单层分类、受控枚举、分价格、库存增减与流水；关联分类保护 | `AdminCategoriesPage.vue`、`InventoryService`、V2 迁移 | TC-33、TC-34；库存调整与流水冒烟 |
| BR-02 | 余额恒不为负；付款扣减、取消返还与后台调整使用同一原子模型 | `InventoryBalanceEntity`、`InventoryPort`、`inventory_movements` | 领域单测检查减至负数失败；相同幂等键重放后库存仍为 2 |
| NFR-03 | 运动端、平板、手机响应式；文字+色彩状态；可见标签和焦点 | catalog 页面与 `styles.css` | 生产构建通过；仍需团队在两款浏览器上登记 TC-38 手工结果 |

## 本轮不伪造的联合验收

- C 未接入前，不将“加入购物车”计为已完成交易，TC-14 至 TC-23 由 B/C 合并后执行。
- D 未接入前，经销投影已实现，但 TC-29 需 D 的派生身份与专属路由参与。
- E 未接入前，只验收媒体 ID 边界和缺图占位，不验收真实上传／下载。
- F 可以用 A 的 `AuditPort` 替换当前日志适配器；不在 B 内复制审计库。
