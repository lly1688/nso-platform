# 试点监控接入

应用 Prometheus 端点位于 Docker 后端网络的 http://api:8081/actuator/prometheus。不要通过 Nginx 暴露该端点。

将 alerts.yml 导入运营方 Prometheus，并同时接入：

- mysqld-exporter，采集 mysql_global_status_slow_queries；
- node_exporter textfile collector，采集备份脚本写出的 nso_backup_age_hours；
- 应用端点，采集审计写入失败、消息积压和调度任务失败指标。

备份完成后执行 deploy/pilot/scripts/write-backup-metric.ps1。告警阈值为试点默认值，运营方可按值班制度调整接收人和静默窗口。
