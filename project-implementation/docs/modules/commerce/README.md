# 零售交易模块（角色 C）

当前已建立目录骨架，尚未实现购物车或订单业务。空目录中的 `.gitkeep` 用于让 Git 保留目录，添加实际文件后可以删除。

- [实现方案与 A/B 检查](WEMOVE角色C实现方案与AB检查.md)：接入前置项、数据、API、状态机、开发顺序与验证要求。
- [工程入口](../../../README.md)：统一工程路径与运行命令。
- [后端目录](../../../apps/api/src/main/java/wemove/commerce/)：`wemove.commerce`，与身份、商品模块平级；已建 `api/`、`domain/`、`repository/`、`service/`、`platform/`。
- [前端业务目录](../../../apps/web/src/features/commerce/)与[页面目录](../../../apps/web/src/pages/commerce/)：后续添加接口调用、状态、路由与页面。
- [后端测试目录](../../../apps/api/src/test/java/wemove/commerce/)与[验收证据目录](../../verification/commerce/)：当前为空，未产生测试结果。
- 契约、脚本、验收材料分别按工程约定放入 `contracts/openapi/commerce.yaml`、`scripts/smoke/`、`docs/verification/commerce/`。

接口契约与冒烟脚本尚未创建，路由尚未注册；不复制应用启动类、依赖清单、身份系统或迁移入口。

以上路径相对于 `project-implementation/`。成员编号只用于分工记录，不再创建独立的 `partC` 应用或资料目录。
