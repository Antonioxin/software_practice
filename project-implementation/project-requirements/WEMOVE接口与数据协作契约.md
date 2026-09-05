# WEMOVE 接口与数据协作契约

| 属性 | 内容 |
| --- | --- |
| 编号 / 版本 | WEMOVE-CONTRACT V1.0 |
| 日期 / 状态 | 2026-09-05；设计基线建议，待首次团队评审 |
| 上位需求 | [SRS V1.1](WEMOVE网站重构需求文档.md)，尤其第 3.1、3.3、3.4 节 |
| 配套规范 | [团队分工与协作规范](WEMOVE团队分工与协作规范.md)、[需求责任与验收追踪表](WEMOVE需求责任与验收追踪表.md) |
| 适用架构 | 同仓库、模块化后端、单一关系型数据库；A—F 含义见分工文档 |

本文将 SRS 的行为约束转化为可组合的设计。路径、字段命名、Cookie 会话、分整数、预览令牌等是本方案提出的统一选择，并非声称 SRS 已指定这些实现。团队可在 M0 统一调整；一旦消费者开始实现，变更须按兼容性流程评审。SRS 的权限、范围、数值和业务状态不能由某个模块单独改变。

本文是通信与业务边界的可评审设计，尚不是已部署 API 或完整机器可读 OpenAPI 文件。各域开始编码前须将对应操作的字段、必填性及样例落实到 OpenAPI；不得自行补出与本设计冲突的含义。

## 1. 契约分层与所有权

需要同时约定四种接口，只有 HTTP 路径一致还不够：

| 契约层 | 提供方与调用方 | 必须固定的内容 |
| --- | --- | --- |
| 页面与共享 UI | E/A → 各前端模块 | 布局插槽、路由注册、登录状态、请求入口、错误与表单反馈 |
| HTTP API | 各域后端 → 页面/测试程序 | 方法、路径、参数、响应、权限、错误、幂等和状态含义 |
| 后端模块服务 | 业务域 → 其他业务域 | 公开服务签名、投影字段、事务参与方式、失败语义 |
| 数据与迁移 | 各域 → 数据库/升级流程 | 写入所有者、键与引用、金额精度、状态约束、版本与保留规则 |

OpenAPI 采用固定版本 **3.1.1**，用它描述 HTTP 操作、schema 和样例；不是要求追随最新规范。格式依据：[OpenAPI 3.1.1](https://spec.openapis.org/oas/v3.1.1.html)。DTO（数据传输对象）是对外字段清单；它与数据库实体分离，避免数据库增加内部字段后意外向用户泄漏。

各操作必须含：唯一 `operationId`、模块 owner、关联 FR/BR/TC、认证方式、权限与归属、请求/响应 schema、成功及失败样例、业务错误码、幂等要求、版本检查、分页/排序语义。跨模块服务也记录对应输入、输出与异常。

## 2. HTTP、字段与错误规范

### 2.1 通用协议

| 项目 | 统一规定 |
| --- | --- |
| 基础路径 | `/api/v1`；以下路径表均省略此前缀 |
| 数据格式 | JSON UTF-8；请求/响应字段 `camelCase`；路径 `kebab-case`；枚举 `UPPER_SNAKE_CASE` |
| HTTP 动作 | GET 只读；POST 创建或业务命令；PATCH 白名单字段部分更新；DELETE 仅用于购物车项等确实允许移除的资源 |
| 写成功 | 创建资源首次成功 201，并给 `Location`；读取/更新/业务命令 200；不统一包装成永远 200 |
| 删除成功 | 200，`data` 为删除结果或最新购物车；本项目不混用 204 与带正文响应 |
| 资源 ID | 非空不透明字符串，长度不超过 64；不得转成前端数值参与运算；订单号等展示编号与 ID 分开 |
| 币种与金额 | `currency: "CNY"`；`unitPriceFen`、`totalFen` 等均为整数分，禁止同一字段混用元/分或浮点数 |
| 时间 | 时间戳为 UTC ISO 8601 字符串，如 `2026-09-05T03:00:00.000Z`；展示固定 Asia/Shanghai |
| 日期 | `YYYY-MM-DD`，如期望交付日；按上海日期校验，不把日期当 UTC 午夜反复换算 |
| 空值 | 选填字段省略或 `null` 均表示未填写；PATCH 中省略表示不改，`null` 仅用于清空允许为空字段；空字符串不能冒充缺失值 |
| 未定义字段 | 请求统一拒绝并返回 422，避免对象批量赋值；响应新增可选字段由消费者忽略，但枚举变更须评审 |
| 字符长度 | 统一按 Unicode 码点计数，前后端共用样例；不直接使用 JavaScript UTF-16 `length` 充当字符数 |
| 文本规范化 | 必填文本先去首尾空白；密码不修剪、不截断；邮箱去首尾空白并统一大小写；电话按 SRS 规范化 |
| 请求跟踪 | 返回 `X-Request-Id`，由服务端生成排查编号；业务幂等键与排查编号不是同一概念 |
| 私有响应缓存 | 身份、个人业务、经销资料及下载使用 `Cache-Control: no-store`；公共读接口在首期也优先重新请求，避免下线后返回旧数据 |

### 2.2 成功、分页与排序

普通成功响应：

```json
{
  "data": { "id": "ord_example", "status": "PENDING_PAYMENT" },
  "meta": { "requestId": "req_example" }
}
```

分页成功响应：

```json
{
  "data": { "items": [], "page": 1, "pageSize": 12, "total": 0 },
  "meta": { "requestId": "req_example" }
}
```

- 所有分页列表默认 `page=1,pageSize=12`；`pageSize` 范围 1—50。非法类型/范围返回 422；超出最后一页返回空 `items` 与真实 `total`。
- 商品支持 `sort=recommended|priceAsc|priceDesc`；相同推荐序/价格以 `id ASC` 收尾。订单默认 `createdAt DESC,id DESC`。其余列表在操作契约声明固定排序及允许值。
- 稳定排序保证相同数据集的确定结果，不承诺在他人不断新增/删除数据时，基于页码的翻页仍是同一快照。
- 商品参数：`keyword`（trim 后最多 100 字符）、`categoryId`、`age`、`playType`、`scene`；维度单选、维度间取交集，保存在页面 URL 中。
- `age` 取 0—18 整数；商品 `ageMax=null` 表示只检查下限。`scene=INDOOR` 包含 INDOOR/BOTH，OUTDOOR 同理，BOTH 只匹配 BOTH。
- 更改查询或排序回到第 1 页；后退/刷新保留参数。玩法标签由 B 的受控查询接口提供，前端不自行拼写新值。

### 2.3 错误响应

错误采用 `application/problem+json`，以 RFC 9457 的 Problem Details 结构加项目字段。`code` 用于程序分支，`detail` 用于显示，前端不得根据中文文案猜错误类别。结构依据：[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)。

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "code": "INSUFFICIENT_STOCK",
  "detail": "部分商品库存不足，请修改数量后重试。",
  "requestId": "req_example",
  "errors": [{ "field": "items[0].quantity", "code": "INSUFFICIENT_STOCK", "message": "当前可售库存为 1 件。" }],
  "context": { "orderStatus": "PENDING_PAYMENT" }
}
```

`errors` 与 `context` 选填；context 只含调用者有权读取的当前状态/重新确认信息。示例中的 `title` 对应 HTTP 状态，面向用户的具体中文说明放在 `detail`。

| HTTP | 错误码示例 | 前端处理 |
| --- | --- | --- |
| 400 | `MALFORMED_REQUEST` | JSON 语法/请求格式错误，提示请求无效 |
| 401 | `UNAUTHENTICATED`、`SESSION_EXPIRED` | 引导登录，保留非敏感输入及安全回跳路径 |
| 403 | `FORBIDDEN`、`ACCOUNT_DISABLED`、`DEALER_INACTIVE` | 说明无权限；不继续展示旧专属数据 |
| 404 | `RESOURCE_NOT_FOUND` | 不存在与他人私人记录使用相同码和文案，不泄漏存在性 |
| 409 | `STATE_CONFLICT`、`VERSION_CONFLICT`、`CART_CHANGED`、`PRICE_CHANGED`、`INSUFFICIENT_STOCK`、`PRODUCT_UNAVAILABLE`、`IDEMPOTENCY_CONFLICT`、`REQUEST_IN_PROGRESS`、`RESOURCE_IN_USE`、`UNIQUE_CONFLICT` | 按错误类型刷新状态或重新确认，不对所有 409 自动重试 |
| 422 | `VALIDATION_ERROR` | 按 `errors.field` 定位字段，保留非敏感输入 |
| 429 | `RATE_LIMITED` | 返回 `Retry-After` 秒数；按剩余等待时间提示 |
| 500 | `INTERNAL_ERROR` | 通用提示和排查编号；不暴露堆栈、SQL、磁盘路径或秘密 |

没有收到响应的超时与明确 4xx 不同：前端显示“结果暂未确认，可查询记录或用原请求重试”。写请求收到 500 也通过查询或同键重试核实，不推断事务一定已失败。只有服务端确认回滚的业务错误才给确定失败结果。

## 3. 身份与授权契约

### 3.1 统一会话方案

基线采用**服务端会话 + Cookie**。浏览器从同一站点请求 `/api`，开发环境用代理统一入口；确需跨源时，由 A 统一配置精确来源和凭据策略，不由模块自行放宽 CORS。

- 会话 Cookie 设置 `HttpOnly`、`SameSite=Lax`，HTTPS 环境设置 `Secure`；公开网络部署必须 HTTPS。会话标识不能进入 JSON 响应、URL、前端持久化存储或日志。
- 所有写请求校验允许的 Origin 并验证与会话绑定的 `X-CSRF-Token`。`GET /auth/csrf` 为登录前后提供相应令牌；缺失/无效返回 403。SameSite 不能代替完整写请求防护。
- `GET /auth/me` 返回用户公开身份和能力摘要；用户的根身份只存 `USER` 或 `ADMIN`。有效经销资格由 D 的企业合作记录派生，避免 `user.role=DEALER` 与企业状态成为两套互相冲突的事实。
- 空闲 30 分钟过期；服务端按有效认证请求更新最后活动时间。退出撤销当前会话；停用账户撤销全部会话，恢复后旧会话不复活。
- 每次受保护请求重新检查账户；专属请求再检查当前企业合作状态。前端的 `capabilities` 仅控制入口显示，不能代替服务端判定。
- 登陆失败：同一规范化邮箱 10 分钟内连续失败 5 次，暂停尝试 10 分钟；邮箱不存在与密码错误文案一致。
- 注册按来源地址最多 5 次/10 分钟；咨询、申请、询价全部写操作按当前用户**合用一个** 20 次/10 分钟限流桶，由 A 提供，D/F 调用。重试也属于请求，限流后不写业务数据。

### 3.2 授权计算与例外

对受保护操作，许可条件是：

\[
Allow(u,a,r)=SessionValid(u)\land AccountActive(u)\land RoleAllows(u,a)\land OwnsOrAdmin(u,r)\land BusinessAllows(u,a,r)
\]

认证回答“你是谁”，授权回答“你能对这条记录做什么”。角色校验与归属校验缺一不可：普通用户可以看订单，并不表示可以看任意用户的订单。

| 请求类别 | 服务端要求 |
| --- | --- |
| 公开读取 | 仅已发布/公开字段；不因当前登录者身份改变公开商品字段集合 |
| 本人零售/咨询/资料 | 启用 USER，按服务端当前用户限定归属；ADMIN 不能发起个人购物或申请 |
| 经销目录/私有资料/新询价 | 启用 USER + 有效企业合作；管理员使用管理入口维护 |
| 历史申请/询价读取、本人询价关闭 | 启用 USER + 本人/本企业；合作暂停仍允许，保留旧价格快照 |
| 后台读写 | 启用 ADMIN；非管理员请求返回 403；后台也按命令检查状态与版本 |
| 他人私人资源 | 在资源归属检查后统一 404，不返回实际 owner 或存在性 |

资源所属用户/企业由服务器确定。客户端 `userId/role/companyId/isDealer` 不得授权自己；在需要后台选择企业的命令中也须校验企业已归属当前申请用户。账户、申请、合作三个状态独立，恢复账户不改变暂停合作；恢复合作不重新发布渠道。

### 3.3 字段可见性

分别定义 `PublicProduct`、`DealerProduct`、`AdminProduct`、`OwnOrder`、`AdminOrder` 等 DTO，不直接序列化数据库实体。

| 对象 | 可以给普通用户的字段 | 必须隔离的字段 |
| --- | --- | --- |
| 公开商品 | ID、SKU、名称、图片、分类、年龄、玩法、场景、规格、零售价、库存状态、公开资料 | 经销参考价、最小询价量、专属交期与私有文件元数据 |
| 经销目录 | 公开商品信息 + 明确标识的参考价、最小量、现有库存、参考交期 | 企业内部备注、其他企业业务记录 |
| 工单/申请/询价 | 本人提交、公开回复、公开审核原因、状态与允许的历史 | 内部备注、其他用户信息 |
| 公开渠道 | 独立发布的渠道名称、地区、地址、公开联系方式、网站 | 原始申请全量资料与未选择公开的字段 |
| 文件 | 已获授权的标题、版本说明、逻辑 ID 与当前下载标识 | 存储路径、私有存储键及无权访问文件的标题 |

## 4. 前端组合契约

### 4.1 路由与模块注册

E 维护主布局与注册接口，A 提供身份能力。各模块导出路由清单，统一装配；不各建 router 实例或独立挂载整个应用。

| 所有者 | 页面路径范围 |
| --- | --- |
| A | `/login`、`/register`、`/account/profile`、`/admin/users` |
| B | `/products`、`/products/:id`、`/admin/products`、`/admin/categories`、库存子页面 |
| C | `/cart`、`/checkout`、`/account/orders/*`、`/admin/orders/*` |
| D | `/channels`、`/partner/apply`、`/account/applications/*`、`/dealer/catalog`、`/account/inquiries/*`、`/admin/dealership/*` |
| E | `/`、`/about`、`/articles/*`、`/faq`、`/downloads`、`/terms`、`/privacy`、通用错误页、`/admin/content/*`、`/admin/files/*` |
| F | `/contact`、`/account/tickets/*`、`/admin/tickets/*`、`/admin`、`/admin/audit-logs` |

将历史询价放在 `/account/inquiries`，避免整段路由只允许有效经销商而误封暂停合作用户。经销中心入口可以链接到这些本人记录页。管理员的专属目录/文件预览通过后台入口完成。

注册项至少含 `routeId,path,layout,requiredCapability,loadPage`；`layout` 取 public/account/dealer/admin，`routeId` 跨模块唯一。字段表达概念与类型，具体框架的懒加载写法在 M0 适配。一级路径变更需 E 和受影响模块确认。

### 4.2 共享组件与状态

- 共享层提供布局、按钮、输入、表单错误、确认对话框、分页、空状态和错误状态；局部 CSS 使用作用域或命名空间，禁止随意覆盖全局 `button/table/input`。
- 全局仅共享身份摘要、基础配置等必要状态。购物车由 C 的查询能力维护；B 的加购按钮调用统一接口，成功后失效购物车缓存，不写第二份权威购物车。
- 前端数据缓存键必须包含身份/用户维度；退出、身份或资格改变后清除私人缓存。遇到 403/401 时同步处理旧显示，不能只隐藏导航却保留私有详情。
- 所有请求经过 A 维护的 HTTP 入口，统一 Cookie/CSRF、错误映射和追踪编号。禁止默认自动重试所有 POST；需重试的动作保留原幂等键。
- 各页面实现 loading/empty/error/forbidden/not-found/success 状态。写入另有 submitting 和 result-unknown；订单付款、取消、发货等成功后重新读取权威状态。
- 前端表单按相同 schema 尽早提示；后端仍独立验证。确认取消/收货等动作，非敏感输入失败后保留；页面明确标注模拟支付、模拟退款与模拟发货。
- 商品与文章详情支持直接 URL 访问，设置标题和说明；390/768/1440 px、键盘与至少两浏览器目标由每个页面负责人落实。

## 5. HTTP 操作清单与领域数据

### 5.1 清单符号

`P` 为公开，`U` 为启用普通身份（含有效/暂停经销用户），`D` 为当前有效经销商，`A` 为管理员。`U-Own` 为本人业务，不含管理员。`K` 表示必须有 `Idempotency-Key`；`V` 表示必须携带 `expectedVersion` 或申请的 `applicationVersion`。路径只是命名空间，不能替代授权。

每组列出所需操作，不表示一个巨型通用 CRUD 接口。不得提供 `PATCH /orders/{id}` 任意改金额/状态等后门。通用编辑仅白名单字段；业务状态变化使用显式命令。

### 5.2 A：身份与账户

| 操作 | 权限 / 关键输入与输出 |
| --- | --- |
| `GET /auth/csrf` | P；取得匿名/当前会话 CSRF 令牌 |
| `POST /auth/register` | P；email、nickname、password、confirmPassword、adultConfirmed、termsVersion、privacyVersion、termsAccepted、privacyAccepted；只创建 USER |
| `POST /auth/login`、`POST /auth/logout` | 登录 P，退出验证 CSRF 并撤销会话；设置/清除 Cookie；退出已失效会话不产生新会话 |
| `GET /auth/me`、`PATCH /account/profile` | U/A 读取本人身份；资料编辑 U，只有 nickname、phone；邮箱与企业资料不可改 |
| `GET /admin/users`、`GET /admin/users/{id}` | A；筛选邮箱、昵称、角色/派生身份、账户状态；不返回密码摘要 |
| `POST /admin/users/{id}/disable`、`/restore` | A，K/V；reason；目标仅 USER；记录操作，停用撤销会话 |

注册确认版本必须是服务端当前允许版本，true 不能代替未展示的说明；保留确认时间及版本，不采集儿童出生日期。昵称 2—30、邮箱最多 254、密码 8—64 且含字母和数字；各人统一字符与空白规则。

### 5.3 B：商品、分类与库存

| 操作 | 权限 / 关键输入与输出 |
| --- | --- |
| `GET /products`、`GET /products/{id}` | P；列表只含上架；旧下架详情返回 200 的 `UNLISTED` 有限投影和不可购买说明，草稿/不存在 404；不返回专属字段 |
| `GET /categories`、`GET /product-options` | P；启用分类与玩法/场景受控选项 |
| `GET /admin/products`、`GET /admin/products/{id}` | A；可查草稿、下架和专属管理字段 |
| `POST /admin/products`、`PATCH /admin/products/{id}` | A；创建 K，编辑 V；允许不完整草稿；创建可有 initialStock，编辑不接受 stock/initialStock |
| `POST /admin/products/{id}/publish`、`/unpublish` | A，K/V；发布校验完整字段，改变当前可售状态并审计 |
| `GET /admin/categories`、`POST /admin/categories`、`PATCH /admin/categories/{id}` | A；创建 K，编辑 V；name、description、sortOrder、enabled；禁用引用中分类返回 409 |
| `DELETE /admin/categories/{id}` | A，V；仅无引用时可移除；商品统一下架，不提供物理删除商品接口 |
| `POST /admin/products/{id}/stock-adjustments` | A，K；direction=INCREASE/DECREASE、quantity、reason；原子增减，禁止绝对库存覆盖 |
| `GET /admin/products/{id}/stock-movements` | A；分页库存流水 |

商品主要输入：`sku,name,categoryId,summary,description,ageMin,ageMax,playType,scene,material,dimensions,packageContents,instructions,safetyNotes,mainImageId,imageIds,retailUnitPriceFen,dealerEnabled,dealerReferenceUnitPriceFen,minInquiryQuantity,leadTimeText`。规格/单位与各文本字段的 schema 由 B 按 SRS 3.4 明确，E 提供 imageId 引用校验。

草稿允许缺发布必填项但已填值必须合法；SKU 首次赋值后不可改；启用经销业务必须补齐参考价、最小量、交期。商品公开详情库存只返回可售状态，精确库存用于经销目录、管理及合法交易反馈。

### 5.4 C：购物车与零售

| 操作 | 权限 / 关键输入与输出 |
| --- | --- |
| `GET /cart` | U-Own；items、当前价、数量、有效性原因、totalFen、cartVersion；历史无效项不自动丢弃 |
| `POST /cart/items` | U-Own，K；productId、quantity，按同商品累加；每行 1—99、整车 ≤20 行 |
| `PATCH /cart/items/{productId}`、`DELETE /cart/items/{productId}`、`DELETE /cart/items` | U-Own，V；PATCH 为设置新 quantity；修改/清空使用 cartVersion 检查 |
| `POST /checkout-previews` | U-Own；无业务扣减，返回整车服务器预览、previewToken、cartVersion |
| `POST /orders` | U-Own，K；previewToken、cartVersion、shippingAddress、remark；创建待付款订单 |
| `GET /orders`、`GET /orders/{id}` | U-Own；分页/状态筛选及本人快照详情 |
| `POST /orders/{id}/mock-payments` | U-Own，K/V；outcome=SUCCESS/FAILURE；结果 mode=SIMULATED，按状态机处理 |
| `POST /orders/{id}/cancel`、`/confirm-receipt` | U-Own，K/V；取消 reason 必填；确认收货无客户端状态赋值 |
| `GET /admin/orders`、`GET /admin/orders/{id}` | A；状态/时间筛选和管理详情 |
| `POST /admin/orders/{id}/cancel`、`/mock-shipment` | A，K/V；取消 reason；发货 carrierName、trackingNumber；均整单 |

收货地址输入统一为 `recipient,phone,countryOrRegion,region,city,addressLine`；`region` 在地区存在省/州层级时必填，无该层级时可空，前后端按共同地区配置处理。收件人 2—50、国家/城市受控值或 2—100 字符、详细地址 5—200；电话按 SRS 规范化为可选 `+` 加 6—20 位数字。备注选填、最多 2000 字符（此为本设计补充约束）。

### 5.5 D：经销合作、询价与渠道

| 操作 | 权限 / 关键输入与输出 |
| --- | --- |
| `GET /channels`、`GET /channels/{id}` | P；countryOrRegion、city；仅明确发布且关联合作有效的公开渠道 |
| `POST /dealer-applications` | U，K；首次申请；已有待审核/已获批或暂停资格者拒绝 |
| `GET /dealer-applications`、`GET /dealer-applications/{id}` | U-Own；当前申请及历史版本/公开审核结果 |
| `POST /dealer-applications/{id}/resubmit` | U-Own，K/V；仅驳回后重提，追加版本，不覆盖旧稿 |
| `GET /admin/dealer-applications`、`GET /admin/dealer-applications/{id}` | A；全部版本、同企业名/地区疑似重复提示 |
| `POST /admin/dealer-applications/{id}/review` | A，K；applicationVersion、decision=APPROVE/REJECT、publicReason、internalNote、可选 existingCompanyId |
| `GET /dealer/catalog`、`GET /dealer/catalog/{productId}` | D；B 提供上架且启用经销且配置完整的投影；单独标注参考价 |
| `POST /inquiries` | D，K；items、expectedDeliveryDate、deliveryNotes、purpose、remark；后三项可空，日期也选填 |
| `GET /inquiries`、`GET /inquiries/{id}`、`POST /inquiries/{id}/close` | U-Own；历史读取不要求合作仍有效，关闭 K/V；原价格快照可读 |
| `GET /admin/inquiries`、`GET /admin/inquiries/{id}` | A；筛选/查看 |
| `POST /admin/inquiries/{id}/start`、`/replies`、`/close` | A，K/V；回复正文必填，可带每行参考价/交期；关闭原因必填 |
| `GET /admin/companies`、`GET /admin/companies/{id}`、`PATCH /admin/companies/{id}` | A；修改 V，并附依据工单及变更原因；企业主用户不可任意换绑 |
| `POST /admin/companies/{id}/suspend`、`/restore` | A，K/V；reason 必填；暂停同步下线关联渠道 |
| `GET /admin/channels`、`GET /admin/channels/{id}`、`POST /admin/channels`、`PATCH /admin/channels/{id}` | A；创建 K，编辑 V；独立公开字段与可选 companyId，检查公开意愿 |
| `POST /admin/channels/{id}/publish`、`/unpublish` | A，K/V；关联暂停企业不能发布；恢复合作也需人工重新发布 |

申请输入：`companyName,businessType,countryOrRegion,city,contactName,phone,cooperationEmail,businessChannels,website,cooperationIntent,publicChannelConsent`。businessType 固定 RETAIL/WHOLESALE/IMPORT/EDUCATION_ACTIVITY/OTHER；企业名称 2—100、合作意向 10—2000，电话/邮箱复用公共校验；经营渠道说明必填 1—2000（本设计补充上限），网站可空。申请公开意愿默认 false，不等于申请本身公开。

询价 `items=[{productId,quantity}]` 共 1—20 个不同商品，数量 ≥ 当前最小量且 ≤9999；允许超过库存，不重用零售“数量不能超过库存”的验证器。回复行通过原询价 `itemId` 关联，可选 `referenceUnitPriceFen`、`leadTimeText`，不得悄悄替换原始快照或增加采购行。

### 5.6 E：内容、媒体与文件

| 操作 | 权限 / 关键输入与输出 |
| --- | --- |
| `GET /site-settings`、`GET /home` | P；公开品牌、说明版本、联系信息；已启用 Banner、按序有效推荐商品与内容 |
| `GET /articles`、`GET /articles/{id}`、`GET /faqs` | P；仅发布内容，草稿/下线直接请求 404；FAQ 支持分类/商品筛选 |
| `GET /files`、`GET /files/{id}` | P/U/D/A 按逐条可见性过滤；type、productId；私有元数据无权不可见 |
| `GET /files/{fileId}/versions/{downloadId}/content` | 二进制流；校验逻辑资料有效、当前下载版本、当前身份与合作；旧 downloadId 为 404 |
| `GET /admin/site-settings`、`PATCH /admin/site-settings` | A；V；品牌/联系方式/说明及版本；不接收任意配置键 |
| `GET /admin/home`、`PATCH /admin/home` | A；V；推荐商品 ID 与顺序，按 B 投影校验 |
| `GET/POST /admin/articles`、`GET/PATCH /admin/articles/{id}` | A；创建 K，编辑 V；title、body、summary、关联商品与媒体 |
| `GET/POST /admin/faqs`、`GET/PATCH /admin/faqs/{id}` | A；创建 K，编辑 V；question、answer、category、关联商品 |
| `GET/POST /admin/banners`、`GET/PATCH /admin/banners/{id}` | A；创建 K，编辑 V；title、imageId、buttonText、targetUrl、sortOrder |
| `POST /admin/{articles|faqs|banners}/{id}/publish`、`/unpublish` | A，K/V；花括号表示三组独立路径，不是任意资源名入口 |
| `GET /admin/media`、`POST /admin/media`、`DELETE /admin/media/{id}` | A；上传 multipart；删除 V 并检查引用，上传失败不生成可见空记录 |
| `GET /admin/files`、`GET /admin/files/{id}`、`POST /admin/files`、`PATCH /admin/files/{id}` | A；上传 PDF + 元数据；编辑 V；visibility=PUBLIC/DEALER/INTERNAL，权限变化审计 |
| `POST /admin/files/{id}/replace`、`/publish`、`/unpublish` | A；replace 为 multipart/V，发布/下线 K/V；替换生成新下载标识 |

表中 `GET/POST`、`GET/PATCH` 表示分别定义两项操作。图片仅 JPEG/PNG/WebP，≤5 MiB；资料仅 PDF，≤10 MiB；扩展名、声明 MIME 与实际内容三者校验。资源不存在/无权读取返回通用错误，不暴露存储路径。

富文本的允许标签、属性和链接协议由 E 集中定义，服务端清洗后保存/返回，测试危险脚本、事件属性和脚本 URL。不能仅相信编辑器已经过滤。

### 5.7 F：工单、总览和审计

| 操作 | 权限 / 关键输入与输出 |
| --- | --- |
| `POST /tickets` | U，K；type=GENERAL/PRODUCT/AFTER_SALES、subject、body、可选 phone；按类型要求 productId/orderId |
| `GET /tickets`、`GET /tickets/{id}` | U-Own；原文、公开对话、状态、时间，不含 internalNotes |
| `POST /tickets/{id}/messages`、`/close` | U-Own，K/V；仅未关闭，消息追加不覆盖；关闭需界面确认 |
| `GET /admin/tickets`、`GET /admin/tickets/{id}` | A；type、status、时间筛选；后台包含内部备注 |
| `POST /admin/tickets/{id}/start`、`/replies`、`/internal-notes`、`/close` | A，K/V；公开回复非空；关闭原因必填；关闭后全部只读 |
| `GET /admin/dashboard` | A；start、end 时间戳；返回第 10 节定义的统计与 asOf |
| `GET /admin/audit-logs`、`GET /admin/audit-logs/{id}` | A；只读、分页，可按 actor/action/object/时间筛选；不提供修改/删除接口 |

工单 subject 2—100、body 10—2000；公开回复 1—2000、内部备注 ≤2000；必须填写的原因 2—500。GENERAL 不附业务关联，PRODUCT 必须关联商品，AFTER_SALES 必须关联本人订单。无用户附件、无自动退款，无“恢复已关闭工单”的隐含操作。

## 6. 关键完整契约：结算、重试与状态命令

### 6.1 结算预览与订单创建

`POST /checkout-previews` 读取并校验整车，生成服务端持久化或可验签的预览凭据，绑定当前用户、购物车版本、商品/数量和当时零售单价。基线预览有效期 15 分钟（设计参数，可在 M0 调整）；**预览过期不是待付款订单过期**，SRS 的待付款订单仍无自动关闭。

预览响应 `data` 示例：

```json
{
  "previewToken": "preview_opaque_value",
  "cartVersion": 7,
  "expiresAt": "2026-09-05T03:15:00.000Z",
  "currency": "CNY",
  "items": [
    { "productId": "p_a", "sku": "WM-A", "name": "测试商品 A", "quantity": 3, "unitPriceFen": 2990, "subtotalFen": 8970 },
    { "productId": "p_b", "sku": "WM-B", "name": "测试商品 B", "quantity": 1, "unitPriceFen": 5900, "subtotalFen": 5900 }
  ],
  "shippingFen": 0,
  "taxFen": 0,
  "discountFen": 0,
  "totalFen": 14870
}
```

用户确认后：

```http
POST /api/v1/orders
Content-Type: application/json
Idempotency-Key: 7bd2daed-1bca-46a9-a46f-f13e662f0c91
X-CSRF-Token: <当前会话令牌>
```

```json
{
  "previewToken": "preview_opaque_value",
  "cartVersion": 7,
  "shippingAddress": {
    "recipient": "测试用户",
    "phone": "+8613800000000",
    "countryOrRegion": "中国",
    "region": "上海市",
    "city": "上海市",
    "addressLine": "测试路 100 号（测试资料）"
  },
  "remark": null
}
```

首次创建返回 201，`data` 至少含 `id,orderNumber,status,version,currency,totalFen,mode,createdAt`，其中 status=PENDING_PAYMENT、mode=SIMULATED。订单详情另返回地址、明细与金额快照、状态历史和 `allowedActions`。`allowedActions` 是展示提示，命令仍执行服务端检查。

服务端顺序必须是：

1. 校验会话、账户、动作权限、基本输入与请求键格式。
2. 按“当前用户 + 稳定 operationId + 请求键”检查幂等记录。已有同内容完成结果则返回原创建结果；先确认资源归属，**不重新要求购物车仍非空或预览仍有效**。
3. 首次执行才校验预览有效性与归属，在事务中锁定当前购物车并比对 cartVersion、商品和数量。
4. 在并发保护下读取商品可售状态/当前价/库存。购物车变化返回 CART_CHANGED；价格变化返回 PRICE_CHANGED；给出重新预览/确认入口，不静默改价建单。
5. 用可信零售价生成订单/明细/地址快照，移除本次结算购物车项、递增 cartVersion，写状态历史、审计与幂等结果，一起提交。

本接口不接受 `totalFen` 作为请求字段；若篡改提交总额，统一未知字段规则返回 422，不能按篡改数计价。正常请求金额只按服务端计算为 14870 分。库存不足/下架失败不创建订单、不删购物车；下单成功不扣/预占库存。

### 6.2 幂等规范

**幂等**指相同逻辑请求重试不会增加业务效果，可写为 \(effect(f(f(s,k),k))=effect(f(s,k))\)。HTTP 重试次数可以是多次，业务效果必须是一次；禁用按钮只能减少点击，不能处理双标签页、断网重试和并发请求。

- 本项目将 `Idempotency-Key` 作为自定义业务约定，取 UUID 格式；同一确认请求保存同一键，用户改了内容并重新确认才生成新键。
- 创建订单/申请/工单/询价及清单标 K 的命令必须传键。按规范化后的完整语义请求计算摘要，保留数组顺序，密码不进行有损规范化；不得在日志打印原始敏感请求。
- 数据库唯一键为 `(actorId,operationId,idempotencyKey)`；写结果与业务提交同事务完成。并发同键请求由唯一约束/事务协调，等待完成后返回结果；超过短等待预算返回 409 REQUEST_IN_PROGRESS，可带 Retry-After。
- 同键同内容重放已成功结果返回 200，并设置 `Idempotency-Replayed: true`；资源 ID 与原结果一致。调用方随后 GET 详情获取可能已演进的当前状态。
- 同键不同内容返回 409 IDEMPOTENCY_CONFLICT；状态版本不同也属于内容不同，不可悄悄更换 expectedVersion 重用原键。
- 基线所有完成的 K 请求去重记录至少保留 24 小时，订单创建必须满足 SRS 的最低 24 小时。请求失败且事务回滚时无成功结果可重放；确定业务拒绝可以保存短期失败结果，但不得被误记为成功。
- 先检查当次认证、授权、归属及必要限流，再重放；停用账户不能借旧键取数据。对新建询价的重放仍检查当前专属权限，暂停后可以通过历史 GET 读取自己的原记录。
- 状态动作的一次效果在订单保留期间由状态和业务唯一约束保障，不靠 24 小时缓存。键过期或换键都不能重复扣库存/退款/发货。
- 对文件 multipart 上传不默认执行自动重试；响应未知时先查询已上传文件再由管理员确认。下载与 GET 不产生业务写效果。

### 6.3 乐观版本与状态机

可并发编辑的资源返回正整数 `version`，每次有效修改加 1。V 命令传 `expectedVersion`（购物车为 cartVersion，申请审核为 applicationVersion）；资源存在且权限通过后，版本不符返回 409 VERSION_CONFLICT 和可读当前状态。可以用行锁、条件更新或组合实现，但版本和状态校验不能拆成无保护的两步。

订单只允许：

| 当前状态 | 命令与执行者 | 结果 |
| --- | --- | --- |
| PENDING_PAYMENT | 本人模拟 FAILURE | 保持待付款，记失败，不扣库存 |
| PENDING_PAYMENT | 本人模拟 SUCCESS | 商品仍可售且全单库存足够时 PAID，扣一次 |
| PENDING_PAYMENT | 本人/管理员取消 | CANCELLED，无库存变化 |
| PAID | 本人/管理员取消 | CANCELLED，返还一次并记录整单模拟退款 |
| PAID | 管理员模拟发货 | SHIPPED，库存不变 |
| SHIPPED | 本人确认收货 | COMPLETED，库存不变 |
| CANCELLED/COMPLETED | 查看或关联咨询 | 订单状态不变 |

已成功付款后新键请求 FAILURE/SUCCESS 返回当前状态或 409；本设计统一新键返回 409 STATE_CONFLICT，同键重放走幂等结果。已取消后不得再付款；重复取消/收货不产生额外状态历史。付款成功后另一合法请求取消可以成功；同一 PAID 订单竞争取消与发货，最多一个成功。

工单/询价共用 NEW/PROCESSING/REPLIED/CLOSED 枚举，但不共用所有动作：工单允许用户补充，REPLIED 补充转 PROCESSING；NEW/PROCESSING 补充保持状态；询价不修改原采购行，关闭后新建。关闭后所有写入拒绝。申请 PENDING→APPROVED/REJECTED，驳回重提创建新版本；合作 ACTIVE/SUSPENDED 独立保存。

## 7. 后端模块服务与事务契约

### 7.1 公开服务端口

以下为与语言无关的接口签名；`ctx` 是服务端身份上下文，`tx` 是同一数据库事务句柄，均不能由浏览器提供。模块只能导入其他域的公开接口/DTO，不导入内部 repository 或 ORM entity。

| 提供者 | 服务签名 | 调用方及保证 |
| --- | --- | --- |
| A | `Identity.requireActiveActor(session)` → ActorContext | 所有域；当前 accountStatus、baseRole、actorId，不信任客户端 |
| A | `RateLimit.consume(ctx, bucket)` | D/F 使用同一 `customer-contact-writes` 桶；注册/登录另用规定桶 |
| A | `UnitOfWork.run(callback(tx))` | 所有写用例；异常统一回滚，嵌套调用加入原事务而非独立提交 |
| B | `Catalog.getPublicProducts(ids)` | E/F；仅公开/允许的历史关联信息，不泄漏专属字段 |
| B | `Catalog.getRetailSnapshot(tx, items)` | C；当前名称/SKU/零售价/可售状态/库存，在受保护读取边界内核验 |
| B | `Catalog.getDealerProducts(ctx, query, tx?)` | D；D 先检查资格，B 限定上架+经销启用+配置完整；只给内部可信调用者 |
| B | `Inventory.deductForPayment(tx, orderId, items)` | C；全单原子扣减，校验当前可售状态，失败抛业务冲突，不能部分成功 |
| B | `Inventory.restoreForCancellation(tx, orderId, items)` | C；只按该订单原扣减记录返还一次，即使商品已下架仍恢复库存 |
| D | `DealerAccess.requireActive(ctx, tx?)` → CompanyContext | D/E/B 的专属能力；检查账户与当前合作；企业身份由服务端得出 |
| C | `Orders.requireOwnedReference(ctx, orderId)` → OrderReference | F；校验本人归属，返回最小关联投影，不把全部地址交给工单 |
| E | `Assets.requireValidImages(tx, ids)` | B/E；发布/编辑前校验有效图片 |
| E | `Assets.replaceReferences(tx, sourceType, sourceId, ids, active)` | B/E；维护引用登记，与商品/内容修改同事务；删除检查同一登记 |
| E | `Files.listVisible(ctx, filters)`、`Files.openCurrent(ctx, fileId, downloadId)` | B/D/E 页面；逐次授权与版本检查，返回可读流或通用错误 |
| F | `Audit.append(tx, event)` | 所有域；写关键成功动作，最小必要字段，不独立提交 |
| 各域 | `Metrics.read(tx, start, end)` | F；只读聚合，定义见第 10 节，不开放任意跨表查询写入 |

资格检查与文件权限只依赖公共服务接口，在装配层注入，避免 `identity` 与 `dealership` 相互导入内部实现。A 的认证不需要把 D 的实现嵌进用户实体；业务授权组合在用例或专属访问服务中。

### 7.2 强一致事务边界

事务是一个“要么全部生效、要么全部撤回”的业务包。**模块边界与事务边界可以不同**：B 拥有库存规则，C 的付款用例可以调用 B 并把订单和库存放在一个事务里。

| 用例 / 主责 | 必须一起成功的写入 |
| --- | --- |
| 创建订单 / C | 订单与明细/地址快照 + 本次购物车移除/版本 + 状态历史 + 幂等结果 + 成功审计 |
| 模拟付款 / C+B | 全部库存余额/流水 + 付款结果 + 订单状态/版本/历史 + 幂等结果 + 审计 |
| 已付款取消 / C+B | 订单取消 + 退款结果 + 全部库存返还/流水 + 历史/幂等/审计 |
| 申请通过 / D | 当前版本审核结果 + 唯一企业关系 + 合作 ACTIVE + 审核历史/幂等/审计 |
| 合作暂停 / D | 合作 SUSPENDED + 关联渠道下线 + 历史/幂等/审计；下一次访问读取当前状态 |
| 账户停用 / A | 账户停用 + 会话撤销 + 原因/幂等/审计；不改订单/合作历史 |
| 发布商品/内容 / B+E | 主实体状态/版本 + 当前媒体引用登记 + 必要历史/审计 |
| 工单/询价公开回复或关闭 / F/D | 新消息或原因 + 状态/版本 + 历史/幂等；关闭后不可追加 |

不能用异步消息补偿首期要求的同步结果，也不能让库存服务另开连接先 commit。审计入口写失败时该关键业务事务回滚；非业务诊断日志可独立写，但失败尝试不得记成成功事件。

### 7.3 并发与锁顺序

采用统一锁层次，减少多商品或跨域操作死锁：必要的账户/资格状态 → 幂等记录 → 业务主记录（购物车/订单/申请/内容）→ 按 ID 排序的商品/库存 → 按 ID 排序的媒体 → 附属记录/审计。具体数据库的共享锁/排他锁方案在 M0 记录；每个用例只获取所需锁，同级多条记录按 ID 排序。

- 订单付款/取消/发货串行检查同一订单版本和状态；订单付款再按商品 ID 顺序操作库存，后台调整也走同一库存写服务。
- 商品上下架与付款的可售校验保护同一商品记录；不能先无锁读“上架”再无条件扣库存。价变与结算创建同理。
- 购物车修改与结算锁定同一车头/版本，不能删掉用户并发新加的商品。BR-06 要求的订单创建重试先走幂等命中，再读取购物车。
- 审批锁当前申请版本及归属关系；企业主用户唯一约束处理并发。相同企业名/地区只作提示，不能代替归属约束。
- 已发布图片引用的增加/撤销与删除锁同一媒体记录及引用登记。删除不得反向锁商品再与发布形成锁顺序环。
- 数据库发生死锁/序列化冲突时，只能重试已完整回滚的整个事务并保留原幂等键，采用有界次数；不能重试半段扣库存。次数耗尽返回可理解冲突。

采用其他锁算法也可，但须有 TC-17/19/20/23/28/34/35 的证明；不能仅以单人点击页面成功作为并发正确的证据。

## 8. 文件替换、引用与权限联动

E 区分稳定的逻辑 `fileId` 与当前版本的 `downloadId`。PDF 替换保留逻辑关联，产生新 downloadId，原标识立即不再被业务入口下载。合作暂停后，旧页面中的私有链接再次请求也必须失败。

下载路径处理顺序：解析标识 → 验证逻辑资料/当前版本有效 → 校验当前用户及可见性 → 输出正确 MIME 和安全文件名。无权查询资料详情统一 404；会话缺失 401；有身份但请求专属入口不满足资格可 403。公开列表不返回私有标题。

采用服务端受控流读取私有文件，不返回可长期绕过授权的静态 URL。公开文件若也必须支持下线/旧版本失效，同样经过受控入口，不用永久公开存储地址替代。文件已被用户合法下载到本地的副本无法撤回；这里保证的是之后的业务请求失效。

文件系统写入不具备数据库事务能力，故采用：先上传到不可公开的临时区域并校验 → 准备可读取的新文件 → 数据库事务切换当前版本/状态/审计 → 成功后清理旧孤立文件。数据库失败时旧版本保持有效，临时文件可清理；不能先删除旧文件再尝试写数据库。

图片删除检查发布引用：B/E 在自己的发布、编辑、下线事务里调用 `Assets.replaceReferences`；E 删除时检查登记。所有有效引用均受保护可以作为更严格基线；若只保护发布引用，必须明确草稿失效提示。上传失败不保留可见空记录，同名文件使用系统生成存储标识，不能覆盖其他对象。

## 9. 数据所有权、精度与约束

### 9.1 逻辑实体归属

下表是领域归属，不要求每个逻辑实体机械对应一张表。其他模块可使用 ID 引用，不取得直接修改权。

| 所有者 | 逻辑实体 | 关键约束 |
| --- | --- | --- |
| A | users、sessions、consents；公共 idempotency_records | 规范化邮箱唯一；会话可撤销；业务键唯一；用户秘密不序列化 |
| B | categories、products、inventory_balances、inventory_movements | 分类名 trim 后唯一；SKU 首次赋值后固定且唯一；stock ≥0；库存流水关联原业务 |
| C | carts/cart_items、orders/order_items、payment_attempts、refunds、order_history | (user,product) 唯一；订单号唯一；成功支付/取消返还效果唯一；快照不随主数据变 |
| D | applications/application_versions/reviews、companies、channels、inquiries/items/replies/history | 当前申请每用户至多一份；(application,version) 唯一；company.ownerUser 唯一；审批不抢占他人关系 |
| E | brand_settings、articles、faqs、banners、recommendations、assets/asset_references、files/file_versions | 当前版本唯一；私有元数据隔离；公开状态与引用一致 |
| F | tickets/messages/internal_notes/history、audit_events | 内部备注与公开消息分离；关闭只读；成功审计与业务同边界 |

公共幂等表由 A 维护结构，各域通过统一执行器写入，不允许 A 业务接口任意修改其他域的执行结果。审计类似：F 拥有结构和入口，业务方通过它参与同一事务。

### 9.2 金额与库存不变量

令 \(p_i\) 为分整数，零售总额：

\[
M_{fen}=\sum_{i=1}^{n} p_iq_i,\quad 1\leq n\leq20,\quad 1\leq q_i\leq99
\]

单价上限 999999.99 元即 99,999,999 分，最大整单额为：

\[
99{,}999{,}999\times99\times20=197{,}999{,}998{,}020\text{ 分}
\]

因此数据库金额至少使用能容纳该值的 BIGINT/精确十进制，不能用 32 位整数。该单笔金额在 JavaScript 安全整数范围内；跨任意长时间段的累计金额仍需服务端精确求和。仪表盘聚合金额采用**十进制整数分字符串** `netPaidFen`，避免累计超出前端安全整数范围；普通订单/单价金额仍用有界整数，此差异须体现在各 schema 中。

库存恒满足：

\[
stock_{now}=stock_{initial}+\sum adjustments+\sum cancellationReturns-\sum successfulPayments\geq0
\]

初始库存也记来源。后台只接受正整数 quantity 与方向，普通编辑不能覆盖库存；询价不参与此式。付款/返还流水以 `(orderId,productId,effectType)` 等业务唯一键或等价约束保障单次效果。

### 9.3 快照与引用

订单必须保存名称、SKU、数量、零售单价、金额构成、收货地址和联系方式快照。询价保存名称、SKU、参考价和原需求快照，回复另存，不覆盖原记录。

快照回答“当时发生了什么”，当前主数据回答“现在还允许做什么”。付款使用旧订单价格，但检查当前上架/库存；售后引用旧订单不重算金额；商品下架、合作暂停、账户停用不删除业务历史。

所有输入约束以 SRS 3.4 为完整字典，本文列出跨域关键项；各域 OpenAPI 必须逐字段继承，不能因为本文未重复某长度就省略。数据库唯一/CHECK/FK 约束承担最后防线，但仍需要服务端给出可读的业务错误。

## 10. 统计、审计与时间口径

`GET /admin/dashboard?start=<UTC>&end=<UTC>` 要求 start < end，区间为 \([start,end)\)。页面按 Asia/Shanghai 选择边界后转换为 UTC 提交。响应包含 `asOf`、区间及下表字段；无数据返回数值 0，字符串金额返回 `"0"`。

| 指标 | 提供域 | 口径 / 字段 |
| --- | --- | --- |
| 已上架商品数 | B | 当前上架商品，`publishedProductCount` |
| 启用用户数 | A | 当前启用账户（含管理员，作为本设计明确口径），`activeUserCount` |
| 待审核申请数 | D | 当前版本 PENDING，`pendingApplicationCount` |
| 待处理询价数 | D | NEW + PROCESSING，`pendingInquiryCount` |
| 待处理工单数 | F | NEW + PROCESSING，`pendingTicketCount` |
| 待发货订单数 | C | 当前 PAID，`pendingShipmentCount` |
| 区间创建订单数 | C | createdAt 在区间内，包含所有状态，`createdOrderCount` |
| 区间模拟净成交额 | C | 成功付款时间在区间内的订单原付款额，减这些订单截至本次统计已经完成的模拟退款额，`netPaidFen` 为分整数字符串 |

前六项是当前总量，不受区间筛选；后两项按区间。F 用同一只读数据库快照调用各域聚合，返回共同 asOf，避免同时变化时拼出互相矛盾的数据。后台卡片跳转使用对应筛选条件。

例如 9 月 5 日支付 100 元、9 月 6 日取消退款，重查 9 月 5 日的净成交额应减少 100 元；不能只把退款记在 9 月 6 日而保留旧区间净额。该规则来自 FR-32，不能擅自换成一般财务报表口径。

审计事件至少含 `actorId,action,objectType,objectId,occurredAt,result,reason,changeSummary,requestId`。内部动作枚举和可脱敏摘要由 F 汇总，各域按关键操作表调用。只读、无普通删除入口；秘密凭证、完整非必要个人信息和任意请求体不写入审计。

## 11. 契约评审与集成验收清单

每次新增/修改接口，提供方与消费者共同检查：

- [ ] 方法、路径、operationId、owner 与 FR/TC 对应，页面有真实调用入口。
- [ ] 字段类型、单位、必填/空值、长度、枚举、分页/排序和样例一致。
- [ ] 公开/本人/经销/管理员 DTO 分开，验证未登录、越权与私人资源 404。
- [ ] 错误码及字段错误结构一致，覆盖超时“结果未知”与恢复入口。
- [ ] K/V 命令明确重试键、版本、冲突、幂等记录和状态持久约束。
- [ ] 订单库存、申请资格、渠道下线、成功审计没有分开提交。
- [ ] 数据迁移有所有者、唯一键、引用保护、空库及升级验证。
- [ ] 前端 Mock 与真实响应都通过相同 schema；Mock 成功不等于集成成功。
- [ ] 上下游在同一 `main` 组合中通过有关测试，不只通过本模块测试。

优先以 TC-16/17/19/20/23 验证交易，TC-28/32 验证资格联动，TC-35/36 验证文件引用与失效，TC-11 验证跨用户隔离。完整责任和证据填写在[验收追踪表](WEMOVE需求责任与验收追踪表.md)，在没有执行前保持“未执行”。
