<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/nso'
import type { ChangeImpact, ChangeOrder, Project } from '../types'

const projects = ref<Project[]>([])
const changes = ref<ChangeOrder[]>([])
const impacts = ref<ChangeImpact[]>([])
const activeChange = ref<ChangeOrder>()
const form = reactive({
  projectId: undefined as number | undefined,
  changeType: 'DESIGN',
  urgency: 'HIGH',
  beforeContent: '定位销直径 8mm',
  afterContent: '定位销直径 10mm，检验公差收紧',
  reason: '客户装配测试反馈间隙偏大'
})

async function load() {
  projects.value = (await api.projects()).records
  if (!form.projectId && projects.value[0]) form.projectId = projects.value[0].id
  changes.value = (await api.changes(form.projectId)).records
  if (activeChange.value) {
    impacts.value = await api.impacts(activeChange.value.id)
  }
}

async function createChange() {
  await api.createChange(form)
  ElMessage.success('变更单已发起')
  await load()
}

async function selectChange(row: ChangeOrder) {
  activeChange.value = row
  impacts.value = await api.impacts(row.id).catch(() => [])
}

async function analyze(row: ChangeOrder) {
  impacts.value = await api.analyzeChange(row.id)
  activeChange.value = row
  ElMessage.success('影响矩阵已生成')
  await load()
}

async function approve(row: ChangeOrder) {
  await api.approveChange(row.id)
  ElMessage.success('变更已批准')
  await load()
}

async function feedback(row: ChangeOrder) {
  await api.feedbackChange(row.id, { result: '已重排任务并同步责任人', plan: '今日完成', delayDays: 3, reworkQty: 20, responsibleName: '计划员' })
  ElMessage.success('影响项已反馈')
  await load()
}

async function close(row: ChangeOrder) {
  try {
    await api.closeChange(row.id)
    ElMessage.success('变更已关闭')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">发起变更</span>
      </div>
      <div class="panel-body inline-form">
        <el-select v-model="form.projectId" placeholder="项目" @change="load">
          <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo} ${item.productName}`" :value="item.id" />
        </el-select>
        <el-select v-model="form.changeType">
          <el-option label="设计变更" value="DESIGN" />
          <el-option label="材料变更" value="MATERIAL" />
          <el-option label="交期变更" value="DELIVERY" />
        </el-select>
        <el-select v-model="form.urgency">
          <el-option label="高风险" value="HIGH" />
          <el-option label="普通" value="NORMAL" />
        </el-select>
        <el-input v-model="form.reason" placeholder="变更原因" />
        <el-button type="primary" :icon="'Plus'" @click="createChange">发起</el-button>
      </div>
    </div>

    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">变更单</span>
        </div>
        <el-table :data="changes" height="500" @row-click="selectChange">
          <el-table-column prop="changeNo" label="变更单号" width="150" />
          <el-table-column prop="projectNo" label="项目" width="150" />
          <el-table-column prop="changeType" label="类型" width="90" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'CLOSED' ? 'success' : row.urgency === 'HIGH' ? 'danger' : 'warning'" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="180" />
          <el-table-column label="反馈" width="90">
            <template #default="{ row }">{{ row.feedbackCount }}/{{ row.impactCount }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="analyze(row)">分析</el-button>
              <el-button link type="warning" @click.stop="approve(row)">批准</el-button>
              <el-button link type="success" @click.stop="feedback(row)">反馈</el-button>
              <el-button link type="danger" @click.stop="close(row)">关闭</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">影响矩阵</span>
          <span v-if="activeChange" class="subtle">{{ activeChange.changeNo }}</span>
        </div>
        <div class="panel-body stack">
          <el-empty v-if="!impacts.length" description="选择变更并执行影响分析" />
          <div v-for="item in impacts" :key="item.id" class="panel-body" style="border: 1px solid #edf1f5; border-radius: 8px">
            <div class="status-line">
              <el-tag effect="plain">{{ item.objectType }}</el-tag>
              <strong>{{ item.objectName }}</strong>
            </div>
            <p class="subtle">{{ item.departmentName }} · {{ item.suggestedAction }}</p>
            <el-tag :type="item.status === 'FEEDBACK_DONE' ? 'success' : 'warning'" effect="plain">{{ item.status }}</el-tag>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
