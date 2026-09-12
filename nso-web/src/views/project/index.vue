<script setup lang="ts">
// 项目列表、工作台与协同数据状态。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import { openProtectedFile } from '@/utils/file-access';
import { formatChineseDate, formatChineseDateTime } from '@/utils/date';
import FlowQrCard from '@/components/FlowQrCard.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { Customer, CustomerContact, ExternalProjectAccess, Project, ProjectDetail, ProjectMember, ProjectMemberCandidate, ProjectPulse, ProjectStatusHistory, ProjectStats, ProjectWorkspace, ProjectWorkspaceSummary, QrCodeBinding, RiskAction, RiskDetail } from '@/types';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const customers = ref<Customer[]>([]);
const projects = ref<Project[]>([]);
const projectStats = ref<ProjectStats>();
const workspace = ref<ProjectWorkspace>();
const projectPulse = ref<ProjectPulse>();
const selectedProjectId = ref<number>();
const loading = ref(false);
const workspaceLoading = ref(false);
const createDrawerVisible = ref(false);
const customerDialogVisible = ref(false);
const customerSaving = ref(false);
const riskDrawerVisible = ref(false);
const conflictDialogVisible = ref(false);
const documentQr = ref<QrCodeBinding>();
const documentQrVisible = ref(false);
const detailTab = ref('technical');
const statusHistory = ref<ProjectStatusHistory[]>([]);
const riskDetails = ref<RiskDetail[]>([]);
const managerCandidates = ref<ProjectMemberCandidate[]>([]);
const memberCandidates = ref<ProjectMemberCandidate[]>([]);
const memberEditVisible = ref(false);
const managerTransferVisible = ref(false);
const customerAuthorizationVisible = ref(false);
const customerContactVisible = ref(false);
const customerContacts = ref<CustomerContact[]>([]);
const customerAuthorizations = ref<ExternalProjectAccess[]>([]);
const ownerGapCount = ref(0);
const workspaceDocumentPagination = usePagination<ProjectWorkspace['detail']['documents'][number]>({ queryPrefix: 'workspaceDocument' });
const workspaceSamplePagination = usePagination<ProjectWorkspace['detail']['samples'][number]>({ queryPrefix: 'workspaceSample' });
const workspaceChangePagination = usePagination<ProjectWorkspace['detail']['changes'][number]>({ queryPrefix: 'workspaceChange' });
const workspaceTimelinePagination = usePagination<ProjectWorkspace['detail']['timeline'][number]>({ queryPrefix: 'workspaceTimeline' });
const workspaceStatusPagination = usePagination<ProjectStatusHistory>({ queryPrefix: 'workspaceStatus' });
const workspaceRequirementPagination = usePagination<ProjectWorkspace['detail']['requirements'][number]>({ queryPrefix: 'workspaceRequirement' });
const workspaceMemberPagination = usePagination<ProjectWorkspace['detail']['members'][number]>({ queryPrefix: 'workspaceMember' });
const workspaceContactPagination = usePagination<CustomerContact>({ queryPrefix: 'workspaceContact' });
const workspaceAuthorizationPagination = usePagination<ExternalProjectAccess>({ queryPrefix: 'workspaceAuthorization' });
const workspaceConflictPagination = usePagination<ProjectWorkspace['versionConflicts'][number]>({ queryPrefix: 'workspaceConflict' });
const workspaceOwnerGapPagination = usePagination<ProjectWorkspace['ownerGaps'][number]>({ queryPrefix: 'workspaceOwnerGap' });
const workspaceRiskDetailPagination = usePagination<RiskDetail>({ queryPrefix: 'workspaceRiskDetail' });
const workspaceRiskActionPagination = usePagination<RiskAction>({ queryPrefix: 'workspaceRiskAction' });
const isWorkspaceRoute = computed(() => route.name === 'project-workspace');
const quickProjectFilter = ref<'ALL' | 'ACTIVE' | 'DUE' | 'RISK'>('ALL');
const projectFilters = reactive({ keyword: '', stage: '', status: '', riskLevel: '', dueState: '' });
const projectPagination = usePagination<Project>();
projectPagination.configure(
    (params) => api.projects({
        keyword: projectFilters.keyword.trim() || undefined,
        stage: projectFilters.stage || undefined,
        status: projectFilters.status || undefined,
        riskLevel: projectFilters.riskLevel || undefined,
        dueState: projectFilters.dueState || undefined,
        quickFilter: quickProjectFilter.value === 'ALL' ? undefined : quickProjectFilter.value,
        ...params
    }),
    (result) => {
        projects.value = result.list;
    }
);
const form = reactive({
    customerId: undefined as number | undefined,
    customerName: '',
    productName: '',
    quantity: 1,
    targetDate: '',
    managerUserId: undefined as number | undefined,
    priority: 'MEDIUM'
});

const customerForm = reactive({ name: '', industry: '', contactName: '', phone: '', status: 'ENABLED' });
const requirementForm = reactive({ category: 'CUSTOMER', content: '', confirmStatus: 'UNCONFIRMED', reason: '' });
const memberForm = reactive({ userId: undefined as number | undefined, responsibilityCodes: [] as string[], primaryResponsibilityCode: '' });
const memberEditForm = reactive({ id: 0, userId: 0, memberName: '', responsibilityCodes: [] as string[], primaryResponsibilityCode: '' });
const managerTransferForm = reactive({ managerUserId: undefined as number | undefined, reason: '' });
const customerAuthorizationForm = reactive({ contactId: undefined as number | undefined, validUntil: '' });
const customerContactForm = reactive({ contactName: '', phone: '', email: '', positionName: '', preferredChannel: 'LINK', status: 'ENABLED' });
const actionForm = reactive({ actionPlan: '', responsibleUserId: undefined as number | undefined, planFinishTime: '' });
const closeForm = reactive({ id: 0, version: 0, closeSummary: '' });
const emptyWorkspacePage = <T>(params: { pageNo: number; pageSize: number }) => Promise.resolve({
    list: [] as T[], total: 0, pageNo: params.pageNo, pageSize: params.pageSize
});

workspaceDocumentPagination.configure(
    (params) => selectedProjectId.value ? api.documents({ projectId: selectedProjectId.value, ...params }) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.detail.documents = result.list; }
);
workspaceSamplePagination.configure(
    (params) => selectedProjectId.value ? api.samples({ projectId: selectedProjectId.value, ...params }) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.detail.samples = result.list; }
);
workspaceChangePagination.configure(
    (params) => selectedProjectId.value ? api.changes({ projectId: selectedProjectId.value, ...params }) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.detail.changes = result.list; }
);
workspaceTimelinePagination.configure(
    (params) => selectedProjectId.value ? api.projectWorkspaceTimeline(selectedProjectId.value, params) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.detail.timeline = result.list; }
);
workspaceStatusPagination.configure(
    (params) => selectedProjectId.value ? api.projectStatusHistory(selectedProjectId.value, params) : emptyWorkspacePage(params),
    (result) => { statusHistory.value = result.list; }
);
workspaceRequirementPagination.configure(
    (params) => selectedProjectId.value ? api.requirements(selectedProjectId.value, params) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.detail.requirements = result.list; }
);
workspaceMemberPagination.configure(
    (params) => selectedProjectId.value ? api.members(selectedProjectId.value, params) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.detail.members = result.list; }
);
workspaceContactPagination.configure(
    (params) => workspace.value?.detail.project.customerId ? api.customerContacts(workspace.value.detail.project.customerId, params) : emptyWorkspacePage(params),
    (result) => { customerContacts.value = result.list; }
);
workspaceAuthorizationPagination.configure(
    (params) => selectedProjectId.value && auth.can('customer:invite')
        ? api.projectCustomerAuthorizations(selectedProjectId.value, params)
        : emptyWorkspacePage(params),
    (result) => { customerAuthorizations.value = result.list; }
);
workspaceConflictPagination.configure(
    (params) => selectedProjectId.value ? api.projectWorkspaceVersionConflicts(selectedProjectId.value, params) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.versionConflicts = result.list; }
);
workspaceOwnerGapPagination.configure(
    (params) => selectedProjectId.value ? api.projectWorkspaceOwnerGaps(selectedProjectId.value, params) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.ownerGaps = result.list; }
);
workspaceRiskDetailPagination.configure(
    (params) => workspace.value?.currentRisk ? api.riskDetails(workspace.value.currentRisk.id, params) : emptyWorkspacePage(params),
    (result) => { riskDetails.value = result.list; }
);
workspaceRiskActionPagination.configure(
    (params) => workspace.value?.currentRisk ? api.riskActions(workspace.value.currentRisk.id, params) : emptyWorkspacePage(params),
    (result) => { if (workspace.value) workspace.value.riskActions = result.list; }
);
const activeProject = computed<Project | undefined>(() => workspace.value?.detail.project);
const currentRisk = computed(() => workspace.value?.currentRisk);
const activeMembers = computed(() => workspace.value?.detail.members || []);
const sampleStateNeedsReconcile = computed(() => {
    const project = activeProject.value;
    const latestSample = workspace.value?.detail.samples?.[0];
    if (!project || !latestSample) {
        return false;
    }
    const expectedStatus = ({
        CONFIRMED: 'CUSTOMER_CONFIRMED',
        WAIT_CUSTOMER_CONFIRM: 'CUSTOMER_CONFIRMING',
        WAIT_SUPPLEMENT: 'CUSTOMER_CONFIRMING',
        CONDITIONAL_PASS: 'CUSTOMER_CONFIRMING',
        DRAFT: 'SAMPLING',
        CHECKED: 'SAMPLING',
        REWORKING: 'SAMPLING',
        REJECTED: 'SAMPLING'
    } as Record<string, string>)[latestSample.status];
    return Boolean(expectedStatus && (project.status !== expectedStatus || project.sampleStatus !== latestSample.status));
});

const selectedMemberCandidate = computed(() => memberCandidates.value.find(candidate => candidate.userId === memberForm.userId));
const editingMemberCandidate = computed(() => memberCandidates.value.find(candidate => candidate.userId === memberEditForm.userId));
const riskReasons = computed(() => currentRisk.value?.reasons?.filter(Boolean) || []);
const projectPulseMetrics = computed(() => projectPulse.value?.metrics || {});
const projectActions = computed(() => {
    const status = activeProject.value?.status;
    const entries: Array<{
        code: string;
        label: string;
        operationCode?: string;
    }> = [];
    if (status === 'REVIEWING') {
        entries.push({ code: 'APPROVE_REVIEW', label: '评审通过' }, { code: 'REJECT_REVIEW', label: '驳回至草稿' });
    }
    if (sampleStateNeedsReconcile.value) {
        entries.push({ code: 'RECONCILE_SAMPLE_STATE', label: '同步样品状态' });
    }
    if (status === 'CUSTOMER_CONFIRMED') {
        entries.push({ code: 'PREPARE_PRODUCTION', label: '准备投产' });
    }
    if (status === 'PRODUCTION_PREPARING') {
        entries.push({ code: 'START_PRODUCTION', label: '开始投产' });
    }
    if (status === 'PENDING_DELIVERY') {
        entries.push({ code: 'COMPLETE', label: '完成项目' });
    }
    if (status === 'SUSPENDED') {
        entries.push({ code: 'RESUME', label: '恢复项目' });
    }
    if (status === 'COMPLETED') {
        entries.push({ code: 'ARCHIVE', label: '归档项目', operationCode: 'PROJECT_ARCHIVE' });
    }
    if (status === 'ARCHIVED') {
        entries.push({ code: 'RESTORE', label: '恢复归档项目', operationCode: 'PROJECT_RESTORE' });
    }
    if (status && !['ARCHIVED', 'CANCELLED', 'COMPLETED', 'SUSPENDED'].includes(status)) {
        entries.push({ code: 'SUSPEND', label: '暂停项目' }, { code: 'CANCEL', label: '取消项目' });
    }
    return entries;
});
const primaryProjectAction = computed(() => projectActions.value.find((action) => !['SUSPEND', 'CANCEL', 'ARCHIVE', 'RESTORE'].includes(action.code)));

const projectWorkflowSteps = computed(() => {
    const steps = [
        { key: 'project', label: '项目建档', hint: '客户与交期' },
        { key: 'review', label: '需求评审', hint: '确认范围' },
        { key: 'technical', label: '技术包', hint: '图纸与工艺' },
        { key: 'sample', label: '打样检验', hint: '质量确认' },
        { key: 'customer', label: '客户确认', hint: '形成结论' },
        { key: 'delivery', label: '任务交付', hint: '按期完成' }
    ];
    const stageIndex = ({ REQUIREMENT: 1, TECHNICAL: 2, SAMPLE: 3, CONFIRM: 4, EXECUTION: 5, DELIVERY: 5, ARCHIVE: 6 } as Record<string, number>)[activeProject.value?.stage || ''] ?? 0;
    const status = activeProject.value?.status;
    const completed = ['COMPLETED', 'ARCHIVED'].includes(status || '') || stageIndex >= steps.length;
    const halted = ['SUSPENDED', 'CANCELLED'].includes(status || '');
    return steps.map((step, index) => ({
        ...step,
        state: completed || index < stageIndex
            ? 'done' as const
            : index === stageIndex
                ? halted ? 'blocked' as const : 'current' as const
                : 'pending' as const
    }));
});

const stageLabel = (stage?: string) => ({
    REQUIREMENT: '需求评审', TECHNICAL: '图纸发布', SAMPLE: '样品确认',
    CONFIRM: '客户确认', EXECUTION: '正式投产', DELIVERY: '交付', ARCHIVE: '归档'
} as Record<string, string>)[stage || ''] || stage || '-';
const statusLabel = (status?: string) => ({
    DRAFT: '草稿', REVIEWING: '评审中', TECH_PREPARING: '技术准备', TECH_PUBLISHED: '技术包已发布', SAMPLING: '打样中',
    CUSTOMER_CONFIRMING: '客户确认中', CUSTOMER_CONFIRMED: '样品已确认', PRODUCTION_PREPARING: '投产准备', EXECUTING: '生产执行中',
    PENDING_DELIVERY: '待交付', COMPLETED: '已完成', ARCHIVED: '已归档', SUSPENDED: '已暂停', CANCELLED: '已取消', OPEN: '处理中', CLOSED: '已关闭'
} as Record<string, string>)[status || ''] || status || '-';
const statusTag = (status?: string) => ({
    COMPLETED: 'success', ARCHIVED: 'info', CLOSED: 'success', CANCELLED: 'info', REVIEWING: 'warning', SAMPLING: 'warning', CUSTOMER_CONFIRMING: 'warning', SUSPENDED: 'warning'
} as Record<string, string>)[status || ''] || 'info';
const riskLevelLabel = (level?: string) => ({ SERIOUS: '严重', HIGH: '高', MEDIUM: '中', LOW: '低' } as Record<string, string>)[level || ''] || level || '暂无';
const sampleStatusLabel = (status?: string) => ({ NONE: '未打样', DRAFT: '草稿', REWORKING: '整改中', CHECKED: '已检验', WAIT_CUSTOMER_CONFIRM: '待客户确认', WAIT_SUPPLEMENT: '待补充', CONFIRMED: '已通过', CONDITIONAL_PASS: '条件通过', REJECTED: '已驳回' } as Record<string, string>)[status || ''] || status || '未打样';
const sampleStatusTag = (status?: string) => ({ CONFIRMED: 'success', CHECKED: 'success', WAIT_CUSTOMER_CONFIRM: 'warning', WAIT_SUPPLEMENT: 'warning', REWORKING: 'danger', REJECTED: 'danger' } as Record<string, string>)[status || ''] || 'info';
const priorityLabel = (priority?: string) => ({ URGENT: '紧急', HIGH: '高优先级', NORMAL: '普通', MEDIUM: '中优先级', LOW: '低优先级' } as Record<string, string>)[priority || ''] || priority || '未设置优先级';
const stageOptions = [
    { value: 'REQUIREMENT', label: '需求评审' }, { value: 'TECHNICAL', label: '图纸发布' },
    { value: 'SAMPLE', label: '样品确认' }, { value: 'CONFIRM', label: '客户确认' },
    { value: 'EXECUTION', label: '正式投产' }, { value: 'DELIVERY', label: '交付' }, { value: 'ARCHIVE', label: '归档' }
];
const statusOptions = [
    { value: 'DRAFT', label: '草稿' }, { value: 'REVIEWING', label: '评审中' }, { value: 'TECH_PREPARING', label: '技术准备' },
    { value: 'TECH_PUBLISHED', label: '技术包已发布' }, { value: 'SAMPLING', label: '打样中' }, { value: 'CUSTOMER_CONFIRMING', label: '客户确认中' },
    { value: 'CUSTOMER_CONFIRMED', label: '样品已确认' }, { value: 'PRODUCTION_PREPARING', label: '投产准备' }, { value: 'EXECUTING', label: '生产执行中' },
    { value: 'PENDING_DELIVERY', label: '待交付' }, { value: 'COMPLETED', label: '已完成' }, { value: 'ARCHIVED', label: '已归档' },
    { value: 'SUSPENDED', label: '已暂停' }, { value: 'CANCELLED', label: '已取消' }
];
const riskOptions = [{ value: 'SERIOUS', label: '严重' }, { value: 'HIGH', label: '高' }, { value: 'MEDIUM', label: '中' }, { value: 'LOW', label: '低' }];
const dueOptions = [{ value: 'OVERDUE', label: '已超期' }, { value: 'DUE_SOON', label: '三天内到期' }, { value: 'NO_DATE', label: '未设置交期' }];
const terminalProjectStatuses = new Set(['COMPLETED', 'ARCHIVED', 'CANCELLED']);
const isActiveProject = (project: Project) => !terminalProjectStatuses.has(project.status);
const isHighRiskProject = (project: Project) => ['SERIOUS', 'HIGH'].includes(project.riskLevel);
const isDueProject = (project: Project) => isActiveProject(project) && Boolean(project.targetDate) && project.daysLeft <= 3;
const dueText = (project: Project) => {
    if (!project.targetDate) {
        return '未设置交期';
    }
    if (!isActiveProject(project)) {
        return '已结束';
    }
    if (project.daysLeft < 0) {
        return `超期 ${Math.abs(project.daysLeft)} 天`;
    }
    if (project.daysLeft <= 3) {
        return `剩余 ${project.daysLeft} 天`;
    }
    return `剩余 ${project.daysLeft} 天`;
};
const projectActionText = (project: Project) => project.recommendedActionLabel || (project.blockerCount ? '处理阻塞' : project.pendingDecisionCount ? '处理待决策' : '打开工作台');

const projectMetricCards = computed(() => [
    { key: 'ALL', label: '全部项目', value: projectStats.value?.totalProjects ?? projectPagination.total, hint: '当前数据范围', tone: 'neutral' },
    { key: 'ACTIVE', label: '进行中', value: projectStats.value?.inProgressProjects ?? projects.value.filter(isActiveProject).length, hint: '未完成项目', tone: 'blue' },
    { key: 'DUE', label: '临期/超期', value: projectStats.value?.dueProjects ?? projects.value.filter(isDueProject).length, hint: '三天内需关注', tone: 'orange' },
    { key: 'RISK', label: '高风险', value: projectStats.value?.highRiskProjects ?? projects.value.filter(isHighRiskProject).length, hint: '高/严重风险', tone: 'red' }
]);
const stageDistribution = computed(() => stageOptions.map(stage => ({ ...stage, count: projectStats.value?.stageDistribution?.[stage.value] || 0 })));
const attentionProjects = computed(() => (projectStats.value?.attentionProjects?.list || []).map(project => {
    if (!isActiveProject(project)) {
        return undefined;
    }
    if (project.targetDate && project.daysLeft < 0) {
        return { project, reason: `已超期 ${Math.abs(project.daysLeft)} 天`, tone: 'danger', rank: 4 };
    }
    if (isHighRiskProject(project)) {
        return { project, reason: `${riskLevelLabel(project.riskLevel)}风险项目`, tone: 'danger', rank: 3 };
    }
    if (project.targetDate && project.daysLeft <= 3) {
        return { project, reason: `三天内到期，剩余 ${project.daysLeft} 天`, tone: 'warning', rank: 2 };
    }
    if (['DRAFT', 'REVIEWING'].includes(project.status)) {
        return { project, reason: project.status === 'DRAFT' ? '待提交评审' : '评审处理中', tone: 'info', rank: 1 };
    }
    if (!project.targetDate) {
        return { project, reason: '未设置目标交期', tone: 'info', rank: 0 };
    }
    return undefined;
}).filter((item): item is {
    project: Project;
    reason: string;
    tone: string;
    rank: number;
} => Boolean(item)).sort((a, b) => b.rank - a.rank || a.project.daysLeft - b.project.daysLeft));
const hasProjectFilters = computed(() => Boolean(projectFilters.keyword || projectFilters.stage || projectFilters.status || projectFilters.riskLevel || projectFilters.dueState || quickProjectFilter.value !== 'ALL'));
const stageShare = (count: number) => (projectStats.value?.totalProjects || projectPagination.total.value) ? Math.max(4, Math.round((count / (projectStats.value?.totalProjects || projectPagination.total.value)) * 100)) : 0;

function selectQuickProjectFilter(key: 'ALL' | 'ACTIVE' | 'DUE' | 'RISK') {
    quickProjectFilter.value = key;
    Object.assign(projectFilters, { stage: '', status: '', riskLevel: '', dueState: '' });
}

function selectStageFilter(stage: string) {
    quickProjectFilter.value = 'ALL';
    projectFilters.stage = projectFilters.stage === stage ? '' : stage;
}

function clearProjectFilters() {
    quickProjectFilter.value = 'ALL';
    Object.assign(projectFilters, { keyword: '', stage: '', status: '', riskLevel: '', dueState: '' });
}

function openProgressAction() {
    const target = workspace.value?.progress?.nextActionRoute;
    if (!target) return;
    const [path, search = ''] = target.split('?');
    const query = Object.fromEntries(new URLSearchParams(search));
    if (activeProject.value?.id) query.projectId = String(activeProject.value.id);
    void router.push({ path, query });
}

const responsibilityLabel = (code: string) => ({
    SALES: '销售协同', PROJECT_MANAGER: '项目经理', TECHNICAL: '技术/设计', PROCESS: '工艺',
    TECH_OWNER: '技术负责人', TECH_MEMBER: '技术成员', PROCESS_OWNER: '工艺负责人', PROCESS_MEMBER: '工艺成员',
    PURCHASE_OWNER: '采购负责人', PURCHASE_MEMBER: '采购成员', PRODUCTION_OWNER: '生产负责人', PRODUCTION_MEMBER: '生产成员',
    QUALITY_OWNER: '质量负责人', QUALITY_MEMBER: '质量成员', PROJECT_DEPUTY: '项目副经理', COLLABORATOR: '协同成员', VIEWER: '仅查看',
    PURCHASER: '采购', PRODUCTION: '计划/生产', QUALITY: '质量', FIELD_USER: '现场执行', CUSTOMER_CONFIRM: '客户确认'
} as Record<string, string>)[code] || code;
const riskTag = (level?: string) => ({ SERIOUS: 'danger', HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' } as Record<string, string>)[level || ''] || 'info';
const confirmTag = (status?: string) => ({ CUSTOMER_CONFIRMED: 'success', INTERNAL_CONFIRMED: 'success', UNCONFIRMED: 'warning', DISPUTED: 'danger' } as Record<string, string>)[status || ''] || 'info';
const dateText = formatChineseDateTime;
const asDetail = () => workspace.value?.detail as ProjectDetail | undefined;

function projectStatsParams() {
    return {
        keyword: projectFilters.keyword.trim() || undefined,
        stage: projectFilters.stage || undefined,
        status: projectFilters.status || undefined,
        riskLevel: projectFilters.riskLevel || undefined,
        dueState: projectFilters.dueState || undefined,
        quickFilter: quickProjectFilter.value === 'ALL' ? undefined : quickProjectFilter.value,
        pageNo: 1,
        pageSize: 5
    };
}

async function refreshProjectStats() {
    const stats = await api.projectStats(projectStatsParams());
    projectStats.value = stats;
    return stats;
}

async function loadProjects() {
    // 按权限并行加载项目页基础数据，避免无权接口被调用。
    loading.value = true;
    try {
        const [customerData, , managerData, statsData] = await Promise.all([
            auth.can('customer:view') ? api.customers({ pageSize: 100 }) : Promise.resolve({ list: [] }),
            projectPagination.reload(),
            auth.can('project:create') ? api.projectManagerCandidates({ pageSize: 100 }) : Promise.resolve({ list: [] }),
            refreshProjectStats()
        ]);
        customers.value = customerData.list || [];
        managerCandidates.value = managerData.list || [];
        projectStats.value = statsData;
        if (!form.customerId && customers.value[0]) {
            form.customerId = customers.value[0].id;
        }
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目列表加载失败'));
    }
    finally {
        loading.value = false;
    }
}

watch(
    () => [projectFilters.keyword, projectFilters.stage, projectFilters.status, projectFilters.riskLevel, projectFilters.dueState, quickProjectFilter.value],
    () => {
        loading.value = true;
        void Promise.all([projectPagination.reset(), refreshProjectStats()])
            .catch((error) => ElMessage.error(userFacingError(error, '项目列表加载失败')))
            .finally(() => { loading.value = false; });
    }
);

function workspaceFromSummary(summary: ProjectWorkspaceSummary): ProjectWorkspace {
    return {
        detail: {
            project: summary.project,
            requirements: [],
            members: [],
            documents: [],
            samples: [],
            changes: [],
            tasks: [],
            risks: [],
            timeline: []
        },
        currentRisk: summary.currentRisk,
        riskActions: [],
        milestones: summary.milestones,
        versionConflicts: [],
        flowQrCode: summary.flowQrCode,
        ownerGaps: []
    };
}

async function loadWorkspaceTab(tab = detailTab.value) {
    if (!workspace.value || !selectedProjectId.value) {
        return;
    }
    if (tab === 'technical') {
        await workspaceDocumentPagination.reload();
        return;
    }
    if (tab === 'samples') {
        await workspaceSamplePagination.reload();
        return;
    }
    if (tab === 'changes') {
        await workspaceChangePagination.reload();
        return;
    }
    if (tab === 'timeline') {
        await workspaceTimelinePagination.reload();
        return;
    }
    if (tab === 'status-history') {
        await workspaceStatusPagination.reload();
        return;
    }
    if (tab === 'configuration') {
        await Promise.all([
            workspaceRequirementPagination.reload(),
            workspaceMemberPagination.reload(),
            workspaceContactPagination.reload(),
            workspaceAuthorizationPagination.reload()
        ]);
        if (auth.can('project:member:manage')) {
            memberCandidates.value = (await api.projectMemberCandidates(selectedProjectId.value, { pageSize: 100 })).list || [];
        }
    }
}

watch(detailTab, () => {
    void loadWorkspaceTab().catch((error) => ElMessage.error(userFacingError(error, '项目工作台列表加载失败')));
});

watch(riskDrawerVisible, (visible) => {
    if (visible && currentRisk.value) {
        void Promise.all([workspaceRiskDetailPagination.reload(), workspaceRiskActionPagination.reload()])
            .catch((error) => ElMessage.error(userFacingError(error, '风险明细加载失败')));
    }
});

async function openProject(projectId: number, updateRoute = true) {
    // 路由切换与工作台数据保持同步，失败时恢复上一个可用状态。
    if (updateRoute) {
        await router.push({ name: 'project-workspace', query: projectListPagingQuery(projectId) });
        return;
    }
    const previousProjectId = selectedProjectId.value;
    const previousWorkspace = workspace.value;
    workspaceLoading.value = true;
    selectedProjectId.value = projectId;
    if (previousProjectId !== projectId) {
        workspace.value = undefined;
    }
    try {
        const [next, pulse] = await Promise.all([
            api.projectWorkspaceSummary(projectId),
            api.projectPulse(projectId)
        ]);
        if (!next.flowQrCode && auth.can('project:view')) {
            next.flowQrCode = await api.createProjectFlowQr(projectId);
        }
        workspace.value = workspaceFromSummary(next);
        projectPulse.value = pulse;
        ownerGapCount.value = next.ownerGapCount;
        statusHistory.value = [];
        riskDetails.value = [];
        customerContacts.value = [];
        customerAuthorizations.value = [];
        memberCandidates.value = [];
        detailTab.value = 'technical';
        await Promise.all([
            workspaceDocumentPagination.reset(),
            workspaceConflictPagination.reset(),
            workspaceOwnerGapPagination.reset()
        ]);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目工作台加载失败'));
        if (previousProjectId !== projectId || !previousWorkspace) {
            workspace.value = undefined;
            projectPulse.value = undefined;
            selectedProjectId.value = undefined;
            if (isWorkspaceRoute.value) {
                await router.replace({ name: 'project-workspace' });
            }
        }
    }
    finally {
        workspaceLoading.value = false;
    }
}

function projectListPagingQuery(projectId?: number) {
    const query: Record<string, string> = {
        pageNo: String(projectPagination.pageNo.value),
        pageSize: String(projectPagination.pageSize.value)
    };
    if (projectId) {
        query.id = String(projectId);
    }
    return query;
}

function openRow(row: Project) {
    void router.push({ name: 'project-workspace', query: projectListPagingQuery(row.id) });
}

function changeWorkspaceProject(projectId?: number) {
    workspace.value = undefined;
    projectPulse.value = undefined;
    if (projectId) {
        void router.replace({ name: 'project-workspace', query: projectListPagingQuery(projectId) });
    }
    else {
        selectedProjectId.value = undefined;
        void router.replace({ name: 'project-workspace', query: projectListPagingQuery() });
    }
}

async function closeWorkspace() {
    workspace.value = undefined;
    projectPulse.value = undefined;
    selectedProjectId.value = undefined;
    await router.replace({ name: 'projects', query: projectListPagingQuery() });
}

function openCreate() {
    createDrawerVisible.value = true;
}

function handleCreateDrawerClosed() {
    if (route.name === 'projects' && route.query.create) {
        void router.replace({ name: 'projects' });
    }
}

async function createProject() {
    if (!form.productName.trim()) {
        return ElMessage.warning('请填写产品名称');
    }
    if (!form.customerId && !form.customerName.trim()) {
        return ElMessage.warning('请选择或填写客户');
    }
    if (!form.managerUserId) {
        return ElMessage.warning('请选择项目经理账号');
    }
    try {
        const created = await api.createProject({ ...form, productName: form.productName.trim(), customerName: form.customerName.trim() });
        ElMessage.success('项目已创建');
        createDrawerVisible.value = false;
        Object.assign(form, { customerName: '', productName: '', quantity: 1, targetDate: '', managerUserId: undefined, priority: 'MEDIUM' });
        await loadProjects();
        await openProject(created.id);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目创建失败'));
    }
}

async function createCustomer() {
    if (!customerForm.name.trim()) {
        return ElMessage.warning('请填写客户名称');
    }
    customerSaving.value = true;
    try {
        const customer = await api.createCustomer({ ...customerForm, name: customerForm.name.trim() });
        customers.value = [customer, ...customers.value];
        form.customerId = customer.id;
        customerDialogVisible.value = false;
        Object.assign(customerForm, { name: '', industry: '', contactName: '', phone: '' });
        ElMessage.success('客户已创建');
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '客户创建失败'));
    }
    finally {
        customerSaving.value = false;
    }
}

async function submitReview(row: Project) {
    try {
        const requirementPage = await api.requirements(row.id);
        if ((requirementPage.list || []).length === 0) {
            ElMessage.warning('请先在“需求与成员”中填写需求内容并点击“新增需求”，列表出现记录后才能提交评审');
            return;
        }
        await ElMessageBox.confirm('确认提交需求评审？', '提交评审', { type: 'warning' });
        await api.submitReview(row.id);
        ElMessage.success('已提交需求评审');
        await loadProjects();
        if (row.id === selectedProjectId.value) {
            await openProject(row.id, false);
        }
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '提交需求评审失败'));
        }
    }
}

async function runProjectAction(action: {
    code: string;
    label: string;
    operationCode?: string;
}) {
    const project = activeProject.value;
    if (!project) {
        return;
    }
    try {
        const { value } = await ElMessageBox.prompt(`请填写“${action.label}”原因。该记录会写入状态历史与审计日志。`, action.label, {
            inputPattern: /\S+/, inputErrorMessage: '原因不能为空', confirmButtonText: '确认执行', cancelButtonText: '取消'
        });
        let confirmationId: number | undefined;
        if (action.operationCode) {
            const confirmation = await api.requestOperationConfirmation({
                operationCode: action.operationCode, businessType: 'PROJECT', businessId: project.id, reason: value, channel: 'IN_APP'
            });
            await api.confirmOperation(confirmation.id);
            confirmationId = confirmation.id;
        }
        await api.projectAction(project.id, action.code, { reason: value, version: project.version, confirmationId });
        ElMessage.success(`${action.label}已完成`);
        await loadProjects();
        await refreshWorkspace();
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, `${action.label}失败`));
        }
    }
}

function handleProjectActionCommand(code: string) {
    const action = projectActions.value.find((item) => item.code === code);
    if (action) {
        void runProjectAction(action);
    }
}

async function copyCurrentProject() {
    const project = activeProject.value;
    if (!project) {
        return;
    }
    try {
        const { value } = await ElMessageBox.prompt('请输入复制后的产品/治具名称；需求与项目成员会一并复制，新项目保持草稿。', '复制项目', { inputValue: `${project.productName}-复制`, inputPattern: /\S+/, inputErrorMessage: '名称不能为空' });
        const copy = await api.copyProject(project.id, { productName: value, reason: '项目复制' });
        ElMessage.success(`已创建复制项目 ${copy.projectNo}`);
        await loadProjects();
        await openProject(copy.id);
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '复制项目失败'));
        }
    }
}

async function refreshWorkspace() {
    if (selectedProjectId.value) {
        await openProject(selectedProjectId.value, false);
    }
}

async function addRequirement() {
    const detail = asDetail();
    if (!detail?.project.id) {
        return;
    }
    if (!requirementForm.content.trim()) {
        return ElMessage.warning('请填写需求内容');
    }
    try {
        await api.saveRequirement(detail.project.id, { ...requirementForm, content: requirementForm.content.trim() });
        requirementForm.content = '';
        ElMessage.success('需求项已保存');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '需求保存失败'));
    }
}

async function confirmRequirement(row: Record<string, unknown>, status: string) {
    try {
        await api.confirmRequirement(Number(row.id), { ...row, confirmStatus: status, reason: status === 'CUSTOMER_CONFIRMED' ? '客户已确认' : status === 'DISPUTED' ? '存在异议，等待处理' : '内部已确认' });
        ElMessage.success('需求状态已更新');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '确认失败'));
    }
}

async function addMember() {
    const detail = asDetail();
    if (!detail?.project.id) {
        return;
    }
    if (!memberForm.userId || memberForm.responsibilityCodes.length === 0 || !memberForm.primaryResponsibilityCode) {
        return ElMessage.warning('请选择账号、项目职责和主职责');
    }
    try {
        await api.addMember(detail.project.id, { ...memberForm });
        Object.assign(memberForm, { userId: undefined, responsibilityCodes: [], primaryResponsibilityCode: '' });
        ElMessage.success('项目成员已加入');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '添加成员失败'));
    }
}

function resetPrimary(formState: {
    responsibilityCodes: string[];
    primaryResponsibilityCode: string;
}) {
    if (!formState.responsibilityCodes.includes(formState.primaryResponsibilityCode)) {
        formState.primaryResponsibilityCode = formState.responsibilityCodes[0] || '';
    }
}

function resetMemberResponsibilities() {
    memberForm.responsibilityCodes = [];
    memberForm.primaryResponsibilityCode = '';
}

function memberLabel(row: ProjectMember) {
    return row.responsibilityCodes?.join(' / ') || row.projectRole;
}

function editMember(row: ProjectMember) {
    Object.assign(memberEditForm, {
        id: row.id, userId: row.userId, memberName: row.memberName,
        responsibilityCodes: [...(row.responsibilityCodes || [row.projectRole])],
        primaryResponsibilityCode: row.primaryResponsibilityCode || row.projectRole
    });
    memberEditVisible.value = true;
}

async function saveMemberResponsibilities() {
    const detail = asDetail();
    if (!detail || memberEditForm.responsibilityCodes.length === 0 || !memberEditForm.primaryResponsibilityCode) {
        return ElMessage.warning('至少保留一项职责并指定主职责');
    }
    try {
        await api.updateProjectMember(detail.project.id, memberEditForm.id, {
            responsibilityCodes: memberEditForm.responsibilityCodes,
            primaryResponsibilityCode: memberEditForm.primaryResponsibilityCode
        });
        memberEditVisible.value = false;
        ElMessage.success('成员职责已更新');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '职责调整失败'));
    }
}

async function changeMemberStatus(row: ProjectMember, restore = false) {
    const detail = asDetail();
    if (!detail) {
        return;
    }
    try {
        if (restore) {
            await api.restoreProjectMember(detail.project.id, row.id);
        }
        else {
            await api.removeProjectMember(detail.project.id, row.id);
        }
        ElMessage.success(restore ? '成员已恢复' : '成员已移除');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, restore ? '恢复成员失败' : '移除成员失败'));
    }
}

async function transferManager() {
    const detail = asDetail();
    if (!detail || !managerTransferForm.managerUserId || !managerTransferForm.reason.trim()) {
        return ElMessage.warning('请选择新项目经理并填写交接原因');
    }
    try {
        await api.transferProjectManager(detail.project.id, { managerUserId: managerTransferForm.managerUserId, reason: managerTransferForm.reason.trim() });
        managerTransferVisible.value = false;
        Object.assign(managerTransferForm, { managerUserId: undefined, reason: '' });
        ElMessage.success('项目经理已交接');
        await loadProjects();
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '项目经理交接失败'));
    }
}

async function authorizeCustomer() {
    const detail = asDetail();
    if (!detail || !customerAuthorizationForm.contactId) {
        return ElMessage.warning('请选择客户联系人');
    }
    try {
        await api.authorizeProjectCustomer(detail.project.id, { contactId: customerAuthorizationForm.contactId, validUntil: customerAuthorizationForm.validUntil || undefined });
        customerAuthorizationVisible.value = false;
        ElMessage.success('客户联系人已获得项目确认授权');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '客户项目授权失败'));
    }
}

async function createCustomerContact() {
    const detail = asDetail();
    if (!detail?.project.customerId || !customerContactForm.contactName.trim()) {
        return ElMessage.warning('请填写客户联系人姓名');
    }
    try {
        await api.saveCustomerContact(detail.project.customerId, { ...customerContactForm, contactName: customerContactForm.contactName.trim() });
        customerContactVisible.value = false;
        Object.assign(customerContactForm, { contactName: '', phone: '', email: '', positionName: '', preferredChannel: 'LINK', status: 'ENABLED' });
        ElMessage.success('客户联系人已保存');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '保存客户联系人失败'));
    }
}

async function revokeCustomerAuthorization(access: ExternalProjectAccess) {
    const detail = asDetail();
    if (!detail) {
        return;
    }
    try {
        const { value } = await ElMessageBox.prompt('请填写撤销原因；已发出的客户确认链接会立即失效。', '撤销客户项目授权', { inputPattern: /\S+/, inputErrorMessage: '撤销原因不能为空' });
        await api.revokeProjectCustomerAuthorization(detail.project.id, access.id, value);
        ElMessage.success('客户项目授权已撤销');
        await refreshWorkspace();
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '撤销客户项目授权失败'));
        }
    }
}

async function createRiskAction() {
    const risk = currentRisk.value;
    if (!risk) {
        return;
    }
    if (!actionForm.actionPlan.trim() || !actionForm.responsibleUserId || !actionForm.planFinishTime) {
        return ElMessage.warning('请填写措施、责任人和计划完成时间');
    }
    try {
        await api.createRiskAction(risk.id, {
            actionPlan: actionForm.actionPlan.trim(),
            responsibleUserId: actionForm.responsibleUserId,
            planFinishTime: actionForm.planFinishTime,
            idempotencyKey: crypto.randomUUID()
        });
        Object.assign(actionForm, { actionPlan: '', responsibleUserId: undefined, planFinishTime: '' });
        ElMessage.success('风险处置项已创建');
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '风险处置项创建失败'));
    }
}

function prepareClose(action: RiskAction) {
    Object.assign(closeForm, { id: action.id, version: action.version, closeSummary: '' });
}

async function closeRiskAction() {
    if (!closeForm.closeSummary.trim()) {
        return ElMessage.warning('关闭处置项必须填写总结');
    }
    try {
        await api.closeRiskAction(closeForm.id, { closeSummary: closeForm.closeSummary.trim(), version: closeForm.version });
        ElMessage.success('处置项已关闭');
        Object.assign(closeForm, { id: 0, version: 0, closeSummary: '' });
        await refreshWorkspace();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '关闭处置项失败'));
    }
}

async function syncTechnicalTasks() {
    const project = activeProject.value;
    if (!project) {
        return;
    }
    try {
        await ElMessageBox.confirm('将所有未完成任务同步到当前技术包版本。已完成任务不会改动。', '同步技术版本', { type: 'warning' });
        const result = await api.syncTechnicalPackageTasks(project.id);
        ElMessage.success(`已同步 ${result.updatedTaskCount} 个未完成任务至 ${result.versionNo}`);
        conflictDialogVisible.value = false;
        await refreshWorkspace();
    }
    catch {
        // 用户取消或请求失败时保持当前工作台状态。
    }
}

async function showDocumentQr(documentId: number) {
    try {
        documentQr.value = await api.createDocumentQr(documentId);
        documentQrVisible.value = true;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '文件二维码生成失败'));
    }
}

async function viewDocument(row: { id: number; fileObjectId?: number; fileName?: string }) {
    if (!row.fileObjectId) return ElMessage.warning('当前文件内容不可用');
    try {
        await openProtectedFile(api.documentInlineUrl(row.id), { preview: true, fileName: row.fileName });
    } catch (error) {
        ElMessage.error(userFacingError(error, '文件查看失败'));
    }
}

async function downloadDocument(row: { id: number; fileObjectId?: number; fileName?: string }) {
    if (!row.fileObjectId) return ElMessage.warning('当前文件内容不可用');
    try {
        await openProtectedFile(api.documentDownloadUrl(row.id), { preview: false, fileName: row.fileName });
    } catch (error) {
        ElMessage.error(userFacingError(error, '文件下载失败'));
    }
}

function openPulseAction(item: ProjectPulse['blockers'][number]) {
    if (item.route) {
        void router.push(item.route);
    }
    else {
        void router.push({ name: 'actions', query: { tab: item.sourceType === 'CAPA_CASE' ? 'exceptions' : 'actions' } });
    }
}

watch([() => route.name, () => route.query.id], ([routeName, id]) => {
    if (routeName !== 'project-workspace') {
        workspace.value = undefined;
        projectPulse.value = undefined;
        selectedProjectId.value = undefined;
        return;
    }
    const projectId = Number(id);
    if (Number.isInteger(projectId) && projectId > 0) {
        if (projectId !== selectedProjectId.value || !workspace.value) {
            void openProject(projectId, false);
        }
    }
    else {
        workspace.value = undefined;
        selectedProjectId.value = undefined;
    }
}, { immediate: true });
watch(() => route.query.create, (value) => {
    if (value === '1' && auth.can('project:create')) {
        createDrawerVisible.value = true;
    }
}, { immediate: true });
onMounted(() => {
    void loadProjects();
});
</script>

<template>
    <!-- 项目查询、工作台与维护操作 -->
    <div class="stack project-page" v-loading="loading">
        <section v-if="!isWorkspaceRoute" class="panel project-directory">
            <header class="panel-header project-directory__header">
                <div class="project-directory__title"><span class="panel-title">项目管理</span><span class="project-directory__count">{{ projectPagination.total }} 条</span></div>
                <div class="toolbar-right">
                    <el-button v-if="auth.can('project:create')" type="primary" :icon="'Plus'" @click="openCreate">新建项目</el-button>
                </div>
            </header>
            <div class="project-metric-grid">
                <button
                    v-for="metric in projectMetricCards"
                    :key="metric.key"
                    type="button"
                    class="project-metric"
                    :class="[`project-metric--${metric.tone}`, { 'is-active': quickProjectFilter === metric.key }]"
                    @click="selectQuickProjectFilter(metric.key as 'ALL' | 'ACTIVE' | 'DUE' | 'RISK')"
                >
                    <span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.hint }}</small>
                </button>
            </div>
            <div class="project-filter-bar">
                <el-input v-model="projectFilters.keyword" class="project-filter-bar__keyword" clearable placeholder="搜索项目编号、客户、产品或负责人" />
                <el-select v-model="projectFilters.stage" clearable placeholder="阶段" class="project-filter-bar__select"><el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <el-select v-model="projectFilters.status" clearable placeholder="状态" class="project-filter-bar__select"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <el-select v-model="projectFilters.riskLevel" clearable placeholder="风险" class="project-filter-bar__select"><el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <el-select v-model="projectFilters.dueState" clearable placeholder="交期" class="project-filter-bar__select"><el-option v-for="item in dueOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <el-button v-if="hasProjectFilters" text type="primary" @click="clearProjectFilters">清除筛选</el-button>
            </div>
            <div v-if="projects.length" class="project-table-scroll">
                <!-- 数据表格 -->
                <el-table
                    :data="projects"
                    class="table--interactive project-table"
                    max-height="min(520px, 52dvh)"
                    highlight-current-row
                    :current-row-key="selectedProjectId"
                    row-key="id"
                    @row-click="openRow"
                >
                    <el-table-column label="项目" min-width="180">
                        <template #default="{ row }"><div class="project-cell"><strong>{{ row.projectNo }}</strong><span :title="row.productName">{{ row.productName }} · {{ row.quantity }} 件</span></div>
                        </template>
                    </el-table-column>
                    <el-table-column label="客户 / 负责人" min-width="155">
                        <template #default="{ row }">
                            <div class="project-cell"><strong :title="row.customerName">{{ row.customerName || '未填写客户' }}</strong><span>负责人：{{ row.ownerName || '未分配' }}</span></div>
                        </template>
                    </el-table-column>
                    <el-table-column label="进度" min-width="145">
                        <template #default="{ row }">
                            <div class="project-tags"><el-tag size="small" effect="plain">{{ stageLabel(row.stage) }}</el-tag><el-tag size="small" :type="statusTag(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></div>
                        </template>
                    </el-table-column>
                    <el-table-column label="目标交期" width="125">
                        <template #default="{ row }">
                            <div class="project-cell project-cell--date"><strong>{{ formatChineseDate(row.targetDate, '未设置') }}</strong><span :class="{ 'danger-text': row.daysLeft < 0 && isActiveProject(row), 'warning-text': row.daysLeft >= 0 && row.daysLeft <= 3 && isActiveProject(row) }">{{ dueText(row) }}</span></div>
                        </template>
                    </el-table-column>
                    <el-table-column label="样品" width="108">
                        <template #default="{ row }">
                            <el-tag size="small" :type="sampleStatusTag(row.sampleStatus)" effect="plain">{{ sampleStatusLabel(row.sampleStatus) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="风险 / 优先级" width="130">
                        <template #default="{ row }">
                            <div class="project-cell"><el-tag size="small" :type="riskTag(row.riskLevel)" effect="plain">{{ riskLevelLabel(row.riskLevel) }}</el-tag><span>{{ priorityLabel(row.priority) }}</span></div>
                        </template>
                    </el-table-column>
                    <el-table-column label="推进信号" width="150">
                        <template #default="{ row }">
                            <div class="project-cell"><span><el-tag v-if="row.blockerCount" type="danger" size="small" effect="plain">阻塞 {{ row.blockerCount }}</el-tag><el-tag v-else-if="row.pendingDecisionCount" type="warning" size="small" effect="plain">待决策 {{ row.pendingDecisionCount }}</el-tag><el-tag v-else type="info" size="small" effect="plain">暂无阻塞</el-tag></span><small>{{ projectActionText(row) }}</small></div>
                        </template>
                    </el-table-column>
                    <el-table-column label="变更" width="70" align="center"><template #default="{ row }">
                        <strong class="tabular-nums">{{ row.changeCount || 0 }}</strong>
                    </template></el-table-column>
                    <el-table-column label="操作" width="145">
                        <template #default="{ row }">
                            <el-button link type="primary" @click.stop="openRow(row)">打开工作台</el-button>
                            <el-button v-if="auth.can('project:manage') && row.status === 'DRAFT'" link type="warning" @click.stop="submitReview(row)">提交评审</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="projectPagination.pageNo"
                    :page-size="projectPagination.pageSize"
                    :total="projectPagination.total"
                    :loading="projectPagination.loading"
                    @update:page-no="projectPagination.goTo"
                    @update:page-size="projectPagination.changePageSize"
                />
            </div>
            <el-empty v-else-if="!projectPagination.total && !hasProjectFilters" description="暂无项目，创建第一个项目"><el-button v-if="auth.can('project:create')" type="primary" @click="openCreate">新建项目</el-button></el-empty>
            <el-empty v-else description="没有匹配的项目"><el-button type="primary" plain @click="clearProjectFilters">清除筛选</el-button></el-empty>
        </section>

        <section v-if="!isWorkspaceRoute" class="project-insights">
            <article class="panel project-insight-panel">
                <header class="project-insight-panel__header"><div><span class="panel-title">阶段分布</span><span class="subtle">按当前项目数据统计</span></div><el-button text type="primary" @click="selectStageFilter('')">查看全部</el-button></header>
                <div class="stage-distribution">
                    <button
                        v-for="item in stageDistribution"
                        :key="item.value"
                        type="button"
                        class="stage-distribution__row"
                        :class="{ 'is-active': projectFilters.stage === item.value }"
                        @click="selectStageFilter(item.value)"
                    >
                        <span>{{ item.label }}</span><strong>{{ item.count }}</strong><span class="stage-distribution__track"><i :style="{ width: `${stageShare(item.count)}%` }" /></span>
                    </button>
                </div>
            </article>
            <article class="panel project-insight-panel">
                <header class="project-insight-panel__header"><div><span class="panel-title">需关注项目</span><span class="subtle">优先处理交期、风险和评审</span></div><span class="subtle">{{ attentionProjects.length }} 项</span></header>
                <div v-if="attentionProjects.length" class="attention-list">
                    <button v-for="item in attentionProjects" :key="item.project.id" type="button" class="attention-row" @click="openRow(item.project)">
                        <span class="attention-row__main"><strong>{{ item.project.projectNo }}</strong><span>{{ item.project.productName }}</span></span><span class="attention-row__reason" :class="`attention-row__reason--${item.tone}`">{{ item.reason }}</span><span class="attention-row__owner">{{ item.project.ownerName || '未分配' }}</span>
                    </button>
                </div>
                <el-empty v-else description="暂无需要优先处理的项目" :image-size="56" />
            </article>
        </section>

        <div v-if="isWorkspaceRoute" class="toolbar project-workspace__toolbar">
            <el-button :icon="'Back'" @click="closeWorkspace">项目列表</el-button>
            <el-select
                v-model="selectedProjectId"
                class="project-workspace__selector"
                filterable
                clearable
                :loading="loading"
                placeholder="选择项目进入工作台"
                aria-label="选择工作台项目"
                @change="changeWorkspaceProject"
                >
                <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo} · ${item.productName}`" :value="item.id" />
            </el-select>
            <span v-if="activeProject" class="subtle project-workspace__context">{{ activeProject.customerName || '未填写客户' }}</span>
        </div>

        <div v-if="isWorkspaceRoute && workspace" class="project-workspace" v-loading="workspaceLoading">
            <main class="project-workspace__main">
                <WorkflowStrip
                    :steps="projectWorkflowSteps"
                    aria-label="项目协同流程"
                />
                <section class="panel project-summary">
                    <div class="project-summary__head">
                        <div>
                            <div class="status-line">
                                <h1>{{ activeProject?.productName }}</h1>
                                <el-tag type="primary" effect="dark">{{ statusLabel(activeProject?.status) }}</el-tag>
                            </div>
                            <p class="project-summary__number">{{ activeProject?.projectNo }}</p>
                        </div>
                        <div class="toolbar-right"><el-button v-if="primaryProjectAction && auth.can('project:manage')" type="primary" @click="handleProjectActionCommand(primaryProjectAction.code)">下一步：{{ primaryProjectAction.label }} <el-icon><ArrowRight /></el-icon></el-button><el-button v-if="auth.can('project:create')" plain @click="copyCurrentProject">复制项目</el-button><el-button v-if="auth.can('project:member:manage')" plain @click="managerTransferVisible = true">经理交接</el-button><el-dropdown v-if="auth.can('project:manage') && projectActions.length" @command="handleProjectActionCommand"><el-button plain>更多操作<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button><template #dropdown>
                            <el-dropdown-menu><el-dropdown-item v-for="action in projectActions" :key="action.code" :command="action.code">{{ action.label }}</el-dropdown-item></el-dropdown-menu>
                        </template></el-dropdown><el-button :icon="'Refresh'" @click="refreshWorkspace">刷新工作台</el-button></div>
                    </div>
                    <div class="project-summary__meta">
                        <div><span>客户名称</span><strong>{{ activeProject?.customerName || '-' }}</strong></div>
                        <div><span>目标交期</span><strong>{{ formatChineseDate(activeProject?.targetDate) }}</strong></div>
                        <div><span>项目经理</span><strong>{{ activeProject?.ownerName || '-' }}</strong></div>
                        <div><span>当前阶段</span><strong>{{ stageLabel(activeProject?.stage) }}</strong></div>
                    </div>
                </section>

                <section v-if="workspace.progress" class="panel project-mainline" aria-label="项目推进主线">
                    <div class="project-mainline__stage">
                        <span class="subtle">当前阶段</span>
                        <strong>{{ workspace.progress.stageLabel }}</strong>
                        <el-tag size="small" :type="workspace.progress.dueState === 'OVERDUE' ? 'danger' : workspace.progress.dueState === 'DUE_SOON' ? 'warning' : 'success'" effect="plain">
                            {{ workspace.progress.dueState === 'OVERDUE' ? '已超期' : workspace.progress.dueState === 'DUE_SOON' ? '三天内到期' : workspace.progress.dueState === 'NO_DATE' ? '未设交期' : '按计划推进' }}
                        </el-tag>
                    </div>
                    <div class="project-mainline__criterion"><span class="subtle">阶段完成条件</span><strong>{{ workspace.progress.completionCriteria }}</strong></div>
                    <div class="project-mainline__next"><span class="subtle">此刻优先动作</span><strong>{{ workspace.progress.nextActionLabel }}</strong><p>{{ workspace.progress.nextActionReason }}</p></div>
                    <el-button type="primary" @click="openProgressAction">立即处理 <el-icon><ArrowRight /></el-icon></el-button>
                </section>

                <section v-if="projectPulse" class="panel project-pulse">
                    <header class="project-pulse__header">
                        <div><span class="panel-title">项目脉搏</span><span class="subtle">{{ stageLabel(projectPulse.stage) }} · {{ statusLabel(projectPulse.status) }}</span></div>
                        <el-button link type="primary" @click="router.push({ name: 'actions', query: { tab: 'actions', projectId: activeProject?.id } })">进入行动中心</el-button>
                    </header>
                    <div class="project-pulse__metrics">
                        <div><span>关键阻塞</span><strong>{{ projectPulse.blockers.length }}</strong></div>
                        <div><span>待决策</span><strong>{{ projectPulse.pendingDecisions.length }}</strong></div>
                        <div><span>版本冲突</span><strong>{{ projectPulse.versionConflicts.length }}</strong></div>
                        <div><span>未关闭异常</span><strong>{{ projectPulse.openExceptions.length }}</strong></div>
                        <div><span>责任缺口</span><strong>{{ projectPulse.ownerGaps.length }}</strong></div>
                    </div>
                    <div class="project-pulse__lists">
                        <div class="project-pulse__list"><h3>关键阻塞</h3><button v-for="item in projectPulse.blockers.slice(0, 4)" :key="item.id" type="button" @click="openPulseAction(item)"><span>{{ item.title }}</span><el-tag size="small" :type="item.overdue ? 'danger' : item.priority === 'CRITICAL' ? 'danger' : 'warning'" effect="plain">{{ item.overdue ? '超期' : item.priority === 'CRITICAL' ? '关键' : '待处理' }}</el-tag></button><p v-if="!projectPulse.blockers.length" class="subtle">暂无关键阻塞</p></div>
                        <div class="project-pulse__list"><h3>待决策</h3><button v-for="item in projectPulse.pendingDecisions.slice(0, 4)" :key="item.id" type="button" @click="router.push({ name: 'actions', query: { tab: 'approvals', approvalId: item.id } })"><span>{{ item.nodeName }} · {{ item.businessType }}</span><el-tag size="small" :type="item.overdue ? 'danger' : 'warning'" effect="plain">{{ item.overdue ? '超期' : '待审批' }}</el-tag></button><p v-if="!projectPulse.pendingDecisions.length" class="subtle">暂无待决策项</p></div>
                        <div class="project-pulse__list"><h3>异常与责任</h3><button v-for="item in projectPulse.openExceptions.slice(0, 3)" :key="item.id" type="button" @click="router.push({ name: 'actions', query: { tab: 'exceptions', exceptionId: item.id } })"><span>{{ item.caseNo }} · {{ item.summary }}</span><el-tag size="small" type="warning" effect="plain">{{ item.status }}</el-tag></button><p v-for="gap in projectPulse.ownerGaps.slice(0, 2)" :key="gap.responsibilityCode" class="subtle">待补齐：{{ responsibilityLabel(gap.responsibilityCode) }}</p><p v-if="!projectPulse.openExceptions.length && !projectPulse.ownerGaps.length" class="subtle">暂无异常与责任缺口</p></div>
                    </div>
                </section>

                <section v-if="workspaceConflictPagination.total" class="workspace-alert">
                    <div class="workspace-alert__body">
                        <h2><el-icon><WarningFilled /></el-icon> 版本冲突警示</h2>
                        <p>检测到 {{ workspaceConflictPagination.total }} 个未完成任务仍引用旧版技术资料。请核对版本差异后同步，避免现场使用非受控版本。</p>
                        <el-button type="danger" @click="conflictDialogVisible = true">查看差异</el-button>
                        <el-button v-if="auth.can('document:sync')" plain type="danger" @click="syncTechnicalTasks">同步至当前技术包</el-button>
                    </div>
                </section>

                <section v-if="ownerGapCount" class="workspace-alert">
                    <div class="workspace-alert__body">
                        <h2><el-icon><WarningFilled /></el-icon> 待补齐专业负责人</h2>
                        <p>存量项目存在多名专业成员，系统没有猜测负责人。请由项目经理在“需求与成员”中指定唯一负责人。</p>
                        <el-tag v-for="gap in workspace.ownerGaps" :key="gap.responsibilityCode" type="warning" effect="plain" style="margin-right: 8px">{{ responsibilityLabel(gap.responsibilityCode) }}</el-tag>
                    </div>
                </section>

                <section class="panel milestone-panel">
                    <h2>项目里程碑</h2>
                    <div class="milestones">
                        <div
                            v-for="milestone in workspace.milestones"
                            :key="milestone.code"
                            class="milestone"
                            :class="`milestone--${milestone.state.toLowerCase()}`"
                        >
                            <span class="milestone__point"><el-icon v-if="milestone.state === 'COMPLETED'"><Check /></el-icon><el-icon v-else-if="milestone.state === 'CURRENT'"><LocationFilled /></el-icon></span>
                            <strong class="milestone__label">{{ milestone.label }}</strong>
                            <span class="milestone__date">{{ milestone.state === 'CURRENT' ? '进行中' : milestone.state === 'COMPLETED' ? dateText(milestone.occurredAt) : '-' }}</span>
                        </div>
                    </div>
                </section>

                <section class="panel workspace-tabs">
                    <el-tabs v-model="detailTab">
                        <el-tab-pane label="技术资料（技术包）" name="technical">
                            <div class="panel-body toolbar">
                                <div class="subtle">当前项目技术文件及其受控版本</div>
                                <el-button v-if="workspaceConflictPagination.total && auth.can('document:sync')" type="primary" :icon="'Refresh'" @click="syncTechnicalTasks">同步任务版本</el-button>
                            </div>
                            <el-table :data="workspace.detail.documents" size="small">
                                <el-table-column prop="fileName" label="文件名称" min-width="220" show-overflow-tooltip />
                                <el-table-column prop="versionNo" label="版本" width="90" />
                                <el-table-column prop="fileType" label="类型" width="110" />
                                <el-table-column label="状态" width="120"><template #default="{ row }">
                                    <el-tag size="small" :type="row.currentVersion ? 'success' : 'info'" effect="plain">{{ row.currentVersion ? '受控生效' : row.status }}</el-tag>
                                </template></el-table-column>
                                <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }">
                                    <el-button link type="primary" :disabled="!row.fileObjectId" @click="viewDocument(row)">查看</el-button><el-button link type="primary" :disabled="!row.fileObjectId" @click="downloadDocument(row)">下载</el-button><el-button link type="primary" @click="showDocumentQr(row.id)">二维码</el-button>
                                </template></el-table-column>
                            </el-table>
                            <PaginationBar
                                :page-no="workspaceDocumentPagination.pageNo"
                                :page-size="workspaceDocumentPagination.pageSize"
                                :total="workspaceDocumentPagination.total"
                                :loading="workspaceDocumentPagination.loading"
                                @update:page-no="workspaceDocumentPagination.goTo"
                                @update:page-size="workspaceDocumentPagination.changePageSize"
                            />
                            <el-empty v-if="!workspace.detail.documents.length" description="暂无技术文件" :image-size="64" />
                        </el-tab-pane>
                        <el-tab-pane label="样品确认" name="samples">
                            <el-table :data="workspace.detail.samples" size="small"><el-table-column prop="sampleNo" label="样品编号" width="160" /><el-table-column prop="purpose" label="用途" min-width="180" /><el-table-column prop="referencedVersion" label="引用版本" width="110" /><el-table-column prop="planFinishDate" label="计划完成" width="145"><template #default="{ row }">{{ formatChineseDate(row.planFinishDate) }}</template></el-table-column><el-table-column prop="confirmConclusion" label="确认结论" min-width="140" /></el-table>
                            <PaginationBar :page-no="workspaceSamplePagination.pageNo" :page-size="workspaceSamplePagination.pageSize" :total="workspaceSamplePagination.total" :loading="workspaceSamplePagination.loading" @update:page-no="workspaceSamplePagination.goTo" @update:page-size="workspaceSamplePagination.changePageSize" />
                            <el-empty v-if="!workspace.detail.samples.length" description="暂无样品确认记录" :image-size="64" />
                        </el-tab-pane>
                        <el-tab-pane label="变更记录" name="changes">
                            <el-table :data="workspace.detail.changes" size="small"><el-table-column prop="changeNo" label="变更单号" width="160" /><el-table-column prop="changeType" label="类型" width="110" /><el-table-column prop="afterContent" label="变更内容" min-width="250" show-overflow-tooltip /><el-table-column prop="status" label="状态" width="110" /><el-table-column prop="delayDays" label="影响天数" width="100" /></el-table>
                            <PaginationBar :page-no="workspaceChangePagination.pageNo" :page-size="workspaceChangePagination.pageSize" :total="workspaceChangePagination.total" :loading="workspaceChangePagination.loading" @update:page-no="workspaceChangePagination.goTo" @update:page-size="workspaceChangePagination.changePageSize" />
                            <el-empty v-if="!workspace.detail.changes.length" description="暂无变更记录" :image-size="64" />
                        </el-tab-pane>
                        <el-tab-pane label="项目事件" name="timeline">
                            <el-timeline class="panel-body" v-if="workspace.detail.timeline.length"><el-timeline-item v-for="item in workspace.detail.timeline" :key="item.id" :timestamp="dateText(item.occurredAt)"><strong>{{ item.title }}</strong><div class="subtle">{{ item.summary }} {{ item.operatorName ? `· ${item.operatorName}` : '' }}</div></el-timeline-item></el-timeline>
                            <PaginationBar :page-no="workspaceTimelinePagination.pageNo" :page-size="workspaceTimelinePagination.pageSize" :total="workspaceTimelinePagination.total" :loading="workspaceTimelinePagination.loading" @update:page-no="workspaceTimelinePagination.goTo" @update:page-size="workspaceTimelinePagination.changePageSize" />
                            <el-empty v-if="!workspaceTimelinePagination.total" description="暂无项目事件" :image-size="64" />
                        </el-tab-pane>
                        <el-tab-pane label="状态历史" name="status-history">
                            <el-table :data="statusHistory" size="small"><el-table-column prop="occurredAt" label="发生时间" width="180"><template #default="{ row }">
                                {{ dateText(row.occurredAt) }}
                            </template></el-table-column><el-table-column prop="actionCode" label="动作" width="180" /><el-table-column prop="beforeStatus" label="变更前" width="130" /><el-table-column prop="afterStatus" label="变更后" width="130" /><el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip /></el-table>
                            <PaginationBar :page-no="workspaceStatusPagination.pageNo" :page-size="workspaceStatusPagination.pageSize" :total="workspaceStatusPagination.total" :loading="workspaceStatusPagination.loading" @update:page-no="workspaceStatusPagination.goTo" @update:page-size="workspaceStatusPagination.changePageSize" />
                            <el-empty v-if="!statusHistory.length" description="暂无状态历史" :image-size="64" />
                        </el-tab-pane>
                        <el-tab-pane label="需求与成员" name="configuration">
                            <div class="panel-body stack">
                                <section>
                                    <div v-if="auth.can('project:requirement:manage')" class="toolbar-left inline-form">
                                        <el-select v-model="requirementForm.category" style="width: 120px"><el-option label="尺寸" value="SIZE" /><el-option label="材料" value="MATERIAL" /><el-option label="性能" value="PERFORMANCE" /><el-option label="外观" value="APPEARANCE" /><el-option label="客户" value="CUSTOMER" /><el-option label="通用" value="GENERAL" /></el-select>
                                        <el-input
                                            v-model="requirementForm.content"
                                            placeholder="需求内容"
                                            style="width: min(360px, 100%)"
                                            @keyup.enter="addRequirement"
                                        />
                                        <el-button type="primary" @click="addRequirement">新增需求</el-button>
                                    </div>
                                    <el-table :data="workspace.detail.requirements" size="small" style="margin-top: 14px"><el-table-column prop="category" label="分类" width="100" /><el-table-column prop="content" label="内容" min-width="240" /><el-table-column label="确认状态" width="150"><template #default="{ row }">
                                        <el-tag size="small" :type="confirmTag(row.confirmStatus)" effect="plain">{{ row.confirmStatus }}</el-tag>
                                    </template></el-table-column><el-table-column label="操作" width="180"><template #default="{ row }">
                                    <el-button v-if="auth.can('project:requirement:manage')" link type="success" @click="confirmRequirement(row, 'CUSTOMER_CONFIRMED')">客户确认</el-button><el-button v-if="auth.can('project:requirement:manage')" link type="warning" @click="confirmRequirement(row, 'INTERNAL_CONFIRMED')">内部确认</el-button>
                                </template></el-table-column></el-table>
                                <PaginationBar :page-no="workspaceRequirementPagination.pageNo" :page-size="workspaceRequirementPagination.pageSize" :total="workspaceRequirementPagination.total" :loading="workspaceRequirementPagination.loading" @update:page-no="workspaceRequirementPagination.goTo" @update:page-size="workspaceRequirementPagination.changePageSize" />
                            </section>
                            <section>
                                <div v-if="auth.can('project:member:manage')" class="toolbar-left inline-form">
                                    <el-select v-model="memberForm.userId" filterable placeholder="从账号目录选择成员" style="width: 220px" @change="resetMemberResponsibilities"><el-option v-for="candidate in memberCandidates" :key="candidate.userId" :label="`${candidate.nickname} · ${candidate.departmentName || '未分部门'}`" :value="candidate.userId" /></el-select>
                                    <el-select v-model="memberForm.responsibilityCodes" multiple collapse-tags placeholder="项目职责" style="width: 220px" @change="resetPrimary(memberForm)"><el-option v-for="code in selectedMemberCandidate?.allowedResponsibilityCodes || []" :key="code" :label="responsibilityLabel(code)" :value="code" /></el-select>
                                    <el-select v-model="memberForm.primaryResponsibilityCode" placeholder="主职责" style="width: 140px"><el-option v-for="code in memberForm.responsibilityCodes" :key="code" :label="responsibilityLabel(code)" :value="code" /></el-select>
                                    <el-button type="primary" @click="addMember">添加成员</el-button>
                                    <el-button v-if="auth.can('customer:invite')" plain @click="customerAuthorizationVisible = true">客户联系人授权</el-button>
                                </div>
                                <el-table :data="workspace.detail.members" size="small" style="margin-top: 14px"><el-table-column prop="memberName" label="成员" width="130" /><el-table-column label="项目职责" min-width="200"><template #default="{ row }">
                                    <el-tag v-for="code in row.responsibilityCodes || [row.projectRole]" :key="code" size="small" effect="plain" style="margin-right:4px">{{ responsibilityLabel(code) }}</el-tag>
                                </template></el-table-column><el-table-column prop="departmentName" label="部门" min-width="140" /><el-table-column label="账号状态" width="110"><template #default="{ row }">
                                <el-tag size="small" :type="row.accountStatus === 'ENABLED' ? 'success' : 'info'">{{ row.accountStatus || '-' }}</el-tag>
                            </template></el-table-column><el-table-column prop="status" label="成员状态" width="100" /><el-table-column v-if="auth.can('project:member:manage')" label="操作" width="180" fixed="right"><template #default="{ row }">
                            <el-button v-if="row.status === 'ACTIVE'" link type="primary" @click="editMember(row)">调整职责</el-button><el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="changeMemberStatus(row)">移除</el-button><el-button v-else link type="success" @click="changeMemberStatus(row, true)">恢复</el-button>
                        </template></el-table-column></el-table>
                        <PaginationBar :page-no="workspaceMemberPagination.pageNo" :page-size="workspaceMemberPagination.pageSize" :total="workspaceMemberPagination.total" :loading="workspaceMemberPagination.loading" @update:page-no="workspaceMemberPagination.goTo" @update:page-size="workspaceMemberPagination.changePageSize" />
                    </section>
                    <section>
                        <div class="panel-header" style="padding: 0 0 10px"><span class="panel-title">客户联系人与项目授权</span><el-button v-if="auth.can('customer:manage')" link type="primary" @click="customerContactVisible = true">新增联系人</el-button><el-button v-if="auth.can('customer:invite')" link type="primary" @click="customerAuthorizationVisible = true">授权联系人</el-button></div>
                        <el-alert title="客户不属于内部账号、部门或项目成员。只有已启用联系人获得当前项目授权后，才能接收样品确认链接。" type="info" :closable="false" style="margin-bottom: 12px" />
                        <el-table :data="customerContacts" size="small"><el-table-column prop="contactName" label="联系人" width="140" /><el-table-column prop="positionName" label="岗位" width="130" /><el-table-column prop="phone" label="电话" min-width="130" /><el-table-column prop="email" label="邮箱" min-width="180" /><el-table-column prop="status" label="状态" width="100" /></el-table>
                        <PaginationBar :page-no="workspaceContactPagination.pageNo" :page-size="workspaceContactPagination.pageSize" :total="workspaceContactPagination.total" :loading="workspaceContactPagination.loading" @update:page-no="workspaceContactPagination.goTo" @update:page-size="workspaceContactPagination.changePageSize" />
                        <el-table :data="customerAuthorizations" size="small" style="margin-top: 12px"><el-table-column prop="contactName" label="已授权联系人" min-width="160" /><el-table-column prop="validUntil" label="有效至" width="170"><template #default="{ row }">
                            {{ dateText(row.validUntil) }}
                        </template></el-table-column><el-table-column prop="status" label="授权状态" width="110"><template #default="{ row }">
                        <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
                    </template></el-table-column><el-table-column prop="revokeReason" label="撤销原因" min-width="180" show-overflow-tooltip /><el-table-column v-if="auth.can('customer:invite')" label="操作" width="100"><template #default="{ row }">
                    <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="revokeCustomerAuthorization(row)">撤销</el-button>
                </template></el-table-column></el-table>
                        <PaginationBar :page-no="workspaceAuthorizationPagination.pageNo" :page-size="workspaceAuthorizationPagination.pageSize" :total="workspaceAuthorizationPagination.total" :loading="workspaceAuthorizationPagination.loading" @update:page-no="workspaceAuthorizationPagination.goTo" @update:page-size="workspaceAuthorizationPagination.changePageSize" />
            </section>
        </div>
    </el-tab-pane>
    </el-tabs>
    </section>
    </main>

    <aside class="project-workspace__side">
        <section v-if="currentRisk" class="panel risk-panel">
            <div class="risk-panel__score"><h2><el-icon><Warning /></el-icon> 交期风险评估</h2><strong class="risk-panel__level">{{ currentRisk.level === 'SERIOUS' || currentRisk.level === 'HIGH' ? '高风险' : '中风险' }}</strong></div>
            <div class="risk-panel__content"><p class="subtle">风险诱因摘要</p><p>{{ riskReasons.join('；') || currentRisk.suggestion || '系统尚未记录风险诱因。' }}</p><el-button style="width:100%; margin-top: 18px" @click="riskDrawerVisible = true">查看风险明细与处置</el-button></div>
        </section>
        <section v-else class="panel risk-panel"><div class="risk-panel__score"><h2><el-icon><CircleCheck /></el-icon> 交期风险评估</h2><strong class="risk-panel__level" style="color: var(--nso-success)">风险可控</strong></div><div class="risk-panel__content"><p>当前项目没有待处置风险。</p></div></section>
        <FlowQrCard
            v-if="workspace.flowQrCode"
            :code="workspace.flowQrCode.code"
            :payload="workspace.flowQrCode.payload"
            :subtitle="`项目 ID：${activeProject?.projectNo || '-'}`"
        />
        <section class="panel"><header class="panel-header"><span class="panel-title">常用操作</span></header><div class="panel-body quick-actions"><el-button v-if="auth.can('change:create')" text :icon="'ArrowRight'" @click="router.push({ name: 'changes' })">发起设计变更（ECR）</el-button><el-button v-if="auth.can('sample:view')" text :icon="'ArrowRight'" @click="router.push({ name: 'samples' })">记录样品测试结果</el-button><el-button v-if="auth.can('task:view')" text :icon="'ArrowRight'" @click="router.push({ name: 'tasks-execution' })">分配加工任务</el-button></div></section>
    </aside>
    </div>

    <section v-else-if="isWorkspaceRoute" class="panel project-workspace__empty" v-loading="workspaceLoading"><el-empty description="选择项目后打开协同工作台" :image-size="80" /></section>

    <el-drawer v-model="createDrawerVisible" title="新建项目" size="min(520px, 100%)" @closed="handleCreateDrawerClosed">
        <el-form label-position="top" @submit.prevent="createProject">
            <el-form-item label="客户"><el-select v-model="form.customerId" placeholder="选择已有客户" clearable filterable style="width:100%"><el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" /></el-select><el-button v-if="auth.can('customer:manage')" link type="primary" @click="customerDialogVisible = true">新建客户</el-button></el-form-item>
            <el-form-item label="或输入新客户名称"><el-input v-model="form.customerName" /></el-form-item>
            <el-form-item label="产品/治具名称" required><el-input v-model="form.productName" /></el-form-item>
            <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" :controls="false" /></el-form-item>
            <el-form-item label="目标交期"><el-date-picker v-model="form.targetDate" format="YYYY年MM月DD日" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
            <el-form-item label="项目经理" required><el-select v-model="form.managerUserId" filterable placeholder="选择项目经理账号" style="width:100%"><el-option v-for="manager in managerCandidates" :key="manager.userId" :label="`${manager.nickname} · ${manager.departmentName || '未分部门'}`" :value="manager.userId" /></el-select><div class="subtle">项目经理由账号目录确定；姓名由账号自动带出，不能手填。</div></el-form-item>
            <el-form-item label="优先级"><el-radio-group v-model="form.priority"><el-radio-button label="HIGH">高</el-radio-button><el-radio-button label="MEDIUM">中</el-radio-button><el-radio-button label="LOW">低</el-radio-button></el-radio-group></el-form-item>
            <el-button native-type="submit" type="primary" style="width:100%">创建项目</el-button>
        </el-form>
    </el-drawer>

    <!-- 操作弹窗 -->
    <el-dialog v-model="customerDialogVisible" title="新建客户" width="min(520px, 92vw)"><el-form label-position="top"><el-form-item label="客户名称" required><el-input v-model="customerForm.name" /></el-form-item><el-form-item label="所属行业"><el-input v-model="customerForm.industry" /></el-form-item><el-form-item label="联系人"><el-input v-model="customerForm.contactName" /></el-form-item><el-form-item label="联系电话"><el-input v-model="customerForm.phone" /></el-form-item></el-form><template #footer>
        <el-button @click="customerDialogVisible = false">取消</el-button><el-button type="primary" :loading="customerSaving" @click="createCustomer">保存客户</el-button>
    </template></el-dialog>

    <el-dialog v-model="memberEditVisible" title="调整项目成员职责" width="min(520px, 92vw)"><el-form label-position="top"><el-form-item label="成员"><el-input :model-value="memberEditForm.memberName" disabled /></el-form-item><el-form-item label="项目职责" required><el-select v-model="memberEditForm.responsibilityCodes" multiple style="width:100%" @change="resetPrimary(memberEditForm)"><el-option v-for="code in editingMemberCandidate?.allowedResponsibilityCodes || memberEditForm.responsibilityCodes" :key="code" :label="responsibilityLabel(code)" :value="code" /></el-select></el-form-item><el-form-item label="主职责" required><el-select v-model="memberEditForm.primaryResponsibilityCode" style="width:100%"><el-option v-for="code in memberEditForm.responsibilityCodes" :key="code" :label="responsibilityLabel(code)" :value="code" /></el-select></el-form-item></el-form><template #footer>
        <el-button @click="memberEditVisible = false">取消</el-button><el-button type="primary" @click="saveMemberResponsibilities">保存职责</el-button>
    </template></el-dialog>

    <el-dialog v-model="managerTransferVisible" title="项目经理交接" width="min(520px, 92vw)"><el-alert title="交接前系统会校验原项目经理未完成任务和风险处置项；交接记录将写入项目事件与交接历史。" type="warning" :closable="false" style="margin-bottom:16px" /><el-form label-position="top"><el-form-item label="新项目经理" required><el-select v-model="managerTransferForm.managerUserId" filterable style="width:100%"><el-option v-for="manager in managerCandidates" :key="manager.userId" :label="`${manager.nickname} · ${manager.departmentName || '未分部门'}`" :value="manager.userId" /></el-select></el-form-item><el-form-item label="交接原因" required><el-input v-model="managerTransferForm.reason" type="textarea" :rows="3" placeholder="例如：人员调岗，由新负责人继续协同" /></el-form-item></el-form><template #footer>
        <el-button @click="managerTransferVisible = false">取消</el-button><el-button type="primary" @click="transferManager">确认交接</el-button>
    </template></el-dialog>

    <el-dialog v-model="customerAuthorizationVisible" title="客户联系人项目授权" width="min(520px, 92vw)"><el-alert title="授权不创建任何登录账号。联系人仅能通过短期样品确认链接访问经脱敏的确认资料；撤销授权会使链接立即失效。" type="info" :closable="false" style="margin-bottom:16px" /><el-form label-position="top"><el-form-item label="客户联系人" required><el-select v-model="customerAuthorizationForm.contactId" filterable style="width:100%"><el-option v-for="contact in customerContacts.filter(item => item.status === 'ENABLED')" :key="contact.id" :label="`${contact.contactName} · ${contact.positionName || '客户联系人'}`" :value="contact.id" /></el-select></el-form-item><el-form-item label="授权有效期（可选）"><el-date-picker v-model="customerAuthorizationForm.validUntil" type="datetime" format="YYYY年MM月DD日 HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item></el-form><template #footer>
        <el-button @click="customerAuthorizationVisible = false">取消</el-button><el-button type="primary" @click="authorizeCustomer">确认授权</el-button>
    </template></el-dialog>

    <el-dialog v-model="customerContactVisible" title="新增客户联系人" width="min(520px, 92vw)"><el-form label-position="top"><el-form-item label="联系人姓名" required><el-input v-model="customerContactForm.contactName" /></el-form-item><el-form-item label="岗位"><el-input v-model="customerContactForm.positionName" /></el-form-item><el-form-item label="电话"><el-input v-model="customerContactForm.phone" /></el-form-item><el-form-item label="邮箱"><el-input v-model="customerContactForm.email" /></el-form-item></el-form><template #footer>
        <el-button @click="customerContactVisible = false">取消</el-button><el-button type="primary" @click="createCustomerContact">保存联系人</el-button>
    </template></el-dialog>

    <el-drawer v-model="riskDrawerVisible" title="风险处置单" size="min(560px, 100%)">
        <div v-if="currentRisk" class="stack"><el-alert :title="`${currentRisk.level} · ${riskReasons.join('；') || currentRisk.suggestion || '项目风险'}`" :type="riskTag(currentRisk.level) === 'danger' ? 'error' : 'warning'" :closable="false" show-icon />
            <section><h3>风险因子明细</h3><el-table :data="riskDetails" size="small"><el-table-column prop="factorName" label="因子" min-width="130" /><el-table-column prop="rawValue" label="原始指标" min-width="110" /><el-table-column prop="scoreDelta" label="分值" width="80"><template #default="{ row }">
                {{ row.scoreDelta > 0 ? '+' : '' }}{{ row.scoreDelta }}
            </template></el-table-column><el-table-column prop="scoreCap" label="上限" width="70" /><el-table-column label="命中" width="70"><template #default="{ row }">
            <el-tag size="small" :type="row.matched ? 'danger' : 'info'">{{ row.matched ? '是' : '否' }}</el-tag>
        </template></el-table-column></el-table>
            <PaginationBar :page-no="workspaceRiskDetailPagination.pageNo" :page-size="workspaceRiskDetailPagination.pageSize" :total="workspaceRiskDetailPagination.total" :loading="workspaceRiskDetailPagination.loading" @update:page-no="workspaceRiskDetailPagination.goTo" @update:page-size="workspaceRiskDetailPagination.changePageSize" />
            <el-empty v-if="!riskDetails.length" description="暂无风险因子明细" :image-size="48" /></section>
        <section><h3>已有处置项</h3><div v-if="workspace?.riskActions.length" class="risk-action-list"><div v-for="action in workspace?.riskActions" :key="action.id" class="risk-action"><strong>{{ action.actionPlan }}</strong><div class="risk-action__meta"><span>{{ action.responsibleName || `用户 ${action.responsibleUserId}` }} · {{ dateText(action.planFinishTime) }}</span><el-tag size="small" :type="action.status === 'CLOSED' ? 'success' : 'warning'">{{ action.status === 'CLOSED' ? '已关闭' : '待完成' }}</el-tag></div><p v-if="action.closeSummary" class="subtle">总结：{{ action.closeSummary }}</p><el-button v-if="action.status !== 'CLOSED' && auth.can('risk:dispose')" link type="primary" @click="prepareClose(action)">关闭处置项</el-button></div></div>
            <PaginationBar :page-no="workspaceRiskActionPagination.pageNo" :page-size="workspaceRiskActionPagination.pageSize" :total="workspaceRiskActionPagination.total" :loading="workspaceRiskActionPagination.loading" @update:page-no="workspaceRiskActionPagination.goTo" @update:page-size="workspaceRiskActionPagination.changePageSize" />
            <el-empty v-if="!workspaceRiskActionPagination.total" description="暂无风险处置项" :image-size="56" /></section>
        <el-divider />
        <section v-if="auth.can('risk:dispose')"><h3>新建处置项</h3><el-form label-position="top"><el-form-item label="措施" required><el-input v-model="actionForm.actionPlan" type="textarea" :rows="3" placeholder="明确处置措施及检查标准" /></el-form-item><el-form-item label="责任人" required><el-select v-model="actionForm.responsibleUserId" placeholder="选择项目成员" style="width:100%"><el-option v-for="member in activeMembers" :key="member.userId" :label="`${member.memberName} · ${member.projectRole}`" :value="member.userId" /></el-select></el-form-item><el-form-item label="计划完成时间" required><el-date-picker v-model="actionForm.planFinishTime" type="datetime" format="YYYY年MM月DD日 HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item><el-button type="primary" @click="createRiskAction">创建处置项</el-button></el-form></section>
        <section v-if="closeForm.id"><el-divider /><h3>关闭处置项</h3><el-input v-model="closeForm.closeSummary" type="textarea" :rows="3" placeholder="填写关闭总结和验证结果" /><el-button type="primary" style="margin-top: 12px" @click="closeRiskAction">确认关闭</el-button></section>
    </div>
    </el-drawer>

    <el-dialog v-model="conflictDialogVisible" title="技术版本差异" width="min(760px, 94vw)"><p class="subtle">活跃任务引用的版本与当前受控技术包不一致；同步只影响未完成任务。</p><el-table :data="workspace?.versionConflicts || []"><el-table-column prop="taskNo" label="任务编号" width="150" /><el-table-column prop="taskTitle" label="任务名称" min-width="180" /><el-table-column prop="referencedVersion" label="任务引用版本" width="130" /><el-table-column prop="currentVersion" label="当前受控版本" width="130" /></el-table>
        <PaginationBar :page-no="workspaceConflictPagination.pageNo" :page-size="workspaceConflictPagination.pageSize" :total="workspaceConflictPagination.total" :loading="workspaceConflictPagination.loading" @update:page-no="workspaceConflictPagination.goTo" @update:page-size="workspaceConflictPagination.changePageSize" />
        <template #footer>
        <el-button @click="conflictDialogVisible = false">取消</el-button><el-button v-if="auth.can('document:sync')" type="primary" @click="syncTechnicalTasks">同步未完成任务</el-button>
    </template></el-dialog>
    <el-dialog v-model="documentQrVisible" title="技术文件二维码" width="min(380px, 92vw)"><FlowQrCard v-if="documentQr" compact :code="documentQr.code" :payload="documentQr.payload" title="受控技术文件" subtitle="扫码后经权限校验查看" /></el-dialog>
    </div>
</template>

<style scoped>
/* 项目工作台局部样式。 */
.project-directory {
    min-width: 0;
    overflow: visible;
}

.project-directory__header {
    flex-wrap: wrap;
    gap: 12px 18px;
}

.project-directory__title {
    display: flex;
    align-items: baseline;
    flex: 1 1 180px;
    gap: 10px;
    min-width: 0;
}

.project-directory__header .toolbar-right {
    flex: 0 0 auto;
}

.project-directory__count {
    color: #86909c;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
}

.project-metric-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1px;
    padding: 1px;
    background: #e5e6eb;
}

.project-metric {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 4px 12px;
    min-width: 0;
    padding: 14px 16px 13px;
    border: 0;
    background: #fff;
    color: #4e5969;
    text-align: left;
    cursor: pointer;
    transition: background-color 180ms ease, box-shadow 180ms ease;
}

.project-metric:hover,
    .project-metric:focus-visible,
    .project-metric.is-active {
    background: #f5f8ff;
    box-shadow: inset 0 -2px 0 var(--nso-blue);
    outline: none;
}

.project-metric span {
    overflow: hidden;
    color: #86909c;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.project-metric strong {
    grid-row: 1 / span 2;
    align-self: center;
    color: #1d2129;
    font-size: 26px;
    font-variant-numeric: tabular-nums;
    line-height: 1;
}

.project-metric small {
    overflow: hidden;
    color: #a9b2c0;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.project-metric--blue strong {
    color: var(--nso-blue-dark);
}

.project-metric--orange strong {
    color: var(--nso-warning);
}

.project-metric--red strong {
    color: var(--nso-danger);
}

.project-filter-bar {
    display: grid;
    grid-template-columns: minmax(260px, 360px) repeat(4, minmax(112px, 144px)) max-content;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f1f3;
}

.project-filter-bar__keyword,
    .project-filter-bar__select {
    min-width: 0;
    width: 100%;
}

.project-filter-bar > .el-button {
    justify-self: end;
    white-space: nowrap;
}

.project-table-scroll {
    max-width: 100%;
    overflow-x: auto;
}

.project-table {
    min-width: 1010px;
    width: 100%;
}

.project-table :deep(.el-table__cell) {
    vertical-align: top;
}

.project-cell {
    display: grid;
    min-width: 0;
    gap: 4px;
    line-height: 1.35;
}

.project-cell strong,
    .project-cell span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.project-cell strong {
    color: #1d2129;
    font-weight: 650;
}

.project-cell span {
    color: #86909c;
    font-size: 12px;
}

.project-cell--date strong {
    font-variant-numeric: tabular-nums;
}

.project-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
}

.tabular-nums {
    font-variant-numeric: tabular-nums;
}

.project-insights {
    display: grid;
    grid-template-columns: minmax(300px, .72fr) minmax(420px, 1fr);
    gap: 14px;
}

.project-insight-panel {
    min-width: 0;
}

.project-insight-panel__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 46px;
    gap: 12px;
    padding: 0 16px;
    border-bottom: 1px solid #f0f1f3;
}

.project-insight-panel__header > div {
    display: flex;
    align-items: baseline;
    gap: 10px;
    min-width: 0;
}

.project-insight-panel__header .subtle {
    overflow: hidden;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.stage-distribution {
    display: grid;
    gap: 2px;
    padding: 10px 16px 12px;
}

.stage-distribution__row {
    display: grid;
    grid-template-columns: 76px 32px minmax(0, 1fr);
    align-items: center;
    gap: 10px;
    min-width: 0;
    padding: 7px 0;
    border: 0;
    background: transparent;
    color: #4e5969;
    font-size: 12px;
    text-align: left;
    cursor: pointer;
}

.stage-distribution__row:hover,
    .stage-distribution__row.is-active {
    color: var(--nso-blue-dark);
}

.stage-distribution__row strong {
    color: #1d2129;
    font-variant-numeric: tabular-nums;
    text-align: right;
}

.stage-distribution__track {
    height: 6px;
    overflow: hidden;
    background: #f0f1f3;
}

.stage-distribution__track i {
    display: block;
    height: 100%;
    background: var(--nso-blue);
}

.attention-list {
    display: grid;
}

.attention-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(104px, 132px) 112px;
    align-items: center;
    gap: 12px;
    min-width: 0;
    padding: 11px 16px;
    border: 0;
    border-bottom: 1px solid var(--nso-divider);
    background: transparent;
    color: #4e5969;
    text-align: left;
    cursor: pointer;
}

.attention-row:last-child {
    border-bottom: 0;
}

.attention-row:hover,
    .attention-row:focus-visible {
    background: #f7f9fc;
    outline: none;
}

.attention-row__main {
    display: grid;
    min-width: 0;
    gap: 3px;
}

.attention-row__main strong,
    .attention-row__main span,
    .attention-row__owner,
    .attention-row__reason {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.attention-row__main strong {
    color: #1d2129;
    font-size: 12px;
}

.attention-row__main span,
    .attention-row__owner {
    color: #86909c;
    font-size: 12px;
}

.attention-row__reason {
    font-size: 12px;
    text-align: right;
}

.attention-row__owner {
    text-align: right;
}

.attention-row__reason--danger {
    color: var(--nso-danger);
}

.attention-row__reason--warning {
    color: var(--nso-warning);
}

.attention-row__reason--info {
    color: var(--nso-blue-dark);
}

.project-pulse {
    overflow: hidden;
}

.project-pulse__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-height: 48px;
    padding: 0 16px;
    border-bottom: 1px solid #f0f1f3;
}

.project-pulse__header > div {
    display: flex;
    align-items: baseline;
    gap: 10px;
    min-width: 0;
}

.project-pulse__metrics {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    border-bottom: 1px solid #f0f1f3;
}

.project-pulse__metrics > div {
    min-width: 0;
    padding: 12px 16px;
    border-right: 1px solid #f0f1f3;
}

.project-pulse__metrics > div:last-child {
    border-right: 0;
}

.project-pulse__metrics span,
.project-pulse__metrics strong {
    display: block;
}

.project-pulse__metrics span {
    overflow: hidden;
    color: #86909c;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.project-pulse__metrics strong {
    margin-top: 4px;
    color: #1d2129;
    font-size: 20px;
    font-variant-numeric: tabular-nums;
    line-height: 1;
}

.project-pulse__lists {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
}

.project-pulse__list {
    min-width: 0;
    padding: 13px 16px 15px;
    border-right: 1px solid #f0f1f3;
}

.project-pulse__list:last-child {
    border-right: 0;
}

.project-pulse__list h3 {
    margin: 0 0 8px;
    color: #4e5969;
    font-size: 12px;
    font-weight: 600;
}

.project-pulse__list button {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    min-width: 0;
    gap: 8px;
    padding: 5px 0;
    border: 0;
    background: transparent;
    color: #4e5969;
    cursor: pointer;
    text-align: left;
}

.project-pulse__list button:hover,
.project-pulse__list button:focus-visible {
    color: var(--nso-blue-dark);
    outline: none;
}

.project-pulse__list button span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.project-pulse__list p {
    margin: 5px 0 0;
    font-size: 12px;
}

@media (max-width: 1120px) {
    .project-filter-bar {
        grid-template-columns: minmax(240px, 1fr) repeat(2, minmax(112px, 144px));
    }

    .project-filter-bar > .el-button {
        justify-self: start;
    }
}

@media (max-width: 1024px) {
    .project-metric-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .project-insights {
        grid-template-columns: 1fr;
    }

    .project-pulse__metrics {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .project-pulse__metrics > div:nth-child(3) {
        border-right: 0;
    }

    .project-pulse__metrics > div:nth-child(n + 4) {
        border-top: 1px solid #f0f1f3;
    }

    .project-pulse__lists {
        grid-template-columns: 1fr;
    }

    .project-pulse__list {
        border-right: 0;
        border-bottom: 1px solid #f0f1f3;
    }

    .project-pulse__list:last-child {
        border-bottom: 0;
    }
}

@media (max-width: 680px) {
    .project-directory__header,
        .project-insight-panel__header {
        align-items: flex-start;
        flex-direction: column;
        padding-top: 10px;
        padding-bottom: 10px;
    }

    .project-directory__title {
        flex: 0 0 auto;
    }

    .project-directory__header .toolbar-right {
        width: 100%;
        margin-left: 0;
    }

    .project-directory__header .toolbar-right .el-button {
        width: 100%;
    }

    .project-filter-bar {
        align-items: stretch;
        grid-template-columns: 1fr;
    }

    .project-filter-bar__keyword,
        .project-filter-bar__select {
        width: 100%;
    }

    .attention-row {
        grid-template-columns: minmax(0, 1fr) auto;
    }

    .attention-row__reason,
        .attention-row__owner {
        text-align: left;
    }

    .attention-row__owner {
        grid-column: 1 / -1;
    }

    .project-pulse__header {
        align-items: flex-start;
        flex-direction: column;
        padding-top: 10px;
        padding-bottom: 10px;
    }

    .project-pulse__metrics {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .project-pulse__metrics > div,
    .project-pulse__metrics > div:nth-child(3) {
        border-right: 1px solid #f0f1f3;
        border-top: 1px solid #f0f1f3;
    }

    .project-pulse__metrics > div:nth-child(1),
    .project-pulse__metrics > div:nth-child(2) {
        border-top: 0;
    }

    .project-pulse__metrics > div:nth-child(even),
    .project-pulse__metrics > div:last-child {
        border-right: 0;
    }
}
</style>
