# WEMOVE 工程入口

这里是 WEMOVE 的统一实现目录，包含一个 Vue 3 + TypeScript 前端、一个 Java 21 + Spring Boot 后端，以及契约、脚本、文档和测试设计。当前已接入身份账户和商品库存模块，后续业务继续接入这套应用。

**目录按工程职责划分，业务归属按模块记录。** 前端和后端分别是可构建、可运行的应用；身份账户、商品库存等业务由各应用内部的代码共同实现。成员姓名、分工和贡献放在[团队协作规范](project-requirements/WEMOVE团队分工与协作规范.md)及[验收追踪表](project-requirements/WEMOVE需求责任与验收追踪表.md)中维护，代码位置不随负责人变化。

## 路径约定：先确认起点

**本文件的目录表使用工程相对路径；运行命令统一从仓库根目录执行。** 两者相差一层 `project-implementation/`，不要混用。

| 名称 | 如何识别 | 从仓库根目录定位 |
| --- | --- | --- |
| 仓库根目录 | 同时包含 `README.md`、`AGENTS.md`、`course-materials/` 与 `project-implementation/` | 当前克隆下来的仓库目录 |
| 工程根目录 | 本 README 所在目录，包含 `apps/`、`contracts/`、`scripts/` 等 | `project-implementation/` |
| 前端根目录 | 包含前端 `package.json`、`package-lock.json` 和 `vite.config.ts` | `project-implementation/apps/web/` |
| 后端根目录 | 包含 `pom.xml` 和 `src/` | `project-implementation/apps/api/` |

例如，目录表中的 `apps/web/src/pages/`，从仓库根目录访问时应写成 `project-implementation/apps/web/src/pages/`。仓库根目录下不另建 `apps/`、`frontend/` 或 `backend/`；所有成员使用这里已有的两个应用。

- 复制本 README 的命令前，先回到仓库根目录。已经进入前端目录时，可以直接运行 `npm run dev`；已经进入后端目录时，可以直接运行 `mvn verify`。不要在这些子目录里再次追加 `project-implementation/` 前缀。
- 文档中的相对链接以**当前 Markdown 文件所在目录**为起点；终端中的相对路径以**终端当前目录**为起点。例如，本文件引用 `apps/web/`，仓库根 README 引用同一目录时写 `project-implementation/apps/web/`。
- 页面地址、接口地址和磁盘路径分别维护：浏览器地址 `/products` 由 `src/router.ts` 汇总的路由决定；接口地址由 HTTP 契约定义；它们不会因为源码文件夹移动而自动改变。
- 共享说明与脚本使用相对路径，不写成员电脑上的 `/Users/...` 或 `C:/Users/...`。终端中包含空格的本地路径需要加引号。

## 目录与存放规则

下表路径均相对于本目录 `project-implementation/`。

| 目录或文件 | 存放内容 | 维护规则 |
| --- | --- | --- |
| [apps/web/](apps/web/) | 统一前端，包名 `wemove-web` | 页面、组件、请求与状态逻辑放在 `src/`；公共运行素材放在 `public/` |
| [apps/api/](apps/api/) | 统一后端，构件名 `wemove-api` | Java 源码在 `src/main/java/`，配置在 `src/main/resources/` |
| [apps/api/src/main/resources/db/migration/](apps/api/src/main/resources/db/migration/) | Flyway 数据库迁移 | V1 为身份底座，V2 为商品库存；新变更追加有序迁移 |
| [contracts/openapi/](contracts/openapi/) | 身份账户与商品库存的接口契约 | 按业务域命名，接口变更同步提供方、调用方和测试 |
| [scripts/smoke/](scripts/smoke/) | 可复现的 API 冒烟脚本 | 身份账户用 `identity-api.sh`，商品库存用 `catalog-api.ps1` |
| [tests/cases/](tests/cases/) | 成员及跨模块验收用例、审核总览 | 保留需求编号、前置条件、步骤、预期结果与审核状态 |
| [docs/modules/](docs/modules/) | 各模块的设计、运行说明、进度与验证记录 | 按业务域 `identity/`、`catalog/` 分类，保留成员贡献证据 |
| [docs/design/references/identity/](docs/design/references/identity/) | 身份账户页面的设计参考图片 | 参考资料与运行时图片分别维护 |
| [docs/verification/](docs/verification/) | 工程验证记录与模块验证截图 | 商品截图位于 `catalog/`；证据与对应验证记录相互链接 |
| [project-requirements/](project-requirements/) | 需求、分工、接口协作与验收追踪文档 | 当前业务范围以 SRS 为准；原始参考文档留在 `references/` |
| [ideation/](ideation/) | 早期构想 | 当前实施范围以需求基线为准 |
| [project_flowchart_view/](project_flowchart_view/) | 项目进度与流程展示资料，含甘特图 PDF 和可编辑的 Excel 文件 | 随仓库共享，调整计划时同步维护展示稿与可编辑文件 |
| [前端设计视觉参考/](前端设计视觉参考/) | 前端风格与视觉参考图片 | 随仓库共享；实际页面使用的素材放入 `apps/web/public/` |
| `.env.example`、`.env` | 共享环境模板与本地配置 | 使用同一套后端环境配置；本地值不写入模板 |
| `.local/`、`.superdesign/` | 本地运行产物、设计工具元数据 | 工具目录不承载业务源码；`.local/` 不纳入版本控制 |

前端单元测试继续放在源码附近的 `*.spec.ts` 中，后端单元测试放在 [apps/api/src/test/](apps/api/src/test/)。这样修改功能时可以同时找到测试；`tests/cases/` 则集中维护需要审核的业务验收设计。

本次整理保留现有 Java 包结构 `com.wemove.identity`，商品库存仍在其 `catalog` 子包中；数据库迁移保留在 Flyway 默认扫描位置。后续新增功能按业务域扩展，并与现有认证、数据访问和事务边界保持一致。

## 查找一个业务模块

| 业务模块 | 接口契约 | 设计与运行资料 | 当前成员 |
| --- | --- | --- | --- |
| 身份与账户 | [identity.yaml](contracts/openapi/identity.yaml) | [模块索引](docs/modules/identity/README.md)、[运行与验证手册](docs/modules/identity/运行与验证手册.md)、[验证记录](docs/modules/identity/验证记录.md) | A |
| 商品与库存 | [catalog.yaml](contracts/openapi/catalog.yaml) | [模块索引](docs/modules/catalog/README.md)、[运行与验证手册](docs/modules/catalog/运行与验证手册.md)、[验证记录](docs/modules/catalog/验证记录.md) | B |

商品前端逻辑入口见 [apps/web/src/features/catalog/](apps/web/src/features/catalog/)，商品后端入口见 [apps/api/src/main/java/com/wemove/identity/catalog/](apps/api/src/main/java/com/wemove/identity/catalog/)。其余成员的需求归属和接入边界见[验收追踪表](project-requirements/WEMOVE需求责任与验收追踪表.md)。

## 本地启动

环境要求：JDK 21、Maven 3.9+、Node.js 20+、npm 10+、MySQL 8.0+。先按[身份账户运行手册](docs/modules/identity/运行与验证手册.md)创建本地数据库、设置数据库账户和管理员初始配置。PowerShell 示例见[商品库存运行手册](docs/modules/catalog/运行与验证手册.md)。

以下各代码块均从**仓库根目录**执行，也就是包含 `course-materials/` 和 `project-implementation/` 的目录。

首次配置时，确认尚无自己的 `.env`，再复制模板并编辑其中的本地参数：

```bash
cp project-implementation/.env.example project-implementation/.env
```

后端配置通过环境变量读取；启动前在当前终端加载 `.env`：

```bash
set -a
. ./project-implementation/.env
set +a
mvn -f project-implementation/apps/api/pom.xml spring-boot:run
```

另开终端，安装前端依赖并启动：

```bash
npm --prefix project-implementation/apps/web ci
npm --prefix project-implementation/apps/web run dev
```

访问 `http://localhost:5173/products` 浏览商品，或访问 `/register` 注册账户。前端开发服务器将 `/api` 代理到 `http://localhost:8080`。管理员登录后可访问 `/admin/products` 和 `/admin/categories`。

## 构建与验证

安装好依赖后，从**仓库根目录**执行：

```bash
mvn -f project-implementation/apps/api/pom.xml verify
npm --prefix project-implementation/apps/web run test
npm --prefix project-implementation/apps/web run build
```

后端构建产物生成在 `apps/api/target/`，前端构建产物生成在 `apps/web/dist/`；均相对于本工程目录，且不纳入版本控制。前端构建同时执行 Vue TypeScript 类型检查。

| 检查 | 当前内容 | 如何解读结果 |
| --- | --- | --- |
| 自动化单元测试 | 前端 4 项、后端 9 项 | 验证已有局部逻辑；结果以本次命令输出为准 |
| API 冒烟 | 身份账户与商品库存两个脚本 | 均需测试数据库；身份脚本从已打包 JAR 自行启动后端，商品脚本访问已运行的应用；步骤与数据影响见各模块运行手册 |
| 成员与集成验收设计 | [审核总览](tests/cases/WEMOVE成员测试用例审核总览.md)中的 159 条用例 | 当前待审核、未执行；单元测试通过不改变这些用例的审核或执行状态 |

身份账户冒烟脚本要求 Linux 或 WSL 环境以及 Bash、curl、jq；需先打包后端并确保对应端口空闲，脚本结束时会停止自己启动的后端。商品库存脚本要求 PowerShell 和已运行的测试应用。API 冒烟会创建或修改测试数据，执行前按运行手册准备对应测试环境。

目前没有配置 CI 工作流或测试覆盖率门槛。每次修改应记录实际执行的检查，模块验证记录与测试设计分别维护。本次目录调整的实际检查及范围见[工程目录重组验证记录](docs/verification/工程目录重组验证记录.md)。

## 新增工作放在哪里

以下路径相对于工程根目录。`<业务域>` 是占位符，创建文件时替换成约定的英文名称，不创建带尖括号的目录。

| 工作类型 | 存放位置 | 接入要求 |
| --- | --- | --- |
| 前端页面 | `apps/web/src/pages/`；新业务可在其下创建 `<业务域>/` | 已有页面保持原位置；在对应业务路由中引用新页面 |
| 业务路由、局部逻辑 | `apps/web/src/features/<业务域>/` | 路由导出放在 `routes.ts`，并在唯一的 `apps/web/src/router.ts` 中导入和注册 |
| 共用布局、组件、请求和会话 | `apps/web/src/components/`、`apps/web/src/services/http.ts`、`apps/web/src/stores/session.ts` | 复用已有入口，跨模块变更与受影响成员一起评审 |
| 网站运行素材 | `apps/web/public/` | 例如 `public/brand-logo.png` 对应浏览器路径 `/brand-logo.png`；设计参考和验收截图放到 `docs/` |
| 后端业务代码 | `apps/api/src/main/java/com/wemove/identity/<业务域>/` | 参照现有 `catalog/` 分为 `api`、`service`、`domain`、`repository`；跨模块端口按需放 `platform` |
| 数据库迁移 | `apps/api/src/main/resources/db/migration/` | 使用全局唯一版本号；先协调其他成员正在编写的版本，已应用的迁移不直接改写 |
| HTTP 接口契约 | `contracts/openapi/<业务域>.yaml` | 当前已有 `identity.yaml`、`catalog.yaml`；新增或变更接口同步实现、调用方和用例 |
| 前端单元测试 | 被测源码附近的 `*.spec.ts` | 使用共用前端的测试命令 |
| 后端单元测试 | `apps/api/src/test/java/` 下与被测代码对应的包路径 | 使用共用后端的测试命令 |
| API 冒烟脚本 | `scripts/smoke/` | 按业务命名；注明执行环境、启动方式和测试数据影响，文件定位基于脚本自身位置 |
| 模块设计、运行和交付记录 | `docs/modules/<业务域>/` | 添加或更新模块 `README.md`，在本文件的模块索引中登记 |
| 验证截图、测试用例 | `docs/verification/<业务域>/`、`tests/cases/` | 证据链接对应记录；用例审核状态与执行结果分别维护 |

业务域沿用分工文档：A 为 `identity`，B 为 `catalog`，C 为 `commerce`，D 为 `dealership`，E 为 `content`，F 按职责使用 `support` 和 `operations`。**成员编号用于分工，业务域用于代码和模块资料命名；不再创建 `partA/`、`partB/`、`partC/` 等成员目录。** 当前仅 `identity`、`catalog` 已有实现，其他名称是后续接入约定，不表示目录或功能已经存在。

后端启动类目前位于 `com.wemove.identity`，现有默认扫描范围由此展开，所以新增业务包先放在它的子包中。例如零售交易代码使用 `com.wemove.identity.commerce`，文件落在 `apps/api/src/main/java/com/wemove/identity/commerce/`。这里的 `identity` 是保留的 Java 根包名，不表示其他模块归成员 A 所有。身份模块自身沿用根包下现有的 `api/`、`service/` 等目录，不再嵌套一层 `identity/`。如果以后调整根包，必须一起处理启动扫描、实体、仓库、测试及所有引用。

以新增“购物车页面”为例，下面是接入位置示例，尚未创建这些文件：

1. 页面放在 `apps/web/src/pages/commerce/CartPage.vue`。
2. 在 `apps/web/src/features/commerce/routes.ts` 引用该页面；从这个路由文件出发，相对导入路径为 `../../pages/commerce/CartPage.vue`。
3. 在 `apps/web/src/router.ts` 导入并展开该模块路由，复用现有会话和权限守卫；路由权限不能替代后端授权。
4. 后端功能进入 `apps/api/src/main/java/com/wemove/identity/commerce/`，对应契约进入 `contracts/openapi/commerce.yaml`；测试和模块文档按上表放置。

各模块共用一个前端依赖清单与锁文件、一个后端 `pom.xml`、一套环境模板及数据库迁移入口。新增模块时不复制这些文件，也不另建登录系统或第二个应用启动入口。

## 旧路径如何对应

以下两列均相对于 `project-implementation/`。更早直接放在仓库根目录的 `partA/`、`partB/` 同样已停用；旧资料只能用于追溯，实际编辑和运行以右列为准。

| 已停用路径 | 当前路径 |
| --- | --- |
| `partA/frontend/` | `apps/web/` |
| `partA/backend/` | `apps/api/` |
| `partA/contracts/openapi.yaml` | `contracts/openapi/identity.yaml` |
| `partB/contracts/catalog.openapi.yaml` | `contracts/openapi/catalog.yaml` |
| `partA/scripts/mysql-api-smoke.sh` | `scripts/smoke/identity-api.sh` |
| `partB/scripts/catalog-api-smoke.ps1` | `scripts/smoke/catalog-api.ps1` |
| `partA/docs/`、`partA/README.md` | `docs/modules/identity/` 及其 `README.md` |
| `partB/docs/`、`partB/README.md` | `docs/modules/catalog/` 及其 `README.md` |
| `partA/picture/` | `docs/design/references/identity/` |
| `partB/screenshots/` | `docs/verification/catalog/` |
| `project-tests/` | `tests/cases/` |
| `partA/` 下的 `.env.example`、`.env`、`.local/`、`.superdesign/` | 工程根目录下的同名文件或目录 |

成员已有未合并分支如果仍使用旧路径，合并前按此表迁移改动并核对新位置的同一份代码，避免把旧目录重新带回主线。调整文件位置时同时修改导入、路由注册、脚本、文档链接及忽略规则，并执行受影响的构建和测试。

仓库协作、分支和 PR 流程见[根目录 README](../README.md)，开发与文档约定见 [AGENTS.md](../AGENTS.md)。
