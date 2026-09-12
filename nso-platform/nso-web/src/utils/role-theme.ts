// 角色对应的界面展示主题。
export interface RoleTheme {
    label: string;
    shortLabel: string;
    icon: string;
    tone: string;
}

// 角色编码与展示信息映射。
const themes: Record<string, RoleTheme> = {
    admin: { label: '系统管理员', shortLabel: '管', icon: 'UserFilled', tone: 'admin' },
    project_manager: { label: '项目经理', shortLabel: '项', icon: 'Management', tone: 'project' },
    technical: { label: '技术设计', shortLabel: '技', icon: 'DocumentChecked', tone: 'technical' },
    process: { label: '工艺工程', shortLabel: '艺', icon: 'Operation', tone: 'process' },
    purchaser: { label: '采购供应', shortLabel: '采', icon: 'Box', tone: 'purchase' },
    production: { label: '计划生产', shortLabel: '产', icon: 'OfficeBuilding', tone: 'production' },
    quality: { label: '质量管理', shortLabel: '质', icon: 'CircleCheckFilled', tone: 'quality' },
    customer_confirm: { label: '客户确认', shortLabel: '客', icon: 'User', tone: 'customer' },
    executive: { label: '经营管理', shortLabel: '管', icon: 'TrendCharts', tone: 'executive' },
    field_user: { label: '现场人员', shortLabel: '现', icon: 'Location', tone: 'field' }
};

const fallback: RoleTheme = { label: '协同成员', shortLabel: '协', icon: 'User', tone: 'default' };

// 按优先级获取用户的展示主题。
export function roleTheme(roles?: string[]): RoleTheme {
    return (roles || []).map((role) => themes[role]).find(Boolean) || fallback;
}
