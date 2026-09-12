-- Keep development fixture credentials intact while presenting the accounts as
-- ordinary users and roles throughout the administrative interface.
UPDATE sys_user
SET nickname = CASE username
    WHEN 'demo-admin' THEN '系统管理员'
    WHEN 'demo-pm' THEN '陈晓明'
    WHEN 'demo-tech' THEN '技术设计工程师'
    WHEN 'demo-process' THEN '工艺工程师'
    WHEN 'demo-purchase' THEN '采购专员'
    WHEN 'demo-production' THEN '生产计划员'
    WHEN 'demo-quality' THEN '质量工程师'
    WHEN 'demo-field' THEN '现场执行员'
    WHEN 'demo-executive' THEN '经营负责人'
    ELSE nickname
END
WHERE tenant_id = 1
  AND username IN (
      'demo-admin', 'demo-pm', 'demo-tech', 'demo-process', 'demo-purchase',
      'demo-production', 'demo-quality', 'demo-field', 'demo-executive'
  );

UPDATE sys_dept
SET leader_name = CASE dept_name
    WHEN '平台运营部' THEN '系统管理员'
    WHEN '项目管理部' THEN '陈晓明'
    WHEN '技术设计部' THEN '技术设计工程师'
    WHEN '工艺工程部' THEN '工艺工程师'
    WHEN '采购供应部' THEN '采购专员'
    WHEN '生产计划部' THEN '生产计划员'
    WHEN '质量管理部' THEN '质量工程师'
    WHEN '客户协同组' THEN '客户接口人'
    WHEN '经营管理部' THEN '经营负责人'
    ELSE leader_name
END
WHERE tenant_id = 1
  AND dept_name IN (
      '平台运营部', '项目管理部', '技术设计部', '工艺工程部', '采购供应部',
      '生产计划部', '质量管理部', '客户协同组', '经营管理部'
  );
