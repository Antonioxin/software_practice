# 角色 B 模块设计

## 1. 边界与组装方式

商品与库存作为 A 底座中的 `catalog` 业务切片运行，共用 Cookie 会话、CSRF、管理员权限、幂等记录、审计端口和 Problem Details。对外 HTTP 边界与对内 Java 端口分开，C/D 不需要调用后台管理接口。

```text
公开商品页 / 运营后台
           │ JSON + Cookie + CSRF
           ▼
PublicCatalogController / AdminCatalogController
           │
           ├─ CatalogService ── Product / Category / Price
           ├─ InventoryService ─ Balance + append-only Movement
           └─ CatalogIntegrationService
                    ├─ CatalogPort → C / D
                    └─ InventoryPort → C
```

## 2. 数据模型

| 表 | 作用 | 核心约束 |
| --- | --- | --- |
| `catalog_categories` | 单层分类 | 规范化名称唯一；被商品引用时不可停用或删除 |
| `catalog_products` | 商品主数据与两类价格 | SKU 唯一且首次赋值后不可变；价格按分保存；状态受控 |
| `inventory_balances` | 每商品当前库存 | 主键为商品 ID；`quantity >= 0`；悲观锁和版本字段 |
| `inventory_movements` | 库存事实流水 | 保存变更前后值、原因、来源与操作人；业务来源键去重 |

商品普通编辑请求不包含库存字段，建档后只能通过库存命令变更。不做物理删除商品，用 `UNLISTED` 保留历史引用。

## 3. 公开读取与价格隔离

公开列表只查询 `PUBLISHED`，同价排序使用商品 ID 作稳定次序。公开 DTO 采用允许字段清单，结构中没有经销参考价、最小询价量或交期。下架详情只保留基本识别信息与不可购买提示；草稿对公开请求返回 404。

筛选规则在服务端执行：年龄为闭区间匹配，空上限表示“以上”；室内／户外各自包含 `BOTH`，选择 `BOTH` 则只匹配 `BOTH`。前端把筛选和页码写入 URL，支持刷新、返回和复制链接。

## 4. 发布、并发与幂等

- 草稿允许不完整，但已填字段仍需合法。发布命令统一校验必填资料、分类状态、价格与库存记录。
- 商品、分类更新携带 `expectedVersion`，旧页面写入返回 409。
- 后台库存只接受“方向 + 正整数变更量 + 原因”，在行锁内计算余额，减至负数整个事务失败。
- 建档、上下架、库存调整要求 `Idempotency-Key`；相同操作人、动作、键与内容返回既有结果，同键不同内容返回 409。
- C 调用 `InventoryPort` 扣减时先按商品 ID 排序取锁，减少死锁风险；付款扣减与取消返还分别按订单 ID 去重。

## 5. 跨模块端口

| 消费方 | 端口 | 用途 |
| --- | --- | --- |
| C | `CatalogPort.getRetailSnapshot` | 以服务端价格、可售状态和当前库存创建交易快照 |
| C | `InventoryPort.deductForPayment` | 在付款成功事务边界扣减，不允许部分成功 |
| C | `InventoryPort.restoreForCancellation` | 已付款待发货订单成功取消时仅返还一次 |
| D | `CatalogPort.getDealerProducts` | 只返回已发布且启用经销业务的专属投影 |
| E | `mainImageId` / `imageIds` | 保存 E 提供的受控媒体引用，不保存服务器路径 |
| F | A 的 `AuditPort` | 商品、分类、发布和库存动作的中心审计接入点 |

## 6. 明确的集成待办

FR-05 的真实加购由 C 实现；当前详情页已校验数量与可购状态，并明示接入边界。实际图片和公开资料由 E 提供。付款并发的订单状态联合验收需等 C 的订单事务接入；B 已提供库存原子端口与去重约束。
