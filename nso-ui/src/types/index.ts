export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  phone?: string | null
  email?: string | null
  gender: 'MALE' | 'FEMALE' | 'UNSPECIFIED'
  departmentName: string
  roles: string[]
  avatarUrl?: string | null
  version: number
  createdAt?: string
}

export interface RuleBlockDetail {
  ruleCode?: string
  reason?: string
  currentValue?: string
  expectedValue?: string
  action?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNo: number
  pageSize: number
}

export interface Customer {
  id: number
  customerCode: string
  name: string
  industry: string
  contactName: string
  phone: string
  status: string
}

export interface Project {
  id: number
  projectNo: string
  customerId?: number
  customerName: string
  productName: string
  quantity: number
  targetDate: string
  ownerName: string
  status: string
  stage: string
  priority: string
  riskLevel: string
  sampleStatus: string
  changeCount: number
  daysLeft: number
}

export interface DocumentVersion {
  id: number
  projectId: number
  fileObjectId: number
  projectNo: string
  fileName: string
  fileType: string
  versionNo: string
  status: string
  effectiveDate: string
  changeSummary: string
  currentVersion: boolean
  sha256: string
  downloadUrl: string
}

export interface Requirement {
  id: number
  projectId: number
  category: string
  content: string
  confirmStatus: string
  lastReason: string
}

export interface ProjectMember {
  id: number
  projectId: number
  userId: number
  memberName: string
  projectRole: string
  departmentName: string
  status: string
}

export interface BomItem {
  id: number
  materialCode: string
  materialName: string
  specification: string
  quantity: number
  unit: string
  sourceType: string
  substituteCode: string
}

export interface Bom {
  id: number
  projectId: number
  projectNo: string
  bomNo: string
  versionNo: string
  boundDocVersionId: number
  status: string
  items: BomItem[]
}

export interface ProcessRoute {
  id: number
  projectId: number
  projectNo: string
  routeNo: string
  versionNo: string
  boundDocVersionId: number
  status: string
  steps: Array<Record<string, unknown>>
}

export interface InspectionSpec {
  id: number
  projectId: number
  projectNo: string
  specNo: string
  versionNo: string
  boundDocVersionId: number
  status: string
  items: Array<Record<string, unknown>>
}

export interface Sample {
  id: number
  projectId: number
  projectNo: string
  sampleNo: string
  purpose: string
  quantity: number
  planFinishDate: string
  referencedVersion: string
  status: string
  confirmConclusion: string
  responsibleName: string
  issueSummary: string
}

export interface SampleCheck {
  id: number
  sampleId: number
  checkItem: string
  measuredValue: string
  result: string
  issueSummary: string
  correctiveAction: string
  checkerName: string
  checkedAt: string
}

export interface ConfirmationToken {
  token: string
  sampleId: number
  expireAt: string
  maxUseCount: number
  usedCount: number
  status: string
}

export interface ChangeOrder {
  id: number
  projectId: number
  projectNo: string
  changeNo: string
  changeType: string
  urgency: string
  beforeContent: string
  afterContent: string
  reason: string
  status: string
  delayDays: number
  reworkQty: number
  impactCount: number
  feedbackCount: number
}

export interface ChangeImpact {
  id: number
  changeId: number
  objectType: string
  objectName: string
  departmentName: string
  suggestedAction: string
  status: string
  feedbackResult: string
  responsibleName: string
  version: number
}

export interface Task {
  id: number
  projectId: number
  projectNo: string
  taskNo: string
  taskType: string
  title: string
  referencedVersion: string
  status: string
  responsibleName: string
  planStart: string
  planFinish: string
  blockReason: string
  version: number
}

export interface Risk {
  id: number
  projectId: number
  projectNo: string
  level: string
  score: number
  reasons: string[]
  suggestion: string
  status: string
}

export interface RiskAction {
  id: number
  riskId: number
  actionPlan: string
  responsibleUserId: number
  responsibleName: string
  planFinishTime: string
  closeSummary?: string
  status: string
  version: number
  createdAt?: string
  closedAt?: string
}

export interface ProjectMilestone {
  code: string
  label: string
  state: 'DONE' | 'CURRENT' | 'PENDING'
  occurredAt?: string
}

export interface VersionConflict {
  taskId: number
  taskNo: string
  taskTitle: string
  referencedVersion?: string
  currentVersion?: string
}

export interface QrCodeBinding {
  code: string
  businessType: string
  businessId: number
  payload: string
  status: string
}

export interface ProjectDetail {
  project: Project
  requirements: Requirement[]
  members: ProjectMember[]
  documents: DocumentVersion[]
  samples: Sample[]
  changes: ChangeOrder[]
  tasks: Task[]
  risks: Risk[]
  timeline: Array<{ id: number; title: string; summary?: string; operatorName?: string; occurredAt?: string }>
}

export interface ProjectWorkspace {
  detail: ProjectDetail
  currentRisk?: Risk
  riskActions: RiskAction[]
  milestones: ProjectMilestone[]
  versionConflicts: VersionConflict[]
  flowQrCode?: QrCodeBinding
}

export interface Message {
  id: number
  title: string
  content: string
  type: string
  status: string
  businessType: string
  businessId: number
  createdAt: string
}

export interface ReportOverview {
  metrics: Record<string, number>
  riskLevels: Array<Record<string, unknown>>
  changeTypes: Array<Record<string, unknown>>
  sampleEfficiency: Array<Record<string, unknown>>
  deliveryStats: Array<Record<string, unknown>>
  delayReasons: Array<Record<string, unknown>>
  departmentLoads: Array<Record<string, unknown>>
}

export interface Workbench {
  metrics: Record<string, number>
  todos: Task[]
  dueTasks: Task[]
  unreadNotifications: Message[]
  watchedProjects: Project[]
  recentOperations: Array<Record<string, unknown>>
  riskCards: Risk[]
  shortcuts: Array<Record<string, unknown>>
}

export type DashboardPeriodCode = '7D' | '30D' | 'MONTH' | 'CUSTOM'

export interface DashboardPeriod {
  period: DashboardPeriodCode
  startDate: string
  endDate: string
}

export interface DashboardChartItem {
  name: string
  value: number
}

export interface DashboardTrendPoint {
  date: string
  dueTasks: number
  completedTasks: number
}

export interface DashboardDepartmentLoad {
  name: string
  todoTasks: number
  overdueTasks: number
  impactedTasks: number
}

export interface DashboardData {
  period: DashboardPeriod
  metrics: Record<string, number>
  deliveryTrend: DashboardTrendPoint[]
  riskDistribution: DashboardChartItem[]
  changeTypes: DashboardChartItem[]
  departmentLoads: DashboardDepartmentLoad[]
  projects: Project[]
  criticalRisks: Risk[]
  todos: Task[]
  dueTasks: Task[]
  unreadNotifications: Message[]
  shortcuts: Array<{ name: string; route: string; permission?: string }>
}

export interface SearchResultItem {
  type: string
  id: number
  title: string
  summary: string
  route: string
}

export interface ExportTask {
  id: number
  exportType: string
  status: string
  fileName: string
  querySummary: string
  downloadCount: number
  createdAt: string
  finishedAt: string
}

export interface ImportTask {
  id: number
  importType: string
  mode: string
  status: string
  successCount: number
  failCount: number
  errors: string[]
  createdAt: string
}

export interface RuleParam {
  id: number
  ruleCode: string
  ruleName: string
  ruleVersion: string
  params: Record<string, unknown>
  status: string
  publishedAt?: string
}

export interface AuditLog {
  id: number
  userName: string
  moduleName: string
  operationType: string
  businessType: string
  businessId: number
  result: string
  summary: string
  operatedAt: string
}
