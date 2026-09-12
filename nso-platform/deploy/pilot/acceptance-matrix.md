# 单组织试点验收证据矩阵

| 验收项 | 目标 | 证据命令或产物 | 责任角色 |
| --- | --- | --- | --- |
| 数据迁移 | 仅 canonical Flyway 目录执行，历史版本不变 | scripts/verify-migrations.ps1；Flyway history 截图 | 开发、DBA |
| 发布门禁 | 后端测试、前端类型检查/构建、Compose 校验通过 | scripts/verify-pilot.ps1 输出 | 开发 |
| 入口安全 | HTTPS、无直连数据端口、受保护 API 返回 Trace ID | deploy/pilot/scripts/preflight.ps1；scripts/smoke-pilot.ps1 | 运维 |
| 恢复演练 | 空环境可恢复 MySQL，MinIO 归档可验证 | backup.ps1 和 restore-verify.ps1 输出 | DBA、运维 |
| 权限回归 | 非成员、越权角色、客户短期确认链接均被验证 | 集成测试报告和手工回归记录 | 测试 |
| 状态迁移 | 合法/非法迁移、越权、幂等、乐观锁、事件与审计均有可追溯证据 | `docs/testing/pilot-state-transition-matrix.md` 和分类 Surefire 报告 | 开发、测试 |
| 业务闭环 | 建档至归档完整演示 | 演示录像、审计事件、业务测试记录 | 业务、测试 |
| 容量 | 项目列表/详情 P95 小于 2 秒，复杂统计小于 5 秒 | deploy/pilot/perf k6 结果、慢日志、执行计划 | 开发、DBA |
| 告警 | 审计失败、调度失败、消息积压、慢 SQL、备份过期接入 | monitoring alerts.yml 和告警平台截图 | 运维 |
