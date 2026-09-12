// 任务类型、状态和异常类型选项。
export const taskTypeOptions = [
    { label: '采购任务', shortLabel: '采购', value: 'PURCHASE', tag: 'info' },
    { label: '生产任务', shortLabel: '生产', value: 'PRODUCTION', tag: 'danger' },
    { label: '检验任务', shortLabel: '检验', value: 'INSPECTION', tag: 'warning' },
    { label: '交付任务', shortLabel: '交付', value: 'DELIVERY', tag: 'success' }
] as const;
export const taskStatusOptions = [
    { label: '待执行', value: 'TODO', tag: 'info' },
    { label: '执行中', value: 'IN_PROGRESS', tag: 'warning' },
    { label: '已阻断', value: 'BLOCKED', tag: 'danger' },
    { label: '已暂停', value: 'PAUSED', tag: 'info' },
    { label: '已完成', value: 'DONE', tag: 'success' }
] as const;
export const exceptionTypeOptions = [
    { label: '物料短缺', value: 'MATERIAL_SHORTAGE' },
    { label: '质量问题', value: 'QUALITY_ISSUE' },
    { label: '设备故障', value: 'EQUIPMENT_FAILURE' },
    { label: '交期延迟', value: 'SCHEDULE_DELAY' },
    { label: '人员不足', value: 'LABOR_SHORTAGE' },
    { label: '客户变更', value: 'CUSTOMER_CHANGE' },
    { label: '其他', value: 'OTHER' }
] as const;
export const assigneeResponsibilities: Record<string, string[]> = {
    PURCHASE: ['PURCHASER', 'PURCHASE_MEMBER', 'PURCHASE_OWNER'],
    PRODUCTION: ['PRODUCTION', 'PRODUCTION_MEMBER', 'PRODUCTION_OWNER'],
    INSPECTION: ['QUALITY', 'QUALITY_MEMBER', 'QUALITY_OWNER'],
    DELIVERY: ['PROJECT_MANAGER']
};

export const productionGateRules = [
    { code: 'SAMPLE_NOT_CONFIRMED', label: '样品客户确认', action: '完成确认或取得有效特殊放行' },
    { code: 'SERIOUS_RISK_OPEN', label: '严重风险关闭', action: '关闭严重风险后再投产' },
    { code: 'MATERIAL_NOT_READY', label: '采购任务完成', action: '确认关键采购任务已完成' },
    { code: 'VERSION_MISMATCH', label: '技术版本一致', action: '同步任务引用的当前发布版本' }
] as const;

const systemTaskTypes: Record<string, { label: string; tag: string }> = {
    SAMPLE_PREPARE: { label: '样品备料', tag: 'warning' },
    SAMPLE_MAKE: { label: '样品制作', tag: 'warning' },
    SAMPLE_REWORK: { label: '样品整改', tag: 'danger' },
    SAMPLE_CONFIRM: { label: '客户确认', tag: 'success' },
    RISK_REVIEW: { label: '风险评审', tag: 'danger' }
};

// 任务与风险的展示转换工具。
export function taskTypeLabel(type: string): string {
    return taskTypeOptions.find((item) => item.value === type)?.shortLabel || systemTaskTypes[type]?.label || type;
}

export function taskTypeTag(type: string): string {
    return taskTypeOptions.find((item) => item.value === type)?.tag || systemTaskTypes[type]?.tag || 'info';
}

export function taskStatusLabel(status: string): string {
    return taskStatusOptions.find((item) => item.value === status)?.label || status;
}

export function taskStatusTag(status: string): string {
    return taskStatusOptions.find((item) => item.value === status)?.tag || 'info';
}

export function riskLevelTag(level: string): string {
    if (level === 'HIGH' || level === 'SERIOUS') {
        return 'danger';
    }
    if (level === 'MEDIUM') {
        return 'warning';
    }
    return 'success';
}

export function riskLevelLabel(level: string): string {
    return ({ SERIOUS: '严重', HIGH: '高', MEDIUM: '中', LOW: '低' } as Record<string, string>)[level] || level;
}

export function riskStatusLabel(status: string): string {
    return ({ OPEN: '待处置', PROCESSING: '处理中', CLOSED: '已关闭' } as Record<string, string>)[status] || status;
}

export function pageList<T>(data: {
    list?: T[];
} | T[] | undefined): T[] {
    if (Array.isArray(data)) {
        return data;
    }
    return data?.list || [];
}
