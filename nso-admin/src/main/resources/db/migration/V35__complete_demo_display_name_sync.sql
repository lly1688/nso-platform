-- V29 normalizes the account directory.  Keep the derived display fields in
-- the historical demo records deterministic even on a fresh bootstrap.
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

UPDATE nso_project_member pm
JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
SET pm.member_name = u.nickname
WHERE pm.tenant_id = 1
  AND pm.deleted = 0
  AND u.username IN (
      'demo-admin', 'demo-pm', 'demo-tech', 'demo-process', 'demo-purchase',
      'demo-production', 'demo-quality', 'demo-field', 'demo-executive'
  );

UPDATE nso_task t
JOIN sys_user u ON u.id = t.assignee_id AND u.tenant_id = t.tenant_id
SET t.responsible_name = u.nickname
WHERE t.tenant_id = 1
  AND t.deleted = 0
  AND u.username IN (
      'demo-admin', 'demo-pm', 'demo-tech', 'demo-process', 'demo-purchase',
      'demo-production', 'demo-quality', 'demo-field', 'demo-executive'
  );
