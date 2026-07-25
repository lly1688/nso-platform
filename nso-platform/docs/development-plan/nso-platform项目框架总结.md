# nso-platform 项目框架总结

## 1. 总体结构

本项目采用“4 个 Maven 模块 + 2 个前端”的简化架构。V1.0 不再把客户、项目、图纸、打样、变更、任务、风险、消息、报表分别拆成独立 Maven 模块，而是在 `nso-server` 中用包进行边界划分。

```text
nso-platform/
├─ pom.xml
├─ docker-compose.yml
├─ AGENTS.md
├─ README.md
├─ db/
│  ├─ migration/
│  ├─ init/
│  └─ demo/
├─ docs/
├─ deploy/
├─ nso-common/
├─ nso-system/
├─ nso-framework/
├─ nso-server/
├─ nso-web/
└─ nso-miniapp/
```

## 2. 模块划分

| 模块 | 职责 | 依赖 |
|---|---|---|
| `nso-common` | 注解、枚举、异常、基础类、统一返回对象、工具类 | 无内部依赖 |
| `nso-system` | 用户、角色、菜单、部门、字典的 Domain、Mapper、Service | `nso-common` |
| `nso-framework` | Security、JWT/微信认证、AOP 切面、全局异常、配置类 | `nso-common`、`nso-system` |
| `nso-server` | Spring Boot 启动、PC 管理端 Controller、小程序接口、9 个业务模块包 | `nso-framework` |
| `nso-web` | Vue 3 + TypeScript PC 管理端 | 独立 npm 项目 |
| `nso-miniapp` | uni-app + Vue 3 微信小程序 | 独立 npm 项目 |

后端 Maven 依赖链：

```text
nso-common
  ↑
nso-system
  ↑
nso-framework
  ↑
nso-server
```

## 3. 后端包结构

`nso-server` 只负责启动和接口聚合，所有正式业务 Controller 放在这里。

```text
nso-server/src/main/java/com/nso/
├─ NsoServerApplication.java
├─ config/
├─ modules/
│  ├─ customer/
│  ├─ project/
│  ├─ document/
│  ├─ sample/
│  ├─ change/
│  ├─ task/
│  ├─ risk/
│  ├─ message/
│  └─ report/
├─ mp/
│  ├─ auth/
│  ├─ project/
│  ├─ document/
│  ├─ sample/
│  ├─ change/
│  ├─ task/
│  └─ message/
└─ web/controller/system/
```

每个业务包统一四层：

```text
modules/<name>/
├─ controller/
├─ domain/
├─ mapper/
└─ service/
   └─ impl/
```

## 4. 配置与数据目录

后端配置：

```text
nso-server/src/main/resources/
├─ application.yml
├─ application-dev.yml
├─ application-prod.yml
├─ db/migration/
└─ logback-spring.xml
```

根目录数据脚本：

```text
db/
├─ migration/    # Flyway 版本迁移脚本整理区
├─ init/         # 字典、菜单、角色等初始化数据
└─ demo/         # 演示数据，生产环境不执行
```

正式发布到后端包内执行的 Flyway 脚本放入 `nso-server/src/main/resources/db/migration`。

## 5. 技术栈

| 层面 | 选型 |
|---|---|
| 基础框架 | Spring Boot 3.5.x / Java 17 |
| ORM | MyBatis-Plus 3.5.x |
| 安全 | Spring Security + JWT + 微信 OAuth |
| 缓存/锁 | Redis + Redisson |
| 数据库迁移 | Flyway |
| 对象存储 | MinIO |
| API 文档 | Knife4j |
| 工具集 | Hutool、Lombok |
| Excel | EasyExcel |
| PC 前端 | Vue 3 + TypeScript + Vite |
| 小程序 | uni-app + Vue 3 + TypeScript |

## 6. 简化原则

- 少模块：只保留 4 个 Maven 模块，降低开发、构建和维护成本。
- 业务用包区分：客户、项目、图纸、打样、变更等业务不再各建 Maven 模块。
- Controller 集中：PC 管理端和小程序接口都在 `nso-server`。
- 公共能力下沉：通用类放 `nso-common`，系统服务放 `nso-system`，安全和技术配置放 `nso-framework`。
- 客户确认入口不单独建前端：V1.0 先由小程序承接，后端接口保留扩展空间。
- 若依只作为架构风格参考：借鉴模块分层和权限思路，不直接复制若依代码。
