import { getData, postData } from '@/utils/request'
import type {
  ChangeImpact,
  ChangeOrder,
  Customer,
  DocumentVersion,
  Message,
  PageResult,
  Project,
  ReportOverview,
  Risk,
  Sample,
  Task
} from '@/types'

export const api = {
  login: (data: { username: string; password: string }) => postData<any>('/admin/auth/login', data),
  me: () => getData<any>('/admin/auth/me'),
  customers: (keyword?: string) => getData<PageResult<Customer>>('/admin/customers', { keyword }),
  createCustomer: (data: Partial<Customer>) => postData<Customer>('/admin/customers', data),
  projects: (keyword?: string) => getData<PageResult<Project>>('/admin/projects', { keyword }),
  createProject: (data: Record<string, unknown>) => postData<Project>('/admin/projects', data),
  project: (id: number) => getData<any>(`/admin/projects/${id}`),
  submitReview: (id: number) => postData<Project>(`/admin/projects/${id}/submit-review`),
  documents: (projectId?: number) => getData<PageResult<DocumentVersion>>('/admin/documents', { projectId }),
  createDocument: (projectId: number, data: Record<string, unknown>) =>
    postData<DocumentVersion>(`/admin/documents/${projectId}/versions`, data),
  publishDocument: (id: number) => postData<DocumentVersion>(`/admin/document-versions/${id}/publish`),
  samples: (projectId?: number) => getData<PageResult<Sample>>('/admin/samples', { projectId }),
  createSample: (data: Record<string, unknown>) => postData<Sample>('/admin/samples', data),
  submitSampleConfirm: (id: number) => postData<Sample>(`/admin/samples/${id}/submit-confirm`),
  confirmSample: (id: number, data: Record<string, unknown>) => postData<Sample>(`/admin/samples/${id}/confirm`, data),
  changes: (projectId?: number) => getData<PageResult<ChangeOrder>>('/admin/changes', { projectId }),
  createChange: (data: Record<string, unknown>) => postData<ChangeOrder>('/admin/changes', data),
  analyzeChange: (id: number) => postData<ChangeImpact[]>(`/admin/changes/${id}/analyze-impact`),
  impacts: (id: number) => getData<ChangeImpact[]>(`/admin/changes/${id}/impacts`),
  approveChange: (id: number) => postData<ChangeOrder>(`/admin/changes/${id}/approve`),
  feedbackChange: (id: number, data: Record<string, unknown>) => postData<ChangeOrder>(`/admin/changes/${id}/feedback`, data),
  closeChange: (id: number) => postData<ChangeOrder>(`/admin/changes/${id}/close`),
  tasks: (projectId?: number) => getData<PageResult<Task>>('/admin/tasks', { projectId }),
  startTask: (id: number) => postData<Task>(`/admin/tasks/${id}/start`),
  feedbackTask: (id: number, data: Record<string, unknown>) => postData<Task>(`/admin/tasks/${id}/feedback`, data),
  risks: (projectId?: number) => getData<PageResult<Risk>>('/admin/risks', { projectId }),
  messages: (status?: string) => getData<PageResult<Message>>('/admin/messages', { status }),
  readMessage: (id: number) => postData<Message>(`/admin/messages/${id}/read`),
  overview: () => getData<ReportOverview>('/admin/reports/overview')
}
