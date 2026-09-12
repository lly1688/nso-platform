-- Only repairs the known historical demo inconsistency.  Normal projects are
-- intentionally excluded: their state must continue to be changed by services.
SET @demo_project_id := (
    SELECT id FROM nso_project
    WHERE tenant_id = 1 AND project_no = 'NSO-DEMO-202607-003' AND deleted = 0
    LIMIT 1
);
SET @demo_sample_id := (
    SELECT id FROM nso_sample
    WHERE tenant_id = 1 AND project_id = @demo_project_id AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);
SET @demo_before_status := (
    SELECT status FROM nso_project
    WHERE id = @demo_project_id AND tenant_id = 1
);

UPDATE nso_sample
SET status = 'CONFIRMED',
    confirm_conclusion = 'PASS'
WHERE id = @demo_sample_id
  AND tenant_id = 1
  AND status IN ('CONFIRMED', 'WAIT_CUSTOMER_CONFIRM', 'CHECKED', 'DRAFT');

UPDATE nso_project
SET status = 'CUSTOMER_CONFIRMED',
    stage = 'SAMPLE',
    sample_status = 'CONFIRMED'
WHERE id = @demo_project_id
  AND tenant_id = 1
  AND EXISTS (
      SELECT 1 FROM nso_sample
      WHERE id = @demo_sample_id
        AND tenant_id = 1
        AND status = 'CONFIRMED'
        AND confirm_conclusion = 'PASS'
  );

INSERT INTO nso_project_status_history
    (tenant_id, project_id, before_status, after_status, action_code, reason, operator_id)
SELECT p.tenant_id, p.id, @demo_before_status, 'CUSTOMER_CONFIRMED', 'DEMO_SAMPLE_STATE_REPAIRED',
       'V27 修复历史样品已确认但项目仍停留在打样状态的不一致数据', p.owner_user_id
FROM nso_project p
WHERE p.id = @demo_project_id
  AND NOT EXISTS (
      SELECT 1 FROM nso_project_status_history h
      WHERE h.tenant_id = p.tenant_id AND h.project_id = p.id
        AND h.action_code = 'DEMO_SAMPLE_STATE_REPAIRED'
  );

INSERT INTO nso_business_event
    (tenant_id, project_id, business_type, business_id, action_code, before_summary, after_summary, operator_id, operator_name)
SELECT p.tenant_id, p.id, 'PROJECT', p.id, 'DEMO_SAMPLE_STATE_REPAIRED', @demo_before_status,
       'CUSTOMER_CONFIRMED / CONFIRMED', p.owner_user_id, COALESCE(p.owner_name, 'SYSTEM')
FROM nso_project p
WHERE p.id = @demo_project_id
  AND NOT EXISTS (
      SELECT 1 FROM nso_business_event e
      WHERE e.tenant_id = p.tenant_id AND e.project_id = p.id
        AND e.action_code = 'DEMO_SAMPLE_STATE_REPAIRED'
  );

INSERT INTO nso_timeline_event
    (tenant_id, project_id, business_type, business_id, event_type, title, summary, operator_name)
SELECT p.tenant_id, p.id, 'PROJECT', p.id, 'DEMO_SAMPLE_STATE_REPAIRED', 'DEMO_SAMPLE_STATE_REPAIRED',
       'V27 已将样品确认状态同步回项目；严重风险和受阻任务保持原状，继续作为交付阻断示例。', COALESCE(p.owner_name, 'SYSTEM')
FROM nso_project p
WHERE p.id = @demo_project_id
  AND NOT EXISTS (
      SELECT 1 FROM nso_timeline_event e
      WHERE e.tenant_id = p.tenant_id AND e.project_id = p.id
        AND e.event_type = 'DEMO_SAMPLE_STATE_REPAIRED'
  );
