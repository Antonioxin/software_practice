# 经销合作模块

本模块由成员 D 主责，对应 FR-08、FR-22—FR-25、FR-30，并向 F 提供待审核申请数和待处理询价数。实现位于共享 Spring Boot/Vue 应用中，没有建立第二套身份、商品或数据库。

## 已接入能力

- 公开渠道按国家／地区、城市查询；只有明确发布且关联企业仍为 `ACTIVE` 的记录可见。
- 普通用户首次申请、查看本人版本、驳回后追加版本重提；待审、已通过或暂停合作不能绕过限制重复申请。
- 管理员读取申请历史并审核当前内容版本；通过时在同一事务内建立唯一企业、激活资格、保存审核与审计。
- 实时派生 `DEALER` 身份；有效经销商读取 B 提供的完整经销商品投影，公共商品接口不增加专属字段。
- 多商品询价保存名称、SKU、参考价和最小量快照；允许超过库存，不扣库存、不创建订单。
- 本人历史读取/关闭，管理员开始处理、逐行报价/交期回复和关闭；关闭后只读。
- 企业暂停/恢复；暂停同步下线关联渠道，恢复不自动重新发布。
- 渠道草稿建立、编辑、发布/下线；关联企业需要公开意愿且合作有效。
- 关键写操作复用 A 的账户锁、幂等执行器和限频端口，复用 F 的同事务审计端口。

## 代码入口

- 后端控制器：[DealershipController.java](../../../apps/api/src/main/java/wemove/dealership/api/DealershipController.java)
- 业务服务：[DealershipService.java](../../../apps/api/src/main/java/wemove/dealership/service/DealershipService.java)
- 资格端口实现：[DealerAccess.java](../../../apps/api/src/main/java/wemove/dealership/service/DealerAccess.java)
- 数据迁移：[V8__dealership_cooperation.sql](../../../apps/api/src/main/resources/db/migration/V8__dealership_cooperation.sql)
- 前端路由：[routes.ts](../../../apps/web/src/features/dealership/routes.ts)
- 接口契约：[dealership.yaml](../../../contracts/openapi/dealership.yaml)
- 测试设计：[成员D测试用例.md](../../../tests/cases/成员D测试用例.md)

## 外部协作边界

B 的经销商品投影已经接入。E 尚需通过统一文件服务为经销资料下载提供逐次资格检查；D 不复制文件表或静态私有链接。F 可注入 `DealershipMetricsPort` 汇总待办，并继续使用现有 `AuditPort` 读取 D 已写入的业务审计。企业资料修正接口保留 `basisTicketId`，待 F 的工单读取端口落地后共同补做关联存在性校验。
