<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import type { ExportTask, ReportOverview } from '@/types'

const overview = ref<ReportOverview>()
const exports = ref<ExportTask[]>([])

const maxRisk = computed(() => {
  const values = overview.value?.riskLevels.map((item) => Number(item.value)) || [1]
  return Math.max(1, ...values)
})

async function load() {
  overview.value = await api.overview()
  exports.value = (await api.exports()).list
}

async function createExport(exportType = 'PROJECT') {
  await api.createExport({ exportType, filters: {}, fields: [] })
  ElMessage.success('导出任务已生成')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <section class="metric-grid">
      <div class="metric">
        <span>项目总数</span>
        <strong>{{ overview?.metrics.projects ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>高风险</span>
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
      <div class="metric">
        <span>已交付</span>
        <strong>{{ overview?.metrics.deliveries ?? 0 }}</strong>
      </div>
    </section>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="'Download'" @click="createExport('PROJECT')">导出项目统计</el-button>
        <el-button :icon="'Download'" @click="createExport('TASK')">导出任务统计</el-button>
      </div>
    </div>

    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">风险等级分布</span>
        </div>
        <div class="panel-body">
          <div v-for="item in overview?.riskLevels" :key="String(item.name)" class="bar-row">
            <span>{{ item.name }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: `${(Number(item.value) / maxRisk) * 100}%` }" />
            </div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">变更与样品复盘</span>
        </div>
        <div class="panel-body stack">
          <div>
            <strong>变更类型</strong>
            <div v-for="item in overview?.changeTypes" :key="String(item.name)" class="status-line">
              <el-tag effect="plain">{{ item.name }}</el-tag>
              <span>{{ item.value }} 单</span>
            </div>
          </div>
          <div>
            <strong>样品效率</strong>
            <div v-for="item in overview?.sampleEfficiency" :key="String(item.name)" class="status-line">
              <el-tag type="success" effect="plain">{{ item.name }}</el-tag>
              <span>{{ item.value }} 单</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">延期归因与交付</span>
        </div>
        <div class="panel-body stack">
          <div v-for="item in overview?.delayReasons" :key="String(item.name)" class="status-line">
            <el-tag effect="plain">{{ item.name }}</el-tag>
            <span>{{ item.value }} 天</span>
          </div>
          <div v-for="item in overview?.deliveryStats" :key="String(item.name)" class="status-line">
            <el-tag type="success" effect="plain">{{ item.name }}</el-tag>
            <span>{{ item.value }}</span>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">部门负载与导出记录</span>
        </div>
        <div class="panel-body stack">
          <div v-for="item in overview?.departmentLoads" :key="String(item.name)" class="status-line">
            <el-tag effect="plain">{{ item.name }}</el-tag>
            <span>{{ item.value }} 项</span>
          </div>
          <el-table :data="exports" height="220">
            <el-table-column prop="exportType" label="类型" width="100" />
            <el-table-column prop="fileName" label="文件" min-width="180" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </div>
      </div>
    </section>
  </div>
</template>
