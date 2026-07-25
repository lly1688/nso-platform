<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api/nso'
import type { Message, Project, ReportOverview, Risk } from '../types'

const overview = ref<ReportOverview>()
const projects = ref<Project[]>([])
const risks = ref<Risk[]>([])
const messages = ref<Message[]>([])

async function load() {
  const [overviewData, projectData, riskData, messageData] = await Promise.all([
    api.overview(),
    api.projects(),
    api.risks(),
    api.messages('UNREAD')
  ])
  overview.value = overviewData
  projects.value = projectData.records
  risks.value = riskData.records
  messages.value = messageData.records
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <section class="metric-grid">
      <div class="metric">
        <span>进行中项目</span>
        <strong>{{ overview?.metrics.projects ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>高风险项目</span>
        <strong>{{ overview?.metrics.highRisks ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>待确认样品</span>
        <strong>{{ overview?.metrics.waitingSamples ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>未关闭变更</span>
        <strong>{{ overview?.metrics.openChanges ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>未读消息</span>
        <strong>{{ overview?.metrics.unreadMessages ?? 0 }}</strong>
      </div>
    </section>

    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">项目总览</span>
          <el-button type="primary" link @click="$router.push('/projects')">新建订单</el-button>
        </div>
        <el-table :data="projects" height="360">
          <el-table-column prop="projectNo" label="项目编号" width="150" />
          <el-table-column prop="customerName" label="客户" min-width="160" />
          <el-table-column prop="productName" label="产品" min-width="160" />
          <el-table-column prop="stage" label="阶段" width="130" />
          <el-table-column label="风险" width="110">
            <template #default="{ row }">
              <el-tag :type="row.riskLevel === 'HIGH' || row.riskLevel === 'SERIOUS' ? 'danger' : 'success'" effect="plain">
                {{ row.riskLevel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="daysLeft" label="剩余天数" width="100" />
        </el-table>
      </div>

      <div class="stack">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">风险解释</span>
          </div>
          <div class="panel-body stack">
            <div v-for="risk in risks" :key="risk.id">
              <div class="status-line">
                <el-tag :type="risk.level === 'HIGH' || risk.level === 'SERIOUS' ? 'danger' : 'warning'" effect="dark">
                  {{ risk.level }}
                </el-tag>
                <strong>{{ risk.projectNo }}</strong>
                <span class="subtle">{{ risk.score }} 分</span>
              </div>
              <p class="subtle">{{ risk.reasons.join('、') }}；{{ risk.suggestion }}</p>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">待办消息</span>
          </div>
          <div class="panel-body stack">
            <div v-for="message in messages" :key="message.id">
              <strong>{{ message.title }}</strong>
              <div class="subtle">{{ message.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
