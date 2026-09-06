# WEMOVE 集成应用：角色 A 底座 + 角色 B 商品库存

本目录以角色 A 的可运行纵向切片为单一应用底座，现已接入角色 B 的商品、分类、价格和库存功能。A 覆盖 FR-09、FR-10、FR-11、FR-12、FR-31；B 覆盖 FR-03、FR-04、FR-05、FR-26、FR-27 与 BR-02 的模块边界。

## 目录

- `frontend/`：Vue 3 + TypeScript + Vite + Element Plus。
- `backend/`：Java 21 + Spring Boot + MySQL 8 + Flyway。
- `contracts/openapi.yaml`：OpenAPI 3.1.1 接口契约。
- `docs/`：设计、运行、进度、验收和个人课程报告。

## 快速启动

1. 在本地 MySQL 创建 `wemove` 数据库和专用账户。
2. 复制配置：`cp .env.example .env`，填写本地 MySQL 凭据并替换管理员初始密码。
3. 加载环境变量后启动后端：`cd backend && mvn spring-boot:run`。
4. 启动前端：`cd frontend && npm install && npm run dev`。
5. 打开 `http://localhost:5173/products`；登录管理员后可访问 `/admin/products` 与 `/admin/categories`。

前端开发服务器将 `/api` 代理到 `http://localhost:8080`，浏览器使用同站 Cookie 会话。完整命令、测试账户和故障排查见 [运行与验证手册](docs/运行与验证手册.md)。

## 验证

```bash
cd backend
mvn verify

cd ../frontend
npm ci
npm run test
npm run build
```

生产部署必须启用 HTTPS，设置 `SESSION_COOKIE_SECURE=true`，并使用强随机管理员密码。

## 交付索引

- [角色 A 模块设计](docs/角色A模块设计.md)：边界、数据、安全决策与跨模块端口。
- [接口与集成说明](docs/接口与集成说明.md)：Cookie、CSRF、能力名和接入方式。
- [角色 A 验收追踪](docs/角色A验收追踪.md)：FR/公共底座到实现及验证的映射。
- [角色 A 进度计划](docs/角色A进度计划.md)：阶段、判据与集成里程碑。
- [运行与验证手册](docs/运行与验证手册.md)：从干净环境启动、测试和手工验收。
- [验证记录](docs/验证记录.md)：本次已执行、通过及受环境限制未执行的检查。
- [Web 开发技术现状报告](docs/Web开发技术现状报告.md)：个人报告，姓名、学号和班级保留待填占位符。
- [OpenAPI 契约](contracts/openapi.yaml)：11 条身份与账户管理路径。
- `scripts/mysql-api-smoke.sh`：使用本地 MySQL 的完整身份 API 冒烟脚本。
- `.superdesign/`：设计系统、页面清单和可恢复的设计工具元数据。
- [角色 B 交付](../partB/README.md)：商品与库存设计、验收、运行手册、冒烟脚本及独立 OpenAPI 契约。
