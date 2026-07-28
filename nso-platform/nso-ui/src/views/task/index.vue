<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import type { Project, ProjectMember, Risk, Task } from '@/types'

const projects = ref<Project[]>([])
const tasks = ref<Task[]>([])
const risks = ref<Risk[]>([])
const deliveryRows = ref<Record<string, unknown>[]>([])
const projectMembers = ref<ProjectMember[]>([])
const activeGroup = ref('')
const loading = ref(false)
const taskFilter = ref('')
const typeFilter = ref('')
const blockError = ref<{ ruleCode?: string; reason?: string; currentValue?: string; expectedValue?: string; action?: string } | null>(null)

const executionForm = reactive({
  projectId: undefined as number | undefined,
  taskType: 'PURCHASE', title: '', referencedVersion: '', responsibleName: '', assigneeId: undefined as number | undefined, planStart: '', planFinish: ''
})
const auth = useAuthStore()

const exceptionForm = reactive({
  taskId: undefined as number | undefined,
  projectId: undefined as number | undefined,
  exceptionType: 'MATERIAL_SHORTAGE', summary: '', reporterName: ''
})

const feedbackForm = reactive({ result: 'DONE', notes: '' })

const taskTypeLabel = (type: string) =>
  ({ PURCHASE: '采购', PRODUCTION: '生产', INSPECTION: '检验', DELIVERY: '交付' } as Record<string, string>)[type] || type

const taskTypeTag = (type: string) =>
  ({ PURCHASE: '', PRODUCTION: 'danger', INSPECTION: 'warning', DELIVERY: 'success' } as Record<string, string>)[type] || 'info'

const statusLabel = (status: string) =>
  ({ TODO: '待执行', IN_PROGRESS: '执行中', BLOCKED: '已阻断', PAUSED: '已暂停', DONE: '已完成' } as Record<string, string>)[status] || status

const statusTag = (status: string) =>
  ({ TODO: 'info', IN_PROGRESS: 'warning', BLOCKED: 'danger', PAUSED: '', DONE: 'success' } as Record<string, string>)[status] || 'info'

const exceptionTypeLabel = (type: string) =>
  ({ MATERIAL_SHORTAGE: '物料短缺', QUALITY_ISSUE: '质量问题', EQUIPMENT_FAILURE: '设备故障', SCHEDULE_DELAY: '交期延迟', LABOR_SHORTAGE: '人员不足', CUSTOMER_CHANGE: '客户变更', OTHER: '其他' } as Record<string, string>)[type] || type

const productionChecklist = computed(() => {
  const prodTasks = tasks.value.filter(t => t.taskType === 'PRODUCTION')
  if (!prodTasks.length) return []
  return prodTasks.map(t => {
    const checks: { label: string; passed: boolean; ruleCode: string; action: string }[] = [
      { label: '样品已客户确认', passed: false, ruleCode: 'SAMPLE_NOT_CONFIRMED', action: '完成样品客户确认' },
      { label: '无未关闭严重风险', passed: false, ruleCode: 'SERIOUS_RISK_OPEN', action: '先关闭风险或走特殊放行流程' },
      { label: '采购任务已全部完成', passed: false, ruleCode: 'MATERIAL_NOT_READY', action: '确认到料后再投产' },
      { label: '引用版本为当前发布版本', passed: false, ruleCode: 'VERSION_MISMATCH', action: '更新任务引用版本' }
    ]
    return { task: t, checks }
  })
})

const filteredTasks = computed(() => {
  let list = tasks.value
  if (activeGroup.value) list = list.filter(t => t.projectNo === activeGroup.value)
  if (taskFilter.value) list = list.filter(t => t.status === taskFilter.value)
  if (typeFilter.value) list = list.filter(t => t.taskType === typeFilter.value)
  return list
})

const projectGroups = computed(() => {
  const set = new Set(tasks.value.map(t => t.projectNo))
  return Array.from(set).sort()
})

async function load() {
  loading.value = true
  try {
    const [projectData, taskData, riskData, deliveryData] = await Promise.all([
      api.projects(), api.tasks() as Promise<any>, auth.can('risk:view') ? api.risks() as Promise<any> : Promise.resolve({ list: [] }), api.deliveries() as Promise<any>
    ])
    projects.value = projectData.list || []
    if (!executionForm.projectId && projects.value[0]) await loadProjectMembers(projects.value[0].id)
    tasks.value = taskData.list || []
    risks.value = riskData.list || []
    deliveryRows.value = deliveryData.list || []
  } catch (error) {
    ElMessage.error(userFacingError(error, '数据加载失败'))
  } finally { loading.value = false }
}

async function loadProjectMembers(projectId?: number) {
  executionForm.projectId = projectId
  executionForm.assigneeId = undefined
  projectMembers.value = []
  if (!projectId || !auth.can('task:plan')) return
  try {
    projectMembers.value = ((await api.members(projectId)) as any).list || []
  } catch (error) {
    ElMessage.error(userFacingError(error, '项目成员加载失败'))
  }
}

function clearBlockError() { blockError.value = null }

async function startTask(row: Task) {
  clearBlockError()
  try {
    await api.startTask(row.id, { version: row.version })
    ElMessage.success(`任务 ${row.taskNo} 已开工`)
    await load()
  } catch (error: any) {
    blockError.value = error?.detail ?? error?.data ?? { reason: userFacingError(error, '任务开工被阻断') }
  }
}

async function pauseTask(row: Task) {
  try {
    await ElMessageBox.confirm('确认暂停此任务？', '暂停任务', { type: 'warning' })
    await api.pauseTask(row.id, { version: row.version })
    ElMessage.success(`任务 ${row.taskNo} 已暂停`)
    await load()
  } catch { /* cancelled */ }
}

async function completeTask(row: Task) {
  try {
    await ElMessageBox.confirm('确认标记此任务为已完成？', '完成任务', { type: 'warning' })
    await api.completeTask(row.id, { version: row.version })
    ElMessage.success(`任务 ${row.taskNo} 已完成`)
    await load()
  } catch { /* cancelled */ }
}

async function submitFeedback(row: Task) {
  if (!feedbackForm.result) { ElMessage.warning('请选择反馈结果'); return }
  try {
    await api.feedbackTask(row.id, { result: feedbackForm.result, notes: feedbackForm.notes, version: row.version })
    ElMessage.success(feedbackForm.result === 'DONE' ? '任务已完成' : '任务已标记为阻断')
    feedbackForm.notes = ''
    clearBlockError()
    await load()
  } catch (error: any) {
    blockError.value = error?.detail ?? error?.data ?? { reason: userFacingError(error, '反馈提交失败') }
  }
}

function doFeedback(row: Task, result: string) {
  feedbackForm.result = result
  submitFeedback(row)
}

async function createExecutionTask() {
  if (!executionForm.projectId) { ElMessage.warning('请选择项目'); return }
  if (!executionForm.title.trim()) { ElMessage.warning('请填写任务标题'); return }
  if (!executionForm.assigneeId) { ElMessage.warning('请选择项目内任务责任人'); return }
  try {
    await api.createExecutionTask({ ...executionForm, title: executionForm.title.trim() })
    ElMessage.success('执行任务已生成')
    executionForm.title = ''
    await load()
  } catch (error) {
    ElMessage.error(userFacingError(error, '任务生成失败'))
  }
}

async function reportException(row?: Task) {
  if (!exceptionForm.summary.trim()) { ElMessage.warning('请填写异常说明'); return }
  const payload: Record<string, unknown> = row
    ? { taskId: row.id, projectId: row.projectId, exceptionType: exceptionForm.exceptionType, summary: exceptionForm.summary.trim(), reporterName: exceptionForm.reporterName }
    : { projectId: exceptionForm.projectId, exceptionType: exceptionForm.exceptionType, summary: exceptionForm.summary.trim(), reporterName: exceptionForm.reporterName }
  try {
    await api.reportException(payload)
    ElMessage.success('异常已上报并生成风险')
    exceptionForm.summary = ''
    await load()
  } catch (error) {
    ElMessage.error(userFacingError(error, '异常上报失败'))
  }
}

async function createDelivery() {
  if (!executionForm.projectId) { ElMessage.warning('请选择项目'); return }
  try {
    await ElMessageBox.confirm('确认创建交付记录？', '交付检查', { type: 'warning' })
    await api.createDelivery({ projectId: executionForm.projectId, quantity: 1, logisticsNo: `LOG-${Date.now()}`, receiver: '客户收货人', feedback: '' })
    ElMessage.success('交付记录已创建')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(userFacingError(error, '交付检查未通过'))
  }
}

onMounted(() => { void load() })
</script>

<template>
  <div class="stack" v-loading="loading">
    <!-- Create task + delivery -->
    <div v-if="auth.can('task:plan') || auth.can('task:execute')" class="panel">
      <div class="panel-header"><span class="panel-title">采购、生产与交付协同</span></div>
      <div class="panel-body inline-form">
        <el-select v-model="executionForm.projectId" placeholder="选择项目 *" filterable style="width: 260px" @change="loadProjectMembers">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
        </el-select>
        <el-select v-model="executionForm.taskType" style="width: 120px">
          <el-option label="采购任务" value="PURCHASE" />
          <el-option label="生产任务" value="PRODUCTION" />
          <el-option label="检验任务" value="INSPECTION" />
          <el-option label="交付任务" value="DELIVERY" />
        </el-select>
        <el-input v-model="executionForm.title" placeholder="任务标题 *" style="width: 180px" />
        <el-input v-model="executionForm.referencedVersion" placeholder="引用版本" style="width: 120px" />
        <el-select v-if="auth.can('task:plan')" v-model="executionForm.assigneeId" placeholder="任务责任人 *" filterable style="width: 160px">
          <el-option v-for="member in projectMembers" :key="member.id" :label="`${member.memberName}（${member.projectRole}）`" :value="member.userId" />
        </el-select>
        <el-date-picker v-model="executionForm.planStart" value-format="YYYY-MM-DD" placeholder="计划开始" style="width: 150px" />
        <el-date-picker v-model="executionForm.planFinish" value-format="YYYY-MM-DD" placeholder="计划完成" style="width: 150px" />
        <el-button v-if="auth.can('task:plan')" type="primary" @click="createExecutionTask">生成任务</el-button>
        <el-button v-if="auth.can('task:execute')" type="success" @click="createDelivery">交付检查</el-button>
      </div>
    </div>

    <!-- Rule-block error banner -->
    <el-alert v-if="blockError" :title="blockError.ruleCode ? `${blockError.ruleCode}: ${blockError.reason}` : blockError.reason" type="error" show-icon :closable="false" @close="clearBlockError">
      <template #default>
        <div class="status-line" style="column-gap:16px; flex-wrap:wrap">
          <span v-if="blockError.currentValue">当前值: <el-tag size="small" type="danger">{{ blockError.currentValue }}</el-tag></span>
          <span v-if="blockError.expectedValue">期望值: <el-tag size="small" type="success">{{ blockError.expectedValue }}</el-tag></span>
          <span v-if="blockError.action">建议: <strong>{{ blockError.action }}</strong></span>
        </div>
        <el-button type="primary" link size="small" style="margin-top:6px" @click="clearBlockError">关闭</el-button>
      </template>
    </el-alert>

    <!-- Task list with production readiness -->
    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">任务执行与版本阻断（{{ filteredTasks.length }}）</span>
          <div style="display:flex; gap:8px">
            <el-select v-model="activeGroup" placeholder="按项目分组" clearable size="small" style="width:160px">
              <el-option v-for="g in projectGroups" :key="g" :label="g" :value="g" />
            </el-select>
            <el-select v-model="taskFilter" placeholder="状态" clearable size="small" style="width:110px">
              <el-option label="待执行" value="TODO" />
              <el-option label="执行中" value="IN_PROGRESS" />
              <el-option label="已阻断" value="BLOCKED" />
              <el-option label="已暂停" value="PAUSED" />
              <el-option label="已完成" value="DONE" />
            </el-select>
            <el-select v-model="typeFilter" placeholder="类型" clearable size="small" style="width:100px">
              <el-option label="采购" value="PURCHASE" />
              <el-option label="生产" value="PRODUCTION" />
              <el-option label="检验" value="INSPECTION" />
              <el-option label="交付" value="DELIVERY" />
            </el-select>
          </div>
        </div>
        <el-table :data="filteredTasks" height="520" size="small">
          <el-table-column prop="taskNo" label="任务号" width="155" />
          <el-table-column prop="projectNo" label="项目" width="150" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="taskTypeTag(row.taskType)" effect="plain" size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="任务" min-width="160" show-overflow-tooltip />
          <el-table-column prop="referencedVersion" label="版本" width="70" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responsibleName" label="责任人" width="90" />
          <el-table-column prop="blockReason" label="阻断原因" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.blockReason" style="color:#f56c6c">{{ row.blockReason }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button v-if="auth.can('task:execute') && ['TODO', 'BLOCKED', 'PAUSED'].includes(row.status)" link type="primary" size="small" @click.stop="startTask(row)">开工</el-button>
              <el-button v-if="auth.can('task:execute') && row.status === 'IN_PROGRESS'" link type="warning" size="small" @click.stop="pauseTask(row)">暂停</el-button>
              <el-button v-if="auth.can('task:feedback') && ['IN_PROGRESS', 'BLOCKED'].includes(row.status)" link type="success" size="small" @click.stop="doFeedback(row, 'DONE')">反馈完成</el-button>
              <el-button v-if="auth.can('task:feedback') && row.status === 'IN_PROGRESS'" link type="danger" size="small" @click.stop="doFeedback(row, 'BLOCKED')">反馈异常</el-button>
              <el-button v-if="auth.can('task:feedback')" link type="danger" size="small" @click.stop="reportException(row)">上报异常</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Right column: production readiness + risks + exception report -->
      <div class="stack">
        <!-- Production readiness checklist -->
        <div class="panel" v-if="productionChecklist.length">
          <div class="panel-header">
            <span class="panel-title">生产就绪检查（{{ productionChecklist.length }} 个生产任务）</span>
          </div>
          <div class="panel-body stack" style="gap: 8px">
            <div v-for="group in productionChecklist" :key="group.task.id" style="border:1px solid #e4e7ed; border-radius:6px; padding:10px">
              <div class="status-line">
                <strong>{{ group.task.taskNo }}</strong>
                <span class="subtle">{{ group.task.title }}</span>
              </div>
              <div v-for="check in group.checks" :key="check.ruleCode" class="status-line" style="margin-top:4px; font-size:12px">
                <el-tag :type="check.passed ? 'success' : 'danger'" size="small" effect="plain">
                  {{ check.passed ? 'PASS' : 'BLOCK' }}
                </el-tag>
                <span>{{ check.label }}</span>
                <span v-if="!check.passed" class="subtle">{{ check.action }}</span>
              </div>
            </div>
            <el-alert title="生产任务开工/完成时会校验：样品已确认、无严重风险、采购已完成、版本匹配" type="info" :closable="false" style="font-size:12px" />
          </div>
        </div>

        <!-- Risk panel -->
        <div v-if="auth.can('risk:view')" class="panel">
          <div class="panel-header">
            <span class="panel-title">风险处置建议（{{ risks.length }}）</span>
          </div>
          <div class="panel-body stack" style="gap: 10px; max-height:320px; overflow-y:auto">
            <div v-for="risk in risks" :key="risk.id" style="border:1px solid #e4e7ed; border-radius:6px; padding:10px">
              <div class="status-line">
                <el-tag :type="risk.level === 'HIGH' || risk.level === 'SERIOUS' ? 'danger' : 'warning'" effect="dark" size="small">
                  {{ risk.level }}
                </el-tag>
                <strong>{{ risk.projectNo }}</strong>
                <span class="subtle">{{ risk.score }} 分</span>
              </div>
              <p class="subtle" style="margin:6px 0 0">{{ (risk.reasons || []).join('；') }}</p>
              <el-alert v-if="risk.suggestion" :title="risk.suggestion" type="warning" :closable="false" style="margin-top:6px" />
            </div>
            <div v-if="!risks.length" class="subtle" style="text-align:center; padding:20px">暂无风险</div>
          </div>
        </div>

        <!-- Exception report form -->
        <div v-if="auth.can('task:feedback')" class="panel">
          <div class="panel-header"><span class="panel-title">快速上报异常</span></div>
          <div class="panel-body stack" style="gap:8px">
            <el-select v-model="exceptionForm.projectId" placeholder="选择项目" filterable size="small" style="width:100%">
              <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
            </el-select>
            <el-select v-model="exceptionForm.exceptionType" placeholder="异常类型" size="small" style="width:100%">
              <el-option v-for="(label, value) in {
                MATERIAL_SHORTAGE: '物料短缺', QUALITY_ISSUE: '质量问题', EQUIPMENT_FAILURE: '设备故障',
                SCHEDULE_DELAY: '交期延迟', LABOR_SHORTAGE: '人员不足', CUSTOMER_CHANGE: '客户变更', OTHER: '其他'
              }" :key="value" :label="label" :value="value" />
            </el-select>
            <el-input v-model="exceptionForm.summary" type="textarea" :rows="2" placeholder="异常说明 *" />
            <el-input v-model="exceptionForm.reporterName" placeholder="上报人" size="small" />
            <el-button type="danger" size="small" @click="reportException()" style="width:100%">上报异常并生成风险</el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- Delivery records -->
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">交付记录（{{ deliveryRows.length }}）</span>
      </div>
      <el-table :data="deliveryRows" height="220" size="small">
        <el-table-column prop="projectNo" label="项目" width="160" />
        <el-table-column prop="quantity" label="交付数量" width="100" />
        <el-table-column prop="logisticsNo" label="物流单号" min-width="180" />
        <el-table-column prop="receiver" label="签收方" width="140" />
        <el-table-column prop="customerFeedback" label="客户反馈" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SHIPPED' ? 'success' : 'warning'" effect="plain" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shippedAt" label="发货时间" width="170" />
      </el-table>
    </div>
  </div>
</template>
