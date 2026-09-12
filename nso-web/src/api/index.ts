import { getData, http, postData, putData } from '@/utils/request';
import type { ActionCenterSummary, ActionItem, ApprovalTemplate, ApprovalTodo, AuditLog, ApiResponse, Bom, CapaAction, CapaCase, CapaEvidence, ChangeImpact, ChangeOrder, ConfirmationToken, Customer, CustomerContact, DashboardData, DashboardInsights, DashboardOverview, DashboardPeriodCode, DashboardSummary, DeliveryReadiness, DocumentVersion, ExportTask, ExternalProjectAccess, FileUploadResult, ImportTask, InspectionSpec, Message, PageParams, PageResult, PasswordRecoveryRequest, ProcessRoute, Project, ProjectPulse, ProjectStatusHistory, ProjectWorkspace, ProjectWorkspaceSummary, ProjectMember, ProjectMemberCandidate, ProjectStats, QrCodeBinding, ReportOverview, Requirement, Risk, RiskDetail, RiskAction, RuleParam, Sample, SampleCheck, SearchResultItem, SupportTicket, SupportTicketAttachment, Task, SystemDept, SystemRole, SystemUser, UserProfile, Workbench } from '@/types';

// 列表接口使用的筛选参数。
type ProjectListParams = PageParams & {
    keyword?: string;
    stage?: string;
    status?: string;
    riskLevel?: string;
    dueState?: string;
    quickFilter?: string;
};

type TaskListParams = PageParams & {
    projectId?: number;
    status?: string;
    taskType?: string;
};

type RiskListParams = PageParams & {
    projectId?: number;
    level?: string;
    status?: string;
};

type DashboardQueryParams = {
    period?: DashboardPeriodCode;
    startDate?: string;
    endDate?: string;
};

// 管理端业务接口封装。
export const api = {
    // 身份认证、个人资料与支持工单。
    login: (data: {
        username: string;
        password: string;
    }) => postData<any>('/admin/auth/login', data),
    refresh: (refreshToken: string) => postData<any>('/admin/auth/refresh', { refreshToken }),
    logout: (refreshToken?: string) => postData<void>('/admin/auth/logout', { refreshToken }),
    requestPasswordRecovery: (data: {
        username: string;
        contactName: string;
        contactValue: string;
        requesterNote?: string;
    }) => postData<void>('/admin/auth/password-recovery-requests', data),
    createGuestSupportTicket: (data: {
        username?: string;
        contactName: string;
        contactValue: string;
        category: SupportTicket['category'];
        priority: SupportTicket['priority'];
        description: string;
    }) => postData<SupportTicket>('/admin/auth/support-tickets', data),
    me: () => getData<any>('/admin/auth/me'),
    profile: () => getData<UserProfile>('/admin/profile'),
    updateProfile: (data: Pick<UserProfile, 'nickname' | 'phone' | 'email' | 'gender' | 'version'>) => putData<UserProfile>('/admin/profile', data),
    changePassword: (data: {
        currentPassword: string;
        newPassword: string;
    }) => putData<void>('/admin/profile/password', data),
    createSupportTicket: (data: {
        category: SupportTicket['category'];
        priority: SupportTicket['priority'];
        description: string;
        pageContext?: string;
    }) => postData<SupportTicket>('/admin/support-tickets', data),
    mySupportTickets: (params: PageParams = {}) => getData<PageResult<SupportTicket>>('/admin/support-tickets/mine', params),
    uploadSupportTicketImage: async (ticketId: number, file: File) => {
        const form = new FormData();
        form.append('file', file);
        const response = await http.post<ApiResponse<SupportTicketAttachment>>(`/admin/support-tickets/${ticketId}/attachments`, form);
        return response.data.data;
    },
    downloadSupportTicketImage: async (ticketId: number, attachmentId: number) => {
        const response = await http.get<Blob>(`/admin/support-tickets/${ticketId}/attachments/${attachmentId}`, { responseType: 'blob' });
        return response.data;
    },
    uploadAvatar: async (file: File) => {
        const form = new FormData();
        form.append('file', file);
        const response = await http.post<ApiResponse<UserProfile>>('/admin/profile/avatar', form);
        return response.data.data;
    },
    profileAvatar: async () => {
        const response = await http.get<Blob>('/admin/profile/avatar', { responseType: 'blob' });
        return URL.createObjectURL(response.data);
    },
    // 工作台、仪表盘与全局检索。
    workbench: () => getData<Workbench>('/admin/workbench'),
    dashboard: (params?: DashboardQueryParams) => getData<DashboardData>('/admin/dashboard', params),
    dashboardOverview: (params?: DashboardQueryParams) => getData<DashboardOverview>('/admin/dashboard/overview', params),
    dashboardInsights: (params?: DashboardQueryParams) => getData<DashboardInsights>('/admin/dashboard/insights', params),
    dashboardSummary: (params?: DashboardQueryParams) => getData<DashboardSummary>('/admin/dashboard/summary', params),
    dashboardProjects: (params: PageParams = {}) => getData<PageResult<Project>>('/admin/dashboard/projects', params),
    dashboardActionRisks: (params: PageParams = {}) => getData<PageResult<Risk>>('/admin/dashboard/actions/risks', params),
    dashboardActionTodos: (params: PageParams = {}) => getData<PageResult<Task>>('/admin/dashboard/actions/todos', params),
    dashboardActionDue: (params: PageParams = {}) => getData<PageResult<Task>>('/admin/dashboard/actions/due', params),
    dashboardActionMessages: (params: PageParams = {}) => getData<PageResult<Message>>('/admin/dashboard/actions/messages', params),
    search: (params: PageParams & { keyword?: string } = {}) => getData<PageResult<SearchResultItem>>('/admin/search', params),
    // 客户、项目与成员协同。
    customers: (params: PageParams & { keyword?: string } = {}) => getData<PageResult<Customer>>('/admin/customers', params),
    createCustomer: (data: Partial<Customer>) => postData<Customer>('/admin/customers', data),
    customerContacts: (customerId: number, params: PageParams = {}) => getData<PageResult<CustomerContact>>(`/admin/customers/${customerId}/contacts`, params),
    saveCustomerContact: (customerId: number, data: Partial<CustomerContact>) => postData<CustomerContact>(`/admin/customers/${customerId}/contacts`, data),
    updateCustomerContact: (customerId: number, contactId: number, data: Partial<CustomerContact>) => putData<CustomerContact>(`/admin/customers/${customerId}/contacts/${contactId}`, data),
    updateCustomerContactStatus: (customerId: number, contactId: number, status: string) => putData<CustomerContact>(`/admin/customers/${customerId}/contacts/${contactId}/status`, { status }),
    projects: (params: ProjectListParams = {}) => getData<PageResult<Project>>('/admin/projects', params),
    projectStats: (params: ProjectListParams = {}) => getData<ProjectStats>('/admin/projects/stats', params),
    createProject: (data: Record<string, unknown>) => postData<Project>('/admin/projects', data),
    projectManagerCandidates: (params: PageParams & { keyword?: string } = {}) => getData<PageResult<ProjectMemberCandidate>>('/admin/projects/manager-candidates', params),
    project: (id: number) => getData<any>(`/admin/projects/${id}`),
    projectWorkspace: (id: number) => getData<ProjectWorkspace>(`/admin/projects/${id}/workspace`),
    projectWorkspaceSummary: (id: number) => getData<ProjectWorkspaceSummary>(`/admin/projects/${id}/workspace/summary`),
    projectPulse: (id: number) => getData<ProjectPulse>(`/admin/projects/${id}/workspace/pulse`),
    projectWorkspaceTimeline: (id: number, params: PageParams = {}) => getData<PageResult<ProjectWorkspace['detail']['timeline'][number]>>(`/admin/projects/${id}/workspace/timeline`, params),
    projectWorkspaceVersionConflicts: (id: number, params: PageParams = {}) => getData<PageResult<ProjectWorkspace['versionConflicts'][number]>>(`/admin/projects/${id}/workspace/version-conflicts`, params),
    projectWorkspaceOwnerGaps: (id: number, params: PageParams = {}) => getData<PageResult<ProjectWorkspace['ownerGaps'][number]>>(`/admin/projects/${id}/workspace/owner-gaps`, params),
    createProjectFlowQr: (id: number) => postData<QrCodeBinding>(`/admin/projects/${id}/flow-qr`),
    submitReview: (id: number) => postData<Project>(`/admin/projects/${id}/submit-review`),
    projectAction: (id: number, action: string, data: {
        reason?: string;
        version?: number;
        confirmationId?: number;
    }) => postData<Project>(`/admin/projects/${id}/actions/${action}`, data),
    copyProject: (id: number, data?: {
        productName?: string;
        targetDate?: string;
        reason?: string;
    }) => postData<Project>(`/admin/projects/${id}/copy`, data),
    projectStatusHistory: (id: number, params: PageParams = {}) => getData<PageResult<ProjectStatusHistory>>(`/admin/projects/${id}/status-history`, params),
    requirements: (projectId: number, params: PageParams = {}) => getData<PageResult<Requirement>>(`/admin/projects/${projectId}/requirements`, params),
    saveRequirement: (projectId: number, data: Record<string, unknown>) => postData<Requirement>(`/admin/projects/${projectId}/requirements`, data),
    confirmRequirement: (id: number, data: Record<string, unknown>) => postData<Requirement>(`/admin/requirements/${id}/confirm`, data),
    members: (projectId: number, params: PageParams = {}) => getData<PageResult<ProjectMember>>(`/admin/projects/${projectId}/members`, params),
    addMember: (projectId: number, data: Record<string, unknown>) => postData<ProjectMember>(`/admin/projects/${projectId}/members`, data),
    projectMemberCandidates: (projectId: number, params: PageParams & { keyword?: string } = {}) => getData<PageResult<ProjectMemberCandidate>>(`/admin/projects/${projectId}/member-candidates`, params),
    updateProjectMember: (projectId: number, memberId: number, data: Record<string, unknown>) => putData<ProjectMember>(`/admin/projects/${projectId}/members/${memberId}`, data),
    removeProjectMember: (projectId: number, memberId: number) => postData<ProjectMember>(`/admin/projects/${projectId}/members/${memberId}/remove`),
    restoreProjectMember: (projectId: number, memberId: number) => postData<ProjectMember>(`/admin/projects/${projectId}/members/${memberId}/restore`),
    transferProjectManager: (projectId: number, data: {
        managerUserId: number;
        reason: string;
    }) => postData<Project>(`/admin/projects/${projectId}/manager-transfer`, data),
    projectCustomerAuthorizations: (projectId: number, params: PageParams = {}) => getData<PageResult<ExternalProjectAccess>>(`/admin/projects/${projectId}/customer-authorizations`, params),
    authorizeProjectCustomer: (projectId: number, data: {
        contactId: number;
        validUntil?: string;
    }) => postData<ExternalProjectAccess>(`/admin/projects/${projectId}/customer-authorizations`, data),
    revokeProjectCustomerAuthorization: (projectId: number, authorizationId: number, reason: string) => postData<void>(`/admin/projects/${projectId}/customer-authorizations/${authorizationId}/revoke`, { reason }),
    // 技术文件、工艺资料与样品管理。
    documents: (params: PageParams & { projectId?: number } = {}) => getData<PageResult<DocumentVersion>>('/admin/documents', params),
    createDocument: (projectId: number, data: Record<string, unknown>) => postData<DocumentVersion>(`/admin/documents/${projectId}/versions`, data),
    uploadDocumentFile: async (projectId: number, file: File) => {
        const form = new FormData();
        form.append('projectId', String(projectId));
        form.append('file', file);
        const response = await http.post('/admin/files/upload', form);
        return response.data.data as {
            id: number;
            fileName: string;
            contentType: string;
            fileSize: number;
            sha256: string;
            downloadUrl: string;
        };
    },
    fileInlineUrl: (id: number) => `/admin/files/${id}/inline`,
    fileDownloadUrl: (id: number) => `/admin/files/${id}/download`,
    documentInlineUrl: (id: number) => `/admin/document-versions/${id}/inline`,
    documentDownloadUrl: (id: number) => `/admin/document-versions/${id}/download`,
    publishDocument: (id: number) => postData<DocumentVersion>(`/admin/document-versions/${id}/publish`),
    createDocumentQr: (id: number) => postData<QrCodeBinding>(`/admin/document-versions/${id}/qr`),
    boms: (params: PageParams & { projectId?: number } = {}) => getData<PageResult<Bom>>('/admin/boms', params),
    createBom: (data: Record<string, unknown>) => postData<Bom>('/admin/boms', data),
    processRoutes: (params: PageParams & { projectId?: number } = {}) => getData<PageResult<ProcessRoute>>('/admin/process-routes', params),
    createProcessRoute: (data: Record<string, unknown>) => postData<ProcessRoute>('/admin/process-routes', data),
    inspectionSpecs: (params: PageParams & { projectId?: number } = {}) => getData<PageResult<InspectionSpec>>('/admin/inspection-specs', params),
    createInspectionSpec: (data: Record<string, unknown>) => postData<InspectionSpec>('/admin/inspection-specs', data),
    publishTechnicalPackage: (projectId: number) => postData<Record<string, unknown>>(`/admin/projects/${projectId}/technical-package/publish`),
    syncTechnicalPackageTasks: (projectId: number) => postData<{
        updatedTaskCount: number;
        versionNo: string;
    }>(`/admin/projects/${projectId}/technical-package/sync-tasks`),
    samples: (params: PageParams & { projectId?: number; status?: string } = {}) => getData<PageResult<Sample>>('/admin/samples', params),
    createSample: (data: Record<string, unknown>) => postData<Sample>('/admin/samples', data),
    submitSampleConfirm: (id: number) => postData<Sample>(`/admin/samples/${id}/submit-confirm`),
    confirmSample: (id: number, data: Record<string, unknown>) => postData<Sample>(`/admin/samples/${id}/confirm`, data),
    uploadSampleConfirmationEvidence: async (id: number, file: File) => {
        const form = new FormData();
        form.append('file', file);
        const response = await http.post<ApiResponse<FileUploadResult>>(`/admin/samples/${id}/confirmation-evidence`, form);
        return response.data.data;
    },
    sampleChecks: (id: number, params: PageParams = {}) => getData<PageResult<SampleCheck>>(`/admin/samples/${id}/checks`, params),
    addSampleCheck: (id: number, data: Record<string, unknown>) => postData<SampleCheck>(`/admin/samples/${id}/checks`, data),
    createConfirmToken: (id: number, data: {
        contactId: number;
        validDays?: number;
        maxUseCount?: number;
    }) => postData<ConfirmationToken>(`/admin/samples/${id}/confirm-token`, data),
    revokeConfirmTokens: (id: number) => postData<void>(`/admin/samples/${id}/confirm-token/revoke`),
    // 变更、任务、风险与交付协同。
    changes: (params: PageParams & { projectId?: number; status?: string; changeType?: string } = {}) => getData<PageResult<ChangeOrder>>('/admin/changes', params),
    createChange: (data: Record<string, unknown>) => postData<ChangeOrder>('/admin/changes', data),
    analyzeChange: (id: number) => postData<ChangeImpact[]>(`/admin/changes/${id}/analyze-impact`),
    impacts: (id: number, params: PageParams = {}) => getData<PageResult<ChangeImpact>>(`/admin/changes/${id}/impacts`, params),
    approveChange: (id: number, data?: Record<string, unknown>) => postData<ChangeOrder>(`/admin/changes/${id}/approve`, data),
    feedbackChange: (id: number, data: Record<string, unknown>) => postData<ChangeOrder>(`/admin/changes/${id}/feedback`, data),
    closeChange: (id: number) => postData<ChangeOrder>(`/admin/changes/${id}/close`),
    tasks: (params: TaskListParams = {}) => getData<PageResult<Task>>('/admin/tasks', params),
    createExecutionTask: (data: Record<string, unknown>) => postData<Task>('/admin/execution/tasks', data),
    startTask: (id: number, data?: Record<string, unknown>) => postData<Task>(`/admin/tasks/${id}/start`, data),
    pauseTask: (id: number, data?: Record<string, unknown>) => postData<Task>(`/admin/tasks/${id}/pause`, data),
    completeTask: (id: number, data?: Record<string, unknown>) => postData<Task>(`/admin/tasks/${id}/complete`, data),
    feedbackTask: (id: number, data: Record<string, unknown>) => postData<Task>(`/admin/tasks/${id}/feedback`, data),
    reportException: (data: Record<string, unknown>) => postData<Risk>('/admin/execution/exceptions', data),
    deliveries: (params: PageParams & { projectId?: number; status?: string } = {}) => getData<PageResult<Record<string, unknown>>>('/admin/deliveries', params),
    deliveryReadiness: (projectId: number) => getData<DeliveryReadiness>(`/admin/projects/${projectId}/delivery-readiness`),
    createDelivery: (data: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/deliveries', data),
    risks: (params: RiskListParams = {}) => getData<PageResult<Risk>>('/admin/risks', params),
    riskDetails: (riskId: number, params: PageParams = {}) => getData<PageResult<RiskDetail>>(`/admin/risks/${riskId}/details`, params),
    overrideRisk: (riskId: number, data: {
        level: string;
        reason: string;
        expiresAt: string;
    }) => postData<Risk>(`/admin/risks/${riskId}/override`, data),
    riskActions: (riskId: number, params: PageParams = {}) => getData<PageResult<RiskAction>>(`/admin/risks/${riskId}/actions`, params),
    createRiskAction: (riskId: number, data: Record<string, unknown>) => postData<RiskAction>(`/admin/risks/${riskId}/actions`, data),
    closeRiskAction: (actionId: number, data: {
        closeSummary: string;
        version: number;
    }) => postData<RiskAction>(`/admin/risk-actions/${actionId}/close`, data),
    actions: (params: PageParams & { sourceType?: string; priority?: string; projectId?: number; dueState?: string } = {}) => getData<PageResult<ActionItem>>('/admin/actions', params),
    actionSummary: () => getData<ActionCenterSummary>('/admin/actions/summary'),
    refreshActions: () => postData<{ refreshed: number }>('/admin/actions/refresh'),
    approvalTodos: (params: PageParams & { businessType?: string } = {}) => getData<PageResult<ApprovalTodo>>('/admin/approval-todos', params),
    approvalTemplates: (params: PageParams & { businessType?: string } = {}) => getData<PageResult<ApprovalTemplate>>('/admin/approval-templates', params),
    createApprovalTemplate: (data: {
        templateCode: string;
        templateName: string;
        businessType: string;
        approvalMode: string;
        slaMinutes?: number;
        nodes: Array<{
            nodeCode: string;
            nodeName: string;
            responsibilityCode: string;
            slaMinutes?: number;
            escalationRole?: string;
        }>;
    }) => postData<ApprovalTemplate>('/admin/approval-templates', data),
    publishApprovalTemplate: (id: number) => postData<ApprovalTemplate>(`/admin/approval-templates/${id}/publish`),
    decideApproval: (id: number, data: {
        decision: 'APPROVED' | 'REJECTED';
        opinion?: string;
        version?: number;
        idempotencyKey: string;
    }) => postData<ApprovalTodo>(`/admin/approval-todos/${id}/decision`, data),
    exceptions: (params: PageParams & { projectId?: number; status?: string } = {}) => getData<PageResult<CapaCase>>('/admin/exceptions', params),
    exception: (id: number) => getData<CapaCase>(`/admin/exceptions/${id}`),
    createException: (data: {
        projectId: number;
        taskId?: number;
        exceptionType: string;
        summary: string;
        reporterName?: string;
        ownerUserId: number;
        dueAt: string;
        idempotencyKey: string;
    }) => postData<CapaCase>('/admin/exceptions', data),
    transitionException: (id: number, data: {
        toStatus: string;
        comment?: string;
        rootCause?: string;
        correctivePlan?: string;
        verificationSummary?: string;
        closeConclusion?: string;
        evidenceRef?: string;
        evidenceSummary?: string;
        version: number;
        idempotencyKey: string;
    }) => postData<CapaCase>(`/admin/exceptions/${id}/transitions`, data),
    addExceptionEvidence: (id: number, data: { evidenceType: string; evidenceRef: string; summary?: string }) => postData<CapaEvidence>(`/admin/exceptions/${id}/evidence`, data),
    exceptionActions: (id: number) => getData<CapaAction[]>(`/admin/exceptions/${id}/actions`),
    createExceptionAction: (id: number, data: {
        title: string;
        taskType?: string;
        assigneeUserId: number;
        planStart: string;
        planFinish: string;
        actionPlan: string;
        idempotencyKey: string;
    }) => postData<CapaAction>(`/admin/exceptions/${id}/actions`, data),
    reopenException: (id: number) => postData<CapaCase>(`/admin/exceptions/${id}/reopen`),
    messages: (params: PageParams & { status?: string } = {}) => getData<PageResult<Message>>('/admin/messages', params),
    readMessage: (id: number) => postData<Message>(`/admin/messages/${id}/read`),
    requestOperationConfirmation: (data: Record<string, unknown>) => postData<{
        id: number;
        challengeId?: number;
        status: string;
        expiresAt: string;
    }>('/admin/operation-confirmations', data),
    confirmOperation: (id: number) => postData<{
        id: number;
        status: string;
    }>(`/admin/operation-confirmations/${id}/confirm`),
    verifyOperationChallenge: (id: number, code: string) => postData<{
        id: number;
        status: string;
    }>(`/admin/operation-confirmations/challenges/${id}/verify`, { code }),
    overview: () => getData<ReportOverview>('/admin/reports/overview'),
    savedViews: (params: PageParams & { targetType?: string } = {}) => getData<PageResult<Record<string, unknown>>>('/admin/views', params),
    saveView: (data: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/views', data),
    exports: (params: PageParams = {}) => getData<PageResult<ExportTask>>('/admin/exports', params),
    createExport: (data: Record<string, unknown>) => postData<ExportTask>('/admin/exports', data),
    importTemplate: (type: string) => getData<Record<string, unknown>>(`/admin/imports/templates/${type}`),
    imports: (params: PageParams & { importType?: string } = {}) => getData<PageResult<ImportTask>>('/admin/imports', params),
    importRows: (data: Record<string, unknown>) => postData<ImportTask>('/admin/imports', data),
    rules: (params: PageParams & { ruleCode?: string } = {}) => getData<PageResult<RuleParam>>('/admin/rules', params),
    saveRule: (data: Record<string, unknown>) => postData<RuleParam>('/admin/rules', data),
    publishRule: (id: number) => postData<RuleParam>(`/admin/rules/${id}/publish`),
    // 平台治理、审计和系统主数据。
    auditLogs: (params: PageParams & { businessType?: string } = {}) => getData<PageResult<AuditLog>>('/admin/audit-logs', params),
    publicConfirmation: (token: string) => getData<Sample>(`/public/confirm/${token}`),
    submitPublicConfirmation: (token: string, data: Record<string, unknown>) => postData<Sample>(`/public/confirm/${token}/decision`, data),
    jobs: (params: PageParams = {}) => getData<PageResult<Record<string, unknown>>>('/admin/monitor/jobs', params),
    runJob: (id: number) => postData<Record<string, unknown>>(`/admin/monitor/jobs/${id}/run`),
    systemUsers: (params: PageParams = {}) => getData<PageResult<SystemUser>>('/admin/system/users', params),
    createSystemUser: (data: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/system/users', data),
    updateSystemUser: (userId: number, data: Record<string, unknown>) => putData<Record<string, unknown>>(`/admin/system/users/${userId}`, data),
    resetSystemUserPassword: (userId: number, password: string) => putData<void>(`/admin/system/users/${userId}/password`, { password }),
    activateSystemUser: (userId: number, data: {
        password: string;
        reason?: string;
    }) => postData<Record<string, unknown>>(`/admin/system/users/${userId}/activate`, data),
    systemUserHandoverCheck: (userId: number) => getData<Record<string, number>>(`/admin/system/users/${userId}/handover-check`),
    updateSystemUserLifecycle: (userId: number, data: {
        status: string;
        reason?: string;
    }) => putData<Record<string, unknown>>(`/admin/system/users/${userId}/lifecycle`, data),
    replaceSystemUserPosts: (userId: number, postIds: number[], primaryPostId?: number) => putData<void>(`/admin/system/users/${userId}/posts`, { postIds, primaryPostId }),
    systemRoles: (params: PageParams = {}) => getData<PageResult<SystemRole>>('/admin/system/roles', params),
    createSystemRole: (data: Record<string, unknown>) => postData<SystemRole>('/admin/system/roles', data),
    updateSystemRole: (roleId: number, data: Record<string, unknown>) => putData<SystemRole>(`/admin/system/roles/${roleId}`, data),
    rolePermissions: (roleId: number) => getData<any>(`/admin/system/roles/${roleId}/permissions`),
    replaceRolePermissions: (roleId: number, permissionCodes: string[]) => putData<any>(`/admin/system/roles/${roleId}/permissions`, { permissionCodes }),
    replaceSystemUserRoles: (userId: number, roleCodes: string[]) => putData<void>(`/admin/system/users/${userId}/roles`, { roleCodes }),
    departments: (params: PageParams = {}) => getData<PageResult<SystemDept>>('/admin/system/departments', params),
    createDepartment: (data: Record<string, unknown>) => postData<SystemDept>('/admin/system/departments', data),
    updateDepartment: (deptId: number, data: Record<string, unknown>) => putData<SystemDept>(`/admin/system/departments/${deptId}`, data),
    systemPosts: (params: PageParams = {}) => getData<PageResult<Record<string, unknown>>>('/admin/system/posts', params),
    createSystemPost: (data: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/system/posts', data),
    updateSystemPost: (postId: number, data: Record<string, unknown>) => putData<Record<string, unknown>>(`/admin/system/posts/${postId}`, data),
    authorizationRequests: (params: PageParams = {}) => getData<PageResult<Record<string, unknown>>>('/admin/system/authorization-requests', params),
    requestProtectedRole: (data: {
        userId: number;
        roleCode: string;
        reason: string;
    }) => postData<Record<string, unknown>>('/admin/system/authorization-requests', data),
    approveProtectedRole: (requestId: number) => postData<Record<string, unknown>>(`/admin/system/authorization-requests/${requestId}/approve`),
    retentionPolicies: (params: PageParams = {}) => getData<PageResult<Record<string, unknown>>>('/admin/system/retention-policies', params),
    updateRetentionPolicy: (policyCode: string, data: Record<string, unknown>) => putData<Record<string, unknown>>(`/admin/system/retention-policies/${policyCode}`, data),
    runRetentionPolicy: (policyCode: string, confirmationRef?: string) => postData<Record<string, unknown>>(`/admin/system/retention-policies/${policyCode}/run`, { confirmationRef }),
    passwordRecoveryRequests: (params: PageParams & { status?: PasswordRecoveryRequest['status'] } = {}) => getData<PageResult<PasswordRecoveryRequest>>('/admin/system/account-support/password-recovery-requests', params),
    reviewPasswordRecovery: (requestId: number, data: {
        status: 'IN_REVIEW' | 'REJECTED';
        handlingNote: string;
        version: number;
    }) => putData<PasswordRecoveryRequest>(`/admin/system/account-support/password-recovery-requests/${requestId}`, data),
    resetRecoveredPassword: (requestId: number, data: {
        handlingNote: string;
        version: number;
    }) => postData<{
        request: PasswordRecoveryRequest;
        temporaryPassword: string;
    }>(`/admin/system/account-support/password-recovery-requests/${requestId}/reset`, data),
    supportTickets: (params: PageParams & { status?: SupportTicket['status'] } = {}) => getData<PageResult<SupportTicket>>('/admin/system/account-support/support-tickets', params),
    updateSupportTicket: (ticketId: number, data: {
        status: 'PROCESSING' | 'RESOLVED' | 'CLOSED';
        handlingNote: string;
        version: number;
    }) => putData<SupportTicket>(`/admin/system/account-support/support-tickets/${ticketId}`, data),
    systemMenus: (params: PageParams = {}) => getData<PageResult<Record<string, unknown>>>('/admin/system/menus', params),
    createSystemMenu: (data: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/system/menus', data),
    dictionaries: (params: PageParams & { type?: string } = {}) => getData<PageResult<Record<string, unknown>>>('/admin/system/dicts', params),
    saveDictionary: (data: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/system/dicts', data),
    generatorTables: (params: PageParams = {}) => getData<PageResult<Record<string, unknown>>>('/admin/tool/gen/tables', params),
    generatorPreview: (id: number) => getData<Record<string, string>>(`/admin/tool/gen/${id}/preview`)
};
