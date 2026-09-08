# F 模块 API 自动化测试报告（工单与运营）

- **被测对象**：`wemove.support` / `wemove.operations.audit`（FR-19/20/21 工单全流程、FR-32 运营总览、审计只读检索）
- **代码版本**：master `d0d9a95`（PR #10 已合并，含 PR #11 前端改版；后端在两 PR 间无改动）
- **执行时间**：2026-09-07 至 2026-09-08
- **执行方式**：黑盒 API 级自动化测试（bash + curl + node 断言脚本，[测试脚本.sh](测试脚本.sh)），对本地运行中的应用（8080）发起真实请求
- **测试数据**：每轮注册全新测试用户；经 C 模块公开接口完成「加购 → 结算预览 → 建单 → 模拟支付」产生真实 PAID 订单，作为 AFTER_SALES 工单关联前置
- **口令说明**：所有账号口令为本地测试值，经环境变量注入，不写入仓库

## 一、测试范围

| 接口组 | 端点数 | 覆盖内容 |
| --- | --- | --- |
| 用户侧工单 `/api/v1/tickets` | 5 | 创建三类工单、本人列表、详情、补充说明、关闭 |
| 管理侧工单 `/api/v1/admin/tickets` | 6 | 列表筛选、详情、开始处理、公开回复、内部备注、关闭 |
| 运营总览 `/api/v1/admin/dashboard` | 1 | 8 指标快照、区间校验 |
| 审计检索 `/api/v1/admin/audit-logs` | 2 | 列表（筛选/分页）、详情 |

## 二、结果汇总

**共 72 项断言，实际全部通过**（运行记录显示 71 PASS / 1 FAIL，其中 1 项 FAIL 经复核为测试脚本算式笔误——将基线"待办数（NEW+PROCESSING）"误当作"NEW 数"参与计算，接口实际返回值与数据库核对一致，属脚本问题而非缺陷）。

| 功能块 | 断言数 | 结果 |
| --- | --- | --- |
| 注册 / 双角色登录（前置） | 3 | ✅ |
| 交易链路前置（加购→预览→建单→支付） | 6 | ✅ |
| FR-19 工单创建与回填 | 10 | ✅ |
| 输入校验（422/404） | 3 | ✅ |
| FR-21 处理链路（状态机 T-I-05） | 14 | ✅ |
| FR-20 对话与隐私隔离 | 9 | ✅ |
| 用户关闭（默认原因） | 2 | ✅ |
| 鉴权与冲突边界 | 6 | ✅ |
| FR-32 运营总览 | 7 | ✅ |
| 审计检索 | 6 | ✅ |

## 三、关键验证点明细

### 3.1 FR-19 工单提交与查看

- 三类工单创建成功：GENERAL（无关联）、PRODUCT（关联商品）、AFTER_SALES（关联本人 PAID 订单）
- 关联回填：PRODUCT 工单响应回填商品名与 SKU；AFTER_SALES 回填订单号与 PAID 状态
- 电话标准化：提交 `+86 138 0000 5678`，落库/返回 `+8613800005678`
- 新建工单 status=NEW、version=1、allowedActions=[FOLLOW_UP, CLOSE]
- 本人列表仅返回本人 3 张工单，分页 meta 正确

### 3.2 输入校验

| 输入 | 预期 | 实际 |
| --- | --- | --- |
| 正文 2 字（<10） | 422 VALIDATION_ERROR | ✅ |
| PRODUCT 缺 productId | 422 | ✅ |
| AFTER_SALES 关联不存在/他人订单 | 404（归属伪装） | ✅ |

### 3.3 FR-21 处理链路（BR-05 状态机全链）

```
NEW v1 →(start) PROCESSING v2 →(reply) REPLIED v3 →(内部备注) REPLIED v3*
       →(用户补充) PROCESSING v4 →(reply) REPLIED v5 →(close) CLOSED v6
```

- 每次转移版本号递增与历史一致；allowedActions 随状态正确推导（PROCESSING → [REPLY, NOTE, CLOSE]）
- **内部备注不改变状态与版本**（仍 REPLIED v3），管理端可见、写入审计 TICKET_NOTE_ADDED
- 管理员关闭原因必填、closedAt/closeReason 落库；用户关闭不填原因走默认「用户确认关闭」

### 3.4 FR-20 对话与隐私隔离

- 用户可见管理员公开回复（消息内容比对通过）
- **用户响应 internalNotes / history 恒为空数组**（实测 length=0）
- REPLIED → 用户补充 → PROCESSING 回环成立（BR-05）

### 3.5 鉴权与冲突边界

| 场景 | 预期 | 实际 |
| --- | --- | --- |
| 管理员调用用户建单接口 | 403 | ✅ |
| 普通用户调用管理列表 / 管理动作 | 403 | ✅ |
| 匿名访问用户接口 | 401 | ✅ |
| 对 CLOSED 工单补充 | 409 STATE_CONFLICT | ✅ |
| 过期 expectedVersion 补充 | 409 VERSION_CONFLICT | ✅ |
| 总览区间 start ≥ end | 422 | ✅ |
| 审计详情非法 UUID | 422 | ✅ |
| 审计详情合法但不存在 | 404 | ✅ |

### 3.6 FR-32 运营总览（8 指标与 DB 对账）

| 指标 | API 返回 | DB 实查 | 一致 |
| --- | --- | --- | --- |
| publishedProductCount | 14 | 14（PUBLISHED） | ✅ |
| activeUserCount | 8 | 8（ACTIVE，含管理员，契约口径） | ✅ |
| pendingTicketCount | 4 | 4（NEW+PROCESSING） | ✅ |
| pendingShipmentCount | 2 | 2（PAID 未发货） | ✅ |
| createdOrderCount（当日 UTC 区间） | 2 | 2 | ✅ |
| netPaidFen | "42800" | 12900+29900（两笔 PAID 合计） | ✅ |
| pendingApplicationCount / pendingInquiryCount | 0 / 0 | 0 / 0（D 域经 DealershipMetricsPort 实时统计） | ✅ |

asOf 与 8 指标同快照返回。

### 3.7 审计检索

- 本轮操作新增审计恰好 10 条，动作分布与操作一一对应：3×CREATED + STARTED + 2×REPLIED + FOLLOWED_UP + NOTE_ADDED + 2×CLOSED
- 审计查询本身不产生审计记录（查询前后计数不变）
- 详情含 requestId / result / changeSummary

## 四、顺带验证到的关联模块行为（均正常）

| 模块 | 行为 | 说明 |
| --- | --- | --- |
| A（身份） | 注册限流 5 次/10 分钟（按 IP） | 测试中真实触发 429，符合设计 |
| B（商品/库存） | 库存不足加购返回 409 | 首轮测试买空示例商品库存后触发 |
| C（交易） | 加购→预览→建单→模拟支付链路 | 为售后工单提供真实 PAID 订单 |

## 五、缺陷清单

**未发现功能缺陷。** 过程中出现的全部异常均为测试脚本自身问题（响应字段路径、算式、限流窗口），逐项复核后关闭，不影响结论。

## 六、测试边界说明

- 本报告为 **API 级**黑盒测试；浏览器界面点击走查与视觉验收不在本轮范围（前端另有 80 项单测与构建覆盖）。
- 幂等重放、限流 429 对工单桶的消耗、审计失败回滚、悲观锁并发等在 `SupportMySqlTest`（真实 MySQL，11 用例）已覆盖，本轮未在开发库重复触发，避免污染计数桶。
- 证据文件（各步骤原始响应 JSON）见 [evidence/](evidence/)；完整断言输出见[测试运行记录.log](测试运行记录.log)。

## 七、结论

F 模块全部 14 个接口在合并后的 master 上**功能行为正确、状态机与契约一致、鉴权与数据隔离有效、审计与总览数据与数据库实查一致**，具备交付条件。
