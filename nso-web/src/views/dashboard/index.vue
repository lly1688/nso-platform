<script setup lang="ts">
// 仪表盘按区域独立加载，避免图表或单个列表阻塞首屏工作。
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { EChartsOption } from 'echarts';
import { ArrowRight, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CountUp from '@/components/CountUp.vue';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import { formatChineseDate } from '@/utils/date';
import { APP_COLORS, CHART_SERIES_COLORS } from '@/theme/designTokens';
import type { DashboardInsights, DashboardOverview, DashboardPeriodCode, Message, Project, Risk, Task } from '@/types';

const DashboardChart = defineAsyncComponent(() => import('@/components/DashboardChart.vue'));

type ActionView = 'risk' | 'todo' | 'due' | 'message';
type InsightView = 'delivery' | 'department' | 'change';

const router = useRouter();
const auth = useAuthStore();
const dashboardOverview = ref<DashboardOverview>();
const dashboardInsights = ref<DashboardInsights>();
const dashboardProjectRows = ref<Project[]>([]);
const dashboardRiskRows = ref<Risk[]>([]);
const dashboardTodoRows = ref<Task[]>([]);
const dashboardDueRows = ref<Task[]>([]);
const dashboardMessageRows = ref<Message[]>([]);
const dashboardProjectPagination = usePagination<Project>({ queryPrefix: 'dashboardProject', defaultPageSize: 8 });
const dashboardRiskPagination = usePagination<Risk>({ queryPrefix: 'dashboardRisk', defaultPageSize: 8 });
const dashboardTodoPagination = usePagination<Task>({ queryPrefix: 'dashboardTodo', defaultPageSize: 8 });
const dashboardDuePagination = usePagination<Task>({ queryPrefix: 'dashboardDue', defaultPageSize: 8 });
const dashboardMessagePagination = usePagination<Message>({ queryPrefix: 'dashboardMessage', defaultPageSize: 8 });
const overviewLoading = ref(false);
const insightsLoading = ref(false);
const refreshLoading = ref(false);
const overviewError = ref('');
const insightsError = ref('');
const projectError = ref('');
const actionError = ref('');
const period = ref<DashboardPeriodCode>('30D');
const customRange = ref<string[]>([]);
const insightView = ref<InsightView>('delivery');
const actionView = ref<ActionView>('risk');
const insightsAnchor = ref<HTMLElement>();
const insightsRequested = ref(false);
let overviewRequestVersion = 0;
let insightsRequestVersion = 0;
let insightsObserver: IntersectionObserver | undefined;

dashboardProjectPagination.configure(
    (params) => api.dashboardProjects({ ...requestParams(), ...params }),
    (result) => { dashboardProjectRows.value = result.list; }
);
dashboardRiskPagination.configure(
    (params) => api.dashboardActionRisks({ ...requestParams(), ...params }),
    (result) => { dashboardRiskRows.value = result.list; }
);
dashboardTodoPagination.configure(
    (params) => api.dashboardActionTodos({ ...requestParams(), ...params }),
    (result) => { dashboardTodoRows.value = result.list; }
);
dashboardDuePagination.configure(
    (params) => api.dashboardActionDue({ ...requestParams(), ...params }),
    (result) => { dashboardDueRows.value = result.list; }
);
dashboardMessagePagination.configure(
    (params) => api.dashboardActionMessages({ ...requestParams(), ...params }),
    (result) => { dashboardMessageRows.value = result.list; }
);

const periodOptions: Array<{ value: DashboardPeriodCode; label: string }> = [
    { value: '7D', label: '近 7 天' },
    { value: '30D', label: '近 30 天' },
    { value: 'MONTH', label: '本月' },
    { value: 'CUSTOM', label: '自定义' }
];
const insightOptions: Array<{ label: string; value: InsightView }> = [
    { label: '交期', value: 'delivery' },
    { label: '部门负载', value: 'department' },
    { label: '变更类型', value: 'change' }
];

const metricCards = computed(() => {
    const metrics = dashboardOverview.value?.metrics || {};
    return [
        { key: 'activeProjects', label: '进行中项目', icon: 'FolderOpened', tone: 'blue', route: 'projects' },
        { key: 'delayedProjects', label: '延期项目', icon: 'WarningFilled', tone: 'danger', route: 'projects' },
        { key: 'pendingSamples', label: '待客户确认', icon: 'TakeawayBox', tone: 'warning', route: 'samples' },
        { key: 'pendingDocuments', label: '待发布图纸', icon: 'DocumentChecked', tone: 'violet', route: 'documents' },
        { key: 'openChanges', label: '未关闭变更', icon: 'Switch', tone: 'blue', route: 'changes' },
        { key: 'reworkQuantity', label: '周期返工数量', icon: 'Operation', tone: 'danger', route: 'reports' }
    ].map((item) => ({
        ...item,
        value: Number(metrics[item.key] || 0),
        action: item.route === 'projects' ? '查看项目' : item.route === 'samples' ? '处理样品' : item.route === 'documents' ? '查看图纸' : item.route === 'changes' ? '处理变更' : item.route === 'reports' ? '查看分析' : '进入列表'
    }));
});

const dashboardWorkflow = [
    { key: 'project', label: '项目建档', hint: '客户与交期' },
    { key: 'review', label: '需求评审', hint: '确认范围' },
    { key: 'technical', label: '技术包', hint: '图纸与工艺' },
    { key: 'sample', label: '打样检验', hint: '质量确认' },
    { key: 'customer', label: '客户确认', hint: '形成结论' },
    { key: 'delivery', label: '任务交付', hint: '按期完成' }
].map((step) => ({ ...step, state: 'neutral' as const }));

const actionTabs = computed(() => [
    { value: 'risk' as const, label: '高风险', count: dashboardRiskPagination.total.value || Number(dashboardOverview.value?.metrics.highRisks || 0) },
    { value: 'todo' as const, label: '我的待办', count: dashboardTodoPagination.total.value || Number(dashboardOverview.value?.metrics.personalTodos || 0) },
    { value: 'due' as const, label: '临期超期', count: dashboardDuePagination.total.value || Number(dashboardOverview.value?.metrics.dueTasks || 0) },
    { value: 'message' as const, label: '未读消息', count: dashboardMessagePagination.total.value || Number(dashboardOverview.value?.metrics.unreadMessages || 0) }
]);
const actionRows = computed(() => {
    switch (actionView.value) {
        case 'risk': return dashboardRiskRows.value;
        case 'todo': return dashboardTodoRows.value;
        case 'due': return dashboardDueRows.value;
        default: return dashboardMessageRows.value;
    }
});
const actionLoading = computed(() => {
    switch (actionView.value) {
        case 'risk': return dashboardRiskPagination.loading.value;
        case 'todo': return dashboardTodoPagination.loading.value;
        case 'due': return dashboardDuePagination.loading.value;
        default: return dashboardMessagePagination.loading.value;
    }
});

const deliveryOption = computed<EChartsOption>(() => {
    const rows = dashboardInsights.value?.deliveryTrend || [];
    return {
        color: [APP_COLORS.primary, APP_COLORS.success],
        tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
        legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: APP_COLORS.textSecondary, fontSize: 12 } },
        grid: { top: 42, right: 12, bottom: 32, left: 34 },
        xAxis: { type: 'category', boundaryGap: false, data: rows.map((row) => row.date.slice(5)), axisLine: { lineStyle: { color: APP_COLORS.border } }, axisLabel: { color: APP_COLORS.textSecondary, fontSize: 11 } },
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: APP_COLORS.divider, type: 'dashed' } }, axisLabel: { color: APP_COLORS.textSecondary, fontSize: 11 } },
        series: [
            { name: '计划到期', type: 'line', smooth: true, showSymbol: false, data: rows.map((row) => row.dueTasks), lineStyle: { width: 2 } },
            { name: '实际完成', type: 'line', smooth: true, showSymbol: false, data: rows.map((row) => row.completedTasks), lineStyle: { width: 2 } }
        ]
    };
});

const riskOption = computed<EChartsOption>(() => {
    const rows = dashboardInsights.value?.riskDistribution || [];
    return {
        color: [APP_COLORS.dangerDark, APP_COLORS.danger, APP_COLORS.warning, APP_COLORS.success],
        tooltip: { trigger: 'item', formatter: '{b}: {c} 项' },
        legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: APP_COLORS.textSecondary, fontSize: 12 } },
        series: [{ type: 'pie', radius: ['46%', '72%'], center: ['36%', '50%'], avoidLabelOverlap: true, label: { show: false }, data: rows.map((row) => ({ ...row, name: riskLevelLabel(row.name) })) }]
    };
});

const changeOption = computed<EChartsOption>(() => {
    const rows = dashboardInsights.value?.changeTypes || [];
    return {
        color: [APP_COLORS.primary],
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { top: 12, right: 20, bottom: 24, left: 80 },
        xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: APP_COLORS.divider, type: 'dashed' } }, axisLabel: { color: APP_COLORS.textSecondary, fontSize: 11 } },
        yAxis: { type: 'category', data: rows.map((row) => row.name), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: APP_COLORS.textSecondary, fontSize: 12 } },
        series: [{ type: 'bar', barMaxWidth: 20, data: rows.map((row) => row.value), itemStyle: { borderRadius: [0, 6, 6, 0] } }]
    };
});

const departmentOption = computed<EChartsOption>(() => {
    const rows = dashboardInsights.value?.departmentLoads || [];
    return {
        color: [CHART_SERIES_COLORS[0], CHART_SERIES_COLORS[3], CHART_SERIES_COLORS[2]],
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: APP_COLORS.textSecondary, fontSize: 12 } },
        grid: { top: 42, right: 14, bottom: 48, left: 34 },
        xAxis: { type: 'category', data: rows.map((row) => row.name), axisLine: { lineStyle: { color: APP_COLORS.border } }, axisLabel: { color: APP_COLORS.textSecondary, fontSize: 11, interval: 0, rotate: rows.length > 4 ? 26 : 0 } },
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: APP_COLORS.divider, type: 'dashed' } }, axisLabel: { color: APP_COLORS.textSecondary, fontSize: 11 } },
        series: [
            { name: '待办', type: 'bar', stack: 'workload', data: rows.map((row) => row.todoTasks), barMaxWidth: 28 },
            { name: '超期', type: 'bar', stack: 'workload', data: rows.map((row) => row.overdueTasks), barMaxWidth: 28 },
            { name: '受影响', type: 'bar', stack: 'workload', data: rows.map((row) => row.impactedTasks), barMaxWidth: 28 }
        ]
    };
});

const riskTag = (level?: string) => ({ SERIOUS: 'danger', HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' } as Record<string, string>)[level || ''] || 'info';
const taskTypeLabel = (type?: string) => ({ TECHNICAL: '技术', DESIGN: '设计', PROCESS: '工艺', PURCHASE: '采购', PRODUCTION: '生产', QUALITY: '质检', INSPECTION: '质检', DELIVERY: '交付', COORDINATION: '协同', SAMPLE_PREPARE: '备料', SAMPLE_MAKE: '制样', SAMPLE_REWORK: '返工' } as Record<string, string>)[type || ''] || type || '-';
const projectStageLabel = (stage?: string) => ({ REQUIREMENT: '需求评审', TECHNICAL: '技术准备', SAMPLE: '打样中', CONFIRM: '客户确认', EXECUTION: '生产中', DELIVERY: '待交付', ARCHIVE: '已归档' } as Record<string, string>)[stage || ''] || stage || '-';
const riskLevelLabel = (level?: string) => ({ SERIOUS: '严重', HIGH: '高', MEDIUM: '中', LOW: '低' } as Record<string, string>)[level || ''] || level || '-';
const terminalProjectStatuses = new Set(['COMPLETED', 'ARCHIVED', 'CANCELLED']);
const isActiveProject = (project: Project) => !terminalProjectStatuses.has(project.status);
const projectDueText = (project: Project) => {
    if (!isActiveProject(project)) return '已结束';
    return project.daysLeft < 0 ? `超期 ${-project.daysLeft} 天` : `${project.daysLeft} 天`;
};
const isOverdue = (date?: string) => Boolean(date && date < new Date().toISOString().slice(0, 10));
const isChartEmpty = (rows?: Array<unknown>) => !rows?.length || rows.every((row: any) => Number(row.value ?? 0) === 0 && Number(row.dueTasks ?? 0) === 0 && Number(row.completedTasks ?? 0) === 0 && Number(row.todoTasks ?? 0) === 0 && Number(row.impactedTasks ?? 0) === 0);

function requestParams() {
    return period.value === 'CUSTOM'
        ? { period: period.value, startDate: customRange.value[0], endDate: customRange.value[1] }
        : { period: period.value };
}

function hasCompletePeriod() {
    return period.value !== 'CUSTOM' || customRange.value.length === 2;
}

async function loadOverview() {
    if (!hasCompletePeriod()) return;
    const requestVersion = ++overviewRequestVersion;
    overviewLoading.value = true;
    overviewError.value = '';
    try {
        const result = await api.dashboardOverview(requestParams());
        if (requestVersion === overviewRequestVersion) {
            dashboardOverview.value = result;
        }
    }
    catch (error) {
        if (requestVersion === overviewRequestVersion) {
            overviewError.value = userFacingError(error, '概览指标加载失败');
        }
    }
    finally {
        if (requestVersion === overviewRequestVersion) {
            overviewLoading.value = false;
        }
    }
}

async function loadInsights() {
    if (!hasCompletePeriod()) return;
    insightsRequested.value = true;
    const requestVersion = ++insightsRequestVersion;
    insightsLoading.value = true;
    insightsError.value = '';
    try {
        const result = await api.dashboardInsights(requestParams());
        if (requestVersion === insightsRequestVersion) {
            dashboardInsights.value = result;
        }
    }
    catch (error) {
        if (requestVersion === insightsRequestVersion) {
            insightsError.value = userFacingError(error, '分析数据加载失败');
        }
    }
    finally {
        if (requestVersion === insightsRequestVersion) {
            insightsLoading.value = false;
        }
    }
}

async function runProjectRequest(request: () => Promise<void>) {
    projectError.value = '';
    try {
        await request();
    }
    catch (error) {
        projectError.value = userFacingError(error, '项目列表加载失败');
    }
}

async function loadProjectPage() {
    await runProjectRequest(() => dashboardProjectPagination.reload());
}

async function goProjectPage(pageNo: number) {
    await runProjectRequest(() => dashboardProjectPagination.goTo(pageNo));
}

async function changeProjectPageSize(pageSize: number) {
    await runProjectRequest(() => dashboardProjectPagination.changePageSize(pageSize));
}

function actionRequest(view: ActionView, request: 'reload' | 'page' | 'size', value?: number) {
    if (view === 'risk') {
        return request === 'reload' ? dashboardRiskPagination.reload() : request === 'page'
            ? dashboardRiskPagination.goTo(value || 1) : dashboardRiskPagination.changePageSize(value || 8);
    }
    if (view === 'todo') {
        return request === 'reload' ? dashboardTodoPagination.reload() : request === 'page'
            ? dashboardTodoPagination.goTo(value || 1) : dashboardTodoPagination.changePageSize(value || 8);
    }
    if (view === 'due') {
        return request === 'reload' ? dashboardDuePagination.reload() : request === 'page'
            ? dashboardDuePagination.goTo(value || 1) : dashboardDuePagination.changePageSize(value || 8);
    }
    return request === 'reload' ? dashboardMessagePagination.reload() : request === 'page'
        ? dashboardMessagePagination.goTo(value || 1) : dashboardMessagePagination.changePageSize(value || 8);
}

async function runActionRequest(view: ActionView, request: 'reload' | 'page' | 'size', value?: number) {
    actionError.value = '';
    try {
        await actionRequest(view, request, value);
    }
    catch (error) {
        if (view === actionView.value) {
            actionError.value = userFacingError(error, '行动中心加载失败');
        }
    }
}

async function loadActionPage(view = actionView.value) {
    await runActionRequest(view, 'reload');
}

async function refreshPeriod() {
    if (!hasCompletePeriod()) return;
    await Promise.allSettled([
        loadOverview(),
        loadProjectPage(),
        loadActionPage(),
        insightsRequested.value ? loadInsights() : Promise.resolve()
    ]);
}

async function refreshDashboard() {
    if (!hasCompletePeriod()) {
        ElMessage.warning('请选择统计开始和结束日期');
        return;
    }
    refreshLoading.value = true;
    try {
        await Promise.allSettled([
            loadOverview(),
            loadProjectPage(),
            loadActionPage(),
            insightsRequested.value ? loadInsights() : Promise.resolve()
        ]);
    }
    finally {
        refreshLoading.value = false;
    }
}

function onPeriodChange() {
    if (period.value !== 'CUSTOM') {
        void refreshPeriod();
    }
}

function onCustomRangeChange() {
    if (customRange.value.length === 2) {
        void refreshPeriod();
    }
}

function observeInsights() {
    if (!insightsAnchor.value) return;
    if (typeof IntersectionObserver === 'undefined') {
        void loadInsights();
        return;
    }
    insightsObserver = new IntersectionObserver(([entry]) => {
        if (!entry?.isIntersecting) return;
        insightsObserver?.disconnect();
        insightsObserver = undefined;
        void loadInsights();
    }, { rootMargin: '300px 0px' });
    insightsObserver.observe(insightsAnchor.value);
}

function navigate(name: string) {
    void router.push({ name });
}

function openProject(project: Project) {
    void router.push({ name: 'project-workspace', query: { id: String(project.id) } });
}

function openTask(task: Task) {
    void router.push({ name: 'tasks-execution', query: { id: String(task.id) } });
}

function openRisk(risk: Risk) {
    void router.push({ name: 'tasks-risks', query: { projectId: String(risk.projectId), riskId: String(risk.id) } });
}

function openMessage(message: Message) {
    const routes: Record<string, { name: string; query?: Record<string, string> }> = {
        PROJECT: { name: 'project-workspace', query: { id: String(message.businessId) } },
        SAMPLE: { name: 'samples' },
        CHANGE: { name: 'changes' },
        RISK: { name: 'tasks-risks' },
        RISK_ACTION: { name: 'tasks-risks' },
        TASK: { name: 'tasks-execution' },
        DELIVERY: { name: 'tasks-deliveries' }
    };
    void router.push(routes[message.businessType] || { name: 'actions' });
}

function canUseShortcut(item: DashboardOverview['shortcuts'][number]) {
    if (!item.permission) return true;
    if (item.permission === 'sample:submit') {
        return auth.can(item.permission) || auth.can('sample:proxy-confirm');
    }
    return auth.can(item.permission);
}

async function markRead(message: Message) {
    try {
        await api.readMessage(message.id);
        dashboardMessageRows.value = dashboardMessageRows.value.filter((item) => item.id !== message.id);
        dashboardMessagePagination.total.value = Math.max(0, dashboardMessagePagination.total.value - 1);
        if (dashboardOverview.value) {
            dashboardOverview.value = {
                ...dashboardOverview.value,
                metrics: {
                    ...dashboardOverview.value.metrics,
                    unreadMessages: Math.max(0, Number(dashboardOverview.value.metrics.unreadMessages || 0) - 1)
                }
            };
        }
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '消息状态更新失败'));
    }
}

watch(actionView, (view) => {
    void loadActionPage(view);
});

onMounted(() => {
    void Promise.allSettled([loadOverview(), loadProjectPage(), loadActionPage()]);
    observeInsights();
});

onBeforeUnmount(() => {
    insightsObserver?.disconnect();
});
</script>

<template>
    <section class="dashboard-page" aria-label="仪表盘">
        <div class="dashboard-heading">
            <div>
                <h1>仪表盘</h1>
                <p>{{ dashboardOverview ? `${formatChineseDate(dashboardOverview.period.startDate)} 至 ${formatChineseDate(dashboardOverview.period.endDate)}` : '正在准备业务数据' }}</p>
            </div>
            <div class="dashboard-filters" aria-label="统计周期">
                <span class="dashboard-filters__label">统计范围</span>
                <el-select v-model="period" class="dashboard-period" @change="onPeriodChange">
                    <el-option v-for="option in periodOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <el-date-picker
                    v-if="period === 'CUSTOM'"
                    v-model="customRange"
                    type="daterange"
                    format="YYYY年MM月DD日"
                    value-format="YYYY-MM-DD"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    class="dashboard-range"
                    @change="onCustomRangeChange"
                />
                <el-tooltip content="刷新数据" placement="top">
                    <el-button :icon="Refresh" :loading="refreshLoading" aria-label="刷新仪表盘数据" @click="refreshDashboard" />
                </el-tooltip>
            </div>
        </div>

        <WorkflowStrip :steps="dashboardWorkflow" aria-label="项目协同流程" />

        <section class="dashboard-overview dashboard-panel" aria-labelledby="overview-title" :aria-busy="overviewLoading && !dashboardOverview">
            <header class="dashboard-panel__header dashboard-overview__header">
                <div>
                    <h2 id="overview-title">经营概览</h2>
                    <span>关键项目与交付信号</span>
                </div>
                <span>{{ dashboardOverview ? `${formatChineseDate(dashboardOverview.period.startDate)} 至 ${formatChineseDate(dashboardOverview.period.endDate)}` : '加载中' }}</span>
            </header>
            <div class="dashboard-metrics" aria-label="关键指标">
                <template v-if="dashboardOverview">
                    <button
                        v-for="(item, index) in metricCards"
                        :key="item.key"
                        type="button"
                        class="dashboard-metric"
                        :class="`dashboard-metric--${item.tone}`"
                        :aria-label="`${item.label} ${item.value}，${item.action}`"
                        @click="navigate(item.route)"
                    >
                        <span class="dashboard-metric__icon-wrap"><el-icon class="dashboard-metric__icon"><component :is="item.icon" /></el-icon></span>
                        <span class="dashboard-metric__copy">
                            <span class="dashboard-metric__label">{{ item.label }}</span>
                            <strong><CountUp :to="item.value" :delay="index * 0.05" :duration="0.7" separator="," /></strong>
                            <small class="dashboard-metric__action">{{ item.action }} <el-icon><ArrowRight /></el-icon></small>
                        </span>
                    </button>
                </template>
                <template v-else>
                    <div v-for="index in 6" :key="index" class="dashboard-metric dashboard-metric--skeleton" aria-hidden="true">
                        <el-skeleton animated class="dashboard-metric-skeleton">
                            <template #template>
                                <el-skeleton-item variant="circle" class="dashboard-metric-skeleton__icon" />
                                <span class="dashboard-metric-skeleton__copy">
                                    <el-skeleton-item variant="text" class="dashboard-metric-skeleton__value" />
                                    <el-skeleton-item variant="text" class="dashboard-metric-skeleton__label" />
                                </span>
                            </template>
                        </el-skeleton>
                    </div>
                </template>
            </div>
            <div v-if="overviewError" class="dashboard-region-error" role="alert">
                <span>{{ overviewError }}</span><el-button type="primary" link size="small" @click="loadOverview">重试</el-button>
            </div>
        </section>

        <section ref="insightsAnchor" class="dashboard-analysis" aria-label="数据分析">
            <article class="dashboard-panel dashboard-analysis__main" :aria-busy="insightsLoading && !dashboardInsights">
                <header class="dashboard-panel__header dashboard-analysis__header">
                    <div>
                        <h2>数据趋势</h2>
                        <span>按交期、部门或变更类型切换查看</span>
                    </div>
                    <el-segmented v-model="insightView" class="dashboard-segmented" :options="insightOptions" size="small" aria-label="数据分析视图" />
                </header>
                <template v-if="dashboardInsights">
                    <DashboardChart v-if="insightView === 'delivery'" :option="deliveryOption" :empty="isChartEmpty(dashboardInsights.deliveryTrend)" label="交期执行趋势图" />
                    <DashboardChart v-else-if="insightView === 'department'" :option="departmentOption" :empty="!dashboardInsights.departmentLoads.length" label="部门负载统计图" />
                    <DashboardChart v-else :option="changeOption" :empty="isChartEmpty(dashboardInsights.changeTypes)" label="变更类型统计图" />
                </template>
                <div v-else class="dashboard-chart-skeleton" aria-label="正在加载数据趋势">
                    <el-skeleton animated>
                        <template #template>
                            <el-skeleton-item v-for="index in 5" :key="index" variant="text" class="dashboard-chart-skeleton__line" />
                        </template>
                    </el-skeleton>
                </div>
                <div v-if="insightsError" class="dashboard-region-error" role="alert">
                    <span>{{ insightsError }}</span><el-button type="primary" link size="small" @click="loadInsights">重试</el-button>
                </div>
            </article>

            <article class="dashboard-panel dashboard-analysis__risk" :aria-busy="insightsLoading && !dashboardInsights">
                <header class="dashboard-panel__header">
                    <div>
                        <h2>风险等级分布</h2>
                        <span>{{ dashboardOverview?.metrics.highRisks || 0 }} 个项目需关注</span>
                    </div>
                    <el-button type="primary" link @click="navigate('tasks-risks')">查看全部风险 <el-icon><ArrowRight /></el-icon></el-button>
                </header>
                <DashboardChart v-if="dashboardInsights" :option="riskOption" :empty="isChartEmpty(dashboardInsights.riskDistribution)" label="风险等级分布图" />
                <div v-else class="dashboard-chart-skeleton" aria-label="正在加载风险分布">
                    <el-skeleton animated><template #template><el-skeleton-item v-for="index in 5" :key="index" variant="text" class="dashboard-chart-skeleton__line" /></template></el-skeleton>
                </div>
                <div v-if="insightsError" class="dashboard-region-error dashboard-region-error--compact" role="alert">
                    <span>{{ insightsError }}</span><el-button type="primary" link size="small" @click="loadInsights">重试</el-button>
                </div>
            </article>
        </section>

        <section class="dashboard-workspace">
            <article class="dashboard-panel dashboard-projects" :aria-busy="dashboardProjectPagination.loading && !dashboardProjectRows.length">
                <header class="dashboard-panel__header"><h2>项目总览</h2><el-button type="primary" link @click="navigate('projects')">查看全部项目 <el-icon><ArrowRight /></el-icon></el-button></header>
                <div v-if="projectError" class="dashboard-region-error" role="alert">
                    <span>{{ projectError }}</span><el-button type="primary" link size="small" @click="loadProjectPage">重试</el-button>
                </div>
                <div v-if="dashboardProjectPagination.loading && !dashboardProjectRows.length" class="dashboard-table-skeleton" aria-label="正在加载项目列表">
                    <el-skeleton v-for="index in 6" :key="index" animated><template #template><el-skeleton-item variant="text" /></template></el-skeleton>
                </div>
                <template v-else>
                    <el-table :data="dashboardProjectRows" class="table--interactive" size="small" max-height="392" @row-click="openProject">
                        <el-table-column prop="projectNo" label="项目编号" min-width="148" />
                        <el-table-column prop="customerName" label="客户" min-width="126" show-overflow-tooltip />
                        <el-table-column prop="productName" label="产品" min-width="140" show-overflow-tooltip />
                        <el-table-column label="阶段" width="108"><template #default="{ row }"><el-tag effect="plain" size="small">{{ projectStageLabel(row.stage) }}</el-tag></template></el-table-column>
                        <el-table-column label="风险" width="88"><template #default="{ row }"><el-tag :type="riskTag(row.riskLevel)" effect="plain" size="small">{{ riskLevelLabel(row.riskLevel) }}</el-tag></template></el-table-column>
                        <el-table-column label="剩余" width="78"><template #default="{ row }"><span :class="{ 'danger-text': isActiveProject(row) && row.daysLeft < 0, 'warning-text': isActiveProject(row) && row.daysLeft >= 0 && row.daysLeft <= 3 }">{{ projectDueText(row) }}</span></template></el-table-column>
                        <el-table-column label="操作" width="82" fixed="right"><template #default="{ row }"><el-button type="primary" link size="small" @click.stop="openProject(row)">打开 <el-icon><ArrowRight /></el-icon></el-button></template></el-table-column>
                    </el-table>
                    <el-empty v-if="!dashboardProjectRows.length" class="dashboard-empty" :image-size="44" description="当前数据范围内暂无项目" />
                </template>
                <PaginationBar
                    :page-no="dashboardProjectPagination.pageNo"
                    :page-size="dashboardProjectPagination.pageSize"
                    :total="dashboardProjectPagination.total"
                    :loading="dashboardProjectPagination.loading"
                    @update:page-no="goProjectPage"
                    @update:page-size="changeProjectPageSize"
                />
            </article>

            <aside class="dashboard-panel dashboard-action-center" aria-labelledby="action-center-title" :aria-busy="actionLoading && !actionRows.length">
                <header class="dashboard-panel__header">
                    <div>
                        <h2 id="action-center-title">行动中心</h2>
                        <span>优先处理异常与临期事项</span>
                    </div>
                    <el-button type="primary" link @click="navigate('tasks')">查看全部任务 <el-icon><ArrowRight /></el-icon></el-button>
                </header>

                <div class="dashboard-action-tabs" role="tablist" aria-label="待处理事项类型">
                    <button v-for="item in actionTabs" :key="item.value" type="button" role="tab" :aria-selected="actionView === item.value" :class="{ 'is-active': actionView === item.value }" @click="actionView = item.value">
                        <span>{{ item.label }}<b v-if="item.count">（{{ item.count }}）</b></span>
                    </button>
                </div>
                <div v-if="actionError" class="dashboard-region-error" role="alert">
                    <span>{{ actionError }}</span><el-button type="primary" link size="small" @click="loadActionPage">重试</el-button>
                </div>

                <div class="dashboard-action-content">
                    <div v-if="actionLoading && !actionRows.length" class="dashboard-list dashboard-list--skeleton" aria-label="正在加载行动项">
                        <el-skeleton v-for="index in 5" :key="index" animated><template #template><el-skeleton-item variant="text" /></template></el-skeleton>
                    </div>
                    <template v-else>
                        <div v-if="actionView === 'risk'" class="dashboard-list dashboard-list--risks">
                            <button v-for="risk in dashboardRiskRows" :key="risk.id" type="button" class="risk-row" @click="openRisk(risk)">
                                <span><el-tag :type="riskTag(risk.level)" effect="dark" size="small">{{ riskLevelLabel(risk.level) }}</el-tag><strong>{{ risk.projectNo }}</strong></span><em>去处理 <el-icon><ArrowRight /></el-icon></em>
                                <b>{{ risk.score }} 分</b><p>{{ risk.suggestion || risk.reasons.join('；') }}</p>
                            </button>
                            <el-empty v-if="!dashboardRiskRows.length" class="dashboard-empty dashboard-empty--compact" :image-size="40" description="暂无高风险项" />
                        </div>
                        <div v-else-if="actionView === 'todo'" class="dashboard-list">
                            <button v-for="task in dashboardTodoRows" :key="task.id" type="button" class="task-row" @click="openTask(task)">
                                <el-tag effect="plain" size="small">{{ taskTypeLabel(task.taskType) }}</el-tag><strong>{{ task.taskNo }}</strong><span>{{ task.title }}</span><time>{{ formatChineseDate(task.planFinish) }} · 去处理 <el-icon><ArrowRight /></el-icon></time>
                            </button>
                            <el-empty v-if="!dashboardTodoRows.length" class="dashboard-empty dashboard-empty--compact" :image-size="40" description="暂无待办任务" />
                        </div>
                        <div v-else-if="actionView === 'due'" class="dashboard-list">
                            <button v-for="task in dashboardDueRows" :key="task.id" type="button" class="task-row" @click="openTask(task)">
                                <el-tag :type="isOverdue(task.planFinish) ? 'danger' : 'warning'" effect="plain" size="small">{{ taskTypeLabel(task.taskType) }}</el-tag><strong>{{ task.taskNo }}</strong><span>{{ task.title }}</span><time>{{ formatChineseDate(task.planFinish) }} · 去处理 <el-icon><ArrowRight /></el-icon></time>
                            </button>
                            <el-empty v-if="!dashboardDueRows.length" class="dashboard-empty dashboard-empty--compact" :image-size="40" description="暂无临期任务" />
                        </div>
                        <div v-else class="dashboard-list">
                            <div v-for="message in dashboardMessageRows" :key="message.id" class="message-row"><div><strong>{{ message.title }}</strong><span>{{ message.content }}</span></div><span class="message-row__actions"><el-button link type="primary" size="small" @click="openMessage(message)">查看</el-button><el-button link size="small" @click="markRead(message)">已读</el-button></span></div>
                            <el-empty v-if="!dashboardMessageRows.length" class="dashboard-empty dashboard-empty--compact" :image-size="40" description="暂无未读消息" />
                        </div>
                    </template>
                </div>
                <PaginationBar v-if="actionView === 'risk'" :page-no="dashboardRiskPagination.pageNo" :page-size="dashboardRiskPagination.pageSize" :total="dashboardRiskPagination.total" :loading="dashboardRiskPagination.loading" @update:page-no="(value) => runActionRequest('risk', 'page', value)" @update:page-size="(value) => runActionRequest('risk', 'size', value)" />
                <PaginationBar v-else-if="actionView === 'todo'" :page-no="dashboardTodoPagination.pageNo" :page-size="dashboardTodoPagination.pageSize" :total="dashboardTodoPagination.total" :loading="dashboardTodoPagination.loading" @update:page-no="(value) => runActionRequest('todo', 'page', value)" @update:page-size="(value) => runActionRequest('todo', 'size', value)" />
                <PaginationBar v-else-if="actionView === 'due'" :page-no="dashboardDuePagination.pageNo" :page-size="dashboardDuePagination.pageSize" :total="dashboardDuePagination.total" :loading="dashboardDuePagination.loading" @update:page-no="(value) => runActionRequest('due', 'page', value)" @update:page-size="(value) => runActionRequest('due', 'size', value)" />
                <PaginationBar v-else :page-no="dashboardMessagePagination.pageNo" :page-size="dashboardMessagePagination.pageSize" :total="dashboardMessagePagination.total" :loading="dashboardMessagePagination.loading" @update:page-no="(value) => runActionRequest('message', 'page', value)" @update:page-size="(value) => runActionRequest('message', 'size', value)" />

                <footer v-if="dashboardOverview?.shortcuts.filter(canUseShortcut).length" class="dashboard-action-footer">
                    <span>快捷操作</span>
                    <div class="dashboard-shortcuts"><el-button v-for="item in dashboardOverview.shortcuts.filter(canUseShortcut)" :key="item.name" size="small" @click="router.push(item.route)">{{ item.name }}</el-button></div>
                </footer>
            </aside>
        </section>
    </section>
</template>
