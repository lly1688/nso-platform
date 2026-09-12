-- V11 seeded several task labels before the runtime demo accounts exist.
-- Backfill those historical display values directly so fresh installations and
-- upgraded tenant-1 demo data present the same normalized account names.
UPDATE nso_project_member
SET member_name = CASE member_name
    WHEN '演示系统管理员' THEN '系统管理员'
    WHEN '演示销售项目经理' THEN '陈晓明'
    WHEN '演示技术设计' THEN '技术设计工程师'
    WHEN '演示工艺人员' THEN '工艺工程师'
    WHEN '演示采购人员' THEN '采购专员'
    WHEN '演示计划生产' THEN '生产计划员'
    WHEN '演示质量人员' THEN '质量工程师'
    WHEN '演示现场人员' THEN '现场执行员'
    WHEN '演示管理层' THEN '经营负责人'
    ELSE member_name
END
WHERE tenant_id = 1
  AND deleted = 0
  AND member_name IN (
      '演示系统管理员', '演示销售项目经理', '演示技术设计', '演示工艺人员',
      '演示采购人员', '演示计划生产', '演示质量人员', '演示现场人员', '演示管理层'
  );

UPDATE nso_task
SET responsible_name = CASE responsible_name
    WHEN '演示系统管理员' THEN '系统管理员'
    WHEN '演示销售项目经理' THEN '陈晓明'
    WHEN '演示技术设计' THEN '技术设计工程师'
    WHEN '演示工艺人员' THEN '工艺工程师'
    WHEN '演示采购人员' THEN '采购专员'
    WHEN '演示计划生产' THEN '生产计划员'
    WHEN '演示质量人员' THEN '质量工程师'
    WHEN '演示现场人员' THEN '现场执行员'
    WHEN '演示管理层' THEN '经营负责人'
    ELSE responsible_name
END
WHERE tenant_id = 1
  AND deleted = 0
  AND responsible_name IN (
      '演示系统管理员', '演示销售项目经理', '演示技术设计', '演示工艺人员',
      '演示采购人员', '演示计划生产', '演示质量人员', '演示现场人员', '演示管理层'
  );
