# 智慧非标订单打样与变更协同平台 V1.0

这是一个包含后端、Web 管理端、微信小程序和部署文档的总项目仓库。

## 目录结构

```text
nso-platform/
├─ AGENTS.md
├─ README.md
├─ docker-compose.yml
├─ pom.xml
├─ db/
│  ├─ migration/
│  ├─ init/
│  └─ demo/
├─ docs/
│  ├─ product-specs/
│  ├─ database/
│  ├─ api/
│  ├─ deployment/
│  └─ development-plan/
├─ deploy/
├─ nso-common/
├─ nso-system/
├─ nso-framework/
├─ nso-server/
├─ nso-web/
└─ nso-miniapp/
```

## 模块说明

- `nso-common`：通用模块，放置注解、枚举、异常、基础类和工具类。
- `nso-system`：系统模块，放置用户、角色、菜单、部门、字典等 Domain、Mapper、Service，不放 Controller。
- `nso-framework`：框架模块，放置 Spring Security、JWT/微信认证、AOP、全局异常和配置类。
- `nso-server`：Spring Boot 启动模块，集中放置 PC 管理端 Controller、小程序接口和业务模块包。
- `nso-web`：Vue 3 + TypeScript PC 管理端。
- `nso-miniapp`：uni-app + Vue 3 微信小程序。
- `docs/product-specs`：产品需求规格说明书，作为开发事实来源。
- `docs/database`：数据库设计和迁移说明。
- `docs/api`：接口设计和联调说明。
- `docs/development-plan`：阶段计划和验收记录。
- `deploy`：Docker、Nginx 和部署脚本。

后端 Maven 依赖链：

```text
nso-common -> nso-system -> nso-framework -> nso-server
```

## 本地基础服务

复制环境变量示例后按需修改：

```bash
copy .env.example .env
```

启动 MySQL、Redis、MinIO：

```bash
docker compose up -d
```

检查 Docker Compose 配置：

```bash
docker compose config
```

## 后端验证

```bash
mvn clean test
```

## 小程序验证

```bash
cd nso-miniapp
npm install
npm run type-check
```

当前阶段只完成项目骨架和基础配置，不创建客户、项目、图纸、样品、变更等业务表。
