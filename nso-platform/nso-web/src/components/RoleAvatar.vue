<script setup lang="ts">
// 角色头像展示规则。
import { computed } from 'vue';
import { roleTheme } from '@/utils/role-theme';

const props = withDefaults(defineProps<{
    roles?: string[];
    src?: string;
    size?: number;
}>(), {
    roles: () => [],
    src: '',
    size: 40
});

const theme = computed(() => roleTheme(props.roles));
</script>

<template>
    <!-- 角色头像 -->
    <el-avatar :size="size" :src="src || undefined" class="role-avatar" :class="`role-avatar--${theme.tone}`">
        <el-icon><component :is="theme.icon" /></el-icon>
    </el-avatar>
</template>
