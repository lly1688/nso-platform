<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/nso'
import type { Project, Sample } from '../types'

const projects = ref<Project[]>([])
const samples = ref<Sample[]>([])
const form = reactive({
  projectId: undefined as number | undefined,
  purpose: '验证结构和表面处理',
  quantity: 1,
  planFinishDate: '',
  referencedVersion: 'V2',
  responsibleName: '许质检'
})

async function load() {
  projects.value = (await api.projects()).records
  if (!form.projectId && projects.value[0]) form.projectId = projects.value[0].id
  samples.value = (await api.samples(form.projectId)).records
}

async function createSample() {
  await api.createSample(form)
  ElMessage.success('样品单已创建')
  await load()
}

async function submitConfirm(row: Sample) {
  await api.submitSampleConfirm(row.id)
  ElMessage.success('已提交客户确认')
  await load()
}

async function confirm(row: Sample, conclusion = 'PASS') {
  await api.confirmSample(row.id, { conclusion, opinion: conclusion === 'PASS' ? '确认通过' : '需要整改', confirmer: '客户确认人' })
  ElMessage.success('确认结论已记录')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">样品申请</span>
      </div>
      <div class="panel-body inline-form">
        <el-select v-model="form.projectId" placeholder="项目" @change="load">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo} ${item.productName}`" :value="item.id" />
        </el-select>
        <el-input v-model="form.purpose" placeholder="打样目的" />
        <el-input v-model="form.referencedVersion" placeholder="引用版本" />
        <el-date-picker v-model="form.planFinishDate" value-format="YYYY-MM-DD" placeholder="计划完成" style="width: 100%" />
        <el-button type="primary" :icon="'Plus'" @click="createSample">创建</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">样品确认闭环</span>
      </div>
      <el-table :data="samples" height="500">
        <el-table-column prop="sampleNo" label="样品单" width="150" />
        <el-table-column prop="projectNo" label="项目" width="150" />
        <el-table-column prop="purpose" label="目的" min-width="180" />
        <el-table-column prop="referencedVersion" label="版本" width="90" />
        <el-table-column prop="planFinishDate" label="计划完成" width="120" />
        <el-table-column prop="responsibleName" label="责任人" width="110" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CONFIRMED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="submitConfirm(row)">提交确认</el-button>
            <el-button link type="success" @click="confirm(row)">通过</el-button>
            <el-button link type="danger" @click="confirm(row, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
