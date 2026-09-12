<script setup lang="ts">
// 任务执行、反馈与异常上报状态。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import type { Project, ProjectMember, Task } from '@/types';
import { assigneeResponsibilities, productionGateRules, taskStatusLabel, taskStatusOptions, taskStatusTag, taskTypeLabel, taskTypeOptions, taskTypeTag } from './shared';

interface RuleBlockError {
    ruleCode?: string;
    reason?: string;
    currentValue?: string;
    expectedValue?: string;
    action?: string;
}

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const projects = ref<Project[]>([]);
const projectMembers = ref<ProjectMember[]>([]);
const tasks = ref<Task[]>([]);
const loading = ref(false);
const membersLoading = ref(false);
const activeGroup = ref<number>();
const taskFilter = ref('');
const typeFilter = ref('');
const blockError = ref<RuleBlockError | null>(null);
const executionForm = reactive({
    projectId: undefined as number | undefined,
    taskType: 'PURCHASE',
    title: '',
    assigneeId: undefined as number | undefined,
    planStart: '',
    planFinish: ''
});

const feedbackForm = reactive({ result: 'DONE', notes: '' });
const taskPagination = usePagination<Task>();
taskPagination.configure(
    (params) => api.tasks({
        projectId: activeGroup.value,
        status: taskFilter.value || undefined,
        taskType: typeFilter.value || undefined,
        ...params
    }),
    (result) => {
        tasks.value = result.list;
    }
);
const eligibleProjectMembers = computed(() => {
    const expected = assigneeResponsibilities[executionForm.taskType] || ['PROJECT_MANAGER'];
    return projectMembers.value.filter((member) => {
        const responsibilities = member.responsibilityCodes?.length ? member.responsibilityCodes : [member.projectRole];
        return responsibilities.some((code) => expected.includes(code));
    });
});

const productionTaskCount = computed(() => tasks.value.filter((task) => task.taskType === 'PRODUCTION').length);
const executionWorkflow = computed(() => {
    const currentIndex = executionForm.projectId ? 1 : tasks.value.length ? 2 : 0;
    const steps = [
        { key: 'project', label: '选择项目', hint: '引用技术版本' },
        { key: 'task', label: '创建任务', hint: '指定责任人' },
        { key: 'execute', label: '任务执行', hint: '反馈真实进度' },
        { key: 'delivery', label: '交付', hint: '完成交付预检' }
    ];
    return steps.map((step, index) => ({
        ...step,
        state: index < currentIndex ? 'done' as const : index === currentIndex ? 'current' as const : 'pending' as const
    }));
});
watch(() => executionForm.taskType, () => {
    if (executionForm.assigneeId && !eligibleProjectMembers.value.some((member) => member.userId === executionForm.assigneeId)) {
        executionForm.assigneeId = undefined;
    }
});

function memberResponsibilitiesLabel(member: ProjectMember): string {
    return (member.responsibilityCodes?.length ? member.responsibilityCodes : [member.projectRole]).join(' / ');
}

function canOperateTask(row: Task): boolean {
    return auth.user?.roles?.includes('admin') === true || auth.user?.userId === row.assigneeId;
}

function clearBlockError() {
    blockError.value = null;
}

function setBlockError(error: any, fallback: string) {
    blockError.value = error?.detail ?? error?.data ?? { reason: userFacingError(error, fallback) };
}

async function fetchTasks() {
    await taskPagination.reload();
}

async function refreshTasks() {
    try {
        await fetchTasks();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '任务列表刷新失败'));
    }
}

async function loadInitialData() {
    loading.value = true;
    try {
        const [, projectData] = await Promise.all([
            taskPagination.reload(),
            auth.can('task:plan') ? api.projects() : Promise.resolve(undefined)
        ]);
        projects.value = projectData?.list || [];
        const queryProjectId = Number(Array.isArray(route.query.projectId) ? route.query.projectId[0] : route.query.projectId);
        if (Number.isInteger(queryProjectId) && queryProjectId > 0 && projects.value.some((project) => project.id === queryProjectId)) {
            activeGroup.value = queryProjectId;
            if (auth.can('task:plan')) {
                await loadProjectMembers(queryProjectId);
            }
        }
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '任务数据加载失败'));
    }
    finally {
        loading.value = false;
    }
}

async function loadProjectMembers(projectId?: number) {
    executionForm.projectId = projectId;
    executionForm.assigneeId = undefined;
    projectMembers.value = [];
    if (!projectId || !auth.can('task:plan')) {
        return;
    }
    membersLoading.value = true;
    try {
        projectMembers.value = (await api.members(projectId, { pageSize: 100 })).list || [];
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目成员加载失败'));
    }
    finally {
        membersLoading.value = false;
    }
}

watch([activeGroup, taskFilter, typeFilter], () => {
    void taskPagination.reset().catch((error) => ElMessage.error(userFacingError(error, '任务列表加载失败')));
});

async function createExecutionTask() {
    if (!executionForm.projectId) {
        ElMessage.warning('请选择项目');
        return;
    }
    if (!executionForm.title.trim()) {
        ElMessage.warning('请填写任务标题');
        return;
    }
    if (!eligibleProjectMembers.value.length) {
        ElMessage.warning('当前项目没有可承担此类任务的有效成员，请先配置项目成员职责');
        return;
    }
    if (!executionForm.assigneeId) {
        ElMessage.warning('请选择项目内任务责任人');
        return;
    }
    try {
        await api.createExecutionTask({ ...executionForm, title: executionForm.title.trim() });
        ElMessage.success('执行任务已生成');
        executionForm.title = '';
        await refreshTasks();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '任务生成失败'));
    }
}

async function startTask(row: Task) {
    clearBlockError();
    try {
        await api.startTask(row.id, { version: row.version });
        ElMessage.success(`任务 ${row.taskNo} 已开工`);
        await refreshTasks();
    }
    catch (error: any) {
        setBlockError(error, '任务开工被阻断');
    }
}

async function pauseTask(row: Task) {
    try {
        await ElMessageBox.confirm('确认暂停此任务？', '暂停任务', { type: 'warning' });
        await api.pauseTask(row.id, { version: row.version });
        ElMessage.success(`任务 ${row.taskNo} 已暂停`);
        await refreshTasks();
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '任务暂停失败'));
        }
    }
}

async function submitFeedback(row: Task, result: 'DONE' | 'BLOCKED') {
    feedbackForm.result = result;
    try {
        await api.feedbackTask(row.id, { result, notes: feedbackForm.notes, version: row.version });
        ElMessage.success(result === 'DONE' ? '任务已完成' : '任务已标记为阻断');
        feedbackForm.notes = '';
        clearBlockError();
        await refreshTasks();
    }
    catch (error: any) {
        setBlockError(error, '反馈提交失败');
    }
}

function reportException(row: Task) {
    void router.push({
        name: 'tasks-risks',
        query: { projectId: String(row.projectId), taskId: String(row.id) }
    });
}

onMounted(() => {
    void loadInitialData();
});
</script>

<template>
    <!-- 任务查询、执行列表与反馈操作 -->
    <div class="module-page" v-loading="loading">
        <WorkflowStrip
            :steps="executionWorkflow"
            aria-label="任务执行流程"
        />
        <section v-if="auth.can('task:plan')" class="panel">
            <div class="panel-header">
                <span class="panel-title">任务排程</span>
            </div>
            <div class="panel-body flow-form-grid task-create-form">
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">项目 <em>*</em></span>
                    <el-select
                        v-model="executionForm.projectId"
                        placeholder="选择项目"
                        filterable
                        @change="loadProjectMembers"
                    >
                        <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
                    </el-select>
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">任务类型 <em>*</em></span>
                    <el-select v-model="executionForm.taskType">
                        <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </label>
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">任务标题 <em>*</em></span>
                    <el-input v-model="executionForm.title" placeholder="例如：完成首件加工" maxlength="200" />
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">技术版本</span>
                    <el-input class="task-control-version" model-value="当前已发布版本（自动引用）" disabled />
                </label>
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">任务责任人 <em>*</em></span>
                    <el-select
                        v-model="executionForm.assigneeId"
                        placeholder="选择项目成员"
                        filterable
                        :loading="membersLoading"
                        no-data-text="无匹配岗位的项目成员"
                    >
                        <el-option
                            v-for="member in eligibleProjectMembers"
                            :key="member.id"
                            :label="`${member.memberName}（${memberResponsibilitiesLabel(member)}）`"
                            :value="member.userId"
                        />
                    </el-select>
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">计划开始</span>
                    <el-date-picker v-model="executionForm.planStart" format="YYYY年MM月DD日" value-format="YYYY-MM-DD" placeholder="选择日期" />
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">计划完成</span>
                    <el-date-picker v-model="executionForm.planFinish" format="YYYY年MM月DD日" value-format="YYYY-MM-DD" placeholder="选择日期" />
                </label>
                <div class="flow-form-actions">
                    <el-button type="primary" @click="createExecutionTask">生成任务</el-button>
                </div>
            </div>
        </section>

        <el-alert
            v-if="blockError"
            class="task-block-alert"
            :title="blockError.ruleCode ? `${blockError.ruleCode}: ${blockError.reason}` : blockError.reason"
            type="error"
            show-icon
            :closable="false"
            >
            <template #default>
                <div class="status-line task-block-details">
                    <span v-if="blockError.currentValue">当前值：<el-tag size="small" type="danger">{{ blockError.currentValue }}</el-tag></span>
                    <span v-if="blockError.expectedValue">期望值：<el-tag size="small" type="success">{{ blockError.expectedValue }}</el-tag></span>
                    <span v-if="blockError.action">建议：<strong>{{ blockError.action }}</strong></span>
                    <el-button type="primary" link size="small" @click="clearBlockError">关闭</el-button>
                </div>
            </template>
        </el-alert>

        <section class="panel">
            <div class="panel-header task-panel-header">
                <span class="panel-title">任务执行与版本阻断（{{ taskPagination.total }}）</span>
                <div class="task-filter-bar">
                    <el-select v-model="activeGroup" class="task-filter-project" placeholder="按项目筛选" clearable filterable size="small">
                        <el-option v-for="project in projects" :key="project.id" :label="project.projectNo" :value="project.id" />
                    </el-select>
                    <el-select v-model="taskFilter" class="task-filter-status" placeholder="状态" clearable size="small">
                        <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                    <el-select v-model="typeFilter" class="task-filter-type" placeholder="类型" clearable size="small">
                        <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.shortLabel" :value="item.value" />
                    </el-select>
                </div>
            </div>
            <!-- 数据表格 -->
            <el-table :data="tasks" height="520" size="small">
                <el-table-column prop="taskNo" label="任务号" width="155" />
                <el-table-column prop="projectNo" label="项目" width="150" />
                <el-table-column label="类型" width="80">
                    <template #default="{ row }">
                        <el-tag :type="taskTypeTag(row.taskType)" effect="plain" size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="title" label="任务" min-width="180" show-overflow-tooltip />
                <el-table-column prop="referencedVersion" label="版本" width="80" />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="taskStatusTag(row.status)" effect="plain" size="small">{{ taskStatusLabel(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="responsibleName" label="责任人" width="100" />
                <el-table-column prop="blockReason" label="阻断原因" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span v-if="row.blockReason" class="task-block-reason">{{ row.blockReason }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="270" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="auth.can('task:execute') && canOperateTask(row) && ['TODO', 'BLOCKED', 'PAUSED'].includes(row.status)" link type="primary" size="small" @click.stop="startTask(row)">开工</el-button>
                        <el-button v-if="auth.can('task:execute') && canOperateTask(row) && row.status === 'IN_PROGRESS'" link type="warning" size="small" @click.stop="pauseTask(row)">暂停</el-button>
                        <el-button v-if="auth.can('task:feedback') && canOperateTask(row) && ['IN_PROGRESS', 'BLOCKED'].includes(row.status)" link type="success" size="small" @click.stop="submitFeedback(row, 'DONE')">反馈完成</el-button>
                        <el-button v-if="auth.can('task:feedback') && canOperateTask(row) && row.status === 'IN_PROGRESS'" link type="danger" size="small" @click.stop="submitFeedback(row, 'BLOCKED')">反馈异常</el-button>
                        <el-button v-if="auth.can('task:feedback')" link type="danger" size="small" @click.stop="reportException(row)">上报异常</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="taskPagination.pageNo"
                :page-size="taskPagination.pageSize"
                :total="taskPagination.total"
                :loading="taskPagination.loading"
                @update:page-no="taskPagination.goTo"
                @update:page-size="taskPagination.changePageSize"
            />
        </section>

        <section v-if="productionTaskCount" class="panel">
            <div class="panel-header">
                <span class="panel-title">投产校验规则</span>
                <span class="subtle">适用于 {{ productionTaskCount }} 个生产任务</span>
            </div>
            <div class="panel-body task-rule-list">
                <article v-for="rule in productionGateRules" :key="rule.code" class="task-rule-item">
                    <el-tag effect="plain" size="small">{{ rule.code }}</el-tag>
                    <strong>{{ rule.label }}</strong>
                    <span>{{ rule.action }}</span>
                </article>
            </div>
        </section>
    </div>
</template>
