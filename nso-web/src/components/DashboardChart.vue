<script setup lang="ts">
// 图表输入与绘制生命周期。
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { init, use, type ECharts } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';

use([BarChart, CanvasRenderer, GridComponent, LegendComponent, LineChart, PieChart, TooltipComponent]);

const props = defineProps<{
    option: EChartsOption;
    empty?: boolean;
    label: string;
}>();
const chartElement = ref<HTMLDivElement>();
let chart: ECharts | undefined;
let resizeObserver: ResizeObserver | undefined;

async function render() {
    // 等待容器完成渲染后初始化图表，并在数据更新时复用实例。
    await nextTick();
    if (!chartElement.value) {
        return;
    }
    if (!chart) {
        chart = init(chartElement.value);
    }
    chart.setOption(props.option, { notMerge: true, lazyUpdate: true });
    chart.resize();
}

onMounted(() => {
    void render();
    if (chartElement.value) {
        resizeObserver = new ResizeObserver(() => chart?.resize());
        resizeObserver.observe(chartElement.value);
    }
});

watch(
    () => props.option,
    () => {
        void render();
    },
    { deep: true }
);
onBeforeUnmount(() => {
    // 组件卸载时解除尺寸监听并销毁图表实例。
    resizeObserver?.disconnect();
    chart?.dispose();
    chart = undefined;
});
</script>

<template>
    <!-- 图表容器 -->
    <div class="dashboard-chart" :aria-label="label" role="img">
        <div ref="chartElement" class="dashboard-chart__canvas" />
        <div v-if="empty" class="dashboard-chart__empty">当前周期暂无数据</div>
    </div>
</template>

<style scoped>
/* 图表容器样式。 */
.dashboard-chart {
    position: relative;
    min-height: 260px;
}

.dashboard-chart__canvas {
    width: 100%;
    height: 260px;
}

.dashboard-chart__empty {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    color: var(--nso-muted);
    font-size: 13px;
    pointer-events: none;
}
</style>
