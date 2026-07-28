<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import DashboardChart from '@/components/DashboardChart.vue'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import type { DashboardData, DashboardPeriodCode, Message, Project, Risk, Task } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const dashboard = ref<DashboardData>()
const loading = ref(false)
const loadError = ref('')
const period = ref<DashboardPeriodCode>('30D')
const customRange = ref<string[]>([])

const periodOptions: Array<{ value: DashboardPeriodCode; label: string }> = [
  { value: '7D', label: '近 7 天' },
  { value: '30D', label: '近 30 天' },
  { value: 'MONTH', label: '本月' },
  { value: 'CUSTOM', label: '自定义' }
]

const metricCards = computed(() => {
  const metrics = dashboard.value?.metrics || {}
  return [
    { key: 'activeProjects', label: '进行中项目', icon: 'FolderOpened', tone: 'blue', route: 'projects' },
    { key: 'delayedProjects', label: '延期项目', icon: 'WarningFilled', tone: 'danger', route: 'projects' },
    { key: 'pendingSamples', label: '待客户确认', icon: 'TakeawayBox', tone: 'warning', route: 'samples' },
    { key: 'pendingDocuments', label: '待发布图纸', icon: 'DocumentChecked', tone: 'violet', route: 'documents' },
    { key: 'openChanges', label: '未关闭变更', icon: 'Switch', tone: 'blue', route: 'changes' },
    { key: 'reworkQuantity', label: '周期返工数量', icon: 'Operation', tone: 'danger', route: 'reports' }
  ].map((item) => ({ ...item, value: Number(metrics[item.key] || 0) }))
})

const deliveryOption = computed<EChartsOption>(() => {
  const rows = dashboard.value?.deliveryTrend || []
  return {
    color: ['#0d4dcc', '#159b72'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
    legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#667085', fontSize: 12 } },
    grid: { top: 42, right: 12, bottom: 32, left: 34 },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map((row) => row.date.slice(5)), axisLine: { lineStyle: { color: '#dfe3e9' } }, axisLabel: { color: '#69778b', fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf0f4' } }, axisLabel: { color: '#69778b', fontSize: 11 } },
    series: [
      { name: '计划到期', type: 'line', smooth: true, showSymbol: false, data: rows.map((row) => row.dueTasks), lineStyle: { width: 2 } },
      { name: '实际完成', type: 'line', smooth: true, showSymbol: false, data: rows.map((row) => row.completedTasks), lineStyle: { width: 2 } }
    ]
  }
})

const riskOption = computed<EChartsOption>(() => {
  const rows = dashboard.value?.riskDistribution || []
  return {
    color: ['#b42318', '#d92d20', '#e66a00', '#159b72'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} 项' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#667085', fontSize: 12 } },
    series: [{ type: 'pie', radius: ['46%', '72%'], center: ['36%', '50%'], avoidLabelOverlap: true, label: { show: false }, data: rows }]
  }
})

const changeOption = computed<EChartsOption>(() => {
  const rows = dashboard.value?.changeTypes || []
  return {
    color: ['#0d4dcc'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 12, right: 20, bottom: 24, left: 80 },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf0f4' } }, axisLabel: { color: '#69778b', fontSize: 11 } },
    yAxis: { type: 'category', data: rows.map((row) => row.name), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#44546a', fontSize: 12 } },
    series: [{ type: 'bar', barMaxWidth: 20, data: rows.map((row) => row.value), itemStyle: { borderRadius: [0, 3, 3, 0] } }]
  }
})

const departmentOption = computed<EChartsOption>(() => {
  const rows = dashboard.value?.departmentLoads || []
  return {
    color: ['#0d4dcc', '#d92d20', '#7454b8'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#667085', fontSize: 12 } },
    grid: { top: 42, right: 14, bottom: 48, left: 34 },
    xAxis: { type: 'category', data: rows.map((row) => row.name), axisLine: { lineStyle: { color: '#dfe3e9' } }, axisLabel: { color: '#69778b', fontSize: 11, interval: 0, rotate: rows.length > 4 ? 26 : 0 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf0f4' } }, axisLabel: { color: '#69778b', fontSize: 11 } },
    series: [
      { name: '待办', type: 'bar', stack: 'workload', data: rows.map((row) => row.todoTasks), barMaxWidth: 28 },
      { name: '超期', type: 'bar', stack: 'workload', data: rows.map((row) => row.overdueTasks), barMaxWidth: 28 },
      { name: '受影响', type: 'bar', stack: 'workload', data: rows.map((row) => row.impactedTasks), barMaxWidth: 28 }
    ]
  }
})

const riskTag = (level?: string) => ({ SERIOUS: 'danger', HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' } as Record<string, string>)[level || ''] || 'info'
const statusLabel = (status?: string) => ({ TODO: '待处理', BLOCKED: '已阻塞', IN_PROGRESS: '进行中', PAUSED: '已暂停', DONE: '已完成' } as Record<string, string>)[status || ''] || status || '-'
const taskTypeLabel = (type?: string) => ({ TECHNICAL: '技术', PURCHASE: '采购', PRODUCTION: '生产', INSPECTION: '质检', DELIVERY: '交付' } as Record<string, string>)[type || ''] || type || '-'
const projectStageLabel = (stage?: string) => ({ REQUIREMENT: '需求评审', TECHNICAL: '技术准备', SAMPLE: '打样中', CONFIRM: '客户确认', EXECUTION: '生产中', DELIVERY: '待交付' } as Record<string, string>)[stage || ''] || stage || '-'
const numberText = (value: number) => new Intl.NumberFormat('zh-CN').format(value)
const isChartEmpty = (rows?: Array<unknown>) => !rows?.length || rows.every((row: any) =>
  Number(row.value ?? 0) === 0 && Number(row.dueTasks ?? 0) === 0 && Number(row.completedTasks ?? 0) === 0 && Number(row.todoTasks ?? 0) === 0 && Number(row.impactedTasks ?? 0) === 0
)

function requestParams() {
  if (period.value !== 'CUSTOM') return { period: period.value }
  return { period: period.value, startDate: customRange.value[0], endDate: customRange.value[1] }
}

async function load() {
  if (period.value === 'CUSTOM' && customRange.value.length !== 2) {
    ElMessage.warning('请选择统计开始和结束日期')
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    dashboard.value = await api.dashboard(requestParams())
  } catch (error) {
    loadError.value = userFacingError(error, '仪表盘加载失败')
  } finally {
    loading.value = false
  }
}

function onPeriodChange() {
  if (period.value !== 'CUSTOM') void load()
}

function navigate(name: string) {
  void router.push({ name })
}

function openProject(project: Project) {
  void router.push({ name: 'projects', query: { id: String(project.id) } })
}

function openTask(task: Task) {
  void router.push({ name: 'tasks', query: { id: String(task.id) } })
}

function openRisk(risk: Risk) {
  void router.push({ name: 'tasks', query: { projectId: String(risk.projectId), riskId: String(risk.id) } })
}

function canUseShortcut(item: DashboardData['shortcuts'][number]) {
  if (!item.permission) return true
  if (item.permission === 'sample:submit') return auth.can(item.permission) || auth.can('sample:proxy-confirm')
  return auth.can(item.permission)
}

async function markRead(message: Message) {
  try {
    await api.readMessage(message.id)
    if (!dashboard.value) return
    dashboard.value = {
      ...dashboard.value,
      metrics: { ...dashboard.value.metrics, unreadMessages: Math.max(0, Number(dashboard.value.metrics.unreadMessages || 0) - 1) },
      unreadNotifications: dashboard.value.unreadNotifications.filter((item) => item.id !== message.id)
    }
  } catch (error) {
    ElMessage.error(userFacingError(error, '消息状态更新失败'))
  }
}

onMounted(() => { void load() })
</script>

<template>
  <section class="dashboard-page" aria-label="仪表盘">
    <div class="dashboard-heading">
      <div>
        <h1>仪表盘</h1>
        <p>{{ dashboard ? `${dashboard.period.startDate} 至 ${dashboard.period.endDate}` : '正在准备业务数据' }}</p>
      </div>
      <div class="dashboard-filters" aria-label="统计周期">
        <el-select v-model="period" class="dashboard-period" @change="onPeriodChange">
          <el-option v-for="option in periodOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-date-picker
          v-if="period === 'CUSTOM'"
          v-model="customRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="dashboard-range"
          @change="load"
        />
        <el-button :icon="Refresh" :loading="loading" aria-label="刷新仪表盘" @click="load" />
      </div>
    </div>

    <el-alert v-if="loadError" type="error" :title="loadError" show-icon :closable="false">
      <template #default><el-button type="primary" link @click="load">重新加载</el-button></template>
    </el-alert>

    <template v-if="loading && !dashboard">
      <section class="dashboard-skeleton" aria-label="正在加载仪表盘">
        <el-skeleton v-for="index in 6" :key="index" animated><template #template><el-skeleton-item variant="rect" class="dashboard-skeleton__metric" /></template></el-skeleton>
      </section>
    </template>

    <template v-else-if="dashboard">
      <section class="dashboard-metrics" aria-label="关键指标">
        <button v-for="item in metricCards" :key="item.key" type="button" class="dashboard-metric" :class="`dashboard-metric--${item.tone}`" @click="navigate(item.route)">
          <span class="dashboard-metric__label">{{ item.label }}</span>
          <el-icon class="dashboard-metric__icon"><component :is="item.icon" /></el-icon>
          <strong>{{ numberText(item.value) }}</strong>
        </button>
      </section>

      <section class="dashboard-chart-grid" aria-label="统计图表">
        <article class="dashboard-panel dashboard-panel--wide">
          <header class="dashboard-panel__header"><h2>交期执行趋势</h2><span>{{ dashboard.period.period === 'MONTH' ? '本月' : '统计周期' }}</span></header>
          <DashboardChart :option="deliveryOption" :empty="isChartEmpty(dashboard.deliveryTrend)" label="交期执行趋势图" />
        </article>
        <article class="dashboard-panel">
          <header class="dashboard-panel__header"><h2>风险等级分布</h2><button type="button" @click="navigate('tasks')">风险中心</button></header>
          <DashboardChart :option="riskOption" :empty="isChartEmpty(dashboard.riskDistribution)" label="风险等级分布图" />
        </article>
        <article class="dashboard-panel">
          <header class="dashboard-panel__header"><h2>变更类型</h2><button type="button" @click="navigate('changes')">查看变更</button></header>
          <DashboardChart :option="changeOption" :empty="isChartEmpty(dashboard.changeTypes)" label="变更类型统计图" />
        </article>
        <article class="dashboard-panel dashboard-panel--wide">
          <header class="dashboard-panel__header"><h2>部门负载</h2><span>待办、超期、受影响任务</span></header>
          <DashboardChart :option="departmentOption" :empty="!dashboard.departmentLoads.length" label="部门负载统计图" />
        </article>
      </section>

      <section class="dashboard-workspace">
        <article class="dashboard-panel dashboard-projects">
          <header class="dashboard-panel__header"><h2>项目总览</h2><button type="button" @click="navigate('projects')">全部项目</button></header>
          <el-table :data="dashboard.projects" class="table--interactive" size="small" max-height="392" @row-click="openProject">
            <el-table-column prop="projectNo" label="项目编号" min-width="148" />
            <el-table-column prop="customerName" label="客户" min-width="126" show-overflow-tooltip />
            <el-table-column prop="productName" label="产品" min-width="140" show-overflow-tooltip />
            <el-table-column label="阶段" width="108"><template #default="{ row }"><el-tag effect="plain" size="small">{{ projectStageLabel(row.stage) }}</el-tag></template></el-table-column>
            <el-table-column label="风险" width="88"><template #default="{ row }"><el-tag :type="riskTag(row.riskLevel)" effect="plain" size="small">{{ row.riskLevel }}</el-tag></template></el-table-column>
            <el-table-column label="剩余" width="78"><template #default="{ row }"><span :class="{ 'danger-text': row.daysLeft < 0, 'warning-text': row.daysLeft >= 0 && row.daysLeft <= 3 }">{{ row.daysLeft < 0 ? `超期 ${-row.daysLeft} 天` : `${row.daysLeft} 天` }}</span></template></el-table-column>
          </el-table>
          <div v-if="!dashboard.projects.length" class="dashboard-empty">当前数据范围内暂无项目</div>
        </article>

        <aside class="dashboard-rail" aria-label="待处理事项">
          <article class="dashboard-panel">
            <header class="dashboard-panel__header"><h2>高风险项目</h2><span class="danger-text">{{ dashboard.metrics.highRisks || 0 }} 项</span></header>
            <div class="dashboard-list dashboard-list--risks">
              <button v-for="risk in dashboard.criticalRisks" :key="risk.id" type="button" class="risk-row" @click="openRisk(risk)">
                <span><el-tag :type="riskTag(risk.level)" effect="dark" size="small">{{ risk.level }}</el-tag><strong>{{ risk.projectNo }}</strong></span>
                <b>{{ risk.score }} 分</b>
                <p>{{ risk.suggestion || risk.reasons.join('；') }}</p>
              </button>
              <div v-if="!dashboard.criticalRisks.length" class="dashboard-empty dashboard-empty--compact">暂无高风险项目</div>
            </div>
          </article>

          <article class="dashboard-panel">
            <header class="dashboard-panel__header"><h2>我的待办</h2><button type="button" @click="navigate('tasks')">全部任务</button></header>
            <div class="dashboard-list">
              <button v-for="task in dashboard.todos" :key="task.id" type="button" class="task-row" @click="openTask(task)">
                <el-tag effect="plain" size="small">{{ taskTypeLabel(task.taskType) }}</el-tag><strong>{{ task.taskNo }}</strong><span>{{ task.title }}</span><time>{{ task.planFinish || '-' }}</time>
              </button>
              <div v-if="!dashboard.todos.length" class="dashboard-empty dashboard-empty--compact">暂无待办任务</div>
            </div>
          </article>

          <article class="dashboard-panel">
            <header class="dashboard-panel__header"><h2>临期与超期</h2><span class="warning-text">{{ dashboard.metrics.dueTasks || 0 }} 项</span></header>
            <div class="dashboard-list">
              <button v-for="task in dashboard.dueTasks" :key="task.id" type="button" class="task-row" @click="openTask(task)">
                <el-tag :type="task.planFinish && task.planFinish < new Date().toISOString().slice(0, 10) ? 'danger' : 'warning'" effect="plain" size="small">{{ taskTypeLabel(task.taskType) }}</el-tag><strong>{{ task.taskNo }}</strong><span>{{ task.title }}</span><time>{{ task.planFinish || '-' }}</time>
              </button>
              <div v-if="!dashboard.dueTasks.length" class="dashboard-empty dashboard-empty--compact">暂无临期任务</div>
            </div>
          </article>

          <article class="dashboard-panel">
            <header class="dashboard-panel__header"><h2>未读消息</h2><span>{{ dashboard.metrics.unreadMessages || 0 }} 条</span></header>
            <div class="dashboard-list">
              <div v-for="message in dashboard.unreadNotifications" :key="message.id" class="message-row">
                <div><strong>{{ message.title }}</strong><span>{{ message.content }}</span></div><el-button link type="primary" size="small" @click="markRead(message)">已读</el-button>
              </div>
              <div v-if="!dashboard.unreadNotifications.length" class="dashboard-empty dashboard-empty--compact">暂无未读消息</div>
            </div>
          </article>

          <article class="dashboard-panel">
            <header class="dashboard-panel__header"><h2>快捷操作</h2></header>
            <div class="dashboard-shortcuts">
              <el-button v-for="item in dashboard.shortcuts.filter(canUseShortcut)" :key="item.name" size="small" @click="router.push(item.route)">{{ item.name }}</el-button>
            </div>
          </article>
        </aside>
      </section>
    </template>
  </section>
</template>
