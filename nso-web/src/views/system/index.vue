<script setup lang="ts">
// 系统管理导航状态。
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { systemNavItems } from './shared';

const route = useRoute();
const auth = useAuthStore();
const visibleNavItems = computed(() => systemNavItems.filter((item) =>
    (!item.permission || auth.can(item.permission))
    && (!item.roles?.length || item.roles.some((role) => auth.user?.roles?.some((current: string) => current.toLowerCase() === role.toLowerCase())))
));
const activeLabel = computed(() => systemNavItems.find((item) => item.name === route.name)?.label || visibleNavItems.value[0]?.label || '系统管理');
</script>

<template>
    <!-- 系统管理导航与子页面 -->
    <div class="system-layout">
        <section class="system-heading">
            <div>
                <h1>系统管理</h1>
                <p>账号、组织、权限与平台运维治理</p>
            </div>
            <el-tag type="primary" effect="plain">{{ activeLabel }}</el-tag>
        </section>

        <nav class="system-subnav" aria-label="系统管理二级导航">
            <RouterLink
                v-for="item in visibleNavItems"
                :key="item.name"
                :to="item.path"
                class="system-subnav__item"
                >
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
            </RouterLink>
        </nav>

        <RouterView />
    </div>
</template>
