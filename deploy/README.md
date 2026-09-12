# 部署与本地联调

本目录保存部署相关说明。本地依赖由项目根目录的 `docker-compose.yml` 管理；该
Compose 环境是 V1.0 的唯一开发验收基线，包含 MySQL 8.4、Redis 8、MinIO 和 bucket
初始化任务。

## 首次启动

在 `nso-platform` 根目录执行：

```powershell
Copy-Item .env.example .env
docker compose up -d mysql redis minio minio-init
docker compose ps
```

`mysql`、`redis`、`minio` 应为 `healthy`，`minio-init` 应以状态 `exited (0)` 结束。
随后启动后端：

```powershell
mvn -pl nso-admin -am spring-boot:run
```

开发环境默认连接 `127.0.0.1:3307/nso_platform`、`127.0.0.1:6380`、
`http://127.0.0.1:9002`。这些端口刻意避开常见的本机 MySQL/Redis 服务；Flyway 从空库执行 `db/migration` 的所有版本；不要手工
改表，也不要改写已经发布的迁移。

## 常用诊断

```powershell
docker compose logs mysql --tail 100
docker compose logs redis --tail 100
docker compose logs minio --tail 100
docker compose exec mysql mysql -unso_app -p nso_platform -e "select version, success from flyway_schema_history order by installed_rank"
```

- 应用报 Redis `Connection refused`：确认 Docker Desktop 已启动，且 `docker compose
  ps redis` 显示健康；不要通过关闭认证或把 Redis 排除在依赖外来绕过问题。
- Flyway 校验失败：保留失败现场和 `flyway_schema_history` 记录，修复新的迁移或配置；
  已发布版本不可修改。
- MinIO 上传失败：确认 `minio-init` 已完成、`NSO_MINIO_BUCKET` 与应用配置一致，并检查
  `http://127.0.0.1:9003` 的服务状态。

## 停止与数据边界

```powershell
docker compose down
```

该命令保留 named volumes。只有明确需要清空本地测试数据时，才执行
`docker compose down -v`；这会不可恢复地删除 MySQL、Redis 和 MinIO 的本地数据。

## 生产环境

生产环境使用 `application-prod.yml`，数据库、Redis、JWT、MinIO
必须由环境变量或密钥管理系统注入。生产配置不提供默认凭据。
