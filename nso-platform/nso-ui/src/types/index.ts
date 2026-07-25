export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
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
}
