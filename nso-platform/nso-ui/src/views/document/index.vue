<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import type { Bom, DocumentVersion, InspectionSpec, ProcessRoute, Project } from '@/types'

const projects = ref<Project[]>([])
const documents = ref<DocumentVersion[]>([])
const boms = ref<Bom[]>([])
const routes = ref<ProcessRoute[]>([])
const specs = ref<InspectionSpec[]>([])
const activeTab = ref('versions')
const selectedProjectId = ref<number>()
const selectedFile = ref<File>()
const fileInput = ref<HTMLInputElement>()
const auth = useAuthStore()

const versionForm = reactive({
  fileName: '', fileType: 'DRAWING', versionNo: '', effectiveDate: '', changeSummary: ''
})

const bomForm = reactive({
  bomNo: '', versionNo: '', boundDocVersionId: undefined as number | undefined
})

const routeForm = reactive({
  routeNo: '', versionNo: '', boundDocVersionId: undefined as number | undefined
})

const specForm = reactive({
  specNo: '', versionNo: '', boundDocVersionId: undefined as number | undefined
})

const currentVersion = computed(() => documents.value.find(d => d.currentVersion))
const statusTag = (status: string) =>
  ({ DRAFT: 'warning', PUBLISHED: 'success', EFFECTIVE: '', VOID: 'danger' } as Record<string, string>)[status] || 'info'

async function load() {
  try {
    const projectData = await api.projects()
    projects.value = projectData.list || []
    const pid = selectedProjectId.value
    const [docData, bomData, routeData, specData] = await Promise.all([
      api.documents(pid), api.boms(pid), api.processRoutes(pid), api.inspectionSpecs(pid)
    ])
    documents.value = docData.list || []
    boms.value = bomData.list || []
    routes.value = routeData.list || []
    specs.value = specData.list || []
  } catch (error) {
    ElMessage.error(userFacingError(error, '数据加载失败'))
  }
}

async function createVersion() {
  if (!selectedProjectId.value) { ElMessage.warning('请先选择项目'); return }
  if (!selectedFile.value || !versionForm.versionNo.trim()) { ElMessage.warning('请选择技术文件并填写版本号'); return }
  try {
    const file = await api.uploadDocumentFile(selectedProjectId.value, selectedFile.value)
    await api.createDocument(selectedProjectId.value, { ...versionForm, fileName: file.fileName, fileObjectId: file.id, versionNo: versionForm.versionNo.trim() })
    ElMessage.success('技术版本已上传为草稿')
    selectedFile.value = undefined
    if (fileInput.value) fileInput.value.value = ''
    versionForm.fileName = ''; versionForm.versionNo = ''; versionForm.changeSummary = ''
    await load()
  } catch (error) {
    ElMessage.error(userFacingError(error, '版本创建失败'))
  }
}

function selectVersionFile(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0]
  versionForm.fileName = selectedFile.value?.name || ''
}

async function publish(row: DocumentVersion) {
  try {
    await ElMessageBox.confirm(`确认发布版本 ${row.versionNo}？发布后旧版本将自动失效。`, '发布确认', { type: 'warning' })
    await api.publishDocument(row.id)
    ElMessage.success(`版本 ${row.versionNo} 已发布`)
    await load()
  } catch { /* cancelled */ }
}

async function createBom() {
  if (!selectedProjectId.value || !currentVersion.value) { ElMessage.warning('请先发布技术版本'); return }
  try {
    await api.createBom({
      projectId: selectedProjectId.value,
      bomNo: bomForm.bomNo || `BOM-${currentVersion.value.projectNo}`,
      versionNo: bomForm.versionNo || currentVersion.value.versionNo,
      boundDocVersionId: currentVersion.value.id,
      items: [{ materialCode: 'MAT-001', materialName: '关键物料', specification: '按图纸', quantity: 1, unit: '件', sourceType: 'PURCHASE' }]
    })
    ElMessage.success('BOM 已创建')
    bomForm.bomNo = ''
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, 'BOM创建失败')) }
}

async function createRoute() {
  if (!selectedProjectId.value || !currentVersion.value) { ElMessage.warning('请先发布技术版本'); return }
  try {
    await api.createProcessRoute({
      projectId: selectedProjectId.value,
      routeNo: routeForm.routeNo || `PROC-${currentVersion.value.projectNo}`,
      versionNo: routeForm.versionNo || currentVersion.value.versionNo,
      boundDocVersionId: currentVersion.value.id,
      steps: [{ stepNo: 10, stepName: '加工', workInstruction: '按技术版本执行', equipmentName: '通用设备', standardHours: 1, outsourceFlag: false, inspectionPoint: true }]
    })
    ElMessage.success('工艺路线已创建')
    routeForm.routeNo = ''
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '工艺路线创建失败')) }
}

async function createSpec() {
  if (!selectedProjectId.value || !currentVersion.value) { ElMessage.warning('请先发布技术版本'); return }
  try {
    await api.createInspectionSpec({
      projectId: selectedProjectId.value,
      specNo: specForm.specNo || `QC-${currentVersion.value.projectNo}`,
      versionNo: specForm.versionNo || currentVersion.value.versionNo,
      boundDocVersionId: currentVersion.value.id,
      items: [{ itemName: '关键尺寸', standardValue: '符合图纸', samplingRule: '首件全检' }]
    })
    ElMessage.success('检验规范已创建')
    specForm.specNo = ''
    await load()
  } catch (error) { ElMessage.error(userFacingError(error, '检验规范创建失败')) }
}

async function publishPackage() {
  if (!selectedProjectId.value) return
  try {
    await ElMessageBox.confirm('确认发布完整技术包？将校验BOM、工艺、检验规范完整性。', '发布技术包', { type: 'warning' })
    await api.publishTechnicalPackage(selectedProjectId.value)
    ElMessage.success('完整技术包已发布')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(userFacingError(error, '技术包发布校验未通过'))
  }
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="selectedProjectId" placeholder="选择项目" clearable style="width: 320px" @change="load">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
        </el-select>
        <el-tag v-if="currentVersion" type="success" effect="plain">当前生效版本：{{ currentVersion.versionNo }}</el-tag>
        <el-tag v-else-if="selectedProjectId" type="warning" effect="plain">无生效版本</el-tag>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="技术文件版本" name="versions">
        <div v-if="auth.can('document:upload')" class="panel">
          <div class="panel-header"><span class="panel-title">上传新版本</span></div>
          <div class="panel-body inline-form">
            <input ref="fileInput" type="file" accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls,.doc,.docx,.dwg,.dxf,.step,.stp,.zip" @change="selectVersionFile" />
            <el-select v-model="versionForm.fileType" style="width: 120px">
              <el-option label="图纸" value="DRAWING" />
              <el-option label="3D模型" value="MODEL_3D" />
              <el-option label="BOM" value="BOM" />
              <el-option label="工艺卡" value="PROCESS" />
              <el-option label="检验规范" value="INSPECTION" />
              <el-option label="客户附件" value="CUSTOMER" />
            </el-select>
            <el-input v-model="versionForm.versionNo" placeholder="版本号 *" style="width: 120px" />
            <el-date-picker v-model="versionForm.effectiveDate" value-format="YYYY-MM-DD" placeholder="生效日期" style="width: 160px" />
            <el-input v-model="versionForm.changeSummary" placeholder="变更说明" style="width: 240px" />
            <el-button type="primary" @click="createVersion">上传为草稿</el-button>
          </div>
        </div>
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">版本列表</span>
            <el-button v-if="auth.can('document:publish')" type="success" size="small" :disabled="!currentVersion" @click="publishPackage">发布完整技术包</el-button>
          </div>
          <el-table :data="documents" height="400" size="small">
            <el-table-column prop="projectNo" label="项目" width="150" />
            <el-table-column prop="fileName" label="文件" min-width="180" show-overflow-tooltip />
            <el-table-column prop="fileType" label="类型" width="100" />
            <el-table-column prop="versionNo" label="版本" width="90" />
            <el-table-column prop="effectiveDate" label="生效日期" width="120" />
            <el-table-column prop="changeSummary" label="变更说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" effect="plain" size="small">
                  {{ row.currentVersion ? '生效中' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="auth.can('document:publish')" link type="primary" size="small" :disabled="row.currentVersion" @click="publish(row)">发布</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="BOM" name="bom">
        <div v-if="auth.can('bom:manage')" class="panel-body inline-form">
          <el-input v-model="bomForm.bomNo" placeholder="BOM编号" style="width: 200px" />
          <el-input v-model="bomForm.versionNo" placeholder="版本号" style="width: 120px" />
          <el-button type="primary" :disabled="!selectedProjectId || !currentVersion" @click="createBom">创建 BOM</el-button>
        </div>
        <el-table :data="boms" height="420" size="small">
          <el-table-column prop="bomNo" label="BOM编号" width="180" />
          <el-table-column prop="versionNo" label="版本" width="100" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="物料数" width="90">
            <template #default="{ row }">{{ row.items?.length || 0 }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="工艺路线" name="route">
        <div v-if="auth.can('process:manage')" class="panel-body inline-form">
          <el-input v-model="routeForm.routeNo" placeholder="路线编号" style="width: 200px" />
          <el-input v-model="routeForm.versionNo" placeholder="版本号" style="width: 120px" />
          <el-button type="primary" :disabled="!selectedProjectId || !currentVersion" @click="createRoute">创建工艺路线</el-button>
        </div>
        <el-table :data="routes" height="420" size="small">
          <el-table-column prop="routeNo" label="路线编号" width="180" />
          <el-table-column prop="versionNo" label="版本" width="100" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="工序数" width="90">
            <template #default="{ row }">{{ row.steps?.length || 0 }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="检验规范" name="spec">
        <div v-if="auth.can('inspection:manage')" class="panel-body inline-form">
          <el-input v-model="specForm.specNo" placeholder="规范编号" style="width: 200px" />
          <el-input v-model="specForm.versionNo" placeholder="版本号" style="width: 120px" />
          <el-button type="primary" :disabled="!selectedProjectId || !currentVersion" @click="createSpec">创建检验规范</el-button>
        </div>
        <el-table :data="specs" height="420" size="small">
          <el-table-column prop="specNo" label="规范编号" width="180" />
          <el-table-column prop="versionNo" label="版本" width="100" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="检验项" width="90">
            <template #default="{ row }">{{ row.items?.length || 0 }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
