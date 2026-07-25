<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/nso'
import type { Risk, Task } from '../types'

const tasks = ref<Task[]>([])
const risks = ref<Risk[]>([])

async function load() {
  const [taskData, riskData] = await Promise.all([api.tasks(), api.risks()])
  tasks.value = taskData.records
  risks.value = riskData.records
}

async function start(row: Task) {
  try {
    await api.startTask(row.id)
    ElMessage.success('任务已开工')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务开工被阻断')
  }
  await load()
}

async function feedback(row: Task) {
  await api.feedbackTask(row.id, { result: '完成', notes: '现场反馈已完成' })
  ElMessage.success('任务反馈已提交')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="two-column">
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">任务执行与版本阻断</span>
      </div>
      <el-table :data="tasks" height="560">
        <el-table-column prop="taskNo" label="任务号" width="150" />
        <el-table-column prop="projectNo" label="项目" width="150" />
        <el-table-column prop="taskType" label="类型" width="110" />
        <el-table-column prop="title" label="任务" min-width="180" />
        <el-table-column prop="referencedVersion" label="引用版本" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="['BLOCKED', 'PAUSED'].includes(row.status) ? 'danger' : row.status === 'DONE' ? 'success' : 'warning'" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="blockReason" label="阻断原因" min-width="220" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="start(row)">开工</el-button>
            <el-button link type="success" @click="feedback(row)">反馈</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">风险处置建议</span>
      </div>
      <div class="panel-body stack">
        <div v-for="risk in risks" :key="risk.id">
          <div class="status-line">
            <el-tag :type="risk.level === 'HIGH' || risk.level === 'SERIOUS' ? 'danger' : 'warning'" effect="dark">
              {{ risk.level }}
            </el-tag>
            <strong>{{ risk.projectNo }}</strong>
            <span>{{ risk.score }} 分</span>
          </div>
          <p class="subtle">{{ risk.reasons.join('、') }}</p>
          <el-alert :title="risk.suggestion" type="warning" :closable="false" />
        </div>
      </div>
    </div>
  </section>
</template>
