<script setup lang="ts">
import { computed } from 'vue'
import { roleTheme } from '@/utils/role-theme'

const props = withDefaults(defineProps<{
  roles?: string[]
  src?: string
  size?: number
}>(), {
  roles: () => [],
  src: '',
  size: 40
})

const theme = computed(() => roleTheme(props.roles))
</script>

<template>
  <el-avatar :size="size" :src="src || undefined" class="role-avatar" :class="`role-avatar--${theme.tone}`">
    <el-icon><component :is="theme.icon" /></el-icon>
  </el-avatar>
</template>
