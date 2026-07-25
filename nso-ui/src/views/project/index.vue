<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/nso'
import type { Customer, Project } from '../types'

const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const activeProject = ref<any>()
const form = reactive({
  customerId: undefined as number | undefined,
  customerName: '',
  productName: '',
  quantity: 1,
  targetDate: '',
  ownerName: '周项目',
  priority: 'HIGH'
})

async function load() {
  const [customerData, projectData] = await Promise.all([api.customers(), api.projects()])
  customers.value = customerData.records
  projects.value = projectData.records
  if (!form.customerId && customers.value[0]) {
    form.customerId = customers.value[0].id
  }
}

async function createProject() {
  await api.createProject(form)
  ElMessage.success('项目已创建')
  await load()
}

async function openProject(row: Project) {
  activeProject.value = await api.project(row.id)
}

async function submitReview(row: Project) {
  await api.submitReview(row.id)
  ElMessage.success('已提交需求评审')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">快速建档</span>
      </div>
      <div class="panel-body inline-form">
        <el-select v-model="form.customerId" placeholder="客户">
          <el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-input v-model="form.productName" placeholder="产品名称" />
        <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        <el-date-picker v-model="form.targetDate" value-format="YYYY-MM-DD" placeholder="目标交期" style="width: 100%" />
        <el-button type="primary" :icon="'Plus'" @click="createProject">创建订单</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">客户与项目</span>
      </div>
      <el-table :data="projects" height="430" @row-click="openProject">
        <el-table-column prop="projectNo" label="项目编号" width="150" />
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column prop="productName" label="产品" min-width="150" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="targetDate" label="目标交期" width="120" />
        <el-table-column prop="stage" label="当前阶段" width="130" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openProject(row)">详情</el-button>
            <el-button link type="warning" @click.stop="submitReview(row)">评审</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="activeProject" class="panel">
      <div class="panel-header">
        <span class="panel-title">项目时间轴：{{ activeProject.project.projectNo }}</span>
      </div>
      <div class="panel-body">
        <el-timeline>
          <el-timeline-item v-for="item in activeProject.timeline" :key="item.id" :timestamp="item.occurredAt">
            <strong>{{ item.title }}</strong>
            <div class="subtle">{{ item.summary }} · {{ item.operatorName }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>
