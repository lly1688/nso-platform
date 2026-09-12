<script setup lang="ts">
// 任务风险识别、处置与趋势状态。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import type { Project, Risk, Task } from '@/types';
import { exceptionTypeOptions, pageList, riskLevelLabel, riskLevelTag, riskStatusLabel } from './shared';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const projects = ref<Project[]>([]);
const projectTasks = ref<Task[]>([]);
const risks = ref<Risk[]>([]);
const loading = ref(false);
const tasksLoading = ref(false);
const initialized = ref(false);
let replacingQuery = false;
const canViewRisks = computed(() => auth.can('risk:view'));
const canReportException = computed(() => auth.can('task:feedback'));
const hasPageAccess = computed(() => canViewRisks.value || canReportException.value);
const riskSummary = computed(() => {
    const currentPage = risks.value;
    return {
        total: riskPagination.total,
        urgent: currentPage.filter((risk) => risk.level === 'SERIOUS' || risk.level === 'HIGH').length,
        pending: currentPage.filter((risk) => risk.status !== 'CLOSED').length,
        closed: currentPage.filter((risk) => risk.status === 'CLOSED').length
    };
});
const exceptionForm = reactive({
    projectId: undefined as number | undefined,
    taskId: undefined as number | undefined,
    exceptionType: 'MATERIAL_SHORTAGE',
    summary: '',
    reporterName: ''
});
const riskPagination = usePagination<Risk>();
riskPagination.configure(
    (params) => api.risks(params),
    (result) => {
        risks.value = result.list;
    }
);

function positiveId(value: unknown): number | undefined {
    const source = Array.isArray(value) ? value[0] : value;
    if (typeof source !== 'string' || !/^\d+$/.test(source)) {
        return undefined;
    }
    const parsed = Number(source);
    return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined;
}

async function clearContextQuery(clearProject: boolean, clearTask: boolean) {
    const nextQuery = { ...route.query };
    if (clearProject) {
        delete nextQuery.projectId;
    }
    if (clearTask) {
        delete nextQuery.taskId;
    }
    replacingQuery = true;
    try {
        await router.replace({ query: nextQuery });
    }
    finally {
        replacingQuery = false;
    }
}

async function fetchRisks() {
    await riskPagination.reload();
}

async function refreshRisks() {
    if (!canViewRisks.value) {
        return;
    }
    try {
        await fetchRisks();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '风险列表刷新失败'));
    }
}

async function loadProjectTasks(projectId?: number, preferredTaskId?: number) {
    projectTasks.value = [];
    exceptionForm.taskId = undefined;
    if (!projectId || !canReportException.value) {
        return false;
    }
    tasksLoading.value = true;
    try {
        projectTasks.value = pageList(await api.tasks({ projectId, pageSize: 100 }));
        if (preferredTaskId && projectTasks.value.some((task) => task.id === preferredTaskId)) {
            exceptionForm.taskId = preferredTaskId;
            return true;
        }
        return !preferredTaskId;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '关联任务加载失败'));
        return false;
    }
    finally {
        tasksLoading.value = false;
    }
}

async function applyRouteContext() {
    if (!canReportException.value) {
        if (route.query.projectId || route.query.taskId) {
            await clearContextQuery(true, true);
        }
        return;
    }
    const hasProjectParam = route.query.projectId !== undefined;
    const hasTaskParam = route.query.taskId !== undefined;
    const projectId = positiveId(route.query.projectId);
    const taskId = positiveId(route.query.taskId);
    if (hasProjectParam && !projectId) {
        await clearContextQuery(true, true);
        return;
    }
    if (hasTaskParam && !taskId) {
        await clearContextQuery(false, true);
        return;
    }
    if (!projectId) {
        if (hasTaskParam) {
            await clearContextQuery(false, true);
        }
        return;
    }
    if (!projects.value.some((project) => project.id === projectId)) {
        exceptionForm.projectId = undefined;
        exceptionForm.taskId = undefined;
        projectTasks.value = [];
        await clearContextQuery(true, true);
        return;
    }
    exceptionForm.projectId = projectId;
    const taskMatched = await loadProjectTasks(projectId, taskId);
    if (taskId && !taskMatched) {
        await clearContextQuery(false, true);
    }
}

async function onProjectChange(projectId?: number) {
    exceptionForm.projectId = projectId;
    await clearContextQuery(true, true);
    await loadProjectTasks(projectId);
}

async function loadInitialData() {
    if (!hasPageAccess.value) {
        if (route.query.projectId || route.query.taskId) {
            await clearContextQuery(true, true);
        }
        initialized.value = true;
        return;
    }
    loading.value = true;
    try {
        const [, projectData] = await Promise.all([
            canViewRisks.value ? riskPagination.reload() : Promise.resolve(undefined),
            canReportException.value ? api.projects({ pageSize: 100 }) : Promise.resolve(undefined)
        ]);
        projects.value = pageList(projectData);
        initialized.value = true;
        await applyRouteContext();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '风险与异常数据加载失败'));
    }
    finally {
        loading.value = false;
    }
}

async function reportException() {
    if (!exceptionForm.projectId) {
        ElMessage.warning('请选择项目');
        return;
    }
    if (!exceptionForm.summary.trim()) {
        ElMessage.warning('请填写异常说明');
        return;
    }
    const payload: Record<string, unknown> = {
        projectId: exceptionForm.projectId,
        exceptionType: exceptionForm.exceptionType,
        summary: exceptionForm.summary.trim(),
        reporterName: exceptionForm.reporterName.trim()
    };
    if (exceptionForm.taskId) {
        payload.taskId = exceptionForm.taskId;
    }
    try {
        await api.reportException(payload);
        ElMessage.success('异常已上报并生成风险');
        exceptionForm.projectId = undefined;
        exceptionForm.taskId = undefined;
        exceptionForm.exceptionType = 'MATERIAL_SHORTAGE';
        exceptionForm.summary = '';
        exceptionForm.reporterName = '';
        projectTasks.value = [];
        await clearContextQuery(true, true);
        await refreshRisks();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '异常上报失败'));
    }
}

watch(() => [route.query.projectId, route.query.taskId], () => {
    if (initialized.value && !replacingQuery) {
        void applyRouteContext();
    }
});

onMounted(() => {
    void loadInitialData();
});
</script>

<template>
    <!-- 风险查询、处置建议与列表 -->
    <div class="module-page" v-loading="loading">
        <WorkflowStrip
            :steps="[
                { key: 'report', label: '上报异常', hint: '说明现象与影响', state: 'current' },
                { key: 'risk', label: '生成风险', hint: '系统评估等级', state: riskPagination.total ? 'done' : 'pending' },
                { key: 'dispose', label: '进入处置', hint: '明确责任人与期限' },
                { key: 'close', label: '验证关闭', hint: '保留处置证据' }
            ]"
            aria-label="风险处置流程"
        />
        <section v-if="!hasPageAccess" class="panel task-access-empty">
            <el-empty description="暂无风险与异常访问权限" :image-size="72" />
        </section>

        <section v-else class="task-risk-dashboard">
            <div v-if="canViewRisks" class="task-risk-main">
                <section class="task-risk-summary" aria-label="风险概览">
                    <div class="task-risk-summary__item task-risk-summary__item--total">
                        <span>风险总数</span>
                        <strong>{{ riskSummary.total }}</strong>
                        <small>全部页</small>
                    </div>
                    <div class="task-risk-summary__item task-risk-summary__item--urgent">
                        <span>严重 / 高风险</span>
                        <strong>{{ riskSummary.urgent }}</strong>
                        <small>当前页</small>
                    </div>
                    <div class="task-risk-summary__item task-risk-summary__item--pending">
                        <span>待处置</span>
                        <strong>{{ riskSummary.pending }}</strong>
                        <small>当前页</small>
                    </div>
                    <div class="task-risk-summary__item task-risk-summary__item--closed">
                        <span>已关闭</span>
                        <strong>{{ riskSummary.closed }}</strong>
                        <small>当前页</small>
                    </div>
                </section>

                <section class="panel task-risk-queue">
                    <div class="panel-header task-risk-queue__header">
                        <div>
                            <span class="panel-title">待处置风险</span>
                            <small class="task-risk-queue__hint">按等级优先处理当前页风险</small>
                        </div>
                        <el-tag type="warning" effect="plain" size="small">{{ riskPagination.total }} 条</el-tag>
                    </div>
                <!-- 数据表格 -->
                <el-table class="task-risk-table" :data="risks" max-height="560" size="small">
                    <el-table-column label="等级" width="90">
                        <template #default="{ row }">
                            <el-tag :type="riskLevelTag(row.level)" effect="dark" size="small">{{ riskLevelLabel(row.level) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="projectNo" label="项目" width="170" />
                    <el-table-column prop="score" label="分值" width="80" />
                    <el-table-column label="风险原因" min-width="220">
                        <template #default="{ row }">
                            <span class="task-risk-table__detail">{{ (row.reasons || []).join('；') }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="处置建议" min-width="240">
                        <template #default="{ row }">
                            <span class="task-risk-table__detail">{{ row.suggestion }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'CLOSED' ? 'success' : 'warning'" effect="plain" size="small">{{ riskStatusLabel(row.status) }}</el-tag></template></el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="riskPagination.pageNo"
                    :page-size="riskPagination.pageSize"
                    :total="riskPagination.total"
                    :loading="riskPagination.loading"
                    @update:page-no="riskPagination.goTo"
                    @update:page-size="riskPagination.changePageSize"
                />
                </section>
            </div>

            <aside v-if="canReportException" class="panel task-exception-panel task-risk-report">
                <div class="panel-header">
                    <div>
                        <span class="panel-title">上报异常</span>
                        <small class="task-risk-report__hint">提交后自动生成风险记录</small>
                    </div>
                </div>
                <el-form class="panel-body task-exception-form" label-position="top" @submit.prevent>
                    <el-form-item label="项目" required>
                        <el-select
                            v-model="exceptionForm.projectId"
                            placeholder="选择项目"
                            filterable
                            @change="onProjectChange"
                            >
                            <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="关联任务">
                        <el-select
                            v-model="exceptionForm.taskId"
                            placeholder="不关联具体任务"
                            clearable
                            filterable
                            :loading="tasksLoading"
                            :disabled="!exceptionForm.projectId"
                            >
                            <el-option v-for="task in projectTasks" :key="task.id" :label="`${task.taskNo}  ${task.title}`" :value="task.id" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="异常类型" required>
                        <el-select v-model="exceptionForm.exceptionType">
                            <el-option v-for="item in exceptionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="异常说明" required>
                        <el-input v-model="exceptionForm.summary" type="textarea" :rows="4" maxlength="500" show-word-limit />
                    </el-form-item>
                    <el-form-item label="上报人">
                        <el-input v-model="exceptionForm.reporterName" maxlength="64" />
                    </el-form-item>
                    <el-button type="danger" native-type="submit" @click="reportException">上报异常并生成风险</el-button>
                </el-form>
            </aside>
        </section>
    </div>
</template>
