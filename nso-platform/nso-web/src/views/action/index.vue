<script setup lang="ts">
// 行动中心、审批与异常处置状态。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import { useAuthStore } from '@/stores/auth';
import { formatChineseDateTime } from '@/utils/date';
import { getRuleBlockDetail, userFacingError } from '@/utils/request';
import { openProtectedFile } from '@/utils/file-access';
import type { ActionCenterSummary, ActionItem, ApprovalTemplate, ApprovalTodo, CapaAction, CapaCase, Project, ProjectMember } from '@/types';

type CenterTab = 'actions' | 'approvals' | 'exceptions' | 'templates';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const activeTab = ref<CenterTab>('actions');
const loading = ref(false);
const summary = ref<ActionCenterSummary>();
const actionRows = ref<ActionItem[]>([]);
const approvalRows = ref<ApprovalTodo[]>([]);
const exceptionRows = ref<CapaCase[]>([]);
const templateRows = ref<ApprovalTemplate[]>([]);
const projects = ref<Project[]>([]);
const projectMembers = ref<ProjectMember[]>([]);
const selectedException = ref<CapaCase>();
const selectedExceptionActions = ref<CapaAction[]>([]);
const exceptionDrawerVisible = ref(false);
const exceptionDetailLoading = ref(false);
const createExceptionVisible = ref(false);
const templateDialogVisible = ref(false);
const saving = ref(false);

const actionFilters = reactive({ sourceType: '', priority: '', dueState: '', projectId: undefined as number | undefined });
const approvalBusinessType = ref('');
const exceptionStatus = ref('');
const templateBusinessType = ref('');
const exceptionForm = reactive({
    projectId: undefined as number | undefined,
    taskId: undefined as number | undefined,
    exceptionType: 'EXECUTION_EXCEPTION',
    summary: '',
    reporterName: '',
    ownerUserId: undefined as number | undefined,
    dueAt: localDateTime(1)
});
const transitionForm = reactive({
    comment: '',
    rootCause: '',
    correctivePlan: '',
    verificationSummary: '',
    closeConclusion: '',
    evidenceRef: '',
    evidenceSummary: ''
});
const evidenceForm = reactive({ evidenceType: 'FILE', evidenceRef: '', summary: '' });

async function openEvidence(row: { evidenceRef: string }) {
    const match = /^FILE:(\d+)$/.exec(row.evidenceRef || '');
    if (!match) {
        ElMessage.warning('该证据是文字引用，暂无文件内容');
        return;
    }
    try {
        await openProtectedFile(api.fileInlineUrl(Number(match[1])), { preview: true });
    } catch (error) {
        ElMessage.error(userFacingError(error, '证据文件查看失败'));
    }
}
const correctiveTaskForm = reactive({
    title: '',
    assigneeUserId: undefined as number | undefined,
    planStart: localDate(),
    planFinish: localDate(1),
    actionPlan: ''
});
const templateForm = reactive({
    templateCode: '',
    templateName: '',
    businessType: 'CHANGE',
    approvalMode: 'SERIAL',
    slaMinutes: 1440,
    nodes: [{ nodeCode: 'PM', nodeName: '项目经理审批', responsibilityCode: 'PROJECT_MANAGER', slaMinutes: 1440, escalationRole: 'PROJECT_MANAGER' }]
});

const canViewApprovals = computed(() => auth.can('approval:view') || auth.can('approval:decide'));
const canDecideApprovals = computed(() => auth.can('approval:decide'));
const canManageExceptions = computed(() => auth.can('exception:manage') || auth.can('task:feedback'));
const canManageTemplates = computed(() => auth.can('approval:template:manage'));
const metricCards = computed(() => {
    const metrics = summary.value?.metrics || {};
    return [
        { key: 'openCount', label: '待处理', tone: 'primary' },
        { key: 'overdueCount', label: '已超期', tone: 'danger' },
        { key: 'criticalCount', label: '关键阻塞', tone: 'danger' },
        { key: 'approvalCount', label: '待审批', tone: 'warning' },
        { key: 'exceptionCount', label: '异常闭环', tone: 'warning' }
    ].map((item) => ({ ...item, value: Number(metrics[item.key] || 0) }));
});
const currentExceptionMembers = computed(() => projectMembers.value.filter((member) => member.status === 'ACTIVE'));
const nextTransition = computed(() => {
    const status = selectedException.value?.status;
    return ({ OPEN: 'CONTAINED', CONTAINED: 'ANALYZING', ANALYZING: 'ACTIONING', ACTIONING: 'VERIFYING', VERIFYING: 'CLOSED' } as Record<string, string>)[status || ''];
});
const selectedExceptionOverdue = computed(() => Boolean(selectedException.value?.dueAt && new Date(selectedException.value.dueAt).getTime() < Date.now() && selectedException.value.status !== 'CLOSED'));

const actionPagination = usePagination<ActionItem>({ queryPrefix: 'action' });
const approvalPagination = usePagination<ApprovalTodo>({ queryPrefix: 'approval' });
const exceptionPagination = usePagination<CapaCase>({ queryPrefix: 'exception' });
const templatePagination = usePagination<ApprovalTemplate>({ queryPrefix: 'template' });

actionPagination.configure(
    (params) => api.actions({ sourceType: actionFilters.sourceType || undefined, priority: actionFilters.priority || undefined,
        dueState: actionFilters.dueState || undefined, projectId: actionFilters.projectId, ...params }),
    (result) => { actionRows.value = result.list; }
);
approvalPagination.configure(
    (params) => api.approvalTodos({ businessType: approvalBusinessType.value || undefined, ...params }),
    (result) => { approvalRows.value = result.list; }
);
exceptionPagination.configure(
    (params) => api.exceptions({ status: exceptionStatus.value || undefined, ...params }),
    (result) => { exceptionRows.value = result.list; }
);
templatePagination.configure(
    (params) => api.approvalTemplates({ businessType: templateBusinessType.value || undefined, ...params }),
    (result) => { templateRows.value = result.list; }
);

function localDate(days = 0) {
    const value = new Date();
    value.setDate(value.getDate() + days);
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
}

function localDateTime(days = 0) {
    const value = new Date();
    value.setDate(value.getDate() + days);
    value.setSeconds(0, 0);
    return `${localDatePart(value)}T${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}:00`;
}

function localDatePart(value: Date) {
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
}

function idempotencyKey(prefix: string) {
    const suffix = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    return `${prefix}-${suffix}`;
}

function queryText(value: unknown) {
    return Array.isArray(value) ? value[0] : typeof value === 'string' ? value : undefined;
}

function isCenterTab(value?: string): value is CenterTab {
    return value === 'actions' || value === 'approvals' || value === 'exceptions' || value === 'templates';
}

function priorityLabel(value?: string) {
    return ({ CRITICAL: '关键', HIGH: '高', NORMAL: '常规', LOW: '低' } as Record<string, string>)[value || ''] || value || '-';
}

function priorityTag(value?: string) {
    return ({ CRITICAL: 'danger', HIGH: 'warning', NORMAL: 'primary', LOW: 'info' } as Record<string, string>)[value || ''] || 'info';
}

function sourceLabel(value?: string) {
    return ({ TASK: '任务', RISK_ACTION: '风险处置', APPROVAL_TASK: '审批', CAPA_CASE: '异常 CAPA', MESSAGE: '消息' } as Record<string, string>)[value || ''] || value || '-';
}

function actionStatusLabel(item: ActionItem) {
    if (item.overdue) return '已超期';
    return ({ TODO: '待处理', BLOCKED: '已阻塞', IN_PROGRESS: '处理中', OPEN: '待处理' } as Record<string, string>)[item.sourceStatus || item.actionStatus || ''] || '待处理';
}

function businessLabel(value?: string) {
    return ({ CHANGE: '设计变更', SPECIAL_RELEASE: '特放', EXCEPTION: '异常关闭' } as Record<string, string>)[value || ''] || value || '-';
}

function capaStatusLabel(value?: string) {
    return ({ OPEN: '已登记', CONTAINED: '已遏制', ANALYZING: '根因分析', ACTIONING: '整改执行', VERIFYING: '验证中', CLOSED: '已关闭' } as Record<string, string>)[value || ''] || value || '-';
}

function approvalStatusTag(todo: ApprovalTodo) {
    return todo.overdue ? 'danger' : todo.decision === 'PENDING' ? 'warning' : todo.decision === 'APPROVED' ? 'success' : 'info';
}

function dateText(value?: string) {
    return value ? formatChineseDateTime(value) : '-';
}

async function loadSummary() {
    summary.value = await api.actionSummary();
}

async function loadActiveTab() {
    if (activeTab.value === 'actions') {
        if (auth.can('action:manage')) {
            await api.refreshActions();
        }
        await Promise.all([loadSummary(), actionPagination.reload()]);
        return;
    }
    if (activeTab.value === 'approvals') {
        await Promise.all([loadSummary(), canViewApprovals.value ? approvalPagination.reload() : Promise.resolve()]);
        return;
    }
    if (activeTab.value === 'exceptions') {
        await Promise.all([loadSummary(), exceptionPagination.reload()]);
        return;
    }
    if (canManageTemplates.value) {
        await templatePagination.reload();
    }
}

async function refreshCenter() {
    loading.value = true;
    try {
        if (auth.can('action:manage')) {
            await api.refreshActions();
        }
        await loadActiveTab();
        ElMessage.success('行动中心已刷新');
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '行动中心刷新失败'));
    }
    finally {
        loading.value = false;
    }
}

async function setTab(value: string) {
    const next: CenterTab = isCenterTab(value) ? value : 'actions';
    activeTab.value = next;
    if (queryText(route.query.tab) !== next) {
        await router.replace({ query: { ...route.query, tab: next } });
        return;
    }
    await loadActiveTab();
}

async function reloadActions() {
    try {
        await actionPagination.reset();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '行动项加载失败'));
    }
}

async function reloadApprovals() {
    try {
        await approvalPagination.reset();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '审批待办加载失败'));
    }
}

async function reloadExceptions() {
    try {
        await exceptionPagination.reset();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '异常案例加载失败'));
    }
}

function openAction(item: ActionItem) {
    if (item.sourceType === 'APPROVAL_TASK') {
        void setTab('approvals');
        return;
    }
    if (item.sourceType === 'CAPA_CASE') {
        void setTab('exceptions').then(() => openException(item.sourceId));
        return;
    }
    void router.push(item.route || '/dashboard');
}

async function decideApproval(todo: ApprovalTodo, decision: 'APPROVED' | 'REJECTED') {
    let opinion = '';
    try {
        const result = await ElMessageBox.prompt(
            decision === 'REJECTED' ? '请填写驳回原因' : '可填写审批意见',
            decision === 'REJECTED' ? '驳回审批' : '通过审批',
            { inputType: 'textarea', inputPlaceholder: decision === 'REJECTED' ? '驳回原因' : '审批意见', inputValidator: (value) => decision !== 'REJECTED' || Boolean(value?.trim()) || '请填写驳回原因' }
        );
        opinion = result.value?.trim() || '';
    }
    catch {
        return;
    }
    saving.value = true;
    try {
        await api.decideApproval(todo.id, { decision, opinion: opinion || undefined, version: todo.version, idempotencyKey: idempotencyKey('approval') });
        ElMessage.success(decision === 'APPROVED' ? '审批已通过' : '审批已驳回');
        await Promise.all([reloadApprovals(), loadSummary(), actionPagination.reload()]);
        if (selectedException.value?.id === todo.businessId) {
            await openException(todo.businessId);
        }
    }
    catch (error) {
        if (getRuleBlockDetail(error)?.ruleCode === 'OPTIMISTIC_LOCK') {
            await reloadApprovals();
            ElMessage.warning('数据已更新，请刷新审批列表后重试');
            return;
        }
        ElMessage.error(userFacingError(error, '审批处理失败'));
    }
    finally {
        saving.value = false;
    }
}

async function loadProjects() {
    if (projects.value.length) {
        return;
    }
    try {
        projects.value = (await api.projects({ pageSize: 100 })).list;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目列表加载失败'));
    }
}

async function loadProjectMembers(projectId?: number) {
    projectMembers.value = [];
    if (!projectId) {
        return;
    }
    try {
        projectMembers.value = (await api.members(projectId, { pageSize: 100 })).list;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目成员加载失败'));
    }
}

async function openCreateException() {
    await loadProjects();
    resetExceptionForm();
    createExceptionVisible.value = true;
}

function resetExceptionForm() {
    exceptionForm.projectId = undefined;
    exceptionForm.taskId = undefined;
    exceptionForm.exceptionType = 'EXECUTION_EXCEPTION';
    exceptionForm.summary = '';
    exceptionForm.reporterName = '';
    exceptionForm.ownerUserId = undefined;
    exceptionForm.dueAt = localDateTime(1);
    projectMembers.value = [];
}

async function createException() {
    const projectId = exceptionForm.projectId;
    const ownerUserId = exceptionForm.ownerUserId;
    if (!projectId || !ownerUserId || !exceptionForm.summary.trim() || !exceptionForm.dueAt) {
        ElMessage.warning('请填写项目、异常说明、责任人和处置时限');
        return;
    }
    saving.value = true;
    try {
        const created = await api.createException({
            projectId,
            taskId: exceptionForm.taskId,
            exceptionType: exceptionForm.exceptionType,
            summary: exceptionForm.summary.trim(),
            reporterName: exceptionForm.reporterName.trim() || undefined,
            ownerUserId,
            dueAt: exceptionForm.dueAt,
            idempotencyKey: idempotencyKey('capa')
        });
        createExceptionVisible.value = false;
        ElMessage.success('异常 CAPA 已创建');
        await Promise.all([reloadExceptions(), loadSummary(), actionPagination.reload()]);
        await openException(created.id);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '创建异常 CAPA 失败'));
    }
    finally {
        saving.value = false;
    }
}

async function openException(id: number) {
    exceptionDrawerVisible.value = true;
    exceptionDetailLoading.value = true;
    try {
        const item = await api.exception(id);
        selectedException.value = item;
        await loadProjectMembers(item.projectId);
        selectedExceptionActions.value = await api.exceptionActions(id);
        resetTransitionForm();
        resetEvidenceForm();
        resetCorrectiveTaskForm();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '异常案例详情加载失败'));
        exceptionDrawerVisible.value = false;
    }
    finally {
        exceptionDetailLoading.value = false;
    }
}

function resetTransitionForm() {
    transitionForm.comment = '';
    transitionForm.rootCause = '';
    transitionForm.correctivePlan = '';
    transitionForm.verificationSummary = '';
    transitionForm.closeConclusion = '';
    transitionForm.evidenceRef = '';
    transitionForm.evidenceSummary = '';
}

function resetEvidenceForm() {
    evidenceForm.evidenceType = 'FILE';
    evidenceForm.evidenceRef = '';
    evidenceForm.summary = '';
}

function resetCorrectiveTaskForm() {
    correctiveTaskForm.title = '';
    correctiveTaskForm.assigneeUserId = undefined;
    correctiveTaskForm.planStart = localDate();
    correctiveTaskForm.planFinish = localDate(1);
    correctiveTaskForm.actionPlan = '';
}

async function uploadEvidence(options: any) {
    if (!selectedException.value) {
        options.onError?.(new Error('请先选择异常案例'));
        return;
    }
    try {
        const uploaded = await api.uploadDocumentFile(selectedException.value.projectId, options.file);
        evidenceForm.evidenceRef = `FILE:${uploaded.id}`;
        options.onSuccess?.(uploaded);
        ElMessage.success('证据文件已上传');
    }
    catch (error) {
        options.onError?.(error);
        ElMessage.error(userFacingError(error, '证据文件上传失败'));
    }
}

async function addEvidence() {
    if (!selectedException.value || !evidenceForm.evidenceRef.trim()) {
        ElMessage.warning('请上传文件或填写证据引用');
        return;
    }
    saving.value = true;
    try {
        await api.addExceptionEvidence(selectedException.value.id, {
            evidenceType: evidenceForm.evidenceType,
            evidenceRef: evidenceForm.evidenceRef.trim(),
            summary: evidenceForm.summary.trim() || undefined
        });
        ElMessage.success('证据已添加');
        await openException(selectedException.value.id);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '添加证据失败'));
    }
    finally {
        saving.value = false;
    }
}

async function createCorrectiveTask() {
    if (!selectedException.value || !correctiveTaskForm.title.trim() || !correctiveTaskForm.assigneeUserId || !correctiveTaskForm.planFinish || !correctiveTaskForm.actionPlan.trim()) {
        ElMessage.warning('请填写整改任务、责任人、计划完成日期和整改措施');
        return;
    }
    saving.value = true;
    try {
        await api.createExceptionAction(selectedException.value.id, {
            title: correctiveTaskForm.title.trim(),
            taskType: 'CAPA_ACTION',
            assigneeUserId: correctiveTaskForm.assigneeUserId,
            planStart: correctiveTaskForm.planStart,
            planFinish: correctiveTaskForm.planFinish,
            actionPlan: correctiveTaskForm.actionPlan.trim(),
            idempotencyKey: idempotencyKey('capa-task')
        });
        ElMessage.success('整改任务已创建');
        await openException(selectedException.value.id);
        await Promise.all([loadSummary(), actionPagination.reload()]);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '创建整改任务失败'));
    }
    finally {
        saving.value = false;
    }
}

async function transitionException() {
    const item = selectedException.value;
    const target = nextTransition.value;
    if (!item || !target) {
        return;
    }
    if (target === 'CONTAINED' && !transitionForm.comment.trim()) {
        ElMessage.warning('请填写临时遏制措施');
        return;
    }
    if (target === 'ACTIONING' && (!transitionForm.rootCause.trim() || !transitionForm.correctivePlan.trim())) {
        ElMessage.warning('请填写根因和整改计划');
        return;
    }
    if (target === 'VERIFYING' && !transitionForm.verificationSummary.trim()) {
        ElMessage.warning('请填写验证说明');
        return;
    }
    if (target === 'CLOSED' && !transitionForm.closeConclusion.trim()) {
        ElMessage.warning('请填写关闭结论');
        return;
    }
    saving.value = true;
    try {
        await api.transitionException(item.id, {
            toStatus: target,
            comment: transitionForm.comment.trim() || undefined,
            rootCause: transitionForm.rootCause.trim() || undefined,
            correctivePlan: transitionForm.correctivePlan.trim() || undefined,
            verificationSummary: transitionForm.verificationSummary.trim() || undefined,
            closeConclusion: transitionForm.closeConclusion.trim() || undefined,
            evidenceRef: transitionForm.evidenceRef.trim() || undefined,
            evidenceSummary: transitionForm.evidenceSummary.trim() || undefined,
            version: item.version,
            idempotencyKey: idempotencyKey('capa-transition')
        });
        ElMessage.success(`CAPA 已进入${capaStatusLabel(target)}`);
        await Promise.all([openException(item.id), reloadExceptions(), loadSummary(), actionPagination.reload(), canViewApprovals.value ? approvalPagination.reload() : Promise.resolve()]);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, 'CAPA 状态推进失败'));
    }
    finally {
        saving.value = false;
    }
}

async function openTemplateDialog() {
    templateForm.templateCode = '';
    templateForm.templateName = '';
    templateForm.businessType = 'CHANGE';
    templateForm.approvalMode = 'SERIAL';
    templateForm.slaMinutes = 1440;
    templateForm.nodes = [{ nodeCode: 'PM', nodeName: '项目经理审批', responsibilityCode: 'PROJECT_MANAGER', slaMinutes: 1440, escalationRole: 'PROJECT_MANAGER' }];
    templateDialogVisible.value = true;
}

function addTemplateNode() {
    templateForm.nodes.push({ nodeCode: '', nodeName: '', responsibilityCode: '', slaMinutes: templateForm.slaMinutes, escalationRole: 'PROJECT_MANAGER' });
}

function removeTemplateNode(index: number) {
    if (templateForm.nodes.length > 1) {
        templateForm.nodes.splice(index, 1);
    }
}

async function createTemplate() {
    if (!templateForm.templateCode.trim() || !templateForm.templateName.trim() || templateForm.nodes.some((node) => !node.nodeCode.trim() || !node.nodeName.trim() || !node.responsibilityCode.trim())) {
        ElMessage.warning('请填写模板编码、名称和所有审批节点');
        return;
    }
    saving.value = true;
    try {
        await api.createApprovalTemplate({
            templateCode: templateForm.templateCode.trim().toUpperCase(),
            templateName: templateForm.templateName.trim(),
            businessType: templateForm.businessType,
            approvalMode: templateForm.approvalMode,
            slaMinutes: templateForm.slaMinutes,
            nodes: templateForm.nodes.map((node) => ({
                nodeCode: node.nodeCode.trim().toUpperCase(),
                nodeName: node.nodeName.trim(),
                responsibilityCode: node.responsibilityCode.trim().toUpperCase(),
                slaMinutes: node.slaMinutes,
                escalationRole: node.escalationRole.trim().toUpperCase() || undefined
            }))
        });
        templateDialogVisible.value = false;
        ElMessage.success('审批模板草稿已创建');
        await templatePagination.reset();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '创建审批模板失败'));
    }
    finally {
        saving.value = false;
    }
}

async function publishTemplate(template: ApprovalTemplate) {
    try {
        await ElMessageBox.confirm(`发布后将冻结 ${template.templateName} 的第 ${template.templateVersion} 版，并替换同业务类型的已发布模板。`, '发布审批模板', { type: 'warning' });
        await api.publishApprovalTemplate(template.id);
        ElMessage.success('审批模板已发布');
        await templatePagination.reload();
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '发布审批模板失败'));
        }
    }
}

watch(() => route.query.tab, (value) => {
    const next = queryText(value);
    if (isCenterTab(next) && next !== activeTab.value) {
        activeTab.value = next;
        void loadActiveTab().catch((error) => ElMessage.error(userFacingError(error, '行动中心加载失败')));
    }
});

onMounted(() => {
    const projectId = Number(queryText(route.query.projectId));
    actionFilters.projectId = Number.isInteger(projectId) && projectId > 0 ? projectId : undefined;
    const routeTab = queryText(route.query.tab);
    activeTab.value = isCenterTab(routeTab) ? routeTab : 'actions';
    void loadActiveTab().catch((error) => ElMessage.error(userFacingError(error, '行动中心加载失败')));
    if (activeTab.value === 'exceptions') {
        const exceptionId = Number(queryText(route.query.exceptionId));
        if (Number.isSafeInteger(exceptionId) && exceptionId > 0) {
            void openException(exceptionId);
        }
    }
});
</script>

<template>
    <!-- 行动待办、异常与审批模板 -->
    <div class="module-page action-center" v-loading="loading">
        <header class="module-page__header action-center__header">
            <div>
                <h1>我的行动</h1>
                <p>按风险、时限和计划完成时间聚合待推进事项。</p>
            </div>
            <div class="toolbar-right">
                <el-button v-if="canManageExceptions" type="primary" :icon="'CirclePlus'" @click="openCreateException">登记异常</el-button>
                <el-button :icon="'Refresh'" @click="refreshCenter">刷新</el-button>
            </div>
        </header>

        <section class="action-metrics" aria-label="行动中心摘要">
            <div v-for="metric in metricCards" :key="metric.key" class="action-metric" :class="`action-metric--${metric.tone}`">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
            </div>
        </section>

        <section class="panel action-center__panel">
            <el-tabs v-model="activeTab" class="action-tabs" @tab-change="setTab">
                <el-tab-pane label="行动项" name="actions">
                    <div class="action-toolbar">
                        <div class="action-toolbar__filters">
                            <el-select v-model="actionFilters.sourceType" clearable placeholder="来源类型" style="width: 138px" @change="reloadActions">
                                <el-option label="任务" value="TASK" /><el-option label="风险处置" value="RISK_ACTION" /><el-option label="审批" value="APPROVAL_TASK" /><el-option label="异常 CAPA" value="CAPA_CASE" /><el-option label="消息" value="MESSAGE" />
                            </el-select>
                            <el-select v-model="actionFilters.priority" clearable placeholder="优先级" style="width: 124px" @change="reloadActions">
                                <el-option label="关键" value="CRITICAL" /><el-option label="高" value="HIGH" /><el-option label="常规" value="NORMAL" />
                            </el-select>
                            <el-select v-model="actionFilters.dueState" clearable placeholder="截止状态" style="width: 124px" @change="reloadActions">
                                <el-option label="已超期" value="OVERDUE" /><el-option label="三天内到期" value="DUE_SOON" /><el-option label="未设时限" value="NO_DATE" />
                            </el-select>
                        </div>
                        <span class="subtle">按风险等级、SLA 和计划完成时间排序</span>
                    </div>
                    <!-- 数据表格 -->
                    <el-table :data="actionRows" size="small" class="action-table" :empty-text="actionPagination.loading ? '正在加载' : '暂无待推进事项'">
                        <el-table-column label="优先级" width="90"><template #default="{ row }"><el-tag size="small" :type="priorityTag(row.priority)" effect="plain">{{ priorityLabel(row.priority) }}</el-tag></template></el-table-column>
                        <el-table-column label="事项" min-width="250"><template #default="{ row }"><strong>{{ row.title }}</strong><div v-if="row.summary" class="subtle action-table__summary">{{ row.summary }}</div></template></el-table-column>
                        <el-table-column label="来源" width="112"><template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template></el-table-column>
                        <el-table-column label="项目" width="100"><template #default="{ row }">{{ row.projectId ? `项目 ${row.projectId}` : '平台事项' }}</template></el-table-column>
                        <el-table-column label="责任人" prop="assigneeName" width="120"><template #default="{ row }">{{ row.assigneeName || '-' }}</template></el-table-column>
                        <el-table-column label="SLA" width="172"><template #default="{ row }"><span :class="{ 'action-date--overdue': row.overdue }">{{ dateText(row.slaDueAt) }}</span></template></el-table-column>
                        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag size="small" effect="plain" :type="row.overdue ? 'danger' : 'info'">{{ actionStatusLabel(row) }}</el-tag></template></el-table-column>
                        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openAction(row)">处理</el-button></template></el-table-column>
                    </el-table>
                    <PaginationBar :page-no="actionPagination.pageNo" :page-size="actionPagination.pageSize" :total="actionPagination.total" :loading="actionPagination.loading" @update:page-no="actionPagination.goTo" @update:page-size="actionPagination.changePageSize" />
                </el-tab-pane>

                <el-tab-pane label="审批待办" name="approvals">
                    <el-alert v-if="!canViewApprovals" title="当前账号没有审批待办查看权限" type="info" :closable="false" show-icon />
                    <template v-else>
                        <div class="action-toolbar"><el-select v-model="approvalBusinessType" clearable placeholder="审批类型" style="width: 150px" @change="reloadApprovals"><el-option label="设计变更" value="CHANGE" /><el-option label="特放" value="SPECIAL_RELEASE" /><el-option label="异常关闭" value="EXCEPTION" /></el-select><span class="subtle">审批节点按模板版本冻结，职责调整后实时校验可见权限。</span></div>
                        <el-table :data="approvalRows" size="small" :empty-text="approvalPagination.loading ? '正在加载' : '暂无待审批事项'">
                            <el-table-column label="业务" width="120"><template #default="{ row }">{{ businessLabel(row.businessType) }}</template></el-table-column>
                            <el-table-column prop="nodeName" label="审批节点" min-width="180"><template #default="{ row }"><strong>{{ row.nodeName }}</strong><div class="subtle">{{ row.responsibilityCode }}</div></template></el-table-column>
                            <el-table-column label="项目" width="110"><template #default="{ row }">#{{ row.projectId }}</template></el-table-column>
                            <el-table-column label="时限" width="172"><template #default="{ row }"><span :class="{ 'action-date--overdue': row.overdue }">{{ dateText(row.dueAt) }}</span></template></el-table-column>
                            <el-table-column label="状态" width="96"><template #default="{ row }"><el-tag size="small" :type="approvalStatusTag(row)" effect="plain">{{ row.overdue ? '已超期' : '待审批' }}</el-tag></template></el-table-column>
                            <el-table-column v-if="canDecideApprovals" label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="success" :disabled="saving" @click="decideApproval(row, 'APPROVED')">通过</el-button><el-button link type="danger" :disabled="saving" @click="decideApproval(row, 'REJECTED')">驳回</el-button></template></el-table-column>
                        </el-table>
                        <PaginationBar :page-no="approvalPagination.pageNo" :page-size="approvalPagination.pageSize" :total="approvalPagination.total" :loading="approvalPagination.loading" @update:page-no="approvalPagination.goTo" @update:page-size="approvalPagination.changePageSize" />
                    </template>
                </el-tab-pane>

                <el-tab-pane label="异常 CAPA" name="exceptions">
                    <div class="action-toolbar"><el-select v-model="exceptionStatus" clearable placeholder="闭环状态" style="width: 150px" @change="reloadExceptions"><el-option label="已登记" value="OPEN" /><el-option label="已遏制" value="CONTAINED" /><el-option label="根因分析" value="ANALYZING" /><el-option label="整改执行" value="ACTIONING" /><el-option label="验证中" value="VERIFYING" /><el-option label="已关闭" value="CLOSED" /></el-select><span class="subtle">必须完成证据、整改和关闭审批后才能结案。</span></div>
                    <el-table :data="exceptionRows" size="small" :empty-text="exceptionPagination.loading ? '正在加载' : '暂无异常案例'">
                        <el-table-column prop="caseNo" label="案例编号" width="160" />
                        <el-table-column label="异常说明" min-width="260"><template #default="{ row }"><strong>{{ row.summary }}</strong><div class="subtle">{{ row.exceptionType }}</div></template></el-table-column>
                        <el-table-column prop="ownerName" label="责任人" width="120" />
                        <el-table-column label="处置时限" width="172"><template #default="{ row }"><span :class="{ 'action-date--overdue': row.status !== 'CLOSED' && new Date(row.dueAt).getTime() < Date.now() }">{{ dateText(row.dueAt) }}</span></template></el-table-column>
                        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag size="small" :type="row.status === 'CLOSED' ? 'success' : 'warning'" effect="plain">{{ capaStatusLabel(row.status) }}</el-tag></template></el-table-column>
                        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openException(row.id)">查看</el-button></template></el-table-column>
                    </el-table>
                    <PaginationBar :page-no="exceptionPagination.pageNo" :page-size="exceptionPagination.pageSize" :total="exceptionPagination.total" :loading="exceptionPagination.loading" @update:page-no="exceptionPagination.goTo" @update:page-size="exceptionPagination.changePageSize" />
                </el-tab-pane>

                <el-tab-pane v-if="canManageTemplates" label="审批模板" name="templates">
                    <div class="action-toolbar"><el-select v-model="templateBusinessType" clearable placeholder="业务类型" style="width: 150px" @change="() => templatePagination.reset()"><el-option label="设计变更" value="CHANGE" /><el-option label="特放" value="SPECIAL_RELEASE" /><el-option label="异常关闭" value="EXCEPTION" /></el-select><el-button type="primary" :icon="'CirclePlus'" @click="openTemplateDialog">新建模板</el-button></div>
                    <el-table :data="templateRows" size="small" :empty-text="templatePagination.loading ? '正在加载' : '暂无审批模板'">
                        <el-table-column prop="templateCode" label="模板编码" width="170" />
                        <el-table-column prop="templateName" label="模板名称" min-width="180" />
                        <el-table-column label="业务" width="120"><template #default="{ row }">{{ businessLabel(row.businessType) }}</template></el-table-column>
                        <el-table-column label="方式" width="100"><template #default="{ row }">{{ row.approvalMode === 'COUNTERSIGN' ? '会签' : '串行' }}</template></el-table-column>
                        <el-table-column label="节点" min-width="220"><template #default="{ row }"><el-tag v-for="node in row.nodes" :key="node.nodeCode" size="small" effect="plain" style="margin-right: 4px">{{ node.nodeName }}</el-tag></template></el-table-column>
                        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag></template></el-table-column>
                        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button v-if="row.status !== 'PUBLISHED'" link type="primary" @click="publishTemplate(row)">发布</el-button></template></el-table-column>
                    </el-table>
                    <PaginationBar :page-no="templatePagination.pageNo" :page-size="templatePagination.pageSize" :total="templatePagination.total" :loading="templatePagination.loading" @update:page-no="templatePagination.goTo" @update:page-size="templatePagination.changePageSize" />
                </el-tab-pane>
            </el-tabs>
        </section>

        <!-- 操作弹窗 -->
        <el-dialog v-model="createExceptionVisible" title="登记异常 CAPA" width="min(620px, 94vw)">
            <el-form label-position="top">
                <el-form-item label="项目" required><el-select v-model="exceptionForm.projectId" filterable placeholder="选择项目" style="width: 100%" @change="loadProjectMembers"><el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" /></el-select></el-form-item>
                <el-form-item label="异常类型" required><el-select v-model="exceptionForm.exceptionType" style="width: 100%"><el-option label="现场执行异常" value="EXECUTION_EXCEPTION" /><el-option label="物料短缺" value="MATERIAL_SHORTAGE" /><el-option label="质量异常" value="QUALITY_EXCEPTION" /><el-option label="技术版本异常" value="TECHNICAL_EXCEPTION" /></el-select></el-form-item>
                <el-form-item label="异常说明" required><el-input v-model="exceptionForm.summary" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
                <el-form-item label="责任人" required><el-select v-model="exceptionForm.ownerUserId" filterable placeholder="选择项目成员" style="width: 100%"><el-option v-for="member in currentExceptionMembers" :key="member.userId" :label="`${member.memberName}  ${member.primaryResponsibilityCode || member.projectRole}`" :value="member.userId" /></el-select></el-form-item>
                <el-form-item label="处置时限" required><el-date-picker v-model="exceptionForm.dueAt" type="datetime" format="YYYY年MM月DD日 HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
                <el-form-item label="上报人"><el-input v-model="exceptionForm.reporterName" maxlength="64" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="createExceptionVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="createException">创建 CAPA</el-button></template>
        </el-dialog>

        <el-drawer v-model="exceptionDrawerVisible" title="异常 CAPA 闭环" size="min(760px, 100%)">
            <div v-if="selectedException" v-loading="exceptionDetailLoading" class="capa-drawer">
                <section class="capa-drawer__summary">
                    <div><div class="subtle">{{ selectedException.caseNo }}</div><h2>{{ selectedException.summary }}</h2></div>
                    <el-tag :type="selectedException.status === 'CLOSED' ? 'success' : selectedExceptionOverdue ? 'danger' : 'warning'" effect="dark">{{ selectedExceptionOverdue ? '已超期' : capaStatusLabel(selectedException.status) }}</el-tag>
                </section>
                <div class="capa-drawer__meta"><span>项目 #{{ selectedException.projectId }}</span><span>责任人 {{ selectedException.ownerName || '-' }}</span><span>时限 {{ dateText(selectedException.dueAt) }}</span></div>
                <el-steps :active="['OPEN', 'CONTAINED', 'ANALYZING', 'ACTIONING', 'VERIFYING', 'CLOSED'].indexOf(selectedException.status)" finish-status="success" simple class="capa-steps"><el-step title="登记" /><el-step title="遏制" /><el-step title="分析" /><el-step title="整改" /><el-step title="验证" /><el-step title="关闭" /></el-steps>

                <section class="capa-section"><h3>案例信息</h3><el-descriptions :column="1" size="small" border><el-descriptions-item label="临时遏制">{{ selectedException.containmentPlan || '-' }}</el-descriptions-item><el-descriptions-item label="根因分析">{{ selectedException.rootCause || '-' }}</el-descriptions-item><el-descriptions-item label="整改计划">{{ selectedException.correctivePlan || '-' }}</el-descriptions-item><el-descriptions-item label="验证说明">{{ selectedException.verificationSummary || '-' }}</el-descriptions-item><el-descriptions-item label="关闭结论">{{ selectedException.closeConclusion || '-' }}</el-descriptions-item></el-descriptions></section>

                <section class="capa-section"><div class="capa-section__header"><h3>整改动作</h3><el-button v-if="selectedException.status === 'ACTIONING' && canManageExceptions" link type="primary" @click="router.push({ name: 'tasks-execution' })">前往任务执行</el-button></div><el-table :data="selectedExceptionActions" size="small"><el-table-column prop="title" label="动作" min-width="190" /><el-table-column prop="assigneeName" label="责任人" width="110" /><el-table-column label="时限" width="160"><template #default="{ row }">{{ dateText(row.dueAt) }}</template></el-table-column><el-table-column prop="status" label="状态" width="100" /></el-table><el-empty v-if="!selectedExceptionActions.length" description="暂无整改动作" :image-size="44" /></section>

                <section v-if="selectedException.status === 'ACTIONING' && canManageExceptions" class="capa-section"><h3>新增整改任务</h3><el-form label-position="top" class="capa-inline-form"><el-form-item label="任务名称" required><el-input v-model="correctiveTaskForm.title" /></el-form-item><el-form-item label="责任人" required><el-select v-model="correctiveTaskForm.assigneeUserId" filterable style="width: 100%"><el-option v-for="member in currentExceptionMembers" :key="member.userId" :label="member.memberName" :value="member.userId" /></el-select></el-form-item><el-form-item label="计划开始"><el-date-picker v-model="correctiveTaskForm.planStart" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item><el-form-item label="计划完成" required><el-date-picker v-model="correctiveTaskForm.planFinish" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item><el-form-item class="capa-inline-form__full" label="整改措施" required><el-input v-model="correctiveTaskForm.actionPlan" type="textarea" :rows="2" /></el-form-item></el-form><el-button type="primary" :loading="saving" @click="createCorrectiveTask">创建整改任务</el-button></section>

                <section class="capa-section"><h3>处置证据</h3><el-table :data="selectedException.evidence" size="small"><el-table-column prop="evidenceType" label="类型" width="100" /><el-table-column prop="evidenceRef" label="引用" min-width="190" /><el-table-column prop="summary" label="说明" min-width="160" /><el-table-column label="操作" width="80"><template #default="{ row }"><el-button v-if="row.evidenceRef?.startsWith('FILE:')" link type="primary" size="small" @click="openEvidence(row)">查看</el-button></template></el-table-column><el-table-column label="提交时间" width="170"><template #default="{ row }">{{ dateText(row.createdAt) }}</template></el-table-column></el-table><el-empty v-if="!selectedException.evidence.length" description="尚未提交证据" :image-size="44" /><div v-if="canManageExceptions && selectedException.status !== 'CLOSED'" class="capa-evidence-form"><el-upload :show-file-list="false" :http-request="uploadEvidence" accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"><el-button :icon="'Upload'">上传证据文件</el-button></el-upload><el-select v-model="evidenceForm.evidenceType" style="width: 108px"><el-option label="文件" value="FILE" /><el-option label="记录" value="REFERENCE" /><el-option label="检查" value="CHECK" /></el-select><el-input v-model="evidenceForm.evidenceRef" placeholder="证据引用或文件编号" /><el-input v-model="evidenceForm.summary" placeholder="证据说明" /><el-button type="primary" :loading="saving" @click="addEvidence">添加证据</el-button></div></section>

                <section v-if="nextTransition && canManageExceptions" class="capa-section capa-transition"><h3>推进至{{ capaStatusLabel(nextTransition) }}</h3><el-form label-position="top"><el-form-item v-if="nextTransition === 'CONTAINED'" label="临时遏制措施" required><el-input v-model="transitionForm.comment" type="textarea" :rows="3" /></el-form-item><template v-if="nextTransition === 'ACTIONING'"><el-form-item label="根因分析" required><el-input v-model="transitionForm.rootCause" type="textarea" :rows="3" /></el-form-item><el-form-item label="整改计划" required><el-input v-model="transitionForm.correctivePlan" type="textarea" :rows="3" /></el-form-item></template><el-form-item v-if="nextTransition === 'VERIFYING'" label="验证说明" required><el-input v-model="transitionForm.verificationSummary" type="textarea" :rows="3" /></el-form-item><el-form-item v-if="nextTransition === 'CLOSED'" label="关闭结论" required><el-input v-model="transitionForm.closeConclusion" type="textarea" :rows="3" /></el-form-item><el-form-item label="补充证据引用"><el-input v-model="transitionForm.evidenceRef" /></el-form-item></el-form><el-alert v-if="nextTransition === 'CLOSED'" title="关闭前需要完成异常关闭审批。若审批被驳回，案例将退回整改阶段。" type="warning" :closable="false" show-icon style="margin-bottom: 12px" /><el-button type="primary" :loading="saving" @click="transitionException">推进状态</el-button></section>
            </div>
        </el-drawer>

        <el-dialog v-model="templateDialogVisible" title="新建有限审批模板" width="min(780px, 94vw)">
            <el-form label-position="top"><div class="template-form__base"><el-form-item label="模板编码" required><el-input v-model="templateForm.templateCode" placeholder="例如 CHANGE_FAST" /></el-form-item><el-form-item label="模板名称" required><el-input v-model="templateForm.templateName" /></el-form-item><el-form-item label="业务类型" required><el-select v-model="templateForm.businessType" style="width: 100%"><el-option label="设计变更" value="CHANGE" /><el-option label="特放" value="SPECIAL_RELEASE" /><el-option label="异常关闭" value="EXCEPTION" /></el-select></el-form-item><el-form-item label="审批方式" required><el-radio-group v-model="templateForm.approvalMode"><el-radio-button label="SERIAL">串行</el-radio-button><el-radio-button label="COUNTERSIGN">会签</el-radio-button></el-radio-group></el-form-item><el-form-item label="默认 SLA（分钟）"><el-input-number v-model="templateForm.slaMinutes" :min="30" :max="43200" :controls="false" style="width: 100%" /></el-form-item></div><div class="template-form__header"><h3>审批节点</h3><el-button link type="primary" :icon="'Plus'" @click="addTemplateNode">添加节点</el-button></div><div v-for="(node, index) in templateForm.nodes" :key="index" class="template-node"><span class="template-node__order">{{ index + 1 }}</span><el-input v-model="node.nodeCode" placeholder="节点编码" /><el-input v-model="node.nodeName" placeholder="节点名称" /><el-select v-model="node.responsibilityCode" placeholder="项目职责"><el-option label="项目经理" value="PROJECT_MANAGER" /><el-option label="技术负责人" value="TECHNICAL" /><el-option label="工艺负责人" value="PROCESS" /><el-option label="生产负责人" value="PRODUCTION" /><el-option label="质量负责人" value="QUALITY" /></el-select><el-input-number v-model="node.slaMinutes" :min="30" :max="43200" :controls="false" /><el-button :icon="'Delete'" circle plain type="danger" :disabled="templateForm.nodes.length === 1" @click="removeTemplateNode(index)" /></div></el-form>
            <template #footer><el-button @click="templateDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="createTemplate">保存草稿</el-button></template>
        </el-dialog>
    </div>
</template>

<style scoped>
/* 行动中心局部样式。 */
.action-center__header {
    align-items: flex-start;
}

.action-center__header h1 {
    margin: 0;
}

.action-center__header p {
    margin: 6px 0 0;
    color: var(--nso-muted);
    font-size: 13px;
}

.action-metrics {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    margin-bottom: 16px;
    border: 1px solid var(--nso-border);
    border-radius: 6px;
    background: var(--nso-surface);
}

.action-metric {
    min-width: 0;
    padding: 15px 18px;
    border-right: 1px solid var(--nso-border);
}

.action-metric:last-child {
    border-right: 0;
}

.action-metric span,
.action-metric strong {
    display: block;
}

.action-metric span {
    color: var(--nso-muted);
    font-size: 12px;
}

.action-metric strong {
    margin-top: 6px;
    color: var(--nso-ink);
    font-size: 24px;
    line-height: 1;
}

.action-metric--danger strong {
    color: var(--nso-danger);
}

.action-metric--warning strong {
    color: var(--nso-warning);
}

.action-metric--primary strong {
    color: var(--nso-blue);
}

.action-center__panel {
    min-width: 0;
}

.action-tabs :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 18px;
}

.action-tabs :deep(.el-tabs__content) {
    padding: 16px 18px 18px;
}

.action-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-height: 36px;
    margin-bottom: 14px;
}

.action-toolbar__filters {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.action-table__summary {
    max-width: 420px;
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.action-date--overdue {
    color: var(--nso-danger);
    font-weight: 600;
}

.capa-drawer {
    display: grid;
    gap: 18px;
}

.capa-drawer__summary,
.capa-section__header,
.template-form__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
}

.capa-drawer__summary h2 {
    margin: 4px 0 0;
    font-size: 17px;
    line-height: 1.45;
}

.capa-drawer__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
    color: var(--nso-muted);
    font-size: 12px;
}

.capa-steps {
    overflow-x: auto;
}

.capa-section {
    padding-top: 16px;
    border-top: 1px solid var(--nso-border);
}

.capa-section h3 {
    margin: 0 0 12px;
    font-size: 14px;
}

.capa-inline-form,
.template-form__base {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 12px;
}

.capa-inline-form__full {
    grid-column: 1 / -1;
}

.capa-evidence-form {
    display: grid;
    grid-template-columns: auto 108px minmax(150px, 1fr) minmax(150px, 1fr) auto;
    gap: 8px;
    align-items: center;
    margin-top: 12px;
}

.capa-transition {
    padding-bottom: 8px;
}

.template-form__base {
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.template-form__header {
    align-items: center;
    margin: 8px 0 10px;
}

.template-form__header h3 {
    margin: 0;
    font-size: 14px;
}

.template-node {
    display: grid;
    grid-template-columns: 30px minmax(100px, 1fr) minmax(130px, 1.2fr) minmax(140px, 1.2fr) 110px 32px;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;
}

.template-node__order {
    color: var(--nso-muted);
    font-size: 13px;
    text-align: center;
}

@media (max-width: 960px) {
    .action-metrics {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .action-metric:nth-child(3) {
        border-right: 0;
    }

    .action-metric:nth-child(n + 4) {
        border-top: 1px solid var(--nso-border);
    }

    .capa-evidence-form {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .template-node {
        grid-template-columns: 28px repeat(2, minmax(0, 1fr)) 32px;
    }

    .template-node :nth-child(4),
    .template-node :nth-child(5) {
        grid-column: span 2;
    }
}

@media (max-width: 640px) {
    .action-center__header,
    .action-toolbar {
        align-items: stretch;
        flex-direction: column;
    }

    .action-metrics {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .action-metric,
    .action-metric:nth-child(3) {
        border-right: 1px solid var(--nso-border);
        border-bottom: 1px solid var(--nso-border);
    }

    .action-metric:nth-child(even),
    .action-metric:last-child {
        border-right: 0;
    }

    .action-metric:last-child {
        border-bottom: 0;
    }

    .action-tabs :deep(.el-tabs__header),
    .action-tabs :deep(.el-tabs__content) {
        padding-left: 12px;
        padding-right: 12px;
    }

    .capa-inline-form,
    .template-form__base,
    .capa-evidence-form {
        grid-template-columns: 1fr;
    }

    .template-node {
        grid-template-columns: 24px minmax(0, 1fr) 32px;
    }

    .template-node :nth-child(2),
    .template-node :nth-child(3),
    .template-node :nth-child(4),
    .template-node :nth-child(5) {
        grid-column: 2;
    }

    .template-node :last-child {
        grid-column: 3;
        grid-row: 1;
    }
}
</style>
