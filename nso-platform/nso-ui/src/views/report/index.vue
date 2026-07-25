<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api/nso'
import type { ReportOverview } from '../types'

const overview = ref<ReportOverview>()

const maxRisk = computed(() => {
  const values = overview.value?.riskLevels.map((item) => Number(item.value)) || [1]
  return Math.max(1, ...values)
})

async function load() {
  overview.value = await api.overview()
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
    </section>

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
  </div>
</template>
