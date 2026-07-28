import { createRouter, createWebHistory } from 'vue-router'

import Login from '@/views/login/index.vue'

const Dashboard = () => import('@/views/dashboard/index.vue')
const Projects = () => import('@/views/project/index.vue')
const Documents = () => import('@/views/document/index.vue')
const Samples = () => import('@/views/sample/index.vue')
const Changes = () => import('@/views/change/index.vue')
const Tasks = () => import('@/views/task/index.vue')
const Reports = () => import('@/views/report/index.vue')
const System = () => import('@/views/system/index.vue')
const Profile = () => import('@/views/profile/index.vue')
const PublicConfirm = () => import('@/views/public/confirm/index.vue')

const protectedRoute = (name: string, path: string, component: any, title: string, icon: string, permission?: string, sidebar = true) => ({
  name, path, component, meta: { title, icon, permission, requiresAuth: true, sidebar }
})

export interface RouteSession {
  permissions?: string[]
}

const landingRoutes = [
  '/dashboard', '/projects', '/documents', '/samples', '/changes', '/tasks', '/reports', '/system'
]

function storedSession(): RouteSession | null {
  try {
    return JSON.parse(localStorage.getItem('nso_session') || 'null')
  } catch {
    return null
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => resolveFirstAccessibleRoute(storedSession()) },
    { name: 'login', path: '/login', component: Login, meta: { public: true } },
    protectedRoute('dashboard', '/dashboard', Dashboard, '仪表盘', 'Monitor', 'dashboard:view'),
    protectedRoute('projects', '/projects', Projects, '客户项目', 'FolderOpened', 'project:view'),
    protectedRoute('documents', '/documents', Documents, '技术文件', 'DocumentChecked', 'document:view'),
    protectedRoute('samples', '/samples', Samples, '样品管理', 'TakeawayBox', 'sample:view'),
    protectedRoute('changes', '/changes', Changes, '变更中心', 'Switch', 'change:view'),
    protectedRoute('tasks', '/tasks', Tasks, '任务风险', 'ListChecked', 'task:view'),
    protectedRoute('reports', '/reports', Reports, '统计分析', 'TrendCharts', 'report:view'),
    protectedRoute('system', '/system', System, '系统管理', 'Setting', 'system:manage'),
    protectedRoute('profile', '/profile', Profile, '个人中心', 'User', undefined, false),
    { path: '/project', redirect: '/projects' },
    { path: '/sample', redirect: '/samples' },
    { path: '/change', redirect: '/changes' },
    { path: '/task', redirect: '/tasks' },
    { name: 'public-confirm', path: '/public/confirm/:token', component: PublicConfirm, meta: { public: true } }
  ]
})

export function canAccessPath(path: string, session: RouteSession | null): boolean {
  const target = router.resolve(path)
  const permission = target.meta.permission as string | undefined
  return !permission || Boolean(session?.permissions?.includes(permission))
}

export function resolveFirstAccessibleRoute(session: RouteSession | null): string {
  return landingRoutes.find((path) => canAccessPath(path, session)) || '/profile'
}

router.beforeEach((to) => {
  const hasToken = Boolean(localStorage.getItem('nso_access_token'))
  if (to.meta.public) {
    if (to.name === 'login' && hasToken) return resolveFirstAccessibleRoute(storedSession())
    return true
  }
  if (!hasToken) return { name: 'login', query: { redirect: to.fullPath } }
  const session = storedSession()
  if (!canAccessPath(to.fullPath, session)) return resolveFirstAccessibleRoute(session)
  return true
})
