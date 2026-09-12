import type { PageResult, SystemRole } from '@/types';

// 系统管理子页面导航项。
export interface SystemNavItem {
    name: string;
    path: string;
    label: string;
    icon: string;
    permission?: string;
    roles?: string[];
}

// 系统管理导航与下拉选项。
export const systemNavItems: SystemNavItem[] = [
    { name: 'system-accounts', path: '/system/accounts', label: '账号与交接', icon: 'UserFilled', permission: 'sys:user:read' },
    { name: 'system-organization', path: '/system/organization', label: '组织与岗位', icon: 'OfficeBuilding', permission: 'sys:dept:read' },
    { name: 'system-roles', path: '/system/roles', label: '角色与权限', icon: 'Lock', permission: 'sys:role:read' },
    { name: 'system-rules', path: '/system/rules', label: '规则与字典', icon: 'Operation', roles: ['admin'] },
    { name: 'system-imports', path: '/system/imports', label: '数据导入', icon: 'Upload', roles: ['admin'] },
    { name: 'system-operations', path: '/system/operations', label: '运维审计', icon: 'Memo', permission: 'sys:authorization:audit', roles: ['admin'] },
    { name: 'system-account-support', path: '/system/account-support', label: '账户支持', icon: 'Service', permission: 'sys:account-support:manage' }
];

export const dataScopeOptions: Array<{
    label: string;
    value: SystemRole['dataScope'];
}> = [
    { label: '仅本人', value: 'SELF' },
    { label: '项目成员', value: 'PROJECT_MEMBER' },
    { label: '项目负责人', value: 'PROJECT_OWNER' },
    { label: '指定部门', value: 'CUSTOM_DEPT' },
    { label: '本部门', value: 'DEPT' },
    { label: '部门及下级', value: 'DEPT_AND_CHILD' },
    { label: '全部可见', value: 'ALL' }
];
export const enabledStatusOptions = [
    { label: '启用', value: 'ENABLED' },
    { label: '停用', value: 'DISABLED' }
];

// 页面请求与分页数据的容错工具。
export async function optional<T>(action: Promise<T>, fallback: T): Promise<T> {
    try {
        return await action;
    }
    catch {
        return fallback;
    }
}

export function pageList<T>(data: {
    list?: T[];
} | T[] | undefined): T[] {
    if (Array.isArray(data)) {
        return data;
    }
    return data?.list || [];
}

export function emptyPage<T>(): PageResult<T> {
    return { list: [], total: 0, pageNo: 1, pageSize: 0 };
}

export function isEnabledStatus(status?: string): boolean {
    return status === 'ENABLED' || status === 'ACTIVE';
}
