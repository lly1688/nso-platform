# 单组织试点部署

此目录提供完整的单机试点运行形态：浏览器只访问 Nginx，`nso-admin`、MySQL、Redis 和 MinIO 不映射主机端口。

## 首次部署

1. 复制 `.env.example` 为 `.env.pilot`，替换全部密码和 JWT 密钥。
2. 在 `NSO_TLS_CERT_DIR` 指定的绝对路径放置 `tls.crt` 和 `tls.key`；Nginx 只允许 HTTPS 提供业务页面。
3. 从仓库根目录先执行部署预检：deploy/pilot/scripts/preflight.ps1。
4. 启动服务：

```powershell
docker compose --env-file deploy/pilot/.env.pilot -f deploy/pilot/docker-compose.yml up -d --build
docker compose --env-file deploy/pilot/.env.pilot -f deploy/pilot/docker-compose.yml ps
```

`gateway`、`api`、`mysql`、`redis` 和 `minio` 必须为 `healthy`，`minio-init` 必须以 `exited (0)` 结束。访问 `https://<host>/healthz` 验证入口；Actuator 只留在 Docker 后端网络。

## 备份与恢复验证

将备份存放在异机挂载目录，至少每日执行一次：

```powershell
./deploy/pilot/scripts/backup.ps1 -BackupRoot D:\nso-pilot-backups
./deploy/pilot/scripts/restore-verify.ps1 -BackupDirectory D:\nso-pilot-backups\<timestamp>
```

备份包含 MySQL 一致性转储、MinIO 数据目录归档和 SHA-256 清单。恢复验证使用临时空 MySQL 容器，不会修改试点数据库；正式灾难恢复必须在停机窗口内按运维工单执行，并先在隔离环境完成同一备份的验证。

## 发布门禁

在每个候选发布前执行：

```powershell
./scripts/verify-pilot.ps1
./scripts/verify-pilot.ps1 -BaseUrl https://<host>
```

`-BaseUrl` is optional. When supplied after the gateway is online, the same command also verifies `/healthz`, the protected API boundary, and the `X-Trace-Id` response header.

生产迁移唯一入口为 `nso-admin/src/main/resources/db/migration`。不得执行根目录 `sql/archive` 中的历史快照。

镜像发布时应锁定 digest，并在运营方接入 Trivy/等效扫描器：

```powershell
docker scout cves nso-pilot-api
docker scout cves mysql:8.4.4
./deploy/pilot/scripts/check-tls.ps1 -CertificatePath D:\nso\certs\tls.crt
```

V40 会删除历史重复的 `sys_user` 生命周期索引。历史迁移中的 `VALUES()` 语法保持不变以确保已发布脚本不可变；后续新增迁移必须使用 MySQL 8.4 推荐的别名语法。

发布证据可在候选版本验证后生成：

```powershell
./scripts/verify-pilot.ps1
./scripts/write-release-evidence.ps1 -BaseUrl https://<host>
```

输出文件记录提交号、分支、迁移版本、健康检查结果和未覆盖的验收边界，便于归档到发布工单。
