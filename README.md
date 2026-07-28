# 智慧非标订单打样与变更协同平台 V1.0

标准若依风格前后端分离项目，包含 6 个 Maven 后端模块 + Vue 3 PC 管理端。

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
├─ sql/
├─ nso-common/
├─ nso-system/
├─ nso-framework/
├─ nso-quartz/
├─ nso-generator/
├─ nso-admin/
└─ nso-ui/
```

## 模块说明

| 模块 | 类型 | 职责 |
|------|------|------|
| `nso-common` | Maven JAR | 返回对象、基础实体、枚举、异常、注解、工具类 |
| `nso-system` | Maven JAR | 用户权限、系统管理以及九个核心业务域 |
| `nso-framework` | Maven JAR | Spring Security、JWT、Redis、数据权限、全局异常、MyBatis 配置 |
| `nso-quartz` | Maven JAR | 风险扫描、临期提醒、超时升级、消息重试、报表汇总 |
| `nso-generator` | Maven JAR | 代码生成：Entity、Mapper、Service、Controller、Vue 页面 |
| `nso-admin` | Maven JAR | Spring Boot 启动、登录认证、PC 和小程序 Controller、接口聚合 |
| `nso-ui` | npm 工程 | Vue 3 + TypeScript + Element Plus PC 管理端 |
| `docs/product-specs` | — | 产品需求规格说明书 |
| `docs/database` | — | 数据库设计和迁移说明 |
| `docs/api` | — | 接口设计和联调说明 |
| `docs/development-plan` | — | 开发计划和验收记录 |
| `deploy` | — | Docker、Nginx 和部署脚本 |

## 后端依赖链

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

## 接口前缀

| 客户端 | 前缀 | 说明 |
|--------|------|------|
| PC 管理端 | `/api/v1/admin/**` | 登录、系统管理、业务管理 |
| 微信小程序 | `/api/v1/mp/**` | 移动工作台、扫码、任务反馈 |
| 客户短期确认 | `/api/v1/public/**` | 受限样品确认 |

## 本地基础服务

复制环境变量示例后按需修改：

```bash
copy .env.example .env
```

启动 MySQL、Redis、MinIO（首次启动会自动创建 `nso-files` bucket）：

```bash
docker compose up -d mysql redis minio minio-init
docker compose ps
```

默认开发环境连接上述 Compose 服务，Flyway 会在应用启动时从空库依次执行
`V1` 至最新版本的迁移。启动前请确认 Docker Desktop 已运行，并按需在 `.env`
替换示例密码；生产环境不得使用 `.env.example` 中的默认值。

## 启动后端

```bash
mvn -pl nso-admin -am spring-boot:run
```

开发首个管理员由 `application-dev.yml` 的 `nso.bootstrap` 控制。只允许在开发
环境使用该入口；启动后应立即修改默认密码。微信登录的 `dev:<用户名>` 模拟码同样
只在 `application-dev.yml` 明确开启时可用。

## 本地演示账号

演示账号默认关闭，且仓库不提供任何密码。仅在本地或测试环境同时设置下列环境变量
（密码至少 8 位）后，应用启动时才会幂等创建账号；同名账号不会被重置密码或角色：

```bash
NSO_DEMO_ENABLED=true
NSO_DEMO_DEFAULT_PASSWORD=请使用本地测试密码
```

创建的账号为 `demo-admin`、`demo-pm`、`demo-tech`、`demo-process`、
`demo-purchase`、`demo-production`、`demo-quality`、`demo-customer`、
`demo-executive`，分别对应系统管理员、项目经理、技术、工艺、采购、生产、质量、
客户确认和管理层角色。生产环境应保持 `NSO_DEMO_ENABLED=false`。

## 验证

```bash
mvn clean test
docker compose config
```

## PC 管理端验证

```bash
cd nso-ui
npm install
npm run build
```

更多服务启动、迁移复位边界和故障定位见 [deploy/README.md](deploy/README.md)。
