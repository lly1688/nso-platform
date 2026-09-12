<script setup lang="ts">
// 任务中心子路由状态。
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const pageTitle = computed(() => String(route.meta.title || '任务中心'));
const pageDescription = computed(() => String(route.meta.description || '任务执行、风险处置与交付协同'));
</script>

<template>
    <!-- 任务中心内容 -->
    <div class="module-layout">
        <section class="module-heading">
            <div>
                <h1>{{ pageTitle }}</h1>
                <p>{{ pageDescription }}</p>
            </div>
        </section>

        <RouterView />
    </div>
</template>
