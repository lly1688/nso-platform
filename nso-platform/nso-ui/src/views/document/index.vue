<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/nso'
import type { DocumentVersion, Project } from '../types'

const projects = ref<Project[]>([])
const documents = ref<DocumentVersion[]>([])
const form = reactive({
  projectId: undefined as number | undefined,
  fileName: '总装图.pdf',
  fileType: 'DRAWING',
  versionNo: 'V3',
  effectiveDate: '',
  changeSummary: '补充工艺与检验说明'
})

async function load() {
  const projectData = await api.projects()
  projects.value = projectData.records
  if (!form.projectId && projects.value[0]) {
    form.projectId = projects.value[0].id
  }
  documents.value = (await api.documents(form.projectId)).records
}

async function createVersion() {
  if (!form.projectId) return
  await api.createDocument(form.projectId, form)
  ElMessage.success('技术版本已上传为草稿')
  await load()
}

async function publish(row: DocumentVersion) {
  await api.publishDocument(row.id)
  ElMessage.success('技术版本已发布')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="form.projectId" placeholder="选择项目" style="width: 280px" @change="load">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo} ${item.productName}`" :value="item.id" />
        </el-select>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">上传版本</span>
      </div>
      <div class="panel-body inline-form">
        <el-input v-model="form.fileName" placeholder="文件名" />
        <el-select v-model="form.fileType">
          <el-option label="图纸" value="DRAWING" />
          <el-option label="BOM" value="BOM" />
          <el-option label="工艺卡" value="PROCESS" />
        </el-select>
        <el-input v-model="form.versionNo" placeholder="版本号" />
        <el-date-picker v-model="form.effectiveDate" value-format="YYYY-MM-DD" placeholder="生效日期" style="width: 100%" />
        <el-button type="primary" :icon="'Upload'" @click="createVersion">上传</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">技术文件版本</span>
      </div>
      <el-table :data="documents" height="460">
        <el-table-column prop="projectNo" label="项目" width="150" />
        <el-table-column prop="fileName" label="文件" min-width="180" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column prop="versionNo" label="版本" width="90" />
        <el-table-column prop="effectiveDate" label="生效日期" width="120" />
        <el-table-column prop="changeSummary" label="变更说明" min-width="220" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.currentVersion ? 'success' : row.status === 'DRAFT' ? 'warning' : 'info'" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 'EFFECTIVE'" @click="publish(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
