<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = defineProps<{
  option: EChartsOption
  empty?: boolean
  label: string
}>()

const chartElement = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined
let resizeObserver: ResizeObserver | undefined

async function render() {
  await nextTick()
  if (!chartElement.value) return
  if (!chart) chart = echarts.init(chartElement.value)
  chart.setOption(props.option, { notMerge: true, lazyUpdate: true })
  chart.resize()
}

onMounted(() => {
  void render()
  if (chartElement.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartElement.value)
  }
})

watch(() => props.option, () => { void render() }, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = undefined
})
</script>

<template>
  <div class="dashboard-chart" :aria-label="label" role="img">
    <div ref="chartElement" class="dashboard-chart__canvas" />
    <div v-if="empty" class="dashboard-chart__empty">当前周期暂无数据</div>
  </div>
</template>

<style scoped>
.dashboard-chart { position: relative; min-height: 260px; }
.dashboard-chart__canvas { width: 100%; height: 260px; }
.dashboard-chart__empty { position: absolute; inset: 0; display: grid; place-items: center; color: var(--nso-muted); font-size: 13px; pointer-events: none; }
</style>
