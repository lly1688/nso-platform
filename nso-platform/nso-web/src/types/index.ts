// 管理端接口响应与业务模型类型。
export interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
    traceId: string;
    timestamp: string;
}

export interface UserProfile {
    id: number;
    username: string;
    nickname: string;
    phone?: string | null;
    email?: string | null;
    gender: 'MALE' | 'FEMALE' | 'UNSPECIFIED';
    departmentName: string;
    roles: string[];
    avatarUrl?: string | null;
    version: number;
    createdAt?: string;
}

export interface RuleBlockDetail {
    ruleCode?: string;
    reason?: string;
    currentValue?: string;
    expectedValue?: string;
    action?: string;
}

export interface PageResult<T> {
    list: T[];
    total: number;
    pageNo: number;
    pageSize: number;
}

export interface PageParams {
    pageNo?: number;
    pageSize?: number;
}

// 客户、项目与成员协同模型。
export interface Customer {
    id: number;
    customerCode: string;
    name: string;
    industry: string;
    contactName: string;
    phone: string;
    status: string;
}

export interface CustomerContact {
    id: number;
    customerId: number;
    contactName: string;
    phone?: string;
    email?: string;
    positionName?: string;
    preferredChannel?: string;
    status: string;
}

export interface ExternalProjectAccess {
    id: number;
    identityId: number;
    customerId: number;
    contactId: number;
    contactName: string;
    projectId: number;
    projectNo: string;
    status: string;
    validUntil?: string;
    grantedAt?: string;
    revokedAt?: string;
    revokeReason?: string;
}

export interface Project {
    id: number;
    projectNo: string;
    customerId?: number;
    customerName: string;
    productName: string;
    quantity: number;
    targetDate: string;
    ownerName: string;
    status: string;
    stage: string;
    priority: string;
    riskLevel: string;
    sampleStatus: string;
    changeCount: number;
    daysLeft: number;
    version: number;
    managerUserId?: number;
    blockerCount?: number;
    pendingDecisionCount?: number;
    recommendedActionLabel?: string;
    recommendedActionRoute?: string;
}

export interface ProjectStats {
    totalProjects: number;
    inProgressProjects: number;
    dueProjects: number;
    highRiskProjects: number;
    stageDistribution: Record<string, number>;
    attentionProjects: PageResult<Project>;
}

export interface ProjectStatusHistory {
    beforeStatus?: string;
    afterStatus: string;
    actionCode: string;
    reason?: string;
    occurredAt: string;
}

export interface RiskDetail {
    factorCode: string;
    factorName: string;
    rawValue?: string;
    scoreDelta: number;
    scoreCap?: number;
    matched: boolean;
}

export interface DocumentVersion {
    id: number;
    projectId: number;
    fileObjectId: number;
    projectNo: string;
    fileName: string;
    fileType: string;
    versionNo: string;
    status: string;
    effectiveDate: string;
    changeSummary: string;
    currentVersion: boolean;
    sha256: string;
    downloadUrl: string;
}

export interface Requirement {
    id: number;
    projectId: number;
    category: string;
    content: string;
    confirmStatus: string;
    lastReason: string;
}

export interface ProjectMember {
    id: number;
    projectId: number;
    userId: number;
    memberName: string;
    projectRole: string;
    departmentName: string;
    status: string;
    deptId?: number;
    accountStatus?: string;
    responsibilityCodes: string[];
    primaryResponsibilityCode: string;
}

export interface ProjectMemberCandidate {
    userId: number;
    username: string;
    nickname: string;
    deptId?: number;
    departmentName?: string;
    roleCodes: string[];
    allowedResponsibilityCodes: string[];
}

// 系统用户、角色和组织模型。
export interface SystemUser {
    id: number;
    username: string;
    nickname: string;
    deptId?: number;
    deptName?: string;
    status: string;
    userType: 'INTERNAL' | 'EXTERNAL' | 'EXTERNAL_LEGACY';
    employeeNo?: string;
    forceChangePassword?: boolean;
    postIds?: number[];
    roles: string[];
}

export interface SystemRole {
    id: number;
    roleCode: string;
    roleName: string;
    status: string;
    dataScope: 'SELF' | 'PROJECT_MEMBER' | 'PROJECT_OWNER' | 'CUSTOM_DEPT' | 'DEPT' | 'DEPT_AND_CHILD' | 'ALL';
}

export interface SystemDept {
    id: number;
    parentId?: number;
    deptName: string;
    leaderName?: string;
    leaderUserId?: number;
    ancestors?: string;
    phone?: string;
    sortNo?: number;
    status: string;
}

// 技术资料、工艺与样品模型。
export interface BomItem {
    id: number;
    materialCode: string;
    materialName: string;
    specification: string;
    quantity: number;
    unit: string;
    sourceType: string;
    substituteCode: string;
}

export interface Bom {
    id: number;
    projectId: number;
    projectNo: string;
    bomNo: string;
    versionNo: string;
    boundDocVersionId: number;
    status: string;
    items: BomItem[];
}

export interface ProcessRoute {
    id: number;
    projectId: number;
    projectNo: string;
    routeNo: string;
    versionNo: string;
    boundDocVersionId: number;
    status: string;
    steps: Array<Record<string, unknown>>;
}

export interface InspectionSpec {
    id: number;
    projectId: number;
    projectNo: string;
    specNo: string;
    versionNo: string;
    boundDocVersionId: number;
    status: string;
    items: Array<Record<string, unknown>>;
}

export interface Sample {
    id: number;
    projectId: number;
    projectNo: string;
    sampleNo: string;
    purpose: string;
    quantity: number;
    planFinishDate: string;
    referencedVersion: string;
    status: string;
    confirmConclusion: string;
    responsibleName: string;
    issueSummary: string;
}

export interface SampleCheck {
    id: number;
    sampleId: number;
    checkItem: string;
    measuredValue: string;
    result: string;
    issueSummary: string;
    correctiveAction: string;
    checkerName: string;
    checkedAt: string;
}

export interface FileUploadResult {
    id: number;
    fileName: string;
    contentType: string;
    fileSize: number;
    sha256: string;
    downloadUrl: string;
}

export interface DeliveryBlocker {
    code: string;
    message: string;
}

export interface DeliveryReadiness {
    projectId: number;
    ready: boolean;
    blockers: DeliveryBlocker[];
}

export interface ConfirmationToken {
    token: string;
    sampleId: number;
    expireAt: string;
    maxUseCount: number;
    usedCount: number;
    status: string;
}

// 变更、任务、风险和项目工作台模型。
export interface ChangeOrder {
    id: number;
    projectId: number;
    projectNo: string;
    changeNo: string;
    changeType: string;
    urgency: string;
    beforeContent: string;
    afterContent: string;
    reason: string;
    status: string;
    delayDays: number;
    reworkQty: number;
    impactCount: number;
    feedbackCount: number;
}

export interface ChangeImpact {
    id: number;
    changeId: number;
    objectType: string;
    objectName: string;
    departmentName: string;
    suggestedAction: string;
    status: string;
    feedbackResult: string;
    responsibleName: string;
    version: number;
}

export interface Task {
    id: number;
    projectId: number;
    projectNo: string;
    taskNo: string;
    taskType: string;
    title: string;
    referencedVersion: string;
    status: string;
    responsibleName: string;
    planStart: string;
    planFinish: string;
    blockReason: string;
    version: number;
    assigneeId?: number;
}

export interface Risk {
    id: number;
    projectId: number;
    projectNo: string;
    level: string;
    score: number;
    reasons: string[];
    suggestion: string;
    status: string;
}

export interface RiskAction {
    id: number;
    riskId: number;
    actionPlan: string;
    responsibleUserId: number;
    responsibleName: string;
    planFinishTime: string;
    closeSummary?: string;
    status: string;
    version: number;
    createdAt?: string;
    closedAt?: string;
}

export interface ProjectMilestone {
    code: string;
    label: string;
    state: 'COMPLETED' | 'CURRENT' | 'PENDING';
    occurredAt?: string;
}

export interface VersionConflict {
    taskId: number;
    taskNo: string;
    taskTitle: string;
    referencedVersion?: string;
    currentVersion?: string;
}

export interface QrCodeBinding {
    code: string;
    businessType: string;
    businessId: number;
    payload: string;
    status: string;
}

export interface ProjectDetail {
    project: Project;
    requirements: Requirement[];
    members: ProjectMember[];
    documents: DocumentVersion[];
    samples: Sample[];
    changes: ChangeOrder[];
    tasks: Task[];
    risks: Risk[];
    timeline: Array<{
        id: number;
        title: string;
        summary?: string;
        operatorName?: string;
        occurredAt?: string;
    }>;
}

export interface ProjectWorkspace {
    detail: ProjectDetail;
    currentRisk?: Risk;
    riskActions: RiskAction[];
    milestones: ProjectMilestone[];
    versionConflicts: VersionConflict[];
    flowQrCode?: QrCodeBinding;
    ownerGaps: Array<{
        projectId: number;
        responsibilityCode: string;
        status: string;
        detail: string;
    }>;
    progress?: ProjectProgress;
}

export interface ProjectProgress {
    stage: string;
    stageLabel: string;
    status: string;
    completionCriteria: string;
    dueState: 'OVERDUE' | 'DUE_SOON' | 'ON_TRACK' | 'NO_DATE' | string;
    riskLevel: string;
    blockerCount: number;
    pendingDecisionCount: number;
    nextActionCode: string;
    nextActionLabel: string;
    nextActionReason: string;
    nextActionRoute: string;
}

export interface ProjectWorkspaceSummary {
    project: Project;
    currentRisk?: Risk;
    milestones: ProjectMilestone[];
    flowQrCode?: QrCodeBinding;
    ownerGapCount: number;
}

export interface Message {
    id: number;
    title: string;
    content: string;
    type: string;
    status: string;
    businessType: string;
    businessId: number;
    createdAt: string;
}

export interface ActionItem {
    id: number;
    sourceType: string;
    sourceId: number;
    actionCode: string;
    projectId?: number;
    assigneeUserId?: number;
    assigneeName?: string;
    title: string;
    summary?: string;
    priority: string;
    slaDueAt?: string;
    route?: string;
    sourceStatus?: string;
    actionStatus: string;
    overdue: boolean;
}

export interface ActionCenterSummary {
    metrics: Record<string, number>;
    priorityActions: ActionItem[];
}

export interface ApprovalTodo {
    id: number;
    instanceId: number;
    businessType: string;
    businessId: number;
    projectId: number;
    templateCode: string;
    templateVersion: number;
    approvalMode: string;
    nodeCode: string;
    nodeName: string;
    responsibilityCode: string;
    assigneeUserId?: number;
    assigneeName?: string;
    decision: string;
    dueAt?: string;
    overdue: boolean;
    version: number;
}

export interface ApprovalTemplateNode {
    id?: number;
    nodeOrder?: number;
    nodeCode: string;
    nodeName: string;
    responsibilityCode: string;
    slaMinutes?: number;
    escalationRole?: string;
}

export interface ApprovalTemplate {
    id: number;
    templateCode: string;
    businessType: string;
    templateVersion: number;
    templateName: string;
    approvalMode: string;
    slaMinutes?: number;
    status: string;
    nodes: ApprovalTemplateNode[];
    version: number;
}

export interface CapaEvidence {
    id: number;
    evidenceType: string;
    evidenceRef: string;
    summary?: string;
    createdAt?: string;
}

export interface CapaAction {
    sourceType: string;
    sourceId: number;
    title: string;
    status: string;
    assigneeUserId?: number;
    assigneeName?: string;
    dueAt?: string;
    summary?: string;
    version?: number;
}

export interface CapaCase {
    id: number;
    caseNo: string;
    projectId: number;
    taskId?: number;
    riskId?: number;
    exceptionType: string;
    summary: string;
    reporterName?: string;
    ownerUserId: number;
    ownerName?: string;
    dueAt: string;
    status: string;
    containmentPlan?: string;
    rootCause?: string;
    correctivePlan?: string;
    verificationSummary?: string;
    closeConclusion?: string;
    evidence: CapaEvidence[];
    version: number;
    createdAt?: string;
    closedAt?: string;
}

export interface ProjectPulse {
    projectId: number;
    projectNo: string;
    stage: string;
    status: string;
    blockers: ActionItem[];
    pendingDecisions: ApprovalTodo[];
    versionConflicts: VersionConflict[];
    openExceptions: CapaCase[];
    ownerGaps: ProjectWorkspace['ownerGaps'];
    metrics: Record<string, number>;
}

// 统计分析、导入与平台治理模型。
export interface ReportOverview {
    metrics: Record<string, number>;
    riskLevels: Array<Record<string, unknown>>;
    changeTypes: Array<Record<string, unknown>>;
    sampleEfficiency: Array<Record<string, unknown>>;
    deliveryStats: Array<Record<string, unknown>>;
    delayReasons: Array<Record<string, unknown>>;
    departmentLoads: Array<Record<string, unknown>>;
}

export interface Workbench {
    metrics: Record<string, number>;
    todos: Task[];
    dueTasks: Task[];
    unreadNotifications: Message[];
    watchedProjects: Project[];
    recentOperations: Array<Record<string, unknown>>;
    riskCards: Risk[];
    shortcuts: Array<Record<string, unknown>>;
}

export type DashboardPeriodCode = '7D' | '30D' | 'MONTH' | 'CUSTOM';

export interface DashboardPeriod {
    period: DashboardPeriodCode;
    startDate: string;
    endDate: string;
}

export interface DashboardChartItem {
    name: string;
    value: number;
}

export interface DashboardTrendPoint {
    date: string;
    dueTasks: number;
    completedTasks: number;
}

export interface DashboardDepartmentLoad {
    name: string;
    todoTasks: number;
    overdueTasks: number;
    impactedTasks: number;
}

export interface DashboardData {
    period: DashboardPeriod;
    metrics: Record<string, number>;
    deliveryTrend: DashboardTrendPoint[];
    riskDistribution: DashboardChartItem[];
    changeTypes: DashboardChartItem[];
    departmentLoads: DashboardDepartmentLoad[];
    projects: Project[];
    criticalRisks: Risk[];
    todos: Task[];
    dueTasks: Task[];
    unreadNotifications: Message[];
    shortcuts: Array<{
        name: string;
        route: string;
        permission?: string;
    }>;
}

export interface DashboardSummary {
    period: DashboardPeriod;
    metrics: Record<string, number>;
    deliveryTrend: DashboardTrendPoint[];
    riskDistribution: DashboardChartItem[];
    changeTypes: DashboardChartItem[];
    departmentLoads: DashboardDepartmentLoad[];
    shortcuts: Array<{
        name: string;
        route: string;
        permission?: string;
    }>;
}

export interface DashboardOverview {
    period: DashboardPeriod;
    metrics: Record<string, number>;
    shortcuts: DashboardData['shortcuts'];
}

export interface DashboardInsights {
    period: DashboardPeriod;
    deliveryTrend: DashboardTrendPoint[];
    riskDistribution: DashboardChartItem[];
    changeTypes: DashboardChartItem[];
    departmentLoads: DashboardDepartmentLoad[];
}

export interface SearchResultItem {
    type: string;
    id: number;
    title: string;
    summary: string;
    route: string;
}

export interface ExportTask {
    id: number;
    exportType: string;
    status: string;
    fileName: string;
    querySummary: string;
    downloadCount: number;
    createdAt: string;
    finishedAt: string;
}

export interface ImportTask {
    id: number;
    importType: string;
    mode: string;
    status: string;
    successCount: number;
    failCount: number;
    errors: string[];
    createdAt: string;
}

export interface RuleParam {
    id: number;
    ruleCode: string;
    ruleName: string;
    ruleVersion: string;
    params: Record<string, unknown>;
    status: string;
    publishedAt?: string;
}

export interface AuditLog {
    id: number;
    userName: string;
    moduleName: string;
    operationType: string;
    businessType: string;
    businessId: number;
    result: string;
    summary: string;
    operatedAt: string;
}

export interface PasswordRecoveryRequest {
    id: number;
    username: string;
    contactName: string;
    contactValue: string;
    requesterNote?: string | null;
    status: 'PENDING' | 'IN_REVIEW' | 'RESET' | 'REJECTED';
    handlerId?: number | null;
    handlingNote?: string | null;
    reviewedAt?: string | null;
    resetAt?: string | null;
    version: number;
    createdAt: string;
}

export interface SupportTicketAttachment {
    id: number;
    fileName: string;
    contentType: string;
    fileSize: number;
    downloadUrl: string;
}

export interface SupportTicket {
    id: number;
    ticketNo: string;
    requesterUserId?: number | null;
    requesterUsername?: string | null;
    contactName: string;
    contactValue?: string | null;
    source: 'LOGIN' | 'IN_APP';
    category: 'ACCOUNT' | 'PERMISSION' | 'FUNCTION' | 'DATA' | 'OTHER';
    priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
    pageContext?: string | null;
    description: string;
    status: 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED';
    handlerId?: number | null;
    handlingNote?: string | null;
    resolvedAt?: string | null;
    version: number;
    createdAt: string;
    attachments: SupportTicketAttachment[];
}
