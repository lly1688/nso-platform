import { createRouter, createWebHistory } from 'vue-router'

import Login from '@/views/login/index.vue'
import Dashboard from '@/views/dashboard/index.vue'
import Projects from '@/views/project/index.vue'
import Documents from '@/views/document/index.vue'
import Samples from '@/views/sample/index.vue'
import Changes from '@/views/change/index.vue'
import Tasks from '@/views/task/index.vue'
import Reports from '@/views/report/index.vue'
import System from '@/views/system/index.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: Login },
    { path: '/dashboard', component: Dashboard, meta: { title: '工作台', icon: 'Monitor' } },
    { path: '/projects', component: Projects, meta: { title: '客户项目', icon: 'FolderOpened' } },
    { path: '/documents', component: Documents, meta: { title: '技术文件', icon: 'DocumentChecked' } },
    { path: '/samples', component: Samples, meta: { title: '样品管理', icon: 'TakeawayBox' } },
    { path: '/changes', component: Changes, meta: { title: '变更中心', icon: 'Switch' } },
    { path: '/tasks', component: Tasks, meta: { title: '任务风险', icon: 'ListChecked' } },
    { path: '/reports', component: Reports, meta: { title: '统计分析', icon: 'TrendCharts' } },
    { path: '/system', component: System, meta: { title: '系统管理', icon: 'Setting' } }
  ]
})
