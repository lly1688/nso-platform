# NSO 代码结构与分层约定

## 模块职责

| 模块 | 职责 |
| --- | --- |
| `nso-shared` | 无内部模块依赖的基础类型、异常、枚举和工具。 |
| `nso-system` | 系统管理和九个核心业务域的领域模型、持久化与应用服务。 |
| `nso-framework` | 安全、认证、AOP、Web 基础设施、缓存锁和对象存储适配。 |
| `nso-scheduler` | 风险扫描、提醒、升级、重试和汇总等定时任务。 |
| `nso-scaffold` | 表结构读取、模板渲染、预览与下载的代码生成能力。 |
| `nso-admin` | Spring Boot 启动模块与 PC、客户确认接口聚合层。 |
| `nso-web` | Vue 3 PC 管理端。 |

## 业务三层

业务域位于 `nso-system/src/main/java/com/nso/business/<domain>`，按以下职责组织：

```text
nso-admin/web/controller/{business,publicapi}
    -> nso-system/business/<domain>/service
        -> nso-system/business/<domain>/mapper
            -> MySQL

business/<domain>/
├── domain/        # Entity、DTO、VO
├── mapper/        # MyBatis-Plus Mapper
└── service/
    ├── I*Service  # 业务服务接口
    └── impl/      # 事务、状态迁移、规则校验和跨域编排
```

Controller 只处理权限、参数和响应，不能直接依赖 Mapper。Service 负责事务、幂等、并发控制与业务规则；Mapper 只处理持久化。

`file` 域遵循相同结构：`domain/FileObject`、`mapper/FileObjectMapper`、`service/IFileService`、`service/impl/FileServiceImpl`。

## 合理例外

- `business/core` 定义跨域 DTO、租户上下文与 Port，不是独立业务域。
- `business/support` 提供平台级审计、编号、事件等支撑服务；`business/report` 负责跨域聚合查询。这两类代码不为形式创建空 Entity 或 Mapper。
- `nso-shared`、`nso-framework`、`nso-scheduler`、`nso-scaffold` 依职责组织，不强制套用业务三层。

## 维护规则

- 只为已有实现创建包和目录；不保留空的 `package-info.java`、空 Mapper 或空 `impl` 目录作为模板。
- 所有结构调整必须同步更新本说明，并保留 Maven 模块依赖边界。
- 不将 `target/`、`node_modules/`、`dist/`、日志或 IDE 生成目录写入代码结构说明；它们是构建产物，不是源码结构。
