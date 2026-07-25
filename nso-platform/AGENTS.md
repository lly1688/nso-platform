# 项目说明

本项目是"智慧非标订单打样与变更协同平台 V1.0"。

完整需求文档位于：

- docs/development-plan/智慧非标订单打样与变更协同平台V1.0_需求规格说明书_V1.5_若依标准模块架构版.md

## 当前工程结构

- nso-common：通用模块，放置注解、枚举、异常、基础类、工具类
- nso-system：系统与业务模块，放置用户/角色/菜单/部门 Domain/Mapper/Service，以及 business/ 下九个核心业务域
- nso-framework：框架模块，放置 Security、JWT/微信认证、AOP、全局异常、Redis、存储、BaseController
- nso-quartz：定时任务模块，放置风险扫描、临期提醒、超时升级、消息重试、报表汇总
- nso-generator：代码生成模块，放置表结构读取、模板引擎、代码预览与下载
- nso-admin：Spring Boot 启动与接口模块，集中放置 auth、system、business、miniapp、publicapi Controller
- nso-ui：Vue 3 + TypeScript + Element Plus PC 管理端
- docs：需求、数据库、接口和开发计划文档
- deploy：部署脚本、Nginx、Docker 等配置

后端 Maven 依赖链固定为：

```text
nso-common
    ↑
nso-system
    ↑
nso-framework

nso-quartz  ──→ nso-common、nso-system
nso-generator ──→ nso-common
nso-admin  ──→ nso-framework、nso-system、nso-quartz、nso-generator
```

## 技术基线

- Java 17
- Spring Boot 3.5.x
- Spring Security + JWT
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 8.6
- Redisson
- Flyway
- MinIO
- Quartz
- Vue 3 + TypeScript + Vite + Element Plus
- Pinia + Axios + ECharts

## 工程约束

- 后端采用 6 个 Maven 模块的模块化单体架构。
- 九个业务域统一放在 `nso-system/src/main/java/com/nso/business/<name>`，采用 Controller → Service → Mapper 三层调用。
- PC 管理端 Controller 放在 `nso-admin/web/controller/business`。
- 微信小程序 Controller 放在 `nso-admin/web/controller/miniapp`。
- 客户短期确认 Controller 放在 `nso-admin/web/controller/publicapi`。
- `nso-common` 不得依赖其他内部模块，保持轻量（不含 Spring Security、Redis、MinIO 等）。
- `nso-system` 只能依赖 `nso-common`；如需缓存/锁/存储，通过 Port 接口由 `nso-framework` 实现。
- `nso-framework` 可以依赖 `nso-common` 和 `nso-system`。
- `nso-admin` 作为启动和接口聚合层，依赖 `nso-framework`、`nso-system`、`nso-quartz`、`nso-generator`。
- Controller 只负责权限校验、参数接收和响应封装，不直接调用 Mapper。
- Service 负责事务、状态迁移、规则校验和跨业务编排。
- 核心业务规则放在 Service 层。
- 所有数据库变更必须通过 Flyway 脚本完成，生产脚本统一放在 `nso-admin/src/main/resources/db/migration`。
- 禁止手工修改已经发布的 Flyway 脚本。
- 接口前缀：`/api/v1/admin/**`（PC）、`/api/v1/mp/**`（小程序）、`/api/v1/public/**`（客户确认）。
- 所有业务异常使用统一错误码。
- 核心数据以 MySQL 为准，Redis 不得作为唯一数据源。
- 重要写操作必须考虑事务、幂等和并发控制。
- 权限校验同时包含接口权限和数据权限。
- 每次任务完成后必须运行相关测试或构建命令。
- 不得修改与当前任务无关的模块。

## 验证命令

后端：

```bash
mvn clean test
```

PC 管理端：

```bash
cd nso-ui
npm install
npm run build
```

部署配置：

```bash
docker compose config
```
