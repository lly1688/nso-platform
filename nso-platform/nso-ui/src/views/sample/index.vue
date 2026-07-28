<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import type { ConfirmationToken, Project, Sample, SampleCheck } from '@/types'

const projects = ref<Project[]>([])
const samples = ref<Sample[]>([])
const checks = ref<SampleCheck[]>([])
const token = ref<ConfirmationToken>()
const activeSample = ref<Sample>()
const loading = ref(false)
const sampleFilter = ref('')
const auth = useAuthStore()

const form = reactive({
  projectId: undefined as number | undefined,
  purpose: '', quantity: 1, planFinishDate: '', referencedVersion: '', responsibleName: ''
})

const checkForm = reactive({
  checkItem: '', measuredValue: '', result: 'PASS', issueSummary: '', correctiveAction: '', checkerName: ''
})

const statusTag = (status: string) =>
  ({ DRAFT: 'info', REWORKING: 'danger', CHECKED: 'warning', WAIT_CUSTOMER_CONFIRM: '', WAIT_SUPPLEMENT: 'warning', CONFIRMED: 'success', CONDITIONAL_PASS: 'warning', REJECTED: 'danger' } as Record<string, string>)[status] || 'info'

const statusLabel = (status: string) =>
  ({ DRAFT: '草稿', REWORKING: '整改中', CHECKED: '已检验', WAIT_CUSTOMER_CONFIRM: '待客户确认', WAIT_SUPPLEMENT: '待补充', CONFIRMED: '已通过', CONDITIONAL_PASS: '条件通过', REJECTED: '已驳回' } as Record<string, string>)[status] || status

const confirmLabel = (status: string) =>
  ({ PASS: '通过', CONDITIONAL_PASS: '条件通过', WAIT_SUPPLEMENT: '待补充', REJECT: '驳回', PENDING: '待确认' } as Record<string, string>)[status] || status

const filteredSamples = computed(() => {
  if (!sampleFilter.value) return samples.value
  return samples.value.filter(s => s.status === sampleFilter.value)
})

async function load() {
  loading.value = true
  try {
    projects.value = ((await api.projects()) as any).list || []
    samples.value = ((await api.samples(undefined)) as any).list || []
  } catch (error) {
    ElMessage.error(userFacingError(error, '数据加载失败'))
  } finally { loading.value = false }
}

async function createSample() {
  if (!form.projectId) { ElMessage.warning('请选择项目'); return }
  if (!form.purpose.trim()) { ElMessage.warning('请填写打样目的'); return }
  try {
    await api.createSample({ ...form, purpose: form.purpose.trim() })
    ElMessage.success('样品单已创建')
    form.purpose = ''; form.referencedVersion = ''
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '样品创建失败')) }
}

async function selectSample(row: Sample) {
  activeSample.value = row
  token.value = undefined
  try {
    checks.value = ((await api.sampleChecks(row.id)) as any).list || []
  } catch { checks.value = [] }
}

async function addCheck() {
  if (!activeSample.value) return
  if (!checkForm.checkItem.trim()) { ElMessage.warning('请填写检验项目'); return }
  try {
    await api.addSampleCheck(activeSample.value.id, { ...checkForm, checkItem: checkForm.checkItem.trim() })
    ElMessage.success('检验记录已保存')
    checkForm.checkItem = ''; checkForm.measuredValue = ''; checkForm.issueSummary = ''
    await selectSample(activeSample.value)
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '检验保存失败')) }
}

async function submitConfirm(row: Sample) {
  try {
    await ElMessageBox.confirm('确认提交客户确认？样品将进入"待客户确认"状态。', '提交确认', { type: 'warning' })
    await api.submitSampleConfirm(row.id)
    ElMessage.success('已提交客户确认')
    await load()
  } catch { /* cancelled */ }
}

async function confirmSample(row: Sample, conclusion: string) {
  const opinion = conclusion === 'PASS' ? '样品确认通过，可进入生产准备' : '样品需整改，详见检验意见'
  try {
    await api.confirmSample(row.id, { conclusion, opinion, confirmer: '客户确认人' })
    ElMessage.success(conclusion === 'PASS' ? '样品已通过' : '样品已驳回，自动生成整改任务')
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '确认操作失败')) }
}

async function createToken(row: Sample) {
  try {
    token.value = await api.createConfirmToken(row.id) as ConfirmationToken
    ElMessage.success('客户确认令牌已生成')
  } catch (error) { ElMessage.error(userFacingError(error, '令牌生成失败')) }
}

function copyToken() {
  if (!token.value?.token) return
  navigator.clipboard.writeText(`${window.location.origin}/public/confirm/${token.value.token}`)
  ElMessage.success('确认链接已复制')
}

onMounted(load)
</script>

<template>
  <div class="stack" v-loading="loading">
    <!-- Create sample -->
    <div v-if="auth.can('sample:create')" class="panel">
      <div class="panel-header"><span class="panel-title">样品申请</span></div>
      <div class="panel-body inline-form">
        <el-select v-model="form.projectId" placeholder="选择项目 *" filterable style="width: 260px">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
        </el-select>
        <el-input v-model="form.purpose" placeholder="打样目的 *" style="width: 220px" />
        <el-input-number v-model="form.quantity" :min="1" style="width: 100px" />
        <el-input v-model="form.referencedVersion" placeholder="引用技术版本" style="width: 140px" />
        <el-date-picker v-model="form.planFinishDate" value-format="YYYY-MM-DD" placeholder="计划完成" style="width: 160px" />
        <el-input v-model="form.responsibleName" placeholder="责任人" style="width: 120px" />
        <el-button type="primary" @click="createSample">创建样品单</el-button>
      </div>
    </div>

    <!-- Sample list -->
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">样品确认闭环（{{ samples.length }}）</span>
        <el-radio-group v-model="sampleFilter" size="small">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="DRAFT">草稿</el-radio-button>
          <el-radio-button value="CHECKED">已检验</el-radio-button>
          <el-radio-button value="WAIT_CUSTOMER_CONFIRM">待确认</el-radio-button>
          <el-radio-button value="CONFIRMED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="filteredSamples" height="340" highlight-current-row @row-click="selectSample" size="small">
        <el-table-column prop="sampleNo" label="样品单号" width="160" />
        <el-table-column prop="projectNo" label="项目" width="150" />
        <el-table-column prop="purpose" label="打样目的" min-width="180" show-overflow-tooltip />
        <el-table-column prop="referencedVersion" label="版本" width="90" />
        <el-table-column prop="planFinishDate" label="计划完成" width="120" />
        <el-table-column prop="responsibleName" label="责任人" width="100" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结论" width="90">
          <template #default="{ row }">
            <span v-if="row.confirmConclusion" class="subtle">{{ confirmLabel(row.confirmConclusion) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.can('sample:submit') && ['DRAFT', 'CHECKED'].includes(row.status)" link type="primary" size="small" @click.stop="submitConfirm(row)">提交确认</el-button>
            <el-button v-if="auth.can('sample:submit') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="warning" size="small" @click.stop="createToken(row)">生成令牌</el-button>
            <el-button v-if="auth.can('sample:proxy-confirm') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="success" size="small" @click.stop="confirmSample(row, 'PASS')">通过</el-button>
            <el-button v-if="auth.can('sample:proxy-confirm') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="warning" size="small" @click.stop="confirmSample(row, 'CONDITIONAL_PASS')">条件通过</el-button>
            <el-button v-if="auth.can('sample:proxy-confirm') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="primary" size="small" @click.stop="confirmSample(row, 'WAIT_SUPPLEMENT')">待补充</el-button>
            <el-button v-if="auth.can('sample:proxy-confirm') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="danger" size="small" @click.stop="confirmSample(row, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Sample detail & checks -->
    <section v-if="activeSample" class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">样品检验：{{ activeSample.sampleNo }}</span>
          <el-tag :type="statusTag(activeSample.status)" effect="dark" size="small">{{ statusLabel(activeSample.status) }}</el-tag>
        </div>
        <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
          <el-descriptions-item label="打样目的">{{ activeSample.purpose }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ activeSample.quantity }}</el-descriptions-item>
          <el-descriptions-item label="引用版本">{{ activeSample.referencedVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任人">{{ activeSample.responsibleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="确认结论">{{ confirmLabel(activeSample.confirmConclusion) }}</el-descriptions-item>
          <el-descriptions-item label="问题摘要">{{ activeSample.issueSummary || '无' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="auth.can('sample:inspect')" class="panel-body inline-form">
          <el-input v-model="checkForm.checkItem" placeholder="检验项目 *" style="width: 160px" />
          <el-input v-model="checkForm.measuredValue" placeholder="实测值" style="width: 140px" />
          <el-select v-model="checkForm.result" style="width: 100px">
            <el-option label="合格" value="PASS" />
            <el-option label="不合格" value="FAIL" />
          </el-select>
          <el-input v-model="checkForm.checkerName" placeholder="检验人" style="width: 110px" />
          <el-button type="primary" @click="addCheck">保存检验记录</el-button>
        </div>
        <el-table :data="checks" height="200" size="small">
          <el-table-column prop="checkItem" label="检验项目" width="140" />
          <el-table-column prop="measuredValue" label="实测值" min-width="140" />
          <el-table-column prop="result" label="结论" width="80" />
          <el-table-column prop="checkerName" label="检验人" width="100" />
          <el-table-column prop="checkedAt" label="时间" width="160" />
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header"><span class="panel-title">客户短期确认入口</span></div>
        <div class="panel-body">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="令牌">
              <template v-if="token">
                <code style="font-size: 12px; word-break: break-all">{{ token.token }}</code>
              </template>
              <span v-else class="subtle">选择"待客户确认"样品后生成令牌</span>
            </el-descriptions-item>
            <el-descriptions-item label="有效期">{{ token?.expireAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="使用次数">{{ token ? `${token.usedCount || 0}/${token.maxUseCount}` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="公共路由">
              <code v-if="token" style="font-size: 12px">/public/confirm/{{ token.token }}</code>
              <span v-else class="subtle">-</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-button v-if="token" type="primary" size="small" style="margin-top: 12px" @click="copyToken">复制确认链接</el-button>
        </div>
      </div>
    </section>
  </div>
</template>
