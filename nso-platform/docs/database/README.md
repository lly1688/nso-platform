# 数据库文档

本目录用于保存数据库设计、表结构说明、Flyway 迁移说明和初始化数据说明。

正式数据库变更必须通过 `nso-admin/src/main/resources/db/migration` 中的 Flyway 脚本完成；该目录是唯一生产迁移入口。根目录 `sql/archive` 仅保留历史快照，严禁作为 Flyway 或生产升级脚本执行。
