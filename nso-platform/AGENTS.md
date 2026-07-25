# 项目说明

本项目是“智慧非标订单打样与变更协同平台 V1.0”。

完整需求文档位于：

- docs/product-specs/智慧非标订单打样与变更协同平台V1.0_需求规格说明书_V1.3_三端总体架构版.md

## 当前工程结构

- nso-common：通用模块，放置注解、枚举、异常、基础类、工具类
- nso-system：系统模块，放置用户、角色、菜单、部门、字典等 Domain/Mapper/Service，不放 Controller
- nso-framework：框架模块，放置 Security、JWT/微信认证、AOP、全局异常、配置类
- nso-server：Spring Boot 启动模块，集中放置所有 Controller、业务包和小程序接口
- nso-web：Vue 3 管理端
- nso-miniapp：uni-app 微信小程序
- docs：需求、数据库、接口和开发计划文档
- deploy：部署脚本、Nginx、Docker 等配置

后端 Maven 依赖链固定为：

```text
nso-common -> nso-system -> nso-framework -> nso-server
```

## 技术基线

- Java 17
- Spring Boot 3.x
- Spring Security 6.x
- MySQL 8
- Redis 8
- Flyway
- Vue 3
- TypeScript
- Element Plus
- uni-app

说明：当前本机和后端工程先使用 JDK 17，符合 Spring Boot 3.x 最低要求；后续如统一升级 Java 21，再集中调整 Maven、IDE 和部署环境。

## 工程约束

- 后端采用 4 个 Maven 模块的模块化单体架构。
- 业务功能在 `nso-server/src/main/java/com/nso/modules/<name>` 下按包区分，不再为每个业务功能创建独立 Maven 模块。
- PC 管理端 Controller 放在 `com.nso.web.controller`。
- 微信小程序接口放在 `com.nso.mp`。
- `nso-common` 不得依赖其他内部模块。
- `nso-system` 只能依赖 `nso-common`。
- `nso-framework` 可以依赖 `nso-common` 和 `nso-system`。
- `nso-server` 作为启动和接口聚合层，依赖 `nso-framework`。
- 不得在 Controller 中直接编写复杂业务逻辑。
- Controller 只负责参数接收、校验和响应。
- 核心业务规则放在 Application 或 Domain 层。
- 所有数据库变更必须通过 Flyway 脚本完成。
- 禁止手工修改已经发布的 Flyway 脚本。
- 所有接口统一使用 /api/v1 前缀。
- 所有业务异常使用统一错误码。
- 核心数据以 MySQL 为准，Redis 不得作为唯一数据源。
- 重要写操作必须考虑事务、幂等和并发控制。
- 权限校验同时包含接口权限和数据权限。
- 每次任务完成后必须运行相关测试或构建命令。
- 不得修改与当前任务无关的模块。

## 验证命令

后端，当前可用：

```bash
mvn clean test
```

Web 端，初始化 `package.json` 后执行：

```bash
cd nso-web
npm install
npm run build
```

小程序端，当前可用：

```bash
cd nso-miniapp
npm install
npm run type-check
```

部署配置：

```bash
docker compose config
```
