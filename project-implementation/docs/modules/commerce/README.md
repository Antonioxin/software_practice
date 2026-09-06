# 零售交易模块（角色 C）

已在统一应用实现 FR-13—18：购物车、可信结算预览、订单快照、模拟付款、整单取消退款、模拟发货及本人收货。公共幂等/身份锁、B库存边界和F持久审计同时接入。本轮交付可审核源码，未提交、推送或部署；正式团队审核与全组验收仍待完成。

- [运行与验证手册](运行与验证手册.md)：参数、迁移、锁顺序、旧幂等兼容、启动与真实MySQL测试。
- [验证记录](验证记录.md)：本轮实际执行结果与剩余边界。
- [角色C验收追踪](角色C验收追踪.md)：26条原设计对应证据与未逐条验收说明。
- [成员C贡献记录](成员C贡献记录.md)：C0—C5交付及A/B/F协作职责。
- [实施依据与 A/B 检查](WEMOVE角色C实现方案与AB检查.md)：从源工作区只读带入的新版方案，其“本轮”指方案编写轮次。
- [原方案与历史检查](角色C原方案与历史检查.md)：保留早期方案与历史证据，不替代本轮测试。
- [OpenAPI](../../../contracts/openapi/commerce.yaml)、[HTTP冒烟](../../../scripts/smoke/commerce-api.ps1)、[浏览器传输故障代理](../../../scripts/smoke/commerce-fault-proxy.mjs)。
- [后端源码](../../../apps/api/src/main/java/wemove/commerce/)、[前端业务](../../../apps/web/src/features/commerce/)、[页面](../../../apps/web/src/pages/commerce/)、[后端测试](../../../apps/api/src/test/java/wemove/commerce/)、[截图与证据](../../verification/commerce/README.md)。

C与identity/catalog平级，F审计实现在operations.audit，公共服务在platform；D仅根目录未改。所有内容复用现有应用、会话、Flyway与构建入口。
