import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/views/login/index.vue';

// 业务页面按路由懒加载，缩小首屏资源体积。
const Dashboard = () => import('@/views/dashboard/index.vue');
const Actions = () => import('@/views/action/index.vue');
const Projects = () => import('@/views/project/index.vue');
const Documents = () => import('@/views/document/index.vue');
const Samples = () => import('@/views/sample/index.vue');
const Changes = () => import('@/views/change/index.vue');
const Tasks = () => import('@/views/task/index.vue');
const TaskExecution = () => import('@/views/task/execution.vue');
const TaskRisks = () => import('@/views/task/risks.vue');
const TaskDeliveries = () => import('@/views/task/deliveries.vue');
const Reports = () => import('@/views/report/index.vue');
const System = () => import('@/views/system/index.vue');
const SystemAccounts = () => import('@/views/system/accounts.vue');
const SystemOrganization = () => import('@/views/system/organization.vue');
const SystemRoles = () => import('@/views/system/roles.vue');
const SystemRules = () => import('@/views/system/rules.vue');
const SystemImports = () => import('@/views/system/imports.vue');
const SystemOperations = () => import('@/views/system/operations.vue');
const SystemAccountSupport = () => import('@/views/system/account-support.vue');
const Profile = () => import('@/views/profile/index.vue');
const PublicConfirm = () => import('@/views/public/confirm/index.vue');

// 受保护路由统一补齐权限和侧边栏元信息。
const protectedRoute = (name: string, path: string, component: any, title: string, icon: string, permission?: string, sidebar = true) => ({
    name, path, component, meta: { title, icon, permission: permission && !permission.startsWith('sys:') ? `nso:${permission}` : permission, requiresAuth: true, sidebar }
});

export interface RouteSession {
    permissions?: string[];
    roles?: string[];
    forceChangePassword?: boolean;
}

// 登录后优先跳转到当前账号可访问的业务页面。
const landingRoutes = [
    '/dashboard', '/actions', '/projects', '/documents', '/samples', '/changes', '/tasks/execution', '/reports', '/system'
];

function storedSession(): RouteSession | null {
    try {
        return JSON.parse(localStorage.getItem('nso_session') || 'null');
    }
    catch {
        return null;
    }
}

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: () => resolveFirstAccessibleRoute(storedSession()) },
        { name: 'login', path: '/login', component: Login, meta: { public: true } },
        protectedRoute('dashboard', '/dashboard', Dashboard, '仪表盘', 'Monitor', 'dashboard:view'),
        protectedRoute('actions', '/actions', Actions, '我的行动', 'List', 'action:view'),
        protectedRoute('projects', '/projects', Projects, '客户项目', 'FolderOpened', 'project:view'),
        protectedRoute('project-workspace', '/project-workspace', Projects, '项目工作台', 'DataBoard', 'project:view'),
        protectedRoute('documents', '/documents', Documents, '技术文件', 'DocumentChecked', 'document:view'),
        protectedRoute('samples', '/samples', Samples, '样品管理', 'TakeawayBox', 'sample:view'),
        protectedRoute('changes', '/changes', Changes, '变更中心', 'Switch', 'change:view'),
        {
            name: 'tasks',
            path: '/tasks',
            component: Tasks,
            redirect: (to: any) => ({ path: '/tasks/execution', query: to.query, hash: to.hash }),
            meta: { title: '任务中心', permission: 'task:view', requiresAuth: true, sidebar: false },
            children: [
                {
                    name: 'tasks-execution',
                    path: 'execution',
                    component: TaskExecution,
                    meta: { title: '任务执行', description: '任务排程、进度反馈与版本阻断', icon: 'List', permission: 'task:view', requiresAuth: true, sidebar: true }
                },
                {
                    name: 'tasks-risks',
                    path: 'risks',
                    component: TaskRisks,
                    meta: { title: '任务风险', description: '风险处置建议与异常上报', icon: 'WarningFilled', permission: 'task:view', sidebarPermissions: ['risk:view', 'task:feedback'], requiresAuth: true, sidebar: true }
                },
                {
                    name: 'tasks-deliveries',
                    path: 'deliveries',
                    component: TaskDeliveries,
                    meta: { title: '交付管理', description: '交付预检、真实交付与客户反馈', icon: 'Van', permission: 'task:view', requiresAuth: true, sidebar: true }
                }
            ]
        },
        protectedRoute('reports', '/reports', Reports, '统计分析', 'TrendCharts', 'report:view'),
        {
            name: 'system',
            path: '/system',
            component: System,
            redirect: '/system/accounts',
            meta: { title: '系统管理', icon: 'Setting', permission: 'sys:user:read', requiresAuth: true, sidebar: true },
            children: [
                { name: 'system-accounts', path: 'accounts', component: SystemAccounts, meta: { title: '账号与交接', permission: 'sys:user:read', sidebar: false } },
                { name: 'system-organization', path: 'organization', component: SystemOrganization, meta: { title: '组织与岗位', permission: 'sys:dept:read', sidebar: false } },
                { name: 'system-roles', path: 'roles', component: SystemRoles, meta: { title: '角色与权限', permission: 'sys:role:read', sidebar: false } },
                { name: 'system-rules', path: 'rules', component: SystemRules, meta: { title: '规则与字典', roles: ['admin'], sidebar: false } },
                { name: 'system-imports', path: 'imports', component: SystemImports, meta: { title: '数据导入', roles: ['admin'], sidebar: false } },
                { name: 'system-operations', path: 'operations', component: SystemOperations, meta: { title: '运维审计', permission: 'sys:authorization:audit', roles: ['admin'], sidebar: false } },
                { name: 'system-account-support', path: 'account-support', component: SystemAccountSupport, meta: { title: '账户支持', permission: 'sys:account-support:manage', sidebar: false } }
            ]
        },
        protectedRoute('profile', '/profile', Profile, '个人中心', 'User', undefined, false),
        { path: '/project', redirect: '/projects' },
        { path: '/sample', redirect: '/samples' },
        { path: '/change', redirect: '/changes' },
        { path: '/task', redirect: (to: any) => ({ path: '/tasks/execution', query: to.query, hash: to.hash }) },
        { name: 'public-confirm', path: '/public/confirm/:token', component: PublicConfirm, meta: { public: true } }
    ]
});

// 路由权限判断与默认落点计算。
export function canAccessPath(path: string, session: RouteSession | null): boolean {
    const target = router.resolve(path);
    const permission = target.meta.permission as string | undefined;
    const roles = target.meta.roles as string[] | undefined;
    const permissionAllowed = !permission || Boolean(session?.permissions?.includes(permission)
        || (!permission.startsWith('sys:') && session?.permissions?.includes(`nso:${permission}`)));
    const roleAllowed = !roles?.length || roles.some((role) => session?.roles?.some((current) => current.toLowerCase() === role.toLowerCase()));
    return permissionAllowed && roleAllowed;
}

export function resolveFirstAccessibleRoute(session: RouteSession | null): string {
    return landingRoutes.find((path) => canAccessPath(path, session)) || '/profile';
}
// 路由守卫统一处理登录状态、首次改密、权限校验和项目工作台跳转。
router.beforeEach((to) => {
    const hasToken = Boolean(localStorage.getItem('nso_access_token'));
    if (to.meta.public) {
        if (to.name === 'login' && hasToken) {
            return resolveFirstAccessibleRoute(storedSession());
        }
        return true;
    }
    if (!hasToken) {
        return { name: 'login', query: { redirect: to.fullPath } };
    }
    const session = storedSession();
    if (session?.forceChangePassword && to.name !== 'profile') {
        return { name: 'profile' };
    }
    if (!canAccessPath(to.fullPath, session)) {
        return resolveFirstAccessibleRoute(session);
    }
    if (to.name === 'projects' && to.query.id) {
        return { name: 'project-workspace', query: { ...to.query }, replace: true };
    }
    return true;
});
