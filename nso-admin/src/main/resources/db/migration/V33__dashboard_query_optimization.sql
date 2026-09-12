-- 仪表盘未读消息按当前用户、状态和时间分页/计数。
CREATE INDEX idx_message_dashboard_receiver_status_created
    ON nso_message (tenant_id, deleted, receiver_id, status, created_at);
