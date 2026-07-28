<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import type { ChangeImpact, ChangeOrder, Project } from '@/types'

const projects = ref<Project[]>([])
const changes = ref<ChangeOrder[]>([])
const impacts = ref<ChangeImpact[]>([])
const activeChange = ref<ChangeOrder>()
const loading = ref(false)
const changeFilter = ref('')
const auth = useAuthStore()

const form = reactive({
  projectId: undefined as number | undefined,
  changeType: 'DESIGN', urgency: 'NORMAL',
  beforeContent: '', afterContent: '', reason: ''
})

const statusTag = (status: string) =>
  ({ DRAFT: 'info', WAIT_IMPACT: 'warning', WAIT_APPROVAL: '', EXECUTING: 'warning', CLOSED: 'success' } as Record<string, string>)[status] || 'info'

const statusLabel = (status: string) =>
  ({ DRAFT: '草稿/已驳回', WAIT_IMPACT: '待影响分析', WAIT_APPROVAL: '待审批', EXECUTING: '执行中', CLOSED: '已关闭' } as Record<string, string>)[status] || status

const impactTypeTag = (type: string) =>
  ({ DOCUMENT: '', BOM: 'success', INSPECTION: '', SAMPLE: 'warning', PROCUREMENT: 'danger', PRODUCTION: 'danger' } as Record<string, string>)[type] || 'info'

const impactTypeLabel = (type: string) =>
  ({ DOCUMENT: '图纸', BOM: 'BOM', INSPECTION: '检验规范', SAMPLE: '样品', PROCUREMENT: '采购任务', PRODUCTION: '生产任务' } as Record<string, string>)[type] || type

const filteredChanges = computed(() => {
  if (!changeFilter.value) return changes.value
  return changes.value.filter(c => c.status === changeFilter.value)
})

async function load() {
  loading.value = true
  try {
    projects.value = ((await api.projects()) as any).list || []
    changes.value = ((await api.changes(undefined)) as any).list || []
    if (activeChange.value) {
      impacts.value = ((await api.impacts(activeChange.value.id)) as any) || []
    }
  } catch (error) {
    ElMessage.error(userFacingError(error, '数据加载失败'))
  } finally { loading.value = false }
}

async function createChange() {
  if (!form.projectId) { ElMessage.warning('请选择项目'); return }
  if (!form.afterContent.trim()) { ElMessage.warning('请填写变更后内容'); return }
  try {
    await api.createChange({ ...form, afterContent: form.afterContent.trim() })
    ElMessage.success('变更单已发起')
    form.beforeContent = ''; form.afterContent = ''; form.reason = ''
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '变更发起失败')) }
}

async function selectChange(row: ChangeOrder) {
  activeChange.value = row
  try { impacts.value = ((await api.impacts(row.id)) as any) || [] }
  catch { impacts.value = [] }
}

async function analyzeImpact(row: ChangeOrder) {
  try {
    impacts.value = ((await api.analyzeChange(row.id)) as any) || []
    activeChange.value = row
    ElMessage.success(`影响矩阵已生成（${impacts.value.length} 项）`)
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '影响分析失败')) }
}

async function approveChange(row: ChangeOrder) {
  try {
    await ElMessageBox.confirm('确认批准此变更？批准后将通知所有影响项责任人。', '审批变更', { type: 'warning' })
    await api.approveChange(row.id)
    ElMessage.success('变更已批准')
    await load()
  } catch { /* cancelled */ }
}

async function feedbackChange(row: ChangeOrder) {
  try {
    await api.feedbackChange(row.id, {
      result: '已全部重排并同步责任人',
      plan: '今日完成全部调整',
      delayDays: 2, reworkQty: 0,
      responsibleName: '项目协调员'
    } as any)
    ElMessage.success('执行反馈已提交')
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '反馈提交失败')) }
}

async function closeChange(row: ChangeOrder) {
  try {
    await api.closeChange(row.id)
    ElMessage.success('变更已关闭')
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '关闭校验未通过')) }
}

onMounted(load)
</script>

<template>
  <div class="stack" v-loading="loading">
    <!-- Create change -->
    <div v-if="auth.can('change:create')" class="panel">
      <div class="panel-header"><span class="panel-title">发起变更</span></div>
      <div class="panel-body inline-form">
        <el-select v-model="form.projectId" placeholder="选择项目 *" filterable style="width: 260px">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
        </el-select>
        <el-select v-model="form.changeType" style="width: 130px">
          <el-option label="设计变更" value="DESIGN" />
          <el-option label="材料变更" value="MATERIAL_SPEC" />
          <el-option label="工艺变更" value="PROCESS" />
          <el-option label="数量变更" value="QUANTITY" />
          <el-option label="交期变更" value="DELIVERY" />
          <el-option label="包装变更" value="PACKAGING" />
        </el-select>
        <el-select v-model="form.urgency" style="width: 110px">
          <el-option label="高风险" value="HIGH" />
          <el-option label="普通" value="NORMAL" />
          <el-option label="低风险" value="LOW" />
        </el-select>
        <el-input v-model="form.beforeContent" placeholder="变更前内容" style="width: 180px" />
        <el-input v-model="form.afterContent" placeholder="变更后内容 *" style="width: 180px" />
        <el-input v-model="form.reason" placeholder="变更原因" style="width: 220px" />
        <el-button type="primary" @click="createChange">发起变更</el-button>
      </div>
    </div>

    <!-- Change list + impact matrix -->
    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">变更单（{{ changes.length }}）</span>
          <el-radio-group v-model="changeFilter" size="small">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="WAIT_IMPACT">待分析</el-radio-button>
            <el-radio-button value="WAIT_APPROVAL">待审批</el-radio-button>
            <el-radio-button value="EXECUTING">执行中</el-radio-button>
            <el-radio-button value="CLOSED">已关闭</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="filteredChanges" height="480" highlight-current-row @row-click="selectChange" size="small">
          <el-table-column prop="changeNo" label="变更单号" width="150" />
          <el-table-column prop="projectNo" label="项目" width="140" />
          <el-table-column prop="changeType" label="类型" width="90" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="变更原因" min-width="160" show-overflow-tooltip />
          <el-table-column label="反馈" width="70">
            <template #default="{ row }">{{ row.feedbackCount || 0 }}/{{ row.impactCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="延期" width="70">
            <template #default="{ row }">{{ row.delayDays || 0 }}天</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button v-if="auth.can('change:analyze') && ['WAIT_IMPACT', 'DRAFT'].includes(row.status)" link type="primary" size="small" @click.stop="analyzeImpact(row)">影响分析</el-button>
              <el-button v-if="auth.can('change:approve') && row.status === 'WAIT_APPROVAL'" link type="warning" size="small" @click.stop="approveChange(row)">审批</el-button>
              <el-button v-if="auth.can('change:feedback') && row.status === 'EXECUTING'" link type="success" size="small" @click.stop="feedbackChange(row)">执行反馈</el-button>
              <el-button v-if="auth.can('change:close') && row.status === 'EXECUTING'" link type="danger" size="small" @click.stop="closeChange(row)">关闭</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Impact matrix -->
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">影响矩阵</span>
          <span v-if="activeChange" class="subtle">{{ activeChange.changeNo }}</span>
          <el-tag v-if="activeChange" :type="statusTag(activeChange.status)" effect="dark" size="small" style="margin-left: 8px">{{ statusLabel(activeChange.status) }}</el-tag>
        </div>
        <div class="panel-body stack" style="gap: 8px">
          <!-- Before/After comparison -->
          <div v-if="activeChange" style="background: #f5f7fa; border-radius: 6px; padding: 10px">
            <div class="status-line">
              <span class="subtle">变更前：</span><span>{{ activeChange.beforeContent || '（未填写）' }}</span>
            </div>
            <div class="status-line" style="margin-top: 4px">
              <span class="subtle">变更后：</span><strong>{{ activeChange.afterContent }}</strong>
            </div>
            <div class="subtle" style="margin-top: 4px">原因：{{ activeChange.reason || '（未填写）' }}</div>
          </div>

          <el-empty v-if="!impacts.length" description="选择变更单并执行影响分析" :image-size="80" />

          <!-- Impact items -->
          <div v-for="item in impacts" :key="item.id" style="border: 1px solid #e4e7ed; border-radius: 8px; padding: 10px">
            <div class="status-line">
              <el-tag :type="impactTypeTag(item.objectType)" effect="dark" size="small">{{ impactTypeLabel(item.objectType) }}</el-tag>
              <strong>{{ item.objectName }}</strong>
              <el-tag :type="item.status === 'DONE' ? 'success' : 'warning'" effect="plain" size="small">{{ item.status === 'DONE' ? '已完成' : '待反馈' }}</el-tag>
            </div>
            <p class="subtle" style="margin: 6px 0 2px">{{ item.departmentName }} · {{ item.suggestedAction }}</p>
            <div v-if="item.feedbackResult" style="margin-top: 4px">
              <span class="subtle">反馈：</span><span>{{ item.feedbackResult }}</span>
              <span v-if="item.responsibleName" class="subtle" style="margin-left: 8px">· {{ item.responsibleName }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
