<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import FlowQrCard from '@/components/FlowQrCard.vue'
import type { Customer, Project, ProjectDetail, ProjectWorkspace, QrCodeBinding, RiskAction } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const workspace = ref<ProjectWorkspace>()
const selectedProjectId = ref<number>()
const loading = ref(false)
const workspaceLoading = ref(false)
const createDrawerVisible = ref(false)
const customerDialogVisible = ref(false)
const customerSaving = ref(false)
const riskDrawerVisible = ref(false)
const conflictDialogVisible = ref(false)
const documentQr = ref<QrCodeBinding>()
const documentQrVisible = ref(false)
const detailTab = ref('technical')

const form = reactive({
  customerId: undefined as number | undefined,
  customerName: '',
  productName: '',
  quantity: 1,
  targetDate: '',
  ownerName: '',
  priority: 'MEDIUM'
})
const customerForm = reactive({ name: '', industry: '', contactName: '', phone: '', status: 'ENABLED' })
const requirementForm = reactive({ category: 'CUSTOMER', content: '', confirmStatus: 'UNCONFIRMED', reason: '' })
const memberForm = reactive({ userId: undefined as number | undefined, memberName: '', projectRole: '', departmentName: '' })
const actionForm = reactive({ actionPlan: '', responsibleUserId: undefined as number | undefined, planFinishTime: '' })
const closeForm = reactive({ id: 0, version: 0, closeSummary: '' })

const activeProject = computed<Project | undefined>(() => workspace.value?.detail.project)
const currentRisk = computed(() => workspace.value?.currentRisk)
const activeMembers = computed(() => workspace.value?.detail.members || [])
const riskReasons = computed(() => currentRisk.value?.reasons?.filter(Boolean) || [])

const stageLabel = (stage?: string) => ({
  REQUIREMENT: '需求评审', TECHNICAL: '图纸发布', SAMPLE: '样品确认',
  CONFIRM: '样品确认', EXECUTION: '正式投产', DELIVERY: '交付'
} as Record<string, string>)[stage || ''] || stage || '-'

const statusLabel = (status?: string) => ({
  DRAFT: '草稿', REVIEWING: '评审中', TECH_PUBLISHED: '生产准备中', SAMPLE: '样品中',
  EXECUTION: '生产执行中', COMPLETED: '已完成', CANCELLED: '已取消', OPEN: '处理中', CLOSED: '已关闭'
} as Record<string, string>)[status || ''] || status || '-'

const statusTag = (status?: string) => ({
  COMPLETED: 'success', CLOSED: 'success', CANCELLED: 'info', REVIEWING: 'warning', SAMPLE: 'warning'
} as Record<string, string>)[status || ''] || ''

const riskTag = (level?: string) => ({ SERIOUS: 'danger', HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' } as Record<string, string>)[level || ''] || 'info'
const confirmTag = (status?: string) => ({ CUSTOMER_CONFIRMED: 'success', INTERNAL_CONFIRMED: '', UNCONFIRMED: 'warning', DISPUTED: 'danger' } as Record<string, string>)[status || ''] || 'info'
const dateText = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '-'
const asDetail = () => workspace.value?.detail as ProjectDetail | undefined

async function loadProjects() {
  loading.value = true
  try {
    const [customerData, projectData] = await Promise.all([
      auth.can('customer:view') ? api.customers() : Promise.resolve({ list: [] }),
      api.projects()
    ])
    customers.value = customerData.list || []
    projects.value = projectData.list || []
    if (!form.customerId && customers.value[0]) form.customerId = customers.value[0].id
  } catch (error) {
    ElMessage.error(userFacingError(error, '项目列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function openProject(projectId: number, updateRoute = true) {
  workspaceLoading.value = true
  try {
    const next = await api.projectWorkspace(projectId)
    if (!next.flowQrCode && auth.can('project:view')) next.flowQrCode = await api.createProjectFlowQr(projectId)
    workspace.value = next
    selectedProjectId.value = projectId
    detailTab.value = 'technical'
    if (updateRoute) await router.replace({ name: 'projects', query: { id: String(projectId) } })
  } catch (error) {
    ElMessage.error(userFacingError(error, '项目工作台加载失败'))
  } finally {
    workspaceLoading.value = false
  }
}

function openRow(row: Project) { void openProject(row.id) }

async function closeWorkspace() {
  workspace.value = undefined
  selectedProjectId.value = undefined
  await router.replace({ name: 'projects' })
}

function openCreate() { createDrawerVisible.value = true }

async function createProject() {
  if (!form.productName.trim()) return ElMessage.warning('请填写产品名称')
  if (!form.customerId && !form.customerName.trim()) return ElMessage.warning('请选择或填写客户')
  try {
    const created = await api.createProject({ ...form, productName: form.productName.trim(), customerName: form.customerName.trim() })
    ElMessage.success('项目已创建')
    createDrawerVisible.value = false
    Object.assign(form, { customerName: '', productName: '', quantity: 1, targetDate: '', ownerName: '', priority: 'MEDIUM' })
    await loadProjects()
    await openProject(created.id)
  } catch (error) {
    ElMessage.error(userFacingError(error, '项目创建失败'))
  }
}

async function createCustomer() {
  if (!customerForm.name.trim()) return ElMessage.warning('请填写客户名称')
  customerSaving.value = true
  try {
    const customer = await api.createCustomer({ ...customerForm, name: customerForm.name.trim() })
    customers.value = [customer, ...customers.value]
    form.customerId = customer.id
    customerDialogVisible.value = false
    Object.assign(customerForm, { name: '', industry: '', contactName: '', phone: '' })
    ElMessage.success('客户已创建')
  } catch (error) {
    ElMessage.error(userFacingError(error, '客户创建失败'))
  } finally { customerSaving.value = false }
}

async function submitReview(row: Project) {
  try {
    await ElMessageBox.confirm('确认提交需求评审？', '提交评审', { type: 'warning' })
    await api.submitReview(row.id)
    ElMessage.success('已提交需求评审')
    await loadProjects()
    if (row.id === selectedProjectId.value) await openProject(row.id, false)
  } catch { /* user cancelled */ }
}

async function refreshWorkspace() {
  if (selectedProjectId.value) await openProject(selectedProjectId.value, false)
}

async function addRequirement() {
  const detail = asDetail()
  if (!detail?.project.id) return
  if (!requirementForm.content.trim()) return ElMessage.warning('请填写需求内容')
  try {
    await api.saveRequirement(detail.project.id, { ...requirementForm, content: requirementForm.content.trim() })
    requirementForm.content = ''
    ElMessage.success('需求项已保存')
    await refreshWorkspace()
  } catch (error) { ElMessage.error(userFacingError(error, '需求保存失败')) }
}

async function confirmRequirement(row: Record<string, unknown>, status: string) {
  try {
    await api.confirmRequirement(Number(row.id), { ...row, confirmStatus: status, reason: status === 'CUSTOMER_CONFIRMED' ? '客户已确认' : status === 'DISPUTED' ? '存在异议，等待处理' : '内部已确认' })
    ElMessage.success('需求状态已更新')
    await refreshWorkspace()
  } catch (error) { ElMessage.error(userFacingError(error, '确认失败')) }
}

async function addMember() {
  const detail = asDetail()
  if (!detail?.project.id) return
  if (!memberForm.userId || !memberForm.memberName.trim() || !memberForm.projectRole) return ElMessage.warning('请填写成员用户 ID、姓名和项目角色')
  try {
    await api.addMember(detail.project.id, { ...memberForm, memberName: memberForm.memberName.trim() })
    Object.assign(memberForm, { userId: undefined, memberName: '', projectRole: '', departmentName: '' })
    ElMessage.success('项目成员已加入')
    await refreshWorkspace()
  } catch (error) { ElMessage.error(userFacingError(error, '添加成员失败')) }
}

async function createRiskAction() {
  const risk = currentRisk.value
  if (!risk) return
  if (!actionForm.actionPlan.trim() || !actionForm.responsibleUserId || !actionForm.planFinishTime) return ElMessage.warning('请填写措施、责任人和计划完成时间')
  try {
    await api.createRiskAction(risk.id, {
      actionPlan: actionForm.actionPlan.trim(),
      responsibleUserId: actionForm.responsibleUserId,
      planFinishTime: actionForm.planFinishTime,
      idempotencyKey: crypto.randomUUID()
    })
    Object.assign(actionForm, { actionPlan: '', responsibleUserId: undefined, planFinishTime: '' })
    ElMessage.success('风险处置项已创建')
    await refreshWorkspace()
  } catch (error) { ElMessage.error(userFacingError(error, '风险处置项创建失败')) }
}

function prepareClose(action: RiskAction) {
  Object.assign(closeForm, { id: action.id, version: action.version, closeSummary: '' })
}

async function closeRiskAction() {
  if (!closeForm.closeSummary.trim()) return ElMessage.warning('关闭处置项必须填写总结')
  try {
    await api.closeRiskAction(closeForm.id, { closeSummary: closeForm.closeSummary.trim(), version: closeForm.version })
    ElMessage.success('处置项已关闭')
    Object.assign(closeForm, { id: 0, version: 0, closeSummary: '' })
    await refreshWorkspace()
  } catch (error) { ElMessage.error(userFacingError(error, '关闭处置项失败')) }
}

async function syncTechnicalTasks() {
  const project = activeProject.value
  if (!project) return
  try {
    await ElMessageBox.confirm('将所有未完成任务同步到当前技术包版本。已完成任务不会改动。', '同步技术版本', { type: 'warning' })
    const result = await api.syncTechnicalPackageTasks(project.id)
    ElMessage.success(`已同步 ${result.updatedTaskCount} 个未完成任务至 ${result.versionNo}`)
    conflictDialogVisible.value = false
    await refreshWorkspace()
  } catch { /* user cancelled or request failed */ }
}

async function showDocumentQr(documentId: number) {
  try {
    documentQr.value = await api.createDocumentQr(documentId)
    documentQrVisible.value = true
  } catch (error) { ElMessage.error(userFacingError(error, '文件二维码生成失败')) }
}

function downloadDocument(row: { downloadUrl?: string }) {
  if (row.downloadUrl) window.open(row.downloadUrl, '_blank', 'noopener')
  else ElMessage.warning('当前文件没有可下载内容')
}

watch(() => route.query.id, (id) => {
  const projectId = Number(id)
  if (projectId && projectId !== selectedProjectId.value) void openProject(projectId, false)
}, { immediate: true })
watch(() => route.query.create, (value) => { if (value === '1' && auth.can('project:create')) createDrawerVisible.value = true }, { immediate: true })
onMounted(() => { void loadProjects() })
</script>

<template>
  <div class="stack project-page" v-loading="loading">
    <section v-if="!workspace" class="panel project-directory">
      <header class="panel-header">
        <span class="panel-title">项目管理</span>
        <div class="toolbar-right">
          <el-button v-if="auth.can('project:create')" type="primary" :icon="'Plus'" @click="openCreate">新建项目</el-button>
        </div>
      </header>
      <el-table :data="projects" class="table--interactive" height="250" highlight-current-row :current-row-key="selectedProjectId" row-key="id" @row-click="openRow">
        <el-table-column prop="projectNo" label="项目编号" width="170" />
        <el-table-column prop="customerName" label="客户" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品/治具" min-width="210" show-overflow-tooltip />
        <el-table-column label="目标交期" width="130">
          <template #default="{ row }">{{ row.targetDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="阶段" width="120"><template #default="{ row }"><el-tag size="small" effect="plain">{{ stageLabel(row.stage) }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag size="small" :type="statusTag(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openRow(row)">打开工作台</el-button>
            <el-button v-if="auth.can('project:manage') && row.status === 'DRAFT'" link type="warning" @click.stop="submitReview(row)">提交评审</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div v-if="workspace" class="toolbar project-workspace__toolbar">
      <el-button :icon="'Back'" @click="closeWorkspace">项目列表</el-button>
      <span class="subtle">项目详情工作台</span>
    </div>

    <div v-if="workspace" class="project-workspace" v-loading="workspaceLoading">
      <main class="project-workspace__main">
        <section class="panel project-summary">
          <div class="project-summary__head">
            <div>
              <div class="status-line">
                <h1>{{ activeProject?.productName }}</h1>
                <el-tag type="primary" effect="dark">{{ statusLabel(activeProject?.status) }}</el-tag>
              </div>
              <p class="project-summary__number">{{ activeProject?.projectNo }}</p>
            </div>
            <el-button :icon="'Refresh'" @click="refreshWorkspace">刷新工作台</el-button>
          </div>
          <div class="project-summary__meta">
            <div><span>客户名称</span><strong>{{ activeProject?.customerName || '-' }}</strong></div>
            <div><span>目标交期</span><strong>{{ activeProject?.targetDate || '-' }}</strong></div>
            <div><span>项目经理</span><strong>{{ activeProject?.ownerName || '-' }}</strong></div>
            <div><span>当前阶段</span><strong>{{ stageLabel(activeProject?.stage) }}</strong></div>
          </div>
        </section>

        <section v-if="workspace.versionConflicts.length" class="workspace-alert">
          <div class="workspace-alert__body">
            <h2><el-icon><WarningFilled /></el-icon> 版本冲突警示</h2>
            <p>检测到 {{ workspace.versionConflicts.length }} 个未完成任务仍引用旧版技术资料。请核对版本差异后同步，避免现场使用非受控版本。</p>
            <el-button type="danger" @click="conflictDialogVisible = true">查看差异</el-button>
            <el-button v-if="auth.can('document:sync')" plain type="danger" @click="syncTechnicalTasks">同步至当前技术包</el-button>
          </div>
        </section>

        <section class="panel milestone-panel">
          <h2>项目里程碑</h2>
          <div class="milestones">
            <div v-for="milestone in workspace.milestones" :key="milestone.code" class="milestone" :class="`milestone--${milestone.state.toLowerCase()}`">
              <span class="milestone__point"><el-icon v-if="milestone.state === 'DONE'"><Check /></el-icon><el-icon v-else-if="milestone.state === 'CURRENT'"><LocationFilled /></el-icon></span>
              <strong class="milestone__label">{{ milestone.label }}</strong>
              <span class="milestone__date">{{ milestone.state === 'CURRENT' ? '进行中' : dateText(milestone.occurredAt) }}</span>
            </div>
          </div>
        </section>

        <section class="panel workspace-tabs">
          <el-tabs v-model="detailTab">
            <el-tab-pane label="技术资料（技术包）" name="technical">
              <div class="panel-body toolbar">
                <div class="subtle">当前项目技术文件及其受控版本</div>
                <el-button v-if="workspace.versionConflicts.length && auth.can('document:sync')" type="primary" :icon="'Refresh'" @click="syncTechnicalTasks">同步任务版本</el-button>
              </div>
              <el-table :data="workspace.detail.documents" size="small">
                <el-table-column prop="fileName" label="文件名称" min-width="220" show-overflow-tooltip />
                <el-table-column prop="versionNo" label="版本" width="90" />
                <el-table-column prop="fileType" label="类型" width="110" />
                <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag size="small" :type="row.currentVersion ? 'success' : 'info'" effect="plain">{{ row.currentVersion ? '受控生效' : row.status }}</el-tag></template></el-table-column>
                <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="downloadDocument(row)">下载</el-button><el-button link type="primary" @click="showDocumentQr(row.id)">二维码</el-button></template></el-table-column>
              </el-table>
              <el-empty v-if="!workspace.detail.documents.length" description="暂无技术文件" :image-size="64" />
            </el-tab-pane>
            <el-tab-pane label="样品确认" name="samples">
              <el-table :data="workspace.detail.samples" size="small"><el-table-column prop="sampleNo" label="样品编号" width="160" /><el-table-column prop="purpose" label="用途" min-width="180" /><el-table-column prop="referencedVersion" label="引用版本" width="110" /><el-table-column prop="planFinishDate" label="计划完成" width="130" /><el-table-column prop="confirmConclusion" label="确认结论" min-width="140" /></el-table>
              <el-empty v-if="!workspace.detail.samples.length" description="暂无样品确认记录" :image-size="64" />
            </el-tab-pane>
            <el-tab-pane label="变更记录" name="changes">
              <el-table :data="workspace.detail.changes" size="small"><el-table-column prop="changeNo" label="变更单号" width="160" /><el-table-column prop="changeType" label="类型" width="110" /><el-table-column prop="afterContent" label="变更内容" min-width="250" show-overflow-tooltip /><el-table-column prop="status" label="状态" width="110" /><el-table-column prop="delayDays" label="影响天数" width="100" /></el-table>
              <el-empty v-if="!workspace.detail.changes.length" description="暂无变更记录" :image-size="64" />
            </el-tab-pane>
            <el-tab-pane label="项目事件" name="timeline">
              <el-timeline class="panel-body" v-if="workspace.detail.timeline.length"><el-timeline-item v-for="item in workspace.detail.timeline" :key="item.id" :timestamp="dateText(item.occurredAt)"><strong>{{ item.title }}</strong><div class="subtle">{{ item.summary }} {{ item.operatorName ? `· ${item.operatorName}` : '' }}</div></el-timeline-item></el-timeline>
              <el-empty v-else description="暂无项目事件" :image-size="64" />
            </el-tab-pane>
            <el-tab-pane label="需求与成员" name="configuration">
              <div class="panel-body stack">
                <section>
                  <div v-if="auth.can('project:requirement:manage')" class="toolbar-left inline-form">
                    <el-select v-model="requirementForm.category" style="width: 120px"><el-option label="尺寸" value="SIZE" /><el-option label="材料" value="MATERIAL" /><el-option label="性能" value="PERFORMANCE" /><el-option label="外观" value="APPEARANCE" /><el-option label="客户" value="CUSTOMER" /><el-option label="通用" value="GENERAL" /></el-select>
                    <el-input v-model="requirementForm.content" placeholder="需求内容" style="width: min(360px, 100%)" @keyup.enter="addRequirement" />
                    <el-button type="primary" @click="addRequirement">新增需求</el-button>
                  </div>
                  <el-table :data="workspace.detail.requirements" size="small" style="margin-top: 14px"><el-table-column prop="category" label="分类" width="100" /><el-table-column prop="content" label="内容" min-width="240" /><el-table-column label="确认状态" width="150"><template #default="{ row }"><el-tag size="small" :type="confirmTag(row.confirmStatus)" effect="plain">{{ row.confirmStatus }}</el-tag></template></el-table-column><el-table-column label="操作" width="180"><template #default="{ row }"><el-button v-if="auth.can('project:requirement:manage')" link type="success" @click="confirmRequirement(row, 'CUSTOMER_CONFIRMED')">客户确认</el-button><el-button v-if="auth.can('project:requirement:manage')" link type="warning" @click="confirmRequirement(row, 'INTERNAL_CONFIRMED')">内部确认</el-button></template></el-table-column></el-table>
                </section>
                <section>
                  <div v-if="auth.can('project:member:manage')" class="toolbar-left inline-form">
                    <el-input-number v-model="memberForm.userId" :min="1" placeholder="用户 ID" style="width: 124px" />
                    <el-input v-model="memberForm.memberName" placeholder="成员姓名" style="width: 130px" />
                    <el-select v-model="memberForm.projectRole" placeholder="项目角色" style="width: 160px"><el-option label="项目经理" value="PROJECT_MANAGER" /><el-option label="技术负责人" value="TECHNICAL" /><el-option label="工艺负责人" value="PROCESS" /><el-option label="采购负责人" value="PURCHASER" /><el-option label="生产负责人" value="PRODUCTION" /><el-option label="质量负责人" value="QUALITY" /><el-option label="现场执行" value="FIELD_USER" /></el-select>
                    <el-button type="primary" @click="addMember">添加成员</el-button>
                  </div>
                  <el-table :data="workspace.detail.members" size="small" style="margin-top: 14px"><el-table-column prop="memberName" label="成员" width="130" /><el-table-column prop="projectRole" label="项目角色" width="180" /><el-table-column prop="departmentName" label="部门" min-width="160" /><el-table-column prop="status" label="状态" width="100" /></el-table>
                </section>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </main>

      <aside class="project-workspace__side">
        <section v-if="currentRisk" class="panel risk-panel">
          <div class="risk-panel__score"><h2><el-icon><Warning /></el-icon> 交期风险评估</h2><strong class="risk-panel__level">{{ currentRisk.level === 'SERIOUS' || currentRisk.level === 'HIGH' ? '高风险' : '中风险' }}</strong></div>
          <div class="risk-panel__content"><p class="subtle">风险诱因摘要</p><p>{{ riskReasons.join('；') || currentRisk.suggestion || '系统尚未记录风险诱因。' }}</p><el-button v-if="auth.can('risk:dispose')" style="width:100%; margin-top: 18px" @click="riskDrawerVisible = true">查看风险处置单</el-button></div>
        </section>
        <section v-else class="panel risk-panel"><div class="risk-panel__score"><h2><el-icon><CircleCheck /></el-icon> 交期风险评估</h2><strong class="risk-panel__level" style="color: var(--nso-success)">风险可控</strong></div><div class="risk-panel__content"><p>当前项目没有待处置风险。</p></div></section>
        <FlowQrCard v-if="workspace.flowQrCode" :code="workspace.flowQrCode.code" :payload="workspace.flowQrCode.payload" :subtitle="`项目 ID：${activeProject?.projectNo || '-'}`" />
        <section class="panel"><header class="panel-header"><span class="panel-title">常用操作</span></header><div class="panel-body quick-actions"><el-button v-if="auth.can('change:create')" text :icon="'ArrowRight'" @click="router.push({ name: 'changes' })">发起设计变更（ECR）</el-button><el-button v-if="auth.can('sample:view')" text :icon="'ArrowRight'" @click="router.push({ name: 'samples' })">记录样品测试结果</el-button><el-button v-if="auth.can('task:view')" text :icon="'ArrowRight'" @click="router.push({ name: 'tasks' })">分配加工任务</el-button></div></section>
      </aside>
    </div>

    <section v-else class="panel"><el-empty description="从项目列表选择一个项目，打开协同工作台" :image-size="80" /></section>

    <el-drawer v-model="createDrawerVisible" title="新建项目" size="min(520px, 100%)" @closed="router.replace({ name: 'projects', query: selectedProjectId ? { id: String(selectedProjectId) } : {} })">
      <el-form label-position="top" @submit.prevent="createProject">
        <el-form-item label="客户"><el-select v-model="form.customerId" placeholder="选择已有客户" clearable filterable style="width:100%"><el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" /></el-select><el-button v-if="auth.can('customer:manage')" link type="primary" @click="customerDialogVisible = true">新建客户</el-button></el-form-item>
        <el-form-item label="或输入新客户名称"><el-input v-model="form.customerName" /></el-form-item>
        <el-form-item label="产品/治具名称" required><el-input v-model="form.productName" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" /></el-form-item>
        <el-form-item label="目标交期"><el-date-picker v-model="form.targetDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="项目经理"><el-input v-model="form.ownerName" /></el-form-item>
        <el-form-item label="优先级"><el-radio-group v-model="form.priority"><el-radio-button label="HIGH">高</el-radio-button><el-radio-button label="MEDIUM">中</el-radio-button><el-radio-button label="LOW">低</el-radio-button></el-radio-group></el-form-item>
        <el-button native-type="submit" type="primary" style="width:100%">创建项目</el-button>
      </el-form>
    </el-drawer>

    <el-dialog v-model="customerDialogVisible" title="新建客户" width="min(520px, 92vw)"><el-form label-position="top"><el-form-item label="客户名称" required><el-input v-model="customerForm.name" /></el-form-item><el-form-item label="所属行业"><el-input v-model="customerForm.industry" /></el-form-item><el-form-item label="联系人"><el-input v-model="customerForm.contactName" /></el-form-item><el-form-item label="联系电话"><el-input v-model="customerForm.phone" /></el-form-item></el-form><template #footer><el-button @click="customerDialogVisible = false">取消</el-button><el-button type="primary" :loading="customerSaving" @click="createCustomer">保存客户</el-button></template></el-dialog>

    <el-drawer v-model="riskDrawerVisible" title="风险处置单" size="min(560px, 100%)">
      <div v-if="currentRisk" class="stack"><el-alert :title="`${currentRisk.level} · ${riskReasons.join('；') || currentRisk.suggestion || '项目风险'}`" :type="riskTag(currentRisk.level) === 'danger' ? 'error' : 'warning'" :closable="false" show-icon />
        <section><h3>已有处置项</h3><div v-if="workspace?.riskActions.length" class="risk-action-list"><div v-for="action in workspace?.riskActions" :key="action.id" class="risk-action"><strong>{{ action.actionPlan }}</strong><div class="risk-action__meta"><span>{{ action.responsibleName || `用户 ${action.responsibleUserId}` }} · {{ dateText(action.planFinishTime) }}</span><el-tag size="small" :type="action.status === 'CLOSED' ? 'success' : 'warning'">{{ action.status === 'CLOSED' ? '已关闭' : '待完成' }}</el-tag></div><p v-if="action.closeSummary" class="subtle">总结：{{ action.closeSummary }}</p><el-button v-if="action.status !== 'CLOSED' && auth.can('risk:dispose')" link type="primary" @click="prepareClose(action)">关闭处置项</el-button></div></div><el-empty v-else description="暂无风险处置项" :image-size="56" /></section>
        <el-divider />
        <section v-if="auth.can('risk:dispose')"><h3>新建处置项</h3><el-form label-position="top"><el-form-item label="措施" required><el-input v-model="actionForm.actionPlan" type="textarea" :rows="3" placeholder="明确处置措施及检查标准" /></el-form-item><el-form-item label="责任人" required><el-select v-model="actionForm.responsibleUserId" placeholder="选择项目成员" style="width:100%"><el-option v-for="member in activeMembers" :key="member.userId" :label="`${member.memberName} · ${member.projectRole}`" :value="member.userId" /></el-select></el-form-item><el-form-item label="计划完成时间" required><el-date-picker v-model="actionForm.planFinishTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item><el-button type="primary" @click="createRiskAction">创建处置项</el-button></el-form></section>
        <section v-if="closeForm.id"><el-divider /><h3>关闭处置项</h3><el-input v-model="closeForm.closeSummary" type="textarea" :rows="3" placeholder="填写关闭总结和验证结果" /><el-button type="primary" style="margin-top: 12px" @click="closeRiskAction">确认关闭</el-button></section>
      </div>
    </el-drawer>

    <el-dialog v-model="conflictDialogVisible" title="技术版本差异" width="min(760px, 94vw)"><p class="subtle">活跃任务引用的版本与当前受控技术包不一致；同步只影响未完成任务。</p><el-table :data="workspace?.versionConflicts || []"><el-table-column prop="taskNo" label="任务编号" width="150" /><el-table-column prop="taskTitle" label="任务名称" min-width="180" /><el-table-column prop="referencedVersion" label="任务引用版本" width="130" /><el-table-column prop="currentVersion" label="当前受控版本" width="130" /></el-table><template #footer><el-button @click="conflictDialogVisible = false">取消</el-button><el-button v-if="auth.can('document:sync')" type="primary" @click="syncTechnicalTasks">同步未完成任务</el-button></template></el-dialog>
    <el-dialog v-model="documentQrVisible" title="技术文件二维码" width="min(380px, 92vw)"><FlowQrCard v-if="documentQr" compact :code="documentQr.code" :payload="documentQr.payload" title="受控技术文件" subtitle="扫码后经权限校验查看" /></el-dialog>
  </div>
</template>
